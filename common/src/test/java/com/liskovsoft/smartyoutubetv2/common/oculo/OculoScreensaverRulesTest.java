package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OculoScreensaverRulesTest {
    private static OculoScreensaverRules.Art art(String id, boolean priority) {
        return new OculoScreensaverRules.Art(id, "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg", null, priority);
    }

    private static List<String> ids(List<OculoScreensaverRules.Art> list) {
        List<String> result = new ArrayList<>();
        for (OculoScreensaverRules.Art item : list) {
            result.add(item.videoId);
        }
        return result;
    }

    @Test
    public void continueWatchingFirstThenTheOthersOncePerVideo() {
        List<OculoScreensaverRules.Art> recent = Arrays.asList(
                art("a", false), art("b", true), art("c", false), art("b", false), art("d", true));
        assertEquals(Arrays.asList("b", "d", "a", "c"), ids(OculoScreensaverRules.pick(recent, 12)));
        assertEquals(Arrays.asList("b", "d"), ids(OculoScreensaverRules.pick(recent, 2)));
        assertTrue(OculoScreensaverRules.pick(null, 12).isEmpty());
    }

    @Test
    public void nextWraps() {
        assertEquals(0, OculoScreensaverRules.next(-1, 3));
        assertEquals(2, OculoScreensaverRules.next(1, 3));
        assertEquals(0, OculoScreensaverRules.next(2, 3));
        assertEquals(-1, OculoScreensaverRules.next(0, 0));
    }

    @Test
    public void driftStaysInRangeAndMoves() {
        Set<String> places = new HashSet<>();
        for (int step = 0; step < 10; step++) {
            int[] offset = OculoScreensaverRules.drift(step, 100, 60);
            assertTrue(Math.abs(offset[0]) <= 100);
            assertTrue(Math.abs(offset[1]) <= 60);
            places.add(offset[0] + "," + offset[1]);
        }
        assertEquals(10, places.size());
        assertEquals(0, OculoScreensaverRules.drift(0, 100, 60)[0]);
        // Negative steps (overflow) still work
        OculoScreensaverRules.drift(-3, 100, 60);
    }

    @Test
    public void zoomAlternatesInAndOut() {
        float[] in = OculoScreensaverRules.zoom(0);
        float[] out = OculoScreensaverRules.zoom(1);
        assertEquals(1f, in[0], 0f);
        assertEquals(OculoScreensaverRules.ZOOM_MAX, in[1], 0f);
        assertEquals(OculoScreensaverRules.ZOOM_MAX, out[0], 0f);
        assertEquals(1f, out[1], 0f);
        assertTrue(in[2] > 0 && in[2] < 1 && in[3] > 0 && in[3] < 1);
    }

    @Test
    public void clockWakesOnTheNextMinute() {
        assertEquals(60_000, OculoScreensaverRules.untilNextMinute(120_000));
        assertEquals(1_000, OculoScreensaverRules.untilNextMinute(179_000));
        assertEquals(59_999, OculoScreensaverRules.untilNextMinute(120_001));
    }

    @Test
    public void stepsInterpolate() {
        assertEquals(0f, OculoScreensaverRules.progress(-5, 1000), 0f);
        assertEquals(0.5f, OculoScreensaverRules.progress(500, 1000), 0f);
        assertEquals(1f, OculoScreensaverRules.progress(5000, 1000), 0f);
        assertEquals(1.04f, OculoScreensaverRules.lerp(1f, 1.08f, 0.5f), 1e-6f);
        assertEquals(0f, OculoScreensaverRules.ease(0f), 1e-6f);
        assertEquals(0.5f, OculoScreensaverRules.ease(0.5f), 1e-6f);
        assertEquals(1f, OculoScreensaverRules.ease(1f), 1e-6f);
        // A zoom step moves the 896 px picture by less than a pixel
        float perStep = (OculoScreensaverRules.ZOOM_MAX - 1f) * OculoScreensaverRules.TICK_MS / OculoScreensaverRules.ZOOM_MS;
        assertTrue(perStep * 896 / 2 < 1f);
    }
}
