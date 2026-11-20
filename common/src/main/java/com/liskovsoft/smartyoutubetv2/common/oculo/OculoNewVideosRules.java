package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Oculo (0.13.0): pure logic of the « new » dot on Subscriptions video cards ({@link OculoNewVideos}).
 *
 * <p>Subscriptions is a feed, newest first, without publish dates in the data (only « 3 hours ago » texts). The
 * « mark » is the list of the first {@link #MARK_SIZE} video ids shown at the previous visit. At this visit, the
 * videos listed <b>above</b> the first of them that is still in the list were published since: they are new. When
 * none of the marked ids is in the list yet (more new videos than one page), every loaded video is new. A video
 * with any progress, or an upcoming one, never gets the dot. No mark yet (first visit) = no dots.</p>
 *
 * <p>The mark is many ids, not one: with « Hide watched videos » on, the videos watched since the last visit are
 * no longer in the list, and the boundary falls on the next marked one.</p>
 */
public final class OculoNewVideosRules {
    public static final int MARK_SIZE = 60;
    private static final String SEPARATOR = ",";

    private OculoNewVideosRules() {
    }

    /**
     * Index of the first video of {@code ids} that is in {@code mark} (the boundary: videos before it are new);
     * {@code ids.size()} when none is; -1 when there is no mark (first visit: nothing is new).
     */
    public static int boundary(List<String> ids, Collection<String> mark) {
        if (mark == null || mark.isEmpty() || ids == null) {
            return -1;
        }
        for (int i = 0; i < ids.size(); i++) {
            if (mark.contains(ids.get(i))) {
                return i;
            }
        }
        return ids.size();
    }

    /**
     * @param index           position of the video in the Subscriptions list
     * @param boundary        {@link #boundary}
     * @param percentWatched  the card's progress (-1 or 0 = not watched)
     */
    public static boolean isNew(int index, int boundary, float percentWatched, boolean upcoming) {
        return index >= 0 && boundary > 0 && index < boundary && percentWatched <= 0 && !upcoming;
    }

    /** The mark to keep for the next visit: the first {@code max} distinct non-empty ids, in order. */
    public static List<String> top(List<String> ids, int max) {
        Set<String> result = new LinkedHashSet<>();
        if (ids != null) {
            for (String id : ids) {
                if (result.size() >= max) {
                    break;
                }
                if (id != null && !id.isEmpty() && !id.contains(SEPARATOR)) {
                    result.add(id);
                }
            }
        }
        return new ArrayList<>(result);
    }

    public static Set<String> parse(String data) {
        Set<String> result = new LinkedHashSet<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (String id : data.split(SEPARATOR)) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public static String format(Collection<String> ids) {
        StringBuilder result = new StringBuilder();
        if (ids == null) {
            return "";
        }
        for (String id : ids) {
            if (id == null || id.isEmpty() || id.contains(SEPARATOR)) {
                continue;
            }
            if (result.length() > 0) {
                result.append(SEPARATOR);
            }
            result.append(id);
        }
        return result.toString();
    }
}
