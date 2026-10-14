package ru.doshikologiya.nativepreview

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.compose.Banner
import com.yandex.mobile.ads.compose.BannerEvents
import com.yandex.mobile.ads.compose.BannerSize
import com.yandex.mobile.ads.compose.rememberBannerAdState

/**
 * The original cook-ad slot: directly below the instruction and above the
 * previous/next controls. It is an inline ad, never a floating overlay.
 *
 * A single instance survives step changes. No reload on Next/Back or taps.
 * A failed/empty request leaves no reserved banner height.
 */
@Composable
internal fun CookingAdBanner() {
    // Robolectric screenshot/unit tests do not have a working advertising runtime.
    if (Build.FINGERPRINT == "robolectric") return

    val adUnitId = if (BuildConfig.DEBUG) "demo-banner-yandex" else "R-M-20184115-1"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth
        var loaded by remember(adUnitId, width) { mutableStateOf(false) }
        val state = rememberBannerAdState(
            adSize = BannerSize.Inline(width = width, maxHeight = 100.dp),
            events = BannerEvents(
                onAdLoaded = { loaded = true },
                onAdFailedToLoad = { error ->
                    loaded = false
                    Log.w("DoshikAds", "Cooking banner unavailable: ${error.description}")
                },
            ),
        )
        LaunchedEffect(state, adUnitId) {
            // SDK handles any supported refresh schedule, not the cooking steps.
            state.loadAd(AdRequest.Builder(adUnitId).build())
        }
        Column(Modifier.fillMaxWidth()) {
            Banner(
                state = state,
                modifier = if (loaded) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().height(0.dp)
            )
            if (loaded) Spacer(Modifier.height(18.dp))
        }
    }
}
