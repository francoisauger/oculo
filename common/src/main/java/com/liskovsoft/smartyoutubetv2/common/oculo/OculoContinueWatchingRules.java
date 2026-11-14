package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Oculo: selection rules of Home's « Continue watching » row, plain Java (unit-tested on the JVM,
 * see OculoContinueWatchingRulesTest). Also the compact text format of the two Oculo stores
 * (watch log and hidden videos).
 */
public final class OculoContinueWatchingRules {
    /** Only videos played in the last 48 hours. */
    public static final long WINDOW_MS = 48L * 60 * 60 * 1_000;
    /** Started: at least 30 s in. */
    public static final long MIN_POSITION_MS = 30_000;
    /** Not finished: below 90 %... */
    public static final float MAX_PERCENT = 90f;
    /** ...and more than 60 s left. */
    public static final long MIN_REMAINING_MS = 60_000;
    /** Videos of 60 s or less are never proposed (Shorts that were not flagged, clips). */
    public static final long MIN_DURATION_MS = 60_000;
    public static final int MAX_ITEMS = 20;
    /** Watch log entries older than this are dropped (only the 48 h window matters). */
    public static final long LOG_RETENTION_MS = 7L * 24 * 60 * 60 * 1_000;
    public static final int LOG_MAX_SIZE = 300;
    /** « Remove from Continue watching » entries are kept 30 days. */
    public static final long HIDDEN_RETENTION_MS = 30L * 24 * 60 * 60 * 1_000;
    public static final int HIDDEN_MAX_SIZE = 500;

    public static final int FLAG_LIVE = 1;
    public static final int FLAG_SHORTS = 2;

    private OculoContinueWatchingRules() {
    }

    /** One locally played video: when it was last played (watch log) and its saved position (VideoStateService). */
    public static final class Candidate {
        public final String videoId;
        public final long lastWatchedMs;
        public final long positionMs;
        public final long durationMs;
        public final boolean isLive;
        public final boolean isShorts;

        public Candidate(String videoId, long lastWatchedMs, long positionMs, long durationMs, boolean isLive, boolean isShorts) {
            this.videoId = videoId;
            this.lastWatchedMs = lastWatchedMs;
            this.positionMs = positionMs;
            this.durationMs = durationMs;
            this.isLive = isLive;
            this.isShorts = isShorts;
        }
    }

    /** Local log rule: played in the last 48 h, not hidden, and {@link #isLocalUnfinished}. */
    public static boolean isEligible(Candidate c, Map<String, ?> hidden, long nowMs) {
        if (c == null || c.videoId == null || c.videoId.isEmpty()) {
            return false;
        }
        if (hidden != null && hidden.containsKey(c.videoId)) {
            return false;
        }
        long age = nowMs - c.lastWatchedMs;
        if (c.lastWatchedMs <= 0 || age > WINDOW_MS || age < -WINDOW_MS) { // negative: clock moved back a little
            return false;
        }
        return isLocalUnfinished(c);
    }

    /** Started (at least 30 s in) and unfinished (below 90 % and more than 60 s left), no live, no Shorts. */
    public static boolean isLocalUnfinished(Candidate c) {
        if (c == null || c.isLive || c.isShorts) {
            return false;
        }
        if (c.durationMs <= MIN_DURATION_MS || c.positionMs < MIN_POSITION_MS) {
            return false;
        }
        float percent = c.positionMs * 100f / c.durationMs;
        long remaining = c.durationMs - c.positionMs;
        return percent < MAX_PERCENT && remaining > MIN_REMAINING_MS;
    }

