package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OculoGlowColorTest {
    private static float hue(int rgb) {
        float[] hsv = new float[3];
        OculoGlowColor.rgbToHsv(rgb, hsv);
        return hsv[0];
    }

    private static float saturation(int rgb) {
        float[] hsv = new float[3];
        OculoGlowColor.rgbToHsv(rgb, hsv);
        return hsv[1];
    }

    private static float value(int rgb) {
        float[] hsv = new float[3];
        OculoGlowColor.rgbToHsv(rgb, hsv);
        return hsv[2];
    }

    private static float alpha(int argb) {
        return (argb >>> 24) / 255f;
    }

    @Test
    public void picksFirstColorfulCandidate() {
        int red = 0xC0392B;
        int blue = 0x2E86DE;
        int glow = OculoGlowColor.pick(new int[] {red, blue}, new int[] {100, 500}, 1000);
        assertEquals(hue(red), hue(glow & 0xFFFFFF), 3f);
    }

    @Test
    public void skipsGreyAndBlackCandidates() {
        int grey = 0x808585;
        int black = 0x0A0C10;
        int green = 0x2ECC71;
        int glow = OculoGlowColor.pick(new int[] {grey, black, green}, new int[] {400, 400, 200}, 1000);
        assertEquals(hue(green), hue(glow & 0xFFFFFF), 3f);
    }

    @Test
    public void skipsCandidatesCoveringTooLittle() {
        int orange = 0xF39C12; // a small logo
        int purple = 0x8E44AD;
        int glow = OculoGlowColor.pick(new int[] {orange, purple}, new int[] {10, 600}, 1000);
        assertEquals(hue(purple), hue(glow & 0xFFFFFF), 3f);
    }

    @Test
    public void skipsMissingSwatches() {
        int teal = 0x16A085;
        int glow = OculoGlowColor.pick(new int[] {0, teal}, new int[] {0, 300}, 1000);
        assertEquals(hue(teal), hue(glow & 0xFFFFFF), 3f);
    }

    @Test
    public void greyscalePictureKeepsDefaultGlow() {
        assertEquals(0, OculoGlowColor.pick(new int[] {0x777777, 0x222222, 0xEEEEEE},
                new int[] {500, 300, 200}, 1000));
        assertEquals(0, OculoGlowColor.pick(new int[] {0x777777}, new int[] {0}, 0));
        assertEquals(0, OculoGlowColor.pick(null, null, 100));
    }

    @Test
    public void clampsSaturationAndBrightness() {
        int pale = OculoGlowColor.toGlow(0xD8C8C0); // nearly white, slightly warm
        assertEquals(OculoGlowColor.MIN_SATURATION, saturation(pale & 0xFFFFFF), 0.02f);
        assertEquals(OculoGlowColor.VALUE, value(pale & 0xFFFFFF), 0.01f);

        int dark = OculoGlowColor.toGlow(0x400000); // deep, dark red
        assertEquals(OculoGlowColor.MAX_SATURATION, saturation(dark & 0xFFFFFF), 0.02f);
        assertEquals(OculoGlowColor.VALUE, value(dark & 0xFFFFFF), 0.01f);
        assertEquals(0f, hue(dark & 0xFFFFFF), 2f);
    }

    @Test
    public void alphaEvensOutLuminance() {
        int yellow = OculoGlowColor.toGlow(0xF1C40F);
        int blue = OculoGlowColor.toGlow(0x1E3799);
        assertTrue(alpha(yellow) < alpha(blue));
        for (int glow : new int[] {yellow, blue, OculoGlowColor.toGlow(0x27AE60), OculoGlowColor.toGlow(0xE84393)}) {
            assertTrue(alpha(glow) >= OculoGlowColor.MIN_ALPHA - 0.01f);
            assertTrue(alpha(glow) <= OculoGlowColor.MAX_ALPHA + 0.01f);
        }
    }

    @Test
    public void tealGlowMatchesDefaultStrength() {
        // The default glow's own color comes back close to the default 20 %
        int glow = OculoGlowColor.toGlow(0x32E6DC);
        assertEquals(0.20f, alpha(glow), 0.03f);
    }

    @Test
    public void hsvRoundTrip() {
        for (int rgb : new int[] {0xFF0000, 0x00FF00, 0x0000FF, 0x32E6DC, 0x8E44AD, 0xF39C12, 0x123456}) {
            float[] hsv = new float[3];
            OculoGlowColor.rgbToHsv(rgb, hsv);
            int back = OculoGlowColor.hsvToRgb(hsv);
            for (int shift = 0; shift <= 16; shift += 8) {
                assertEquals((rgb >> shift) & 0xFF, (back >> shift) & 0xFF, 1);
            }
        }
    }
}
