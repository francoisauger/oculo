package com.liskovsoft.smartyoutubetv2.common.oculo;

import com.liskovsoft.mediaserviceinterfaces.data.ChapterItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Oculo (0.14.0): the chapter marks given to the seek bar. Upstream's {@code SuggestionsController} and
 * {@code SponsorBlockController} both call {@code PlayerUI.setSeekBarSegments()}; this list type tells the seek bar
 * which layer a call is for (OculoSeekBarLayers), so chapters and SponsorBlock segments are kept and drawn together.
 * The elements are upstream's chapter ticks (fractions of the duration, the first chapter excluded); it also carries
 * every chapter's start and title for the chapter name shown by the time (smarttubetv's OculoChapterLabel).
 */
public final class OculoSeekBarChapters extends ArrayList<SeekBarSegment> {
    private final long[] mStartsMs;
    private final String[] mTitles;

    public OculoSeekBarChapters(List<SeekBarSegment> ticks, long[] startsMs, String[] titles) {
        if (ticks != null) {
            addAll(ticks);
        }
        mStartsMs = startsMs != null ? startsMs : new long[0];
        mTitles = titles != null ? titles : new String[0];
    }

    /**
     * From SuggestionsController: upstream's ticks plus the chapters (sorted by start, as YouTube gives them).
     */
    public static OculoSeekBarChapters from(List<SeekBarSegment> ticks, List<ChapterItem> chapters) {
        int size = chapters != null ? chapters.size() : 0;
        long[] starts = new long[size];
        String[] titles = new String[size];

        for (int i = 0; i < size; i++) {
            ChapterItem chapter = chapters.get(i);
            starts[i] = chapter != null ? chapter.getStartTimeMs() : 0;
            titles[i] = chapter != null ? chapter.getTitle() : null;
        }

        return new OculoSeekBarChapters(ticks, starts, titles);
    }

    public long[] getStartsMs() {
        return mStartsMs;
    }

    public String getTitle(int index) {
        return index >= 0 && index < mTitles.length ? mTitles[index] : null;
    }

    public int getChapterCount() {
        return mStartsMs.length;
    }
}
