package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Oculo (0.14.0): pure logic of the pause screen (smarttubetv's {@code OculoPauseScreen}): when it may show, and its
 * texts (time left, end time, elapsed / total). Times are milliseconds; no Android code, so it is unit tested
 * ({@code OculoPauseScreenRulesTest}).
 */
public final class OculoPauseScreenRules {
    /** The pause screen shows this long after the pause, or after the last key pressed while paused. */
    public static final long SHOW_DELAY_MS = 4_000;
    public static final long FADE_IN_MS = 400;
    public static final long FADE_OUT_MS = 200;
    /** Nothing to show this close to the end (the video is ending, upstream shows its end of video screens). */
    public static final long END_MARGIN_MS = 1_000;

    /** What the player looks like when the delay is over. */
    public static final class State {
        public boolean enabled;
        /** The activity is in the foreground, not in PIP, no Oculo panel / dialog over it. */
        public boolean foreground;
        public boolean hasVideo;
        /** Paused: playWhenReady false while the media is loaded (ExoPlayer STATE_READY). */
        public boolean paused;
        public boolean ended;
        public boolean live;
        public boolean suggestionsShown;
        /** The player's « screen off » mode (its black veil) is on. */
        public boolean screenOff;
        public long positionMs;
        public long durationMs;
    }

    /** Texts that depend on the language (resources in the app, literals in the tests). */
    public static final class Texts {
        /** « %1$d min left », singular and plural (FR: « 1 min restante » / « 23 min restantes »). */
        public final String minutesOne;
        public final String minutesOther;
        /** « %1$d h %2$02d min left ». */
        public final String hoursMinutes;
        /** « Less than 1 min left ». */
        public final String lessThanOneMinute;
        /** « ends at %1$s ». */
        public final String endsAt;
        public final String separator;

        public Texts(String minutesOne, String minutesOther, String hoursMinutes, String lessThanOneMinute,
                     String endsAt, String separator) {
            this.minutesOne = minutesOne;
            this.minutesOther = minutesOther;
            this.hoursMinutes = hoursMinutes;
            this.lessThanOneMinute = lessThanOneMinute;
            this.endsAt = endsAt;
            this.separator = separator;
        }
    }

    private OculoPauseScreenRules() {
    }

    /** Milliseconds before the pause screen may show: {@link #SHOW_DELAY_MS} after the pause or the last key. */
    public static long delayMs(long nowMs, long pausedAtMs, long lastKeyMs) {
        long idleSince = Math.max(pausedAtMs, lastKeyMs);
        return Math.max(0, idleSince + SHOW_DELAY_MS - nowMs);
    }

    public static boolean shouldShow(State s) {
        if (s == null || !s.enabled || !s.foreground || !s.hasVideo || !s.paused || s.ended) {
            return false;
        }

        if (s.suggestionsShown || s.screenOff) {
            return false;
        }

        if (s.live) {
            return true;
        }

        // A known length, and not at the very end (end of video screens, « pause after each video » stop)
        return s.durationMs > 0 && s.durationMs - s.positionMs > END_MARGIN_MS;
    }

    /** Time left in real time: the rest of the video divided by the playback speed. */
    public static long remainingMs(long positionMs, long durationMs, float speed) {
        long rest = Math.max(0, durationMs - Math.max(0, positionMs));
        if (speed <= 0) {
            speed = 1f;
        }
        return (long) (rest / speed);
    }

    /** « 23 min left », « 1 h 05 min left », « Less than 1 min left ». Minutes are rounded up. */
    public static String remainingText(long remainingMs, Texts texts, Locale locale) {
        if (remainingMs < 60_000) {
            return texts.lessThanOneMinute;
        }
        long minutes = (remainingMs + 59_999) / 60_000;
        if (minutes < 60) {
            return String.format(locale, minutes == 1 ? texts.minutesOne : texts.minutesOther, minutes);
        }
        return String.format(locale, texts.hoursMinutes, minutes / 60, minutes % 60);
    }

    /** « 9:47 PM » or « 21:47 », as the app's other clocks (its 12 / 24 hour time format setting). */
    public static String endTime(long nowMs, long remainingMs, boolean is24Hour, Locale locale, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat(is24Hour ? "H:mm" : "h:mm a", locale);
        format.setTimeZone(zone);
        return format.format(new Date(nowMs + remainingMs));
    }

    /** The line under the title: « 23 min left · ends at 9:47 PM ». */
    public static String infoLine(long nowMs, long positionMs, long durationMs, float speed, boolean is24Hour,
                                  Texts texts, Locale locale, TimeZone zone) {
        long remaining = remainingMs(positionMs, durationMs, speed);
        return remainingText(remaining, texts, locale) + texts.separator
                + String.format(locale, texts.endsAt, endTime(nowMs, remaining, is24Hour, locale, zone));
    }

    /** « 4:07 », « 35:10 », « 1:02:03 » (hours only when the video has some). */
    public static String clock(long ms, boolean withHours) {
        long totalSeconds = Math.max(0, ms) / 1_000;
        long hours = totalSeconds / 3_600;
        long minutes = (totalSeconds / 60) % 60;
        long seconds = totalSeconds % 60;
        if (withHours || hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    /** Played part of the bar, 0 to 1. */
    public static float progress(long positionMs, long durationMs) {
        if (durationMs <= 0) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, positionMs / (float) durationMs));
    }

    /** Volume and mute keys change nothing on screen: they do not hide the pause screen. */
    public static boolean isSilentKey(int keyCode) {
        // KeyEvent.KEYCODE_VOLUME_UP 24, VOLUME_DOWN 25, VOLUME_MUTE 164, MUTE 91
        return keyCode == 24 || keyCode == 25 || keyCode == 164 || keyCode == 91;
    }
}
