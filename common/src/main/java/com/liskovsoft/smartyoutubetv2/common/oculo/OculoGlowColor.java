package com.liskovsoft.smartyoutubetv2.common.oculo;

/**
 * Oculo: picks the color of the ambient glow behind Home from a thumbnail's palette (Google TV style), then makes
 * it read as a soft glow on near-black. Pure Java (no android.graphics) so that it runs in JVM unit tests; the
 * palette itself comes from androidx.palette in {@code OculoGlow} (smarttubetv).
 *
 * <p>Candidates are given in order of preference (vibrant, dark vibrant, dominant, light vibrant, muted, dark
 * muted). The first one that covers enough of the picture and is neither near-grey nor near-black wins. Its hue is
 * kept; saturation is clamped to {@link #MIN_SATURATION}..{@link #MAX_SATURATION} and brightness set to
 * {@link #VALUE}, so that every glow has the same strength. The alpha is then chosen so that the glow's luminance
 * on black is about that of the default teal glow (turquoise #32E6DC at 20 %), within
 * {@link #MIN_ALPHA}..{@link #MAX_ALPHA}: a yellow glow is fainter than a blue one, both read alike.
 */
public final class OculoGlowColor {
    /** Below this saturation a swatch is "grey" (HSV). */
    public static final float GREY_SATURATION = 0.18f;
    /** Below this brightness a swatch is "black" (HSV value). */
    public static final float BLACK_VALUE = 0.15f;
    /** A swatch must cover at least this share of the sampled pixels (ignores small logos and text). */
    public static final float MIN_POPULATION_SHARE = 0.03f;
    public static final float MIN_SATURATION = 0.40f;
    public static final float MAX_SATURATION = 0.85f;
    public static final float VALUE = 0.92f;
    public static final float MIN_ALPHA = 0.16f;
    public static final float MAX_ALPHA = 0.30f;
    /** Default teal glow (drawable/oculo_browse_background.xml): #32E6DC at 20 %. */
    public static final int DEFAULT_GLOW = 0x3332E6DC;
    /** Luminance on black of the default glow: 20 % of #32E6DC's relative luminance. */
    private static final float TARGET_LUMINANCE = 0.20f * luminance(0x32E6DC);

    private OculoGlowColor() {
    }

    /**
     * @param rgbs candidate colors (RGB, alpha ignored) in order of preference; 0 entries with population 0 are
     *             skipped (missing swatch)
     * @param populations pixels covered by each candidate
     * @param totalPopulation pixels covered by all the palette's swatches
     * @return the glow color as ARGB (alpha = glow strength), or 0 when no candidate fits (keep the default glow)
     */
    public static int pick(int[] rgbs, int[] populations, int totalPopulation) {
        if (rgbs == null || populations == null || totalPopulation <= 0) {
            return 0;
        }

        float[] hsv = new float[3];
        for (int i = 0; i < rgbs.length && i < populations.length; i++) {
            if (populations[i] <= 0 || populations[i] < MIN_POPULATION_SHARE * totalPopulation) {
                continue;
            }

            rgbToHsv(rgbs[i], hsv);
            if (hsv[1] < GREY_SATURATION || hsv[2] < BLACK_VALUE) {
                continue;
            }

            return toGlow(rgbs[i]);
        }

        return 0;
    }

    /**
     * Clamps a color for the glow: same hue, saturation in range, fixed brightness, alpha from its luminance.
     */
    public static int toGlow(int rgb) {
        float[] hsv = new float[3];
        rgbToHsv(rgb, hsv);
        hsv[1] = clamp(hsv[1], MIN_SATURATION, MAX_SATURATION);
        hsv[2] = VALUE;
        int clamped = hsvToRgb(hsv);

        float luminance = Math.max(0.001f, luminance(clamped));
        float alpha = clamp(TARGET_LUMINANCE / luminance, MIN_ALPHA, MAX_ALPHA);
        return (Math.round(alpha * 255) << 24) | (clamped & 0xFFFFFF);
    }

    /**
     * Relative luminance (sRGB, WCAG), 0..1.
     */
    public static float luminance(int rgb) {
        return 0.2126f * linear((rgb >> 16) & 0xFF) + 0.7152f * linear((rgb >> 8) & 0xFF)
                + 0.0722f * linear(rgb & 0xFF);
    }

    private static float linear(int channel) {
        float c = channel / 255f;
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    /**
     * Same as android.graphics.Color.RGBToHSV: hue 0..360, saturation and value 0..1.
     */
    public static void rgbToHsv(int rgb, float[] hsv) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h;
        if (delta == 0) {
            h = 0;
        } else if (max == r) {
            h = 60 * (((g - b) / delta) % 6);
        } else if (max == g) {
            h = 60 * (((b - r) / delta) + 2);
        } else {
            h = 60 * (((r - g) / delta) + 4);
        }
        if (h < 0) {
            h += 360;
        }

        hsv[0] = h;
        hsv[1] = max == 0 ? 0 : delta / max;
        hsv[2] = max;
    }

    /**
     * Same as android.graphics.Color.HSVToColor, without alpha.
     */
    public static int hsvToRgb(float[] hsv) {
        float h = ((hsv[0] % 360) + 360) % 360;
        float s = hsv[1];
        float v = hsv[2];
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = v - c;
        float r;
        float g;
        float b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
