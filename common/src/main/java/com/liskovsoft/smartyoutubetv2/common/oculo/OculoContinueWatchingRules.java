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
