package ru.doshikologiya.cookingads;

import org.junit.Test;
import static org.junit.Assert.*;

public class BannerBoundsTest {
    @Test public void convertsCssCoordinatesUsingActualWebViewWidth() {
        assertEquals(3, BannerBounds.scale(1080, 360), 0.001);
        assertEquals(2, BannerBounds.scale(720, 360), 0.001);
        assertEquals(0, BannerBounds.scale(1080, 0), 0);
        assertEquals(0, BannerBounds.scale(1080, Double.NaN), 0);
    }

    @Test public void onlyFullyVisibleSlotsCanShowAnAd() {
        assertTrue(BannerBounds.fits(36, 400, 288, 100, 360, 640));
        assertFalse(BannerBounds.fits(36, 580, 288, 100, 360, 640));
        assertFalse(BannerBounds.fits(36, -20, 288, 100, 360, 640));
        assertFalse(BannerBounds.fits(80, 400, 288, 100, 360, 640));
        assertFalse(BannerBounds.fits(Double.NaN, 400, 288, 100, 360, 640));
        assertFalse(BannerBounds.fits(0, 0, 0, 100, 360, 640));
    }
}