    /**
     * Ids to show, most recently played first, at most {@link #MAX_ITEMS}, no duplicates.
     */
    public static List<String> select(List<Candidate> candidates, Map<String, ?> hidden, long nowMs) {
        List<Candidate> eligible = new ArrayList<>();
        if (candidates != null) {
            for (Candidate c : candidates) {
                if (isEligible(c, hidden, nowMs)) {
                    eligible.add(c);
                }
            }
        }

        Collections.sort(eligible, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                return Long.compare(b.lastWatchedMs, a.lastWatchedMs);
            }
        });

        List<String> result = new ArrayList<>();
        for (Candidate c : eligible) {
            if (!result.contains(c.videoId)) {
                result.add(c.videoId);
            }
            if (result.size() >= MAX_ITEMS) {
                break;
            }
        }
        return result;
    }

    // ---- Account history (32.74-14, simplified in 32.74-15) ----

    /** Account history items: started = at least 2 % in (or 30 s when the position is known). */
    public static final float MIN_HISTORY_PERCENT = 2f;
    /**
     * Only the account history's most recent items are considered (32.74-15). The TV history has no dates, so this
     * replaces 32.74-14's estimated 48 h window.
     */
    public static final int HISTORY_TOP = 20;

    /** One item of the signed-in account's YouTube history (TV client: a flat grid, most recent first, no dates). */
    public static final class HistoryItem {
        public final String videoId;
        /** 0..100, -1 = unknown. */
        public final float percent;
        /** Resume position given by YouTube (watch endpoint), -1 = unknown. */
        public final long positionMs;
        public final long durationMs;
        public final boolean isLive;
        public final boolean isShorts;

        public HistoryItem(String videoId, float percent, long positionMs, long durationMs, boolean isLive, boolean isShorts) {
            this.videoId = videoId;
            this.percent = percent;
            this.positionMs = positionMs;
            this.durationMs = durationMs;
            this.isLive = isLive;
            this.isShorts = isShorts;
        }

        long resolvedPositionMs() {
            if (positionMs > 0) {
                return positionMs;
            }
            return percent > 0 && durationMs > 0 ? (long) (durationMs * percent / 100f) : 0;
        }

        float resolvedPercent() {
            if (percent >= 0) {
                return percent;
            }
            return durationMs > 0 ? resolvedPositionMs() * 100f / durationMs : 0;
        }
    }

    /** A video picked for the row and where its progress comes from. */
    public static final class Pick {
        public final String videoId;
        /** True: card from the account's history (position from YouTube); false: local state. */
        public final boolean fromHistory;

        Pick(String videoId, boolean fromHistory) {
            this.videoId = videoId;
            this.fromHistory = fromHistory;
        }

        @Override
        public String toString() {
            return videoId + (fromHistory ? "@h" : "@l");
        }
    }

    public static boolean isHistoryItemUnfinished(HistoryItem h) {
        if (h == null || h.videoId == null || h.videoId.isEmpty() || h.isLive || h.isShorts
                || h.durationMs <= MIN_DURATION_MS) {
            return false;
        }
        long position = h.resolvedPositionMs();
        float percent = h.percent >= 0 ? h.percent : position * 100f / h.durationMs;
        boolean started = percent >= MIN_HISTORY_PERCENT || (h.positionMs > 0 && h.positionMs >= MIN_POSITION_MS);
        if (!started) {
            return false;
        }
        if (position <= 0) {
            position = (long) (h.durationMs * percent / 100f);
        }
        return percent < MAX_PERCENT && h.durationMs - position > MIN_REMAINING_MS;
    }

    private static float localPercent(Candidate c) {
        return c.durationMs > 0 ? c.positionMs * 100f / c.durationMs : 0;
    }

    /**
     * The row (32.74-15), predictable and without any first-launch step:
     * <ul>
     * <li>unfinished videos among the {@link #HISTORY_TOP} most recent items of the account's history (all devices),
     * whatever their date (the TV history has none);</li>
     * <li>plus the unfinished videos played in Oculo in the last 48 h (local log, exact times);</li>
     * <li>de-duplicated by video id, newest first, at most {@link #MAX_ITEMS}, hidden videos left out.</li>
     * </ul>
     * Order: the history's own order (the account's real order); a history item is at least as recent as any video
     * played in Oculo that is ranked below it, which places the local-only videos (just watched in Oculo, history
     * not updated yet) among them by their play time; history items with no Oculo play below them come last, still
     * in history order. Progress of a video in both: the further one (local state, or YouTube's when the video was
     * watched further on another device).
     *
     * @param local   local candidates (exact times and positions, {@link Candidate})
     * @param history the account's history, most recent first (empty: signed out, error, not loaded yet)
     */
    public static List<Pick> selectMerged(List<Candidate> local, List<HistoryItem> history, Map<String, ?> hidden,
                                          long nowMs) {
        Map<String, Candidate> localById = new LinkedHashMap<>();
        if (local != null) {
            for (Candidate c : local) {
                if (c != null && c.videoId != null) {
                    localById.put(c.videoId, c);
                }
            }
        }

        // Distinct history items, most recent first
        List<HistoryItem> page = new ArrayList<>();
        Map<String, Integer> rank = new HashMap<>();
        if (history != null) {
            for (HistoryItem h : history) {
                if (h != null && h.videoId != null && !h.videoId.isEmpty() && !rank.containsKey(h.videoId)) {
                    rank.put(h.videoId, page.size());
                    page.add(h);
                }
            }
        }

        // Each history item is at least as recent as the latest Oculo play ranked at or below it
        long[] times = new long[page.size()];
        long suffixMax = 0;
        for (int i = page.size() - 1; i >= 0; i--) {
            Candidate c = localById.get(page.get(i).videoId);
            if (c != null && c.lastWatchedMs > 0 && c.lastWatchedMs <= nowMs + WINDOW_MS) { // ignore a clock far ahead
                suffixMax = Math.max(suffixMax, c.lastWatchedMs);
            }
            times[i] = suffixMax;
        }

        final List<Object[]> entries = new ArrayList<>(); // {id, time, fromHistory, order}
        Set<String> top = new HashSet<>();
        for (int i = 0; i < page.size() && i < HISTORY_TOP; i++) {
            HistoryItem h = page.get(i);
            top.add(h.videoId);
            if (hidden != null && hidden.containsKey(h.videoId)) {
                continue;
            }
            Candidate c = localById.get(h.videoId);
            boolean fromHistory = c == null || h.resolvedPercent() > localPercent(c);
            boolean eligible = fromHistory ? isHistoryItemUnfinished(h)
                    : isLocalUnfinished(new Candidate(c.videoId, c.lastWatchedMs, c.positionMs, c.durationMs,
                            c.isLive || h.isLive, c.isShorts || h.isShorts));
            if (eligible) {
                entries.add(new Object[] {h.videoId, times[i], fromHistory, i});
            }
        }

        int extra = page.size();
        for (Candidate c : localById.values()) {
            if (top.contains(c.videoId) || !isEligible(c, hidden, nowMs)) {
                continue;
            }
            HistoryItem h = rank.containsKey(c.videoId) ? page.get(rank.get(c.videoId)) : null;
            if (h != null && (h.isLive || h.isShorts)) {
                continue;
            }
            entries.add(new Object[] {c.videoId, c.lastWatchedMs, false, h != null ? rank.get(c.videoId) : extra++});
        }

        Collections.sort(entries, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] a, Object[] b) {
                int byTime = Long.compare((Long) b[1], (Long) a[1]);
                return byTime != 0 ? byTime : Integer.compare((Integer) a[3], (Integer) b[3]);
            }
        });

        List<Pick> result = new ArrayList<>();
        for (Object[] e : entries) {
            result.add(new Pick((String) e[0], (Boolean) e[2]));
            if (result.size() >= MAX_ITEMS) {
                break;
            }
        }
        return result;
    }

    /** Counts for the diagnostic log line: unfinished items among the history's top items. */
    public static int countHistoryUnfinished(List<HistoryItem> history) {
        int count = 0;
        if (history != null) {
            for (int i = 0; i < history.size() && i < HISTORY_TOP; i++) {
                if (isHistoryItemUnfinished(history.get(i))) {
                    count++;
                }
            }
        }
        return count;
    }

    // ---- Stores: "id:timeMs:flags,id:timeMs:flags" (video ids are [A-Za-z0-9_-]) ----

    /** Entry of a store: a time (last played, or hidden at) and flags. */
    public static final class Mark {
        public final long timeMs;
        public final int flags;

        public Mark(long timeMs, int flags) {
            this.timeMs = timeMs;
            this.flags = flags;
        }
    }

    public static Map<String, Mark> parse(String data) {
        Map<String, Mark> result = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (String item : data.split(",")) {
            String[] parts = item.split(":");
            if (parts.length < 2 || parts[0].isEmpty()) {
                continue;
            }
            try {
                long time = Long.parseLong(parts[1]);
                int flags = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                result.remove(parts[0]); // keep insertion order = recency
                result.put(parts[0], new Mark(time, flags));
            } catch (NumberFormatException e) {
                // skip a damaged entry
            }
        }
        return result;
    }

    public static String format(Map<String, Mark> marks) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Mark> entry : marks.entrySet()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(entry.getKey()).append(':').append(entry.getValue().timeMs).append(':').append(entry.getValue().flags);
        }
        return sb.toString();
    }

    /** Drops entries older than {@code retentionMs}, then the oldest ones above {@code maxSize}. */
    public static void prune(Map<String, Mark> marks, long nowMs, long retentionMs, int maxSize) {
        Iterator<Map.Entry<String, Mark>> it = marks.entrySet().iterator();
        while (it.hasNext()) {
            if (nowMs - it.next().getValue().timeMs > retentionMs) {
                it.remove();
            }
        }
        if (marks.size() > maxSize) {
            List<Map.Entry<String, Mark>> entries = new ArrayList<>(marks.entrySet());
            Collections.sort(entries, new Comparator<Map.Entry<String, Mark>>() {
                @Override
                public int compare(Map.Entry<String, Mark> a, Map.Entry<String, Mark> b) {
                    return Long.compare(a.getValue().timeMs, b.getValue().timeMs);
                }
            });
            for (int i = 0; i < entries.size() - maxSize; i++) {
                marks.remove(entries.get(i).getKey());
            }
        }
    }

    // ---- In-place update of the shown row (32.74-16) ----

    /**
     * How to turn the row on screen ({@code shown}) into the new selection ({@code fresh}) with item changes on
     * that row only: remove {@link #remove}, then add {@link #prepend} before and {@link #append} after the kept
     * block {@link #kept} (the longest run of {@code fresh} already on screen in the same order). The row itself is
     * never removed and re-added, so the other rows do not move.
     */
    public static final class RowPlan {
        public final List<String> remove;
        public final List<String> kept;
        public final List<String> prepend;
        public final List<String> append;

        RowPlan(List<String> remove, List<String> kept, List<String> prepend, List<String> append) {
            this.remove = remove;
            this.kept = kept;
            this.prepend = prepend;
            this.append = append;
        }

        public boolean isStructural() {
            return !remove.isEmpty() || !prepend.isEmpty() || !append.isEmpty();
        }
    }

    public static RowPlan planInPlace(List<String> shown, List<String> fresh) {
        Map<String, Integer> shownPos = new HashMap<>();
        for (int i = 0; i < shown.size(); i++) {
            if (!shownPos.containsKey(shown.get(i))) {
                shownPos.put(shown.get(i), i);
            }
        }

        // Longest run fresh[start, end) whose items are on screen in increasing order
        int bestStart = 0;
        int bestEnd = 0;
        for (int start = 0; start < fresh.size(); start++) {
            int end = start;
            int last = -1;
            while (end < fresh.size()) {
                Integer pos = shownPos.get(fresh.get(end));
                if (pos == null || pos <= last) {
                    break;
                }
                last = pos;
                end++;
            }
            if (end - start > bestEnd - bestStart) {
                bestStart = start;
                bestEnd = end;
            }
        }

        List<String> kept = new ArrayList<>(fresh.subList(bestStart, bestEnd));
        Set<String> keptSet = new HashSet<>(kept);
        List<String> remove = new ArrayList<>();
        for (String id : shown) {
            if (!keptSet.contains(id)) {
                remove.add(id);
            }
        }
        return new RowPlan(remove, kept, new ArrayList<>(fresh.subList(0, bestStart)),
                new ArrayList<>(fresh.subList(bestEnd, fresh.size())));
    }

    // ---- Last account history, kept between launches (32.74-16) ----

    /** A cached history older than this is not shown at launch. */
    public static final long HISTORY_CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1_000;
    private static final String HISTORY_CACHE_VERSION = "cw1";

    /** The account history as last fetched: its items (most recent first) and each item's card ({@code Video} spec). */
    public static final class HistoryCache {
        public final String accountKey;
        public final long savedMs;
        public final List<HistoryItem> items;
        /** video id → card spec (SmartTube's {@code Video.toString()}). */
        public final Map<String, String> specs;

        public HistoryCache(String accountKey, long savedMs, List<HistoryItem> items, Map<String, String> specs) {
            this.accountKey = accountKey;
            this.savedMs = savedMs;
            this.items = items;
            this.specs = specs;
        }

        /** Usable at launch: same account, not too old, not from the future. */
        public boolean isUsable(String currentAccountKey, long nowMs) {
            return accountKey != null && accountKey.equals(currentAccountKey) && savedMs <= nowMs + WINDOW_MS
                    && nowMs - savedMs <= HISTORY_CACHE_MAX_AGE_MS;
        }
    }

    /**
     * One line per item: {@code id TAB percent TAB positionMs TAB durationMs TAB flags TAB spec}, after a header line
     * {@code cw1 TAB accountKey TAB savedMs}. Only the {@link #HISTORY_TOP} first items (the only ones the row uses),
     * items without a card spec are skipped. Tabs and line breaks inside a spec (titles) become spaces.
     */
    public static String formatHistoryCache(HistoryCache cache) {
        StringBuilder sb = new StringBuilder();
        sb.append(HISTORY_CACHE_VERSION).append('\t').append(clean(cache.accountKey)).append('\t').append(cache.savedMs);
        int count = 0;
        for (HistoryItem h : cache.items) {
            if (count >= HISTORY_TOP) {
                break;
            }
            String spec = h != null && h.videoId != null ? cache.specs.get(h.videoId) : null;
            if (spec == null || spec.isEmpty() || h.videoId.isEmpty()) {
                continue;
            }
            int flags = (h.isLive ? FLAG_LIVE : 0) | (h.isShorts ? FLAG_SHORTS : 0);
            sb.append('\n').append(clean(h.videoId)).append('\t').append(h.percent).append('\t').append(h.positionMs)
                    .append('\t').append(h.durationMs).append('\t').append(flags).append('\t').append(clean(spec));
            count++;
        }
        return sb.toString();
    }

    /** @return null when empty or damaged */
    public static HistoryCache parseHistoryCache(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        String[] lines = data.split("\n");
        String[] header = lines[0].split("\t", -1);
        if (header.length != 3 || !HISTORY_CACHE_VERSION.equals(header[0])) {
            return null;
        }
        long savedMs;
        try {
            savedMs = Long.parseLong(header[2]);
        } catch (NumberFormatException e) {
            return null;
        }
        List<HistoryItem> items = new ArrayList<>();
        Map<String, String> specs = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split("\t", 6);
            if (parts.length != 6 || parts[0].isEmpty() || parts[5].isEmpty() || specs.containsKey(parts[0])) {
                continue;
            }
            try {
                int flags = Integer.parseInt(parts[4]);
                items.add(new HistoryItem(parts[0], Float.parseFloat(parts[1]), Long.parseLong(parts[2]),
                        Long.parseLong(parts[3]), (flags & FLAG_LIVE) != 0, (flags & FLAG_SHORTS) != 0));
                specs.put(parts[0], parts[5]);
            } catch (NumberFormatException e) {
                // skip a damaged line
            }
        }
        return new HistoryCache(header[1], savedMs, items, specs);
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
