package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Oculo: pure logic of the new-content dot on the rail's Subscriptions icon ({@link OculoNewContent}). A channel is
 * "flagged" while YouTube marks it with new content; the user has "seen" it once Subscriptions or Channels was opened
 * while it was flagged. The dot shows when a flagged channel has not been seen.
 */
public final class OculoNewContentRules {
    private static final String SEPARATOR = ",";

    private OculoNewContentRules() {
    }

    public static boolean hasUnseen(Collection<String> flagged, Collection<String> seen) {
        if (flagged == null) {
            return false;
        }
        for (String id : flagged) {
            if (seen == null || !seen.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Seen ids that are still flagged: a channel that lost its flag and gets new content again shows the dot again.
     */
    public static Set<String> prune(Collection<String> seen, Collection<String> flagged) {
        Set<String> result = new LinkedHashSet<>();
        if (seen == null || flagged == null) {
            return result;
        }
        for (String id : seen) {
            if (flagged.contains(id)) {
                result.add(id);
            }
        }
        return result;
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
