package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Test;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OculoPauseScreenRulesTest {
    private static final OculoPauseScreenRules.Texts EN = new OculoPauseScreenRules.Texts(
            "%1$d min left", "%1$d min left", "%1$d h %2$02d min left", "Less than 1 min left", "ends at %1$s", " · ");
    private static final OculoPauseScreenRules.Texts FR = new OculoPauseScreenRules.Texts(
            "%1$d min restante", "%1$d min restantes", "%1$d h %2$02d min restantes", "Moins d'une minute restante",
            "fin à %1$s", " · ");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    /** 2026-11-26 21:24:10 UTC. */
    private static final long NOW = 1_795_728_250_000L;

    private static OculoPauseScreenRules.State paused() {
        OculoPauseScreenRules.State s = new OculoPauseScreenRules.State();
        s.enabled = true;
        s.foreground = true;
        s.hasVideo = true;
        s.paused = true;
        s.positionMs = 12 * 60_000;
        s.durationMs = 35 * 60_000;
        return s;
    }

    @Test
    public void delayCountsFromThePauseOrTheLastKey() {
        assertEquals(4_000, OculoPauseScreenRules.delayMs(10_000, 10_000, 0));
        assertEquals(1_000, OculoPauseScreenRules.delayMs(13_000, 10_000, 0));
        // A key pressed while paused starts the delay again
        assertEquals(3_000, OculoPauseScreenRules.delayMs(13_000, 10_000, 12_000));
        assertEquals(0, OculoPauseScreenRules.delayMs(20_000, 10_000, 12_000));
    }

    @Test
    public void showsOnlyWhenPausedInTheForeground() {
        assertTrue(OculoPauseScreenRules.shouldShow(paused()));

        OculoPauseScreenRules.State s = paused();
        s.enabled = false;
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        s = paused();
        s.paused = false;
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        s = paused();
        s.foreground = false; // PIP, a panel or dialog open, app in the background
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        s = paused();
        s.ended = true;
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        s = paused();
        s.suggestionsShown = true; // the user is browsing the suggestions
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        s = paused();
        s.screenOff = true;
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        s = paused();
        s.hasVideo = false;
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        assertFalse(OculoPauseScreenRules.shouldShow(null));
    }

    @Test
    public void notAtTheEndNorWithoutALength() {
        OculoPauseScreenRules.State s = paused();
        s.positionMs = s.durationMs - 500;
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        s.positionMs = s.durationMs - 5_000;
        assertTrue(OculoPauseScreenRules.shouldShow(s));
        s.durationMs = 0;
        s.positionMs = 0;
        assertFalse(OculoPauseScreenRules.shouldShow(s));
        // Live: no length needed
        s.live = true;
        assertTrue(OculoPauseScreenRules.shouldShow(s));
    }

    @Test
    public void remainingFollowsTheSpeed() {
        assertEquals(23 * 60_000, OculoPauseScreenRules.remainingMs(12 * 60_000, 35 * 60_000, 1f));
        assertEquals(23 * 30_000, OculoPauseScreenRules.remainingMs(12 * 60_000, 35 * 60_000, 2f));
        assertEquals(0, OculoPauseScreenRules.remainingMs(40 * 60_000, 35 * 60_000, 1f));
        assertEquals(23 * 60_000, OculoPauseScreenRules.remainingMs(12 * 60_000, 35 * 60_000, 0f));
    }

    @Test
    public void remainingTextEnglish() {
        assertEquals("23 min left", OculoPauseScreenRules.remainingText(23 * 60_000, EN, Locale.US));
        // Rounded up: 22 min 10 s is « 23 min »
        assertEquals("23 min left", OculoPauseScreenRules.remainingText(22 * 60_000 + 10_000, EN, Locale.US));
        assertEquals("1 min left", OculoPauseScreenRules.remainingText(60_000, EN, Locale.US));
        assertEquals("Less than 1 min left", OculoPauseScreenRules.remainingText(59_000, EN, Locale.US));
        assertEquals("1 h 00 min left", OculoPauseScreenRules.remainingText(60 * 60_000, EN, Locale.US));
        assertEquals("1 h 05 min left", OculoPauseScreenRules.remainingText(65 * 60_000, EN, Locale.US));
        assertEquals("2 h 31 min left", OculoPauseScreenRules.remainingText(150 * 60_000 + 1, EN, Locale.US));
    }

    @Test
    public void remainingTextFrench() {
        Locale fr = Locale.CANADA_FRENCH;
        assertEquals("23 min restantes", OculoPauseScreenRules.remainingText(23 * 60_000, FR, fr));
        assertEquals("1 min restante", OculoPauseScreenRules.remainingText(60_000, FR, fr));
        assertEquals("Moins d'une minute restante", OculoPauseScreenRules.remainingText(30_000, FR, fr));
        assertEquals("1 h 05 min restantes", OculoPauseScreenRules.remainingText(65 * 60_000, FR, fr));
    }

    @Test
    public void endTimeTwelveAndTwentyFourHours() {
        // 21:24:10 + 23 min = 21:47:10
        assertEquals("9:47 PM", OculoPauseScreenRules.endTime(NOW, 23 * 60_000, false, Locale.US, UTC));
        assertEquals("21:47", OculoPauseScreenRules.endTime(NOW, 23 * 60_000, true, Locale.US, UTC));
        assertEquals("21:47", OculoPauseScreenRules.endTime(NOW, 23 * 60_000, true, Locale.CANADA_FRENCH, UTC));
        // Past midnight
        assertEquals("12:04 AM", OculoPauseScreenRules.endTime(NOW, 160 * 60_000, false, Locale.US, UTC));
        assertEquals("0:04", OculoPauseScreenRules.endTime(NOW, 160 * 60_000, true, Locale.US, UTC));
    }

    @Test
    public void infoLine() {
        assertEquals("23 min left · ends at 9:47 PM",
                OculoPauseScreenRules.infoLine(NOW, 12 * 60_000, 35 * 60_000, 1f, false, EN, Locale.US, UTC));
        assertEquals("23 min restantes · fin à 21:47",
                OculoPauseScreenRules.infoLine(NOW, 12 * 60_000, 35 * 60_000, 1f, true, FR, Locale.CANADA_FRENCH, UTC));
        // At 2x, 23 min of video take 11 min 30 s: « 12 min », ends at 21:35:40
        assertEquals("12 min left · ends at 9:35 PM",
                OculoPauseScreenRules.infoLine(NOW, 12 * 60_000, 35 * 60_000, 2f, false, EN, Locale.US, UTC));
    }

    @Test
    public void clockAndProgress() {
        assertEquals("4:07", OculoPauseScreenRules.clock(247_000, false));
        assertEquals("35:10", OculoPauseScreenRules.clock(2_110_000, false));
        assertEquals("1:02:03", OculoPauseScreenRules.clock(3_723_000, false));
        // Elapsed of a long video keeps the hours so both times read alike
        assertEquals("0:04:07", OculoPauseScreenRules.clock(247_000, true));
        assertEquals("0:00", OculoPauseScreenRules.clock(-5, false));
        assertEquals(0.5f, OculoPauseScreenRules.progress(30_000, 60_000), 0.0001f);
        assertEquals(1f, OculoPauseScreenRules.progress(90_000, 60_000), 0.0001f);
        assertEquals(0f, OculoPauseScreenRules.progress(10, 0), 0.0001f);
    }

    @Test
    public void volumeKeysAreSilent() {
        assertTrue(OculoPauseScreenRules.isSilentKey(24));
        assertTrue(OculoPauseScreenRules.isSilentKey(25));
        assertTrue(OculoPauseScreenRules.isSilentKey(164));
        assertFalse(OculoPauseScreenRules.isSilentKey(23)); // DPAD_CENTER
        assertFalse(OculoPauseScreenRules.isSilentKey(4)); // BACK
    }
}
