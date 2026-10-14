package ru.doshikologiya.cookingads;

import android.view.Gravity;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.YandexAds;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;

@CapacitorPlugin(name = "CookingAds")
public class CookingAdsPlugin extends Plugin {
    private static final String LOG_TAG = "DoshikAds";
    private static final int MAX_HEIGHT_DP = 100;
    private String session;
    private FrameLayout host;
    private BannerAdView banner;
    private boolean loaded;
    private boolean resumed = true;

    @PluginMethod
    public void prepare(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            String requestedSession = call.getString("session", "");
            double scale = BannerBounds.scale(getBridge().getWebView().getWidth(),
                    call.getDouble("viewportWidth", 0.0));
            double width = call.getDouble("width", 0.0);
            if (requestedSession.isEmpty() || scale <= 0 || !Double.isFinite(width) || width <= 0) {
                call.reject("Invalid banner slot");
                return;
            }
            dispose();
            session = requestedSession;
            float density = getContext().getResources().getDisplayMetrics().density;
            int widthDp = (int) Math.floor(width * scale / density);
            if (widthDp < 1) { call.reject("Banner slot is too narrow"); return; }

            host = new FrameLayout(getActivity());
            host.setVisibility(View.INVISIBLE);
            FrameLayout root = getActivity().findViewById(android.R.id.content);
            root.addView(host, new FrameLayout.LayoutParams(
                    Math.round(widthDp * density), Math.round(MAX_HEIGHT_DP * density)));

            JSObject result = new JSObject();
            result.put("height", MAX_HEIGHT_DP * density / scale);
            call.resolve(result);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (requestedSession.equals(session) && !loaded) {
                    Log.w(LOG_TAG, "Banner load timed out before an SDK result");
                    emit(requestedSession, "failed");
                }
            }, 14_000);

            try {
              YandexAds.initialize(getContext(), () -> getActivity().runOnUiThread(() -> {
                if (!requestedSession.equals(session) || host == null || getActivity().isDestroyed()) return;
                try {
                    BannerAdView current = new BannerAdView(getActivity());
                    banner = current;
                    current.setAdSize(BannerAdSize.inline(getContext(), widthDp, MAX_HEIGHT_DP));
                    host.addView(current, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
                    current.setBannerAdEventListener(new BannerAdEventListener() {
                        @Override public void onAdLoaded() {
                            if (banner != current) return;
                            loaded = true;
                            emit(requestedSession, "loaded");
                        }
                        @Override public void onAdFailedToLoad(@NonNull AdRequestError error) {
                            if (banner != current) return;
                            loaded = false;
                            host.setVisibility(View.INVISIBLE);
                            Log.w(LOG_TAG, "Banner load failed: code=" + error.getCode()
                                    + ", description=" + error.getDescription());
                            emit(requestedSession, "failed");
                            // No retry loop: the next cooking session can make another request.
                        }
                        @Override public void onAdClicked() { }
                        @Override public void onImpression(@Nullable ImpressionData data) { }
                    });
                    current.loadAd(new AdRequest.Builder(BuildConfig.BANNER_AD_UNIT_ID).build());
                } catch (RuntimeException error) {
                    Log.e(LOG_TAG, "Banner setup failed", error);
                    emit(requestedSession, "failed");
                    dispose();
                }
              }));
            } catch (RuntimeException error) {
                Log.e(LOG_TAG, "Ads SDK initialization failed", error);
                emit(requestedSession, "failed");
                dispose();
            }
        });
    }

    @PluginMethod
    public void position(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (host != null && call.getString("session", "").equals(session)) {
                WebView web = getBridge().getWebView();
                double scale = BannerBounds.scale(web.getWidth(), call.getDouble("viewportWidth", 0.0));
                double x = call.getDouble("x", 0.0) * scale;
                double y = call.getDouble("y", 0.0) * scale;
                double width = call.getDouble("width", 0.0) * scale;
                double height = call.getDouble("height", 0.0) * scale;
                boolean visible = scale > 0 && loaded && resumed && call.getBoolean("visible", false)
                        && BannerBounds.fits(x, y, width, height, web.getWidth(), web.getHeight());
                host.setVisibility(View.INVISIBLE);
                if (visible) {
                    int[] webOrigin = new int[2];
                    int[] rootOrigin = new int[2];
                    web.getLocationInWindow(webOrigin);
                    ((View) host.getParent()).getLocationInWindow(rootOrigin);
                    host.setX((float) (webOrigin[0] - rootOrigin[0] + x));
                    host.setY((float) (webOrigin[1] - rootOrigin[1] + y));
                    host.setVisibility(View.VISIBLE);
                }
            }
            call.resolve();
        });
    }

    @PluginMethod
    public void release(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (call.getString("session", "").equals(session)) dispose();
            call.resolve();
        });
    }

    private void emit(String id, String state) {
        JSObject event = new JSObject();
        event.put("session", id);
        event.put("state", state);
        notifyListeners("state", event);
    }

    private void dispose() {
        session = null;
        loaded = false;
        if (banner != null) {
            banner.setBannerAdEventListener(null);
            banner.destroy();
            banner = null;
        }
        if (host != null) {
            ((ViewGroup) host.getParent()).removeView(host);
            host = null;
        }
    }

    @Override protected void handleOnPause() {
        resumed = false;
        if (host != null) host.setVisibility(View.INVISIBLE);
    }
    @Override protected void handleOnResume() {
        resumed = true;
        if (session != null) emit(session, "resume");
    }
    @Override protected void handleOnDestroy() { dispose(); }
}
