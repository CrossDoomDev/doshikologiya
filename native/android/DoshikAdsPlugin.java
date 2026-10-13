package ru.doshikologiya.app;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.YandexAds;

// The native ad is positioned directly above the existing HTML cooking ad slot.
// The WebView remains unchanged: on no-fill/failure the slot stays hidden.
@CapacitorPlugin(name = "DoshikAds")
public class DoshikAdsPlugin extends Plugin {
    private static final String LIVE_UNIT = "R-M-20184115-1";
    private static final String DEMO_UNIT = "demo-banner-yandex";
    private BannerAdView banner;
    private boolean loaded = false;
    private boolean wantsVisible = false;
    private int lastLeft = 0, lastTop = 0;
    private int bannerWidth = 0, bannerHeight = 0;
    private boolean requestStarted = false;
    private boolean sdkReady = false;
    private String nativeState = "idle";
    private String nativeError = "";
    private String currentUnit = LIVE_UNIT;
    private int lastAdWidthDp = 0;
    private int lastAdHeightDp = 0;
    private int lastPositionSequence = 0;
    private int slotWidthPx = 0;
    private int slotHeightPx = 0;
    private boolean demoCheckRequested = false;

    private void mark(String state) {
        nativeState = state;
        android.util.Log.d("DoshikAds", "Native ad stage: " + state);
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            JSObject result = new JSObject();
            result.put("state", nativeState);
            result.put("sdkReady", sdkReady);
            result.put("requestStarted", requestStarted);
            result.put("adUnit", currentUnit);
            result.put("loaded", loaded);
            result.put("wantedVisible", wantsVisible);
            result.put("viewExists", banner != null);
            result.put("viewVisible", banner != null && banner.getVisibility() == View.VISIBLE);
            result.put("widthDp", lastAdWidthDp);
            result.put("heightDp", lastAdHeightDp);
            result.put("error", nativeError);
            result.put("lastPositionSequence", lastPositionSequence);
            result.put("slotWidthPx", slotWidthPx);
            result.put("slotHeightPx", slotHeightPx);
            call.resolve(result);
        });
    }

    // Manual SDK check against Yandex's official demo unit. The real unit remains
    // the default after an app restart; no automatic fallback to demo ads.
    @PluginMethod
    public void checkDemo(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (!sdkReady) { call.reject("Yandex SDK has not been initialized yet"); return; }
            if (demoCheckRequested) { call.reject("Demo request was already sent during this session"); return; }
            demoCheckRequested = true;
            int availableWidth = lastAdWidthDp > 0 ? lastAdWidthDp : 320;
            dispose();
            nativeError = "";
            loaded = false;
            wantsVisible = false;
            mark("demo_requested");
            createBanner(availableWidth);
            call.resolve();
        });
    }

    @PluginMethod
    public void load(PluginCall call) {
        Activity activity = getActivity();
        activity.runOnUiThread(() -> {
            if (requestStarted) {
                call.resolve();
                return;
            }
            requestStarted = true;
            mark("sdk_initializing");
            // Do not infer consent from installing the app.
            YandexAds.setUserConsent(false);
            YandexAds.setLocationTracking(false);
            YandexAds.initialize(activity, () -> activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) { mark("activity_destroyed"); return; }
                sdkReady = true;
                mark("sdk_initialized");
                createBanner(call.getInt("width", 320));
            }));
            call.resolve();
        });
    }

    private void createBanner(int cssWidth) {
        if (banner != null) return;
        Activity activity = getActivity();
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        FrameLayout root = activity.findViewById(android.R.id.content);
        if (root == null) {
            nativeError = "Android root content view not found";
            mark("failed");
            notifyListeners("failed", new JSObject());
            return;
        }
        int maxWidthDp = Math.round(getBridge().getWebView().getWidth() / metrics.density);
        int widthDp = Math.max(120, Math.min(cssWidth, maxWidthDp));
        BannerAdSize size = BannerAdSize.sticky(activity, widthDp);
        bannerWidth = Math.round(size.getWidth() * metrics.density);
        bannerHeight = Math.round(size.getHeight() * metrics.density);
        lastAdWidthDp = size.getWidth();
        lastAdHeightDp = size.getHeight();
        banner = new BannerAdView(activity);
        banner.setAdSize(size);
        banner.setVisibility(View.INVISIBLE);
        root.addView(banner, new FrameLayout.LayoutParams(bannerWidth, bannerHeight));
        banner.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                if (activity.isDestroyed()) {
                    dispose();
                    return;
                }
                loaded = true;
                mark("loaded");
                JSObject result = new JSObject();
                result.put("height", size.getHeight());
                notifyListeners("loaded", result);
                reposition();
            }
            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                loaded = false;
                nativeError = String.valueOf(error);
                mark("failed");
                if (banner != null) banner.setVisibility(View.GONE);
                JSObject result = new JSObject();
                result.put("message", error.toString());
                notifyListeners("failed", result);
                // No immediate retry: Yandex discourages repeated failed requests.
            }
            @Override public void onAdClicked() { }
            @Override public void onImpression(@Nullable ImpressionData data) { }
        });
        final boolean debug = (activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        currentUnit = debug || demoCheckRequested ? DEMO_UNIT : LIVE_UNIT;
        mark("request_sent");
        try {
            banner.loadAd(new AdRequest.Builder(currentUnit).build());
        } catch (Exception exception) {
            nativeError = exception.toString();
            mark("failed");
            JSObject result = new JSObject();
            result.put("message", nativeError);
            notifyListeners("failed", result);
        }
    }

    @PluginMethod
    public void position(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            int incomingSequence = call.getInt("sequence", 0);
            if (incomingSequence < lastPositionSequence) {
                call.resolve();
                return;
            }
            lastPositionSequence = incomingSequence;
            wantsVisible = call.getBoolean("visible", false);
            if (wantsVisible) {
                View web = getBridge().getWebView();
                float scale = (float) web.getWidth() / Math.max(1, call.getInt("viewportWidth", 1));
                int[] webPos = new int[2];
                int[] rootPos = new int[2];
                web.getLocationOnScreen(webPos);
                FrameLayout root = getActivity().findViewById(android.R.id.content);
                if (root == null) {
                    wantsVisible = false;
                } else {
                    root.getLocationOnScreen(rootPos);
                    lastLeft = webPos[0] - rootPos[0] + Math.round(call.getFloat("left", 0f) * scale);
                    lastTop = webPos[1] - rootPos[1] + Math.round(call.getFloat("top", 0f) * scale);
                    slotWidthPx = Math.round(call.getFloat("width", 0f) * scale);
                    slotHeightPx = Math.round(call.getFloat("height", 0f) * scale);
                }
            }
            reposition();
            call.resolve();
        });
    }

    private void reposition() {
        if (banner == null) return;
        FrameLayout root = getActivity().findViewById(android.R.id.content);
        // Fail closed: the advertisement must never cover text, controls,
        // the cooking header, or pixels outside its own reserved slot.
        boolean fitsSlot = slotWidthPx >= bannerWidth - 2 && slotHeightPx >= bannerHeight - 2;
        boolean fitsScreen = root != null
            && lastLeft >= 0 && lastTop >= 0
            && lastLeft + bannerWidth <= root.getWidth()
            && lastTop + bannerHeight <= root.getHeight();
        if (!loaded || !wantsVisible || !fitsSlot || !fitsScreen) {
            banner.setVisibility(View.INVISIBLE);
            return;
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) banner.getLayoutParams();
        params.width = bannerWidth;
        params.height = bannerHeight;
        params.leftMargin = lastLeft;
        params.topMargin = lastTop;
        banner.setLayoutParams(params);
        banner.setVisibility(View.VISIBLE);
    }

    private void dispose() {
        if (banner == null) return;
        banner.destroy();
        ViewGroup parent = (ViewGroup) banner.getParent();
        if (parent != null) parent.removeView(banner);
        banner = null;
        loaded = false;
    }

    @Override
    protected void handleOnDestroy() {
        dispose();
        super.handleOnDestroy();
    }
}
