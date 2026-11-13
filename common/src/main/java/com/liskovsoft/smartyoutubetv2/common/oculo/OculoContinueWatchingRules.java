package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static boolean isEligible(Candidate c, Map<String, ?> hidden, long nowMs) {
        if (c == null || c.videoId == null || c.videoId.isEmpty()) {
            return false;
        }
        if (c.isLive || c.isShorts) {
            return false;
        }
        if (hidden != null && hidden.containsKey(c.videoId)) {
            return false;
        }
        long age = nowMs - c.lastWatchedMs;
        if (c.lastWatchedMs <= 0 || age > WINDOW_MS || age < -WINDOW_MS) { // negative: clock moved back a little
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

    // ---- Account history (32.74-14) ----

    /** Account history items: started = at least 2 % in (or 30 s when the position is known). */
    public static final float MIN_HISTORY_PERCENT = 2f;
    /** A history progress change of at least this many points counts as a new watch. */
    public static final float HISTORY_PROGRESS_STEP = 1f;
    /** Seen-history entries: kept as long as the watch log. */
    public static final long SEEN_RETENTION_MS = LOG_RETENTION_MS;
    public static final int SEEN_MAX_SIZE = LOG_MAX_SIZE;

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

    /**
     * Records when Oculo saw each history item at its current progress ({@code seen}: id -> time, flags = percent).
     * The TV history has no dates, so the watch time of a video played on another device is estimated:
     * <ul>
     * <li>very first fetch (empty store): every item is a baseline of unknown age (time 0);</li>
     * <li>later fetches: an item that is new, or whose progress moved by at least {@link #HISTORY_PROGRESS_STEP},
     * gets the fetch time, but only when it is ranked above an item already in the store (an item that only
     * appears at the end of a longer page is old, not new).</li>
     * </ul>
     */
    public static void updateSeen(Map<String, Mark> seen, List<HistoryItem> history, long nowMs) {
        if (seen == null || history == null || history.isEmpty()) {
            return;
        }
        boolean baseline = seen.isEmpty();
        // Lowest-ranked index already known: anything above it that changed is a fresh watch
        int lastKnown = -1;
        for (int i = history.size() - 1; i >= 0; i--) {
            HistoryItem h = history.get(i);
            if (h != null && h.videoId != null && seen.containsKey(h.videoId)) {
                lastKnown = i;
                break;
            }
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            HistoryItem h = history.get(i);
            if (h == null || h.videoId == null || h.videoId.isEmpty()) {
                continue;
            }
            int percent = Math.round(Math.max(0, h.percent));
            Mark old = seen.get(h.videoId);
            long time;
            if (old == null) {
                time = baseline || i > lastKnown ? 0 : nowMs;
            } else if (Math.abs(old.flags - percent) >= HISTORY_PROGRESS_STEP) {
                time = nowMs;
            } else {
                time = old.timeMs;
            }
            seen.remove(h.videoId);
            seen.put(h.videoId, new Mark(time, percent));
        }
    }

    /** Like {@link #prune} but keeps the baseline entries (time 0) of the current history page. */
    public static void pruneSeen(Map<String, Mark> seen, List<HistoryItem> history, long nowMs) {
        java.util.Set<String> current = new java.util.HashSet<>();
        if (history != null) {
            for (HistoryItem h : history) {
                if (h != null && h.videoId != null) {
                    current.add(h.videoId);
                }
            }
        }
        Iterator<Map.Entry<String, Mark>> it = seen.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Mark> e = it.next();
            if (!current.contains(e.getKey()) && nowMs - e.getValue().timeMs > SEEN_RETENTION_MS) {
                it.remove();
            }
        }
        if (seen.size() > SEEN_MAX_SIZE) {
            prune(seen, nowMs, Long.MAX_VALUE, SEEN_MAX_SIZE);
        }
    }

    /**
     * Merge of the local watch log and the account's history (union by video id), most recent first, at most
     * {@link #MAX_ITEMS}. History order is the account's real order across devices, so each history item's time is
     * at least the best known time of every item ranked below it (local exact play time, or the time Oculo saw a
     * new watch, see {@link #updateSeen}); a video watched on a phone after the last one played in Oculo is
     * therefore inside the 48 h window. Progress: the local state wins when present, unless Oculo saw the account's
     * progress change after the local play (watched further on another device).
     *
     * @param local   local candidates (exact times and positions, {@link Candidate})
     * @param history the account's history, most recent first (empty: signed out, error, not loaded yet)
     */
    public static List<Pick> selectMerged(List<Candidate> local, List<HistoryItem> history, Map<String, Mark> seen,
                                          Map<String, ?> hidden, long nowMs) {
        Map<String, Candidate> localById = new LinkedHashMap<>();
        if (local != null) {
            for (Candidate c : local) {
                if (c != null && c.videoId != null) {
                    localById.put(c.videoId, c);
                }
            }
        }

        final List<Object[]> entries = new ArrayList<>(); // {id, time, fromHistory, order}
        java.util.Set<String> inHistory = new java.util.HashSet<>();
        if (history != null) {
            long suffixMax = 0;
            long[] times = new long[history.size()];
            for (int i = history.size() - 1; i >= 0; i--) {
                HistoryItem h = history.get(i);
                if (h != null && h.videoId != null) {
                    Candidate c = localById.get(h.videoId);
                    Mark m = seen != null ? seen.get(h.videoId) : null;
                    long own = Math.max(c != null ? c.lastWatchedMs : 0, m != null ? m.timeMs : 0);
                    if (own <= nowMs + WINDOW_MS) { // ignore a clock far in the future
                        suffixMax = Math.max(suffixMax, own);
                    }
                }
                times[i] = suffixMax;
            }
            for (int i = 0; i < history.size(); i++) {
                HistoryItem h = history.get(i);
                if (h == null || h.videoId == null || h.videoId.isEmpty() || !inHistory.add(h.videoId)) {
                    continue;
                }
                if (hidden != null && hidden.containsKey(h.videoId)) {
                    continue;
                }
                long time = times[i];
                long age = nowMs - time;
                if (time <= 0 || age > WINDOW_MS || age < -WINDOW_MS) {
                    continue;
                }
                Candidate c = localById.get(h.videoId);
                Mark m = seen != null ? seen.get(h.videoId) : null;
                boolean accountNewer = c == null || (m != null && m.timeMs > c.lastWatchedMs);
                boolean eligible;
                if (accountNewer) {
                    eligible = isHistoryItemUnfinished(h);
                } else {
                    // Local position, but the time from history order (already in the window)
                    eligible = isEligible(new Candidate(c.videoId, time, c.positionMs, c.durationMs,
                            c.isLive || h.isLive, c.isShorts || h.isShorts), hidden, nowMs);
                }
                if (eligible) {
                    entries.add(new Object[] {h.videoId, time, accountNewer, entries.size()});
                }
            }
        }

        for (Candidate c : localById.values()) {
            if (!inHistory.contains(c.videoId) && isEligible(c, hidden, nowMs)) {
                entries.add(new Object[] {c.videoId, c.lastWatchedMs, false, entries.size()});
            }
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
}
