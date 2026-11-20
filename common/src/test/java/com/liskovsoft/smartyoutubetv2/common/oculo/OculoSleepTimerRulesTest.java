package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OculoSleepTimerRulesTest {
    private static final long NOW = 1_000_000L;

    @Test
    public void choicesInMenuOrder() {
        assertArrayEquals(new int[]{0, 15, 30, 45, 60, 90, -1}, OculoSleepTimerRules.CHOICES);
        assertTrue(OculoSleepTimerRules.isDuration(15));
        assertFalse(OculoSleepTimerRules.isDuration(OculoSleepTimerRules.OFF));
        assertFalse(OculoSleepTimerRules.isDuration(OculoSleepTimerRules.END_OF_VIDEO));
    }

    @Test
    public void deadlineAndWarning() {
        long deadline = OculoSleepTimerRules.deadline(NOW, 30);
        assertEquals(NOW + 30 * 60_000L, deadline);
        assertEquals(deadline - 30_000, OculoSleepTimerRules.warningAt(NOW, deadline));
        // Less than 30 s left: warn now
        assertEquals(NOW, OculoSleepTimerRules.warningAt(NOW, NOW + 10_000));
        assertEquals(0, OculoSleepTimerRules.deadline(NOW, OculoSleepTimerRules.OFF));
        assertEquals(0, OculoSleepTimerRules.deadline(NOW, OculoSleepTimerRules.END_OF_VIDEO));
    }

    @Test
    public void minutesLeftRoundsUp() {
        long deadline = NOW + 23 * 60_000L;
        assertEquals(23, OculoSleepTimerRules.minutesLeft(NOW, deadline));
        assertEquals(23, OculoSleepTimerRules.minutesLeft(NOW + 1, deadline));
        assertEquals(22, OculoSleepTimerRules.minutesLeft(NOW + 60_000, deadline));
        assertEquals(1, OculoSleepTimerRules.minutesLeft(deadline - 5_000, deadline));
        assertEquals(1, OculoSleepTimerRules.minutesLeft(deadline + 5_000, deadline));
    }

    @Test
    public void secondsLeftCountsDownToZero() {
        long deadline = NOW + 30_000;
        assertEquals(30, OculoSleepTimerRules.secondsLeft(NOW, deadline));
        assertEquals(30, OculoSleepTimerRules.secondsLeft(NOW + 1, deadline));
        assertEquals(1, OculoSleepTimerRules.secondsLeft(deadline - 1, deadline));
        assertEquals(0, OculoSleepTimerRules.secondsLeft(deadline, deadline));
        assertEquals(0, OculoSleepTimerRules.secondsLeft(deadline + 100, deadline));
    }

    @Test
    public void cardClosedByUserOrByTimeout() {
        long deadline = NOW + 30_000;
        assertTrue(OculoSleepTimerRules.closedByUser(NOW + 5_000, deadline));
        assertFalse(OculoSleepTimerRules.closedByUser(deadline - 200, deadline));
        assertFalse(OculoSleepTimerRules.closedByUser(deadline + 200, deadline));
    }
}
