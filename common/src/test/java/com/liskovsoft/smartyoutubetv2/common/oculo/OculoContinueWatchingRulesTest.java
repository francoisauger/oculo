package com.liskovsoft.smartyoutubetv2.common.oculo;

import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Candidate;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Mark;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OculoContinueWatchingRulesTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long HOUR = 60L * 60 * 1_000;
    private static final long MIN = 60_000;

    private static Candidate video(String id, long hoursAgo, long positionMs, long durationMs) {
        return new Candidate(id, NOW - hoursAgo * HOUR, positionMs, durationMs, false, false);
    }

    private static List<String> select(Candidate... candidates) {
        return OculoContinueWatchingRules.select(Arrays.asList(candidates), Collections.<String, Mark>emptyMap(), NOW);
    }

    @Test
    public void keepsOnlyTheLast48Hours() {
        assertEquals(Arrays.asList("a", "b"), select(
                video("a", 1, 5 * MIN, 20 * MIN),
                video("b", 47, 5 * MIN, 20 * MIN),
                video("c", 49, 5 * MIN, 20 * MIN)));
    }

    @Test
    public void startedMeansAtLeast30Seconds() {
        assertEquals(Collections.singletonList("b"), select(
                video("a", 1, 29_000, 20 * MIN),
                video("b", 2, 30_000, 20 * MIN)));
    }

    @Test
    public void finishedAt90PercentOrLessThanAMinuteLeft() {
        assertEquals(Collections.singletonList("ok"), select(
                video("ok", 1, 17 * MIN, 20 * MIN),            // 85 %, 3 min left
                video("ninety", 2, 18 * MIN, 20 * MIN),        // 90 %
                video("lastMinute", 3, 80 * 1_000, 140 * 1_000), // 57 %, but 60 s left
                video("long", 4, 110 * MIN, 120 * MIN)));      // 91.7 %, 10 min left
    }

    @Test
    public void skipsLiveShortsShortClipsAndUnknownDuration() {
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate("live", NOW - HOUR, 5 * MIN, 20 * MIN, true, false));
        candidates.add(new Candidate("short", NOW - HOUR, 40_000, 3 * MIN, false, true));
        candidates.add(video("clip", 1, 35_000, 60_000));
        candidates.add(video("unknown", 1, 5 * MIN, -1));
        candidates.add(video("ok", 1, 5 * MIN, 20 * MIN));
        assertEquals(Collections.singletonList("ok"),
                OculoContinueWatchingRules.select(candidates, null, NOW));
    }

    @Test
    public void hiddenVideosAreLeftOut() {
        Map<String, Mark> hidden = new HashMap<>();
        hidden.put("b", new Mark(NOW - HOUR, 0));
        assertEquals(Collections.singletonList("a"), OculoContinueWatchingRules.select(Arrays.asList(
                video("a", 1, 5 * MIN, 20 * MIN),
                video("b", 2, 5 * MIN, 20 * MIN)), hidden, NOW));
    }

    @Test
    public void mostRecentFirstCappedAt20() {
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            candidates.add(new Candidate("v" + i, NOW - (30 - i) * MIN, 5 * MIN, 20 * MIN, false, false));
        }
        List<String> result = OculoContinueWatchingRules.select(candidates, null, NOW);
        assertEquals(OculoContinueWatchingRules.MAX_ITEMS, result.size());
        assertEquals("v29", result.get(0));
        assertEquals("v10", result.get(19));
    }

    @Test
    public void storeRoundTripAndPrune() {
        Map<String, Mark> marks = OculoContinueWatchingRules.parse("a:100:1,bad,b:x:0,c:300:2,a:400:0");
        assertEquals(Arrays.asList("c", "a"), new ArrayList<>(marks.keySet())); // "a" moved to the end (most recent)
        assertEquals(400, marks.get("a").timeMs);
        assertEquals(2, marks.get("c").flags);
        assertEquals("c:300:2,a:400:0", OculoContinueWatchingRules.format(marks));

        Map<String, Mark> prune = OculoContinueWatchingRules.parse("old:" + (NOW - 31 * 24 * HOUR) + ":0,x:" + (NOW - 3 * HOUR) + ":0,y:" + (NOW - 2 * HOUR) + ":0,z:" + (NOW - HOUR) + ":0");
        OculoContinueWatchingRules.prune(prune, NOW, OculoContinueWatchingRules.HIDDEN_RETENTION_MS, 2);
        assertEquals(Arrays.asList("y", "z"), new ArrayList<>(prune.keySet()));
        assertTrue(OculoContinueWatchingRules.parse(null).isEmpty());
    }

    @Test
    public void subsequence() {
        assertTrue(OculoContinueWatching.isSubsequence(Arrays.asList("a", "c"), Arrays.asList("a", "b", "c")));
        assertFalse(OculoContinueWatching.isSubsequence(Arrays.asList("c", "a"), Arrays.asList("a", "b", "c")));
        assertFalse(OculoContinueWatching.isSubsequence(Collections.singletonList("d"), Arrays.asList("a", "b")));
    }
}
