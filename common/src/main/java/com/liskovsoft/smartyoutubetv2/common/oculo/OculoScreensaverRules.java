package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Oculo (0.13.0): pure logic of the Oculo screensaver ({@link OculoScreensaverView}): which thumbnails, in which
 * order, where the content drifts and how each picture zooms. No timers here.
 */
public final class OculoScreensaverRules {
    /** At most this many pictures in the rotation. */
    public static final int MAX_PICTURES = 12;
    /** A picture stays this long (its zoom lasts {@link #PICTURE_MS} + {@link #CROSSFADE_MS}). */
    public static final long PICTURE_MS = 14_000;
    public static final long CROSSFADE_MS = 2_500;
    /** The whole content moves to its next place every {@link #DRIFT_PERIOD_MS}, in {@link #DRIFT_MS}. */
    public static final long DRIFT_PERIOD_MS = 60_000;
    public static final long DRIFT_MS = 20_000;
    /** Ken Burns: from 1.0 to this scale (or back), over the picture's whole time on screen. */
    public static final float ZOOM_MAX = 1.08f;
    public static final long ZOOM_MS = PICTURE_MS + CROSSFADE_MS * 2;
    /** Zoom and drift steps: 10 a second (each step moves less than a pixel). */
    public static final long TICK_MS = 100;

    /**
     * Drift places, as fractions of the allowed range (x, y), visited in order: spread so that no pixel stays lit
     * in the same place for long.
     */
    private static final float[][] DRIFT = {
            {0f, 0f}, {-0.9f, -0.5f}, {0.7f, 0.6f}, {-0.3f, 0.9f}, {1f, -0.8f},
            {-0.8f, 0.3f}, {0.2f, -1f}, {0.6f, 0.8f}, {-1f, -0.1f}, {0.9f, 0.1f}};

    private OculoScreensaverRules() {
    }

    public static final class Art {
        public final String videoId;
        public final String url;
        public final String fallbackUrl;
        public final boolean priority;

        public Art(String videoId, String url, String fallbackUrl, boolean priority) {
            this.videoId = videoId;
            this.url = url;
            this.fallbackUrl = fallbackUrl;
            this.priority = priority;
        }
    }

    /**
     * The rotation: Continue watching's cards first, then the others, each most recent first; one per video, at most
     * {@code max}.
     */
    public static List<Art> pick(List<Art> recent, int max) {
        List<Art> result = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        if (recent == null) {
            return result;
        }
        for (int pass = 0; pass < 2; pass++) {
            for (Art art : recent) {
                if (result.size() >= max) {
                    return result;
                }
                if (art == null || art.videoId == null || (art.priority != (pass == 0)) || ids.contains(art.videoId)) {
                    continue;
                }
                ids.add(art.videoId);
                result.add(art);
            }
        }
        return result;
    }

    public static int next(int index, int count) {
        return count <= 0 ? -1 : (index + 1) % count;
    }

    /** Drift offset in pixels for step {@code step} (0 = centered), within ±{@code rangeX} / ±{@code rangeY}. */
    public static int[] drift(int step, int rangeX, int rangeY) {
        float[] place = DRIFT[mod(step, DRIFT.length)];
        return new int[]{Math.round(place[0] * rangeX), Math.round(place[1] * rangeY)};
    }

    /**
     * Ken Burns for picture {@code step}: {start scale, end scale, pivot x, pivot y} (pivots as fractions). Even
     * steps zoom in, odd steps zoom out, the pivot walks around the picture.
     */
    public static float[] zoom(int step) {
        boolean in = mod(step, 2) == 0;
        float[][] pivots = {{0.5f, 0.5f}, {0.3f, 0.35f}, {0.7f, 0.4f}, {0.4f, 0.65f}, {0.65f, 0.6f}};
        float[] pivot = pivots[mod(step, pivots.length)];
        return new float[]{in ? 1f : ZOOM_MAX, in ? ZOOM_MAX : 1f, pivot[0], pivot[1]};
    }

    /** Milliseconds from {@code nowMs} to the next whole minute (the clock's next change), at least 1. */
    public static long untilNextMinute(long nowMs) {
        long rest = 60_000 - (((nowMs % 60_000L) + 60_000L) % 60_000L);
        return Math.max(1, rest);
    }

    /** 0 to 1 over {@code durationMs}. */
    public static float progress(long elapsedMs, long durationMs) {
        if (durationMs <= 0 || elapsedMs >= durationMs) {
            return 1f;
        }
        return elapsedMs <= 0 ? 0f : (float) elapsedMs / durationMs;
    }

    public static float lerp(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }

    /** Ease in and out (cosine), for the drift. */
    public static float ease(float fraction) {
        return (float) ((1 - Math.cos(Math.PI * Math.max(0f, Math.min(1f, fraction)))) / 2);
    }

    /** Math.floorMod is API 24+. */
    private static int mod(int value, int divisor) {
        return ((value % divisor) + divisor) % divisor;
    }
}
