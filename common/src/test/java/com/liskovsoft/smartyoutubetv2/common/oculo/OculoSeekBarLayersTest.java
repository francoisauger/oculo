package com.liskovsoft.smartyoutubetv2.common.oculo;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OculoSeekBarLayersTest {
    private static SeekBarSegment segment(float start, float end, int color) {
        SeekBarSegment s = new SeekBarSegment();
        s.startProgress = start;
        s.endProgress = end;
        s.color = color;
        return s;
    }

    /** Chapters at 0 s, 60 s, 150 s of a 300 s video: upstream ticks skip the first one. */
    private static OculoSeekBarChapters chapters() {
        List<SeekBarSegment> ticks = Arrays.asList(segment(0.2f, 0.2001f, 0), segment(0.5f, 0.5001f, 0));
        return new OculoSeekBarChapters(ticks, new long[] {0, 60_000, 150_000}, new String[] {"Intro", "Part 1", "Part 2"});
    }

    private static List<SeekBarSegment> sponsor() {
        return new ArrayList<>(Arrays.asList(segment(0.1f, 0.15f, 1), segment(0.6f, 0.7f, 2)));
    }

    @Test
    public void chaptersThenSponsorKeepsBoth() {
        OculoSeekBarLayers layers = new OculoSeekBarLayers();
        assertTrue(layers.set(chapters()));
        assertFalse(layers.set(sponsor()));

        assertEquals(2, layers.getSponsor().size());
        assertEquals(2, layers.getChapterTicks().size());
        assertArrayEquals(new float[] {0.2f, 0.5f}, layers.getGapFractions(), 1e-6f);
    }

    @Test
    public void sponsorThenChaptersKeepsBoth() {
        OculoSeekBarLayers layers = new OculoSeekBarLayers();
        layers.set(sponsor());
        layers.set(chapters());

        assertEquals(2, layers.getSponsor().size());
        assertEquals(3, layers.getChapters().getChapterCount());
    }

    @Test
    public void sameLayerIsReplacedNotAppended() {
        OculoSeekBarLayers layers = new OculoSeekBarLayers();
        layers.set(chapters());
        layers.set(sponsor());
        layers.set(sponsor()); // SponsorBlock reloaded (button toggled, source changed)
        assertEquals(2, layers.getSponsor().size());

        layers.set(new ArrayList<SeekBarSegment>()); // SponsorBlock off: its layer only
        assertTrue(layers.getSponsor().isEmpty());
        assertEquals(2, layers.getChapterTicks().size());
    }

    @Test
    public void nullResetsBothBetweenVideos() {
        OculoSeekBarLayers layers = new OculoSeekBarLayers();
        layers.set(chapters());
        layers.set(sponsor());

        assertTrue(layers.set(null));

        assertTrue(layers.getSponsor().isEmpty());
        assertTrue(layers.getChapterTicks().isEmpty());
        assertNull(layers.getChapters());
        assertEquals(0, layers.getGapFractions().length);
        assertFalse(layers.set(null)); // no chapters to drop
    }

    @Test
    public void sponsorListIsCopied() {
        OculoSeekBarLayers layers = new OculoSeekBarLayers();
        List<SeekBarSegment> list = sponsor();
        layers.set(list);
        list.clear();
        assertEquals(2, layers.getSponsor().size());
    }

    @Test
    public void gapsSortedUniqueWithoutEnds() {
        List<SeekBarSegment> ticks = Arrays.asList(segment(0.5f, 0.5f, 0), segment(0f, 0f, 0), segment(0.25f, 0.25f, 0),
                segment(0.5f, 0.5f, 0), segment(0.9999f, 1f, 0), segment(-1f, 0f, 0));
        OculoSeekBarLayers layers = new OculoSeekBarLayers();
        layers.set(new OculoSeekBarChapters(ticks, null, null));
        assertArrayEquals(new float[] {0.25f, 0.5f}, layers.getGapFractions(), 1e-6f);
    }

    @Test
    public void chapterLookup() {
        long[] starts = {0, 60_000, 150_000};
        assertEquals(0, OculoSeekBarLayers.chapterIndexAt(starts, 0));
        assertEquals(0, OculoSeekBarLayers.chapterIndexAt(starts, 59_999));
        assertEquals(1, OculoSeekBarLayers.chapterIndexAt(starts, 60_000));
        assertEquals(1, OculoSeekBarLayers.chapterIndexAt(starts, 149_999));
        assertEquals(2, OculoSeekBarLayers.chapterIndexAt(starts, 150_000));
        assertEquals(2, OculoSeekBarLayers.chapterIndexAt(starts, 10_000_000));
        assertEquals(-1, OculoSeekBarLayers.chapterIndexAt(new long[] {5_000, 9_000}, 1_000)); // before the first
        assertEquals(-1, OculoSeekBarLayers.chapterIndexAt(new long[0], 1_000));
        assertEquals(-1, OculoSeekBarLayers.chapterIndexAt(null, 1_000));

        assertEquals(60_000, OculoSeekBarLayers.chapterEndMs(starts, 0));
        assertEquals(150_000, OculoSeekBarLayers.chapterEndMs(starts, 1));
        assertEquals(Long.MAX_VALUE, OculoSeekBarLayers.chapterEndMs(starts, 2));
    }

    @Test
    public void chapterTitles() {
        OculoSeekBarChapters chapters = chapters();
        assertEquals("Part 1", chapters.getTitle(OculoSeekBarLayers.chapterIndexAt(chapters.getStartsMs(), 61_000)));
        assertNull(chapters.getTitle(-1));
        assertNull(chapters.getTitle(3));
    }
}
