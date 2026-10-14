package ru.doshikologiya.cookingads;

/** CSS viewport coordinates are scaled by WebView width, not Android density. */
final class BannerBounds {
    static double scale(double webWidth, double cssViewportWidth) {
        if (!Double.isFinite(webWidth) || !Double.isFinite(cssViewportWidth)
                || webWidth <= 0 || cssViewportWidth <= 0) return 0;
        return webWidth / cssViewportWidth;
    }

    static boolean fits(double x, double y, double width, double height,
                        double viewportWidth, double viewportHeight) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(width)
                && Double.isFinite(height) && width > 0 && height > 0
                && x >= 0 && y >= 0 && x + width <= viewportWidth + 1
                && y + height <= viewportHeight + 1;
    }
}
