package com.liskovsoft.smartyoutubetv2.common.oculo;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Oculo (0.14.0): the two layers of the player's seek bar, chapters and SponsorBlock segments (pure Java, test
 * {@code OculoSeekBarLayersTest}). Upstream's seek bar kept one list of rectangles, appended to by every
 * {@code setSegments()} call and computed with the bar's width at call time, so the two callers' marks could
 * repeat, vanish (bar not laid out yet) or be lost to a reset. Here: {@code null} clears both layers (new video,
 * player reset), an {@link OculoSeekBarChapters} replaces the chapters, any other list replaces the SponsorBlock
 * segments. The seek bar converts the fractions to pixels whenever it lays itself out.
 */
public final class OculoSeekBarLayers {
    /** Chapter starts closer than this to the ends of the bar get no gap. */
    private static final float EDGE = 0.001f;

    private List<SeekBarSegment> mSponsor = Collections.emptyList();
    private OculoSeekBarChapters mChapters;

    /**
     * @return true when the chapters layer changed
     */
    public boolean set(List<SeekBarSegment> segments) {
        if (segments == null) {
            boolean hadChapters = mChapters != null;
            mSponsor = Collections.emptyList();
            mChapters = null;
            return hadChapters;
        }

        if (segments instanceof OculoSeekBarChapters) {
            mChapters = (OculoSeekBarChapters) segments;
            return true;
        }

        mSponsor = new ArrayList<>(segments);
        return false;
    }

    /** SponsorBlock segments, drawn over the track. */
    public List<SeekBarSegment> getSponsor() {
        return mSponsor;
    }

    /** Chapter marks (upstream's ticks), empty when there are no chapters. */
    public List<SeekBarSegment> getChapterTicks() {
        return mChapters != null ? mChapters : Collections.<SeekBarSegment>emptyList();
    }

    public OculoSeekBarChapters getChapters() {
        return mChapters;
    }

    /**
     * Where the bar is split into chapters: the chapter starts as fractions of the duration, sorted, without
     * duplicates, without those at the ends of the bar.
     */
    public float[] getGapFractions() {
        if (mChapters == null || mChapters.isEmpty()) {
            return new float[0];
        }

        float[] result = new float[mChapters.size()];
        int count = 0;

        for (SeekBarSegment tick : mChapters) {
            float f = tick.startProgress;
            if (f > EDGE && f < 1 - EDGE) {
                result[count++] = f;
            }
        }

        result = Arrays.copyOf(result, count);
        Arrays.sort(result);

        int unique = 0;
        for (int i = 0; i < result.length; i++) {
            if (unique == 0 || result[i] - result[unique - 1] > EDGE) {
                result[unique++] = result[i];
            }
        }

        return Arrays.copyOf(result, unique);
    }

    /**
     * Index of the chapter playing at {@code positionMs}: the last one that started at or before it, -1 before the
     * first chapter or without chapters. {@code startsMs} sorted ascending.
     */
    public static int chapterIndexAt(long[] startsMs, long positionMs) {
        if (startsMs == null || startsMs.length == 0 || positionMs < startsMs[0]) {
            return -1;
        }

        int low = 0;
        int high = startsMs.length - 1;

        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (startsMs[mid] <= positionMs) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    /**
     * End of chapter {@code index} (the next chapter's start), {@link Long#MAX_VALUE} for the last one.
     */
    public static long chapterEndMs(long[] startsMs, int index) {
        return startsMs != null && index >= 0 && index + 1 < startsMs.length ? startsMs[index + 1] : Long.MAX_VALUE;
    }
}
