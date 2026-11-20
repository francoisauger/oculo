package com.liskovsoft.smartyoutubetv2.common.oculo;

/**
 * Oculo (0.13.0): pure logic of the player's sleep timer ({@link OculoSleepTimer}). Times are in milliseconds of
 * one monotonic clock (SystemClock.elapsedRealtime in the app).
 */
public final class OculoSleepTimerRules {
    public static final int OFF = 0;
    public static final int END_OF_VIDEO = -1;
    /** The choices, in menu order: Off, 15, 30, 45, 60, 90 min, End of video. */
    public static final int[] CHOICES = {OFF, 15, 30, 45, 60, 90, END_OF_VIDEO};
    /** The « Still watching? » card shows this long before the pause. */
    public static final long WARNING_MS = 30_000;
    /** A card closed earlier than this before the pause was closed by the user (a key), not by its timeout. */
    public static final long DISMISS_MARGIN_MS = 1_500;

    private OculoSleepTimerRules() {
    }

    public static boolean isDuration(int choice) {
        return choice > 0;
    }

    public static long deadline(long nowMs, int minutes) {
        return minutes > 0 ? nowMs + minutes * 60_000L : 0;
    }

    /** When the warning card shows (now if less than {@link #WARNING_MS} is left). */
    public static long warningAt(long nowMs, long deadlineMs) {
        return Math.max(nowMs, deadlineMs - WARNING_MS);
    }

    /** Minutes left, rounded up, at least 1 while the timer runs. */
    public static int minutesLeft(long nowMs, long deadlineMs) {
        long left = deadlineMs - nowMs;
        if (left <= 0) {
            return 1;
        }
        return (int) Math.max(1, (left + 59_999) / 60_000);
    }

    /** Seconds left, rounded up, 0 once reached. */
    public static int secondsLeft(long nowMs, long deadlineMs) {
        long left = deadlineMs - nowMs;
        return left <= 0 ? 0 : (int) ((left + 999) / 1_000);
    }

    /** The card closed without a button press: true when it was the user (a key), not the timeout. */
    public static boolean closedByUser(long nowMs, long deadlineMs) {
        return deadlineMs - nowMs > DISMISS_MARGIN_MS;
    }
}
