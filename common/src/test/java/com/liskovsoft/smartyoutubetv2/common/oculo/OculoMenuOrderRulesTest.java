package com.liskovsoft.smartyoutubetv2.common.oculo;

import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OculoMenuOrderRulesTest {
    private static final long WATCH_LATER = MainUIData.MENU_ITEM_ADD_TO_WATCH_LATER;
    private static final long PLAY = MainUIData.MENU_ITEM_PLAY_VIDEO;
    private static final long HISTORY = MainUIData.MENU_ITEM_REMOVE_FROM_HISTORY;
    private static final long PLAYLIST = MainUIData.MENU_ITEM_ADD_TO_PLAYLIST;
    private static final long CHANNEL = MainUIData.MENU_ITEM_OPEN_CHANNEL;

    @Test
    public void appliedOnceThenUserChoicesKept() {
        assertTrue(OculoMenuOrderRules.shouldApply(false, true)); // existing install, first launch of 0.14.0
        assertTrue(OculoMenuOrderRules.shouldApply(false, false)); // fresh install
        assertFalse(OculoMenuOrderRules.shouldApply(true, true)); // already applied: the user's order wins
        assertTrue(OculoMenuOrderRules.shouldApply(true, false)); // new profile, order built from upstream defaults
    }

    @Test
    public void movesWatchLaterFirstKeepingTheRest() {
        // A saved order where the user moved Open channel up and disabled Remove from history
        List<Long> order = new ArrayList<>(Arrays.asList(CHANNEL, PLAY, HISTORY, WATCH_LATER, PLAYLIST));
        long mask = CHANNEL | PLAYLIST; // Watch later off (upstream default), history off (user)

        long result = OculoMenuOrderRules.apply(order, mask, WATCH_LATER);

        assertEquals(Arrays.asList(WATCH_LATER, CHANNEL, PLAY, HISTORY, PLAYLIST), order);
        assertEquals(CHANNEL | PLAYLIST | WATCH_LATER, result);
        assertEquals(0, result & HISTORY); // still disabled
        assertEquals(0, result & PLAY);
    }

    @Test
    public void alreadyFirstOrMissing() {
        List<Long> first = new ArrayList<>(Arrays.asList(WATCH_LATER, PLAY));
        assertEquals(WATCH_LATER, OculoMenuOrderRules.apply(first, WATCH_LATER, WATCH_LATER));
        assertEquals(Arrays.asList(WATCH_LATER, PLAY), first);

        List<Long> missing = new ArrayList<>(Arrays.asList(PLAY, HISTORY));
        OculoMenuOrderRules.apply(missing, 0, WATCH_LATER);
        assertEquals(Arrays.asList(WATCH_LATER, PLAY, HISTORY), missing);
    }

    @Test
    public void watchLaterIsAHighBit() {
        // 1L << 38: the mask must stay a long (an int shift would wrap)
        assertEquals(1L << 38, WATCH_LATER);
        assertTrue((OculoMenuOrderRules.apply(new ArrayList<Long>(), 0, WATCH_LATER) & (1L << 38)) != 0);
    }
}
