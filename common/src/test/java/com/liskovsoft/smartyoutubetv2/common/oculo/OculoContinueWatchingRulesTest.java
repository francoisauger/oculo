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

    // ---- Account history (32.74-14, rule simplified in 32.74-15) ----

    private static HistoryItem h(String id, float percent) {
        return new HistoryItem(id, percent, -1, 20 * MIN, false, false);
    }

    private static List<String> merged(List<Candidate> local, List<HistoryItem> history, Map<String, Mark> hidden) {
        List<String> result = new ArrayList<>();
        for (Pick p : OculoContinueWatchingRules.selectMerged(local, history, hidden, NOW)) {
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
    public void firstLaunchShowsTheUnfinishedHistoryItems() {
        // François's case: several unfinished videos in the account's history, nothing played in Oculo yet.
        // 32.74-14 treated the first fetch as a baseline of unknown age and showed none of them.
        List<HistoryItem> history = Arrays.asList(h("a", 40), h("done", 97), h("b", 30), h("notStarted", 0), h("c", 12));
        assertEquals(Arrays.asList("a@h", "b@h", "c@h"), merged(null, history, null));
    }

    @Test
    public void onlyTheTop20HistoryItems() {
        List<HistoryItem> history = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            history.add(h("v" + i, i % 2 == 0 ? 50 : 95)); // even = unfinished
        }
        List<String> result = merged(null, history, null);
        assertEquals(10, result.size());
        assertEquals("v0@h", result.get(0));
        assertEquals("v18@h", result.get(9)); // v20 and later are beyond the 20 most recent
    }

    @Test
    public void duplicatesInTheHistoryPageCountOnce() {
        List<HistoryItem> history = new ArrayList<>();
        history.add(h("a", 50));
        for (int i = 0; i < 25; i++) {
            history.add(h("a", 50)); // repeated: still one slot
        }
        history.add(h("b", 50));
        assertEquals(Arrays.asList("a@h", "b@h"), merged(null, history, null));
    }

    @Test
    public void localLogAddsTheLast48HoursOnly() {
        List<HistoryItem> history = Arrays.asList(h("a", 50), h("b", 30));
        List<Candidate> local = Arrays.asList(
                video("new", 0, 5 * MIN, 20 * MIN),    // just watched in Oculo, not in history yet
                video("old", 50, 5 * MIN, 20 * MIN),   // 50 h ago, not in the top history items: left out
                video("b", 2, 5 * MIN, 20 * MIN));     // in history too: one card
        assertEquals(Arrays.asList("new@l", "a@h", "b@h"), merged(local, history, null));
    }

    @Test
    public void localPlayBeyondTheTop20StillCountsWithin48Hours() {
        List<HistoryItem> history = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            history.add(h("v" + i, 95)); // finished
        }
        history.set(22, h("x", 40));
        List<Candidate> local = Collections.singletonList(video("x", 30, 5 * MIN, 20 * MIN));
        assertEquals(Collections.singletonList("x@l"), merged(local, history, null));
    }

    @Test
    public void newestFirstLocalPlaysPlacedByTime() {
        // History order: a, b (phone), x (played in Oculo 30 h ago), y (below x: older), z
        List<HistoryItem> history = Arrays.asList(h("a", 20), h("b", 50), h("x", 40), h("y", 40), h("z", 40));
        List<Candidate> local = Arrays.asList(
                video("x", 30, 8 * MIN, 20 * MIN),
                video("l", 10, 5 * MIN, 20 * MIN)); // played in Oculo 10 h ago, not in the history page
        assertEquals(Arrays.asList("l@l", "a@h", "b@h", "x@l", "y@h", "z@h"), merged(local, history, null));
    }

    @Test
    public void furtherProgressWins() {
        List<HistoryItem> history = Arrays.asList(h("a", 50), h("b", 30));
        // Local says finished: not proposed, even though history still says 30 %
        assertEquals(Collections.singletonList("a@h"),
                merged(Collections.singletonList(video("b", 2, 19 * MIN, 20 * MIN)), history, null));
        // Watched further on another device: YouTube's progress (60 %) instead of the local 25 %
        assertEquals(Arrays.asList("b@h"), merged(Collections.singletonList(video("b", 5, 5 * MIN, 20 * MIN)),
                Collections.singletonList(h("b", 60)), null));
        // Finished elsewhere: left out
        assertTrue(merged(Collections.singletonList(video("b", 5, 5 * MIN, 20 * MIN)),
                Collections.singletonList(h("b", 96)), null).isEmpty());
    }

    @Test
    public void historyHiddenAndCap() {
        List<HistoryItem> history = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            history.add(h("v" + i, 50));
        }
        Map<String, Mark> hidden = new HashMap<>();
        hidden.put("v0", new Mark(NOW, 0));
        List<Candidate> local = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            local.add(video("l" + i, 40 + i, 5 * MIN, 20 * MIN)); // older than nothing known: placed by time
        }
        List<String> result = merged(local, history, hidden);
        assertEquals(OculoContinueWatchingRules.MAX_ITEMS, result.size());
        assertEquals("l0@l", result.get(0));
        assertFalse(result.contains("v0@h"));
        assertTrue(result.contains("v1@h"));
    }

    @Test
    public void hiddenLocalVideoLeftOut() {
        Map<String, Mark> hidden = new HashMap<>();
        hidden.put("a", new Mark(NOW, 0));
        assertTrue(merged(Collections.singletonList(video("a", 1, 5 * MIN, 20 * MIN)),
                Collections.singletonList(h("a", 50)), hidden).isEmpty());
    }

    @Test
    public void signedOutFallsBackToTheLocalLog() {
        assertEquals(Collections.singletonList("a@l"), merged(
                Collections.singletonList(video("a", 1, 5 * MIN, 20 * MIN)), Collections.<HistoryItem>emptyList(), null));
    }
}
