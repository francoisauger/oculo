package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oculo: finds the segments of a card's meta line (the second title, "Channel • 1.2M views • 3 hours ago",
 * items joined by " • " and wrapped in bidi marks by MediaServiceCore) that get the Abricot accent:
 * <ul>
 *     <li>the live viewer count ("1.4K watching", "1,4 k spectateurs");</li>
 *     <li>the relative publish time when it is less than 24 h ("3 hours ago", "Streamed 25 minutes ago",
 *     "il y a 3 h", "Diffusé il y a 3 heures"). "1 day ago" / "il y a 1 jour" and older stay as they are.</li>
 * </ul>
 * Pure Java (no Android), see OculoCardMetaTest.
 */
public final class OculoCardMeta {
    public static final int KIND_LIVE_VIEWERS = 1;
    public static final int KIND_RECENT = 2;

    private static final char DELIM = '•';

    // "1.4K watching", "12 watching", "watching now", "1,4 k spectateurs", "123 personnes regardent", "1.2K viewers"
    private static final Pattern LIVE_VIEWERS = Pattern.compile(
            "(?iu)(\\d.*\\b(watching|viewers?|spectateurs?|spectatrices?|regardent|en train de regarder)\\b)"
                    + "|(^watching now$)|(^en cours de visionnage)");

    // English: "3 hours ago", "1 hour ago", "an hour ago", "25 minutes ago", "5 min ago", "3h ago", "Streamed 2 hours ago"
    private static final Pattern EN_AGO = Pattern.compile(
            "(?iu)\\b(\\d+|an?|one)\\s*(seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|hr|h)\\.?\\s+ago\\b");
    // French: "il y a 3 heures", "il y a 3 h", "il y a 1 heure", "il y a une heure", "il y a 25 minutes", "il y a 5 min"
    private static final Pattern FR_AGO = Pattern.compile(
            "(?iu)\\bil\\s+y\\s+a\\s+(\\d+|une?)\\s*(secondes?|sec|s|minutes?|min|m|heures?|h)\\b");
    private static final Pattern JUST_NOW = Pattern.compile("(?iu)^(just now|moments ago|à l'instant|à l’instant)$");

    private OculoCardMeta() {
    }

    /**
     * One accented range of the meta line: [start, end) in the original text, and its kind.
     */
    public static final class Range {
        public final int start;
        public final int end;
        public final int kind;

        Range(int start, int end, int kind) {
            this.start = start;
            this.end = end;
            this.kind = kind;
        }
    }

    /**
     * The ranges to accent in {@code meta} (empty when none). Each range covers one whole " • " segment, without
     * the surrounding spaces and bidi marks.
     */
    public static List<Range> findAccents(CharSequence meta) {
        List<Range> result = new ArrayList<>();
        if (meta == null || meta.length() == 0) {
            return result;
        }

        String text = meta.toString();
        int segmentStart = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == DELIM) {
                addIfAccented(text, segmentStart, i, result);
                segmentStart = i + 1;
            }
        }
        return result;
    }

    private static void addIfAccented(String text, int start, int end, List<Range> result) {
        while (start < end && isPadding(text.charAt(start))) {
            start++;
        }
        while (end > start && isPadding(text.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return;
        }

        String segment = text.substring(start, end);
        if (isLiveViewers(segment)) {
            result.add(new Range(start, end, KIND_LIVE_VIEWERS));
        } else if (isLessThanADayAgo(segment)) {
            result.add(new Range(start, end, KIND_RECENT));
        }
    }

    public static boolean isLiveViewers(String segment) {
        return segment != null && LIVE_VIEWERS.matcher(clean(segment)).find();
    }

    /**
     * True for a relative time in EN or FR that is less than 24 h ago (seconds, minutes, up to 23 hours).
     */
    public static boolean isLessThanADayAgo(String segment) {
        if (segment == null) {
            return false;
        }
        String text = clean(segment);
        if (JUST_NOW.matcher(text).find()) {
            return true;
        }
        Matcher matcher = EN_AGO.matcher(text);
        if (!matcher.find()) {
            matcher = FR_AGO.matcher(text);
            if (!matcher.find()) {
                return false;
            }
        }

        String amount = matcher.group(1).toLowerCase();
        String unit = matcher.group(2).toLowerCase();
        int value;
        if (Character.isDigit(amount.charAt(0))) {
            try {
                value = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            value = 1; // a, an, one, un, une
        }

        if (unit.startsWith("h")) {
            return value < 24;
        }
        if (unit.startsWith("m")) {
            // minutes; "mo"/"month" never reaches here (the pattern only allows m, min, mins, minute(s))
            return value < 24 * 60;
        }
        // seconds
        return true;
    }

    private static boolean isPadding(char c) {
        return Character.isWhitespace(c) || c == ' ' || c == ' ' || isBidi(c);
    }

    private static boolean isBidi(char c) {
        return (c >= '‪' && c <= '‮') || (c >= '⁦' && c <= '⁩') || c == '‎' || c == '‏';
    }

    /**
     * Bidi marks removed, non-breaking spaces turned into spaces, trimmed.
     */
    private static String clean(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isBidi(c)) {
                continue;
            }
            builder.append(c == ' ' || c == ' ' ? ' ' : c);
        }
        return builder.toString().trim();
    }
}
