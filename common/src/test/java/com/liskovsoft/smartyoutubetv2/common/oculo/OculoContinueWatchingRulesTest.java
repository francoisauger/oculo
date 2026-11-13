package com.liskovsoft.smartyoutubetv2.common.oculo;

import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Candidate;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.HistoryItem;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Mark;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Pick;
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

    // ---- Account history (32.74-14) ----

    private static HistoryItem h(String id, float percent) {
        return new HistoryItem(id, percent, -1, 20 * MIN, false, false);
    }

    private static List<String> merged(List<Candidate> local, List<HistoryItem> history, Map<String, Mark> seen,
                                       Map<String, Mark> hidden) {
        List<String> result = new ArrayList<>();
        for (Pick p : OculoContinueWatchingRules.selectMerged(local, history, seen, hidden, NOW)) {
            result.add(p.toString());
        }
        return result;
    }

    @Test
    public void historyUnfinishedRules() {
        assertTrue(OculoContinueWatchingRules.isHistoryItemUnfinished(h("a", 2)));
        assertFalse(OculoContinueWatchingRules.isHistoryItemUnfinished(h("a", 1)));
        assertTrue(OculoContinueWatchingRules.isHistoryItemUnfinished(new HistoryItem("a", 1, 30_000, 60 * MIN, false, false))); // 30 s known
        assertFalse(OculoContinueWatchingRules.isHistoryItemUnfinished(h("a", 90)));
        assertFalse(OculoContinueWatchingRules.isHistoryItemUnfinished(new HistoryItem("a", 60, -1, 2 * MIN, false, false))); // 48 s left
        assertFalse(OculoContinueWatchingRules.isHistoryItemUnfinished(new HistoryItem("a", 50, -1, 20 * MIN, true, false)));
        assertFalse(OculoContinueWatchingRules.isHistoryItemUnfinished(new HistoryItem("a", 50, -1, 20 * MIN, false, true)));
        assertFalse(OculoContinueWatchingRules.isHistoryItemUnfinished(new HistoryItem("a", 50, -1, 50_000, false, false)));
        assertFalse(OculoContinueWatchingRules.isHistoryItemUnfinished(new HistoryItem("a", 50, -1, -1, false, false)));
    }

    @Test
    public void firstFetchIsABaselineOfUnknownAge() {
        Map<String, Mark> seen = new HashMap<>();
        List<HistoryItem> history = Arrays.asList(h("a", 40), h("b", 30));
        OculoContinueWatchingRules.updateSeen(seen, history, NOW);
        assertEquals(0, seen.get("a").timeMs);
        assertEquals(40, seen.get("a").flags);
        // No dated item: nothing is known to be within 48 h
        assertTrue(merged(null, history, seen, null).isEmpty());
    }

    @Test
    public void newOrProgressedItemsAboveKnownOnesAreFresh() {
        Map<String, Mark> seen = new HashMap<>();
        OculoContinueWatchingRules.updateSeen(seen, Arrays.asList(h("b", 30), h("c", 20)), NOW - 5 * HOUR);
        // Later: "a" new at the top, "b" progressed, "c" unchanged, "old" only at the end of a longer page
        List<HistoryItem> history = Arrays.asList(h("a", 10), h("b", 45), h("c", 20), h("old", 50));
        OculoContinueWatchingRules.updateSeen(seen, history, NOW);
        assertEquals(NOW, seen.get("a").timeMs);
        assertEquals(NOW, seen.get("b").timeMs);
        assertEquals(0, seen.get("c").timeMs);
        assertEquals(0, seen.get("old").timeMs);
        assertEquals(Arrays.asList("a@h", "b@h"), merged(null, history, seen, null));
    }

    @Test
    public void itemsAboveALocallyDatedVideoInheritItsTime() {
        // Watched on a phone (a, b) after "x", played in Oculo 30 h ago; "y" is below "x": age unknown
        List<HistoryItem> history = Arrays.asList(h("a", 20), h("b", 50), h("x", 95), h("y", 40));
        List<Candidate> local = Collections.singletonList(video("x", 30, 19 * MIN, 20 * MIN)); // finished
        Map<String, Mark> seen = new HashMap<>();
        OculoContinueWatchingRules.updateSeen(seen, history, NOW); // baseline
        assertEquals(Arrays.asList("a@h", "b@h"), merged(local, history, seen, null));

        // Same, but "x" was played 50 h ago: outside the window
        List<Candidate> old = Collections.singletonList(video("x", 50, 19 * MIN, 20 * MIN));
        assertTrue(merged(old, history, seen, null).isEmpty());
    }

    @Test
    public void unionWithTheLocalLogLocalWins() {
        List<HistoryItem> history = Arrays.asList(h("a", 50), h("b", 30));
        Map<String, Mark> seen = new HashMap<>();
        OculoContinueWatchingRules.updateSeen(seen, history, NOW - 3 * HOUR); // baseline
        List<Candidate> local = Arrays.asList(
                video("new", 0, 5 * MIN, 20 * MIN),   // just watched in Oculo, not in history yet
                video("b", 2, 5 * MIN, 20 * MIN));    // in history too: local position
        assertEquals(Arrays.asList("new@l", "a@h", "b@l"), merged(local, history, seen, null));

        // Local says finished: not proposed, even though history still says 30 %
        List<Candidate> done = Collections.singletonList(video("b", 2, 19 * MIN, 20 * MIN));
        assertEquals(Collections.singletonList("a@h"), merged(done, history, seen, null));
    }

    @Test
    public void accountProgressSeenAfterTheLocalPlayWins() {
        Map<String, Mark> seen = new HashMap<>();
        OculoContinueWatchingRules.updateSeen(seen, Arrays.asList(h("z", 10)), NOW - 10 * HOUR);
        List<HistoryItem> history = Arrays.asList(h("b", 60), h("z", 10));
        OculoContinueWatchingRules.updateSeen(seen, history, NOW - HOUR); // b moved from local 25 % to 60 %
        List<Candidate> local = Collections.singletonList(video("b", 5, 19 * MIN, 20 * MIN)); // local: finished
        assertEquals(Collections.singletonList("b@h"), merged(local, history, seen, null));
    }

    @Test
    public void historyHiddenAndCap() {
        List<HistoryItem> history = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            history.add(h("v" + i, 50));
        }
        List<Candidate> local = Collections.singletonList(video("v29", 1, 19 * MIN, 20 * MIN)); // dates all above it
        Map<String, Mark> hidden = new HashMap<>();
        hidden.put("v0", new Mark(NOW, 0));
        List<String> result = merged(local, history, new HashMap<String, Mark>(), hidden);
        assertEquals(OculoContinueWatchingRules.MAX_ITEMS, result.size());
        assertEquals("v1@h", result.get(0)); // history order, hidden left out
        assertEquals("v20@h", result.get(19));
    }

    @Test
    public void signedOutFallsBackToTheLocalLog() {
        assertEquals(Collections.singletonList("a@l"), merged(
                Collections.singletonList(video("a", 1, 5 * MIN, 20 * MIN)), Collections.<HistoryItem>emptyList(),
                new HashMap<String, Mark>(), null));
    }
}
