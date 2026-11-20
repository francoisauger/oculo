package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OculoNewVideosRulesTest {
    private static List<String> list(String... ids) {
        return Arrays.asList(ids);
    }

    private static Set<String> set(String... ids) {
        return new LinkedHashSet<>(Arrays.asList(ids));
    }

    @Test
    public void videosAboveThePreviousTopAreNew() {
        // Previous visit: c, d, e at the top. Now a, b were published.
        List<String> now = list("a", "b", "c", "d", "e");
        int boundary = OculoNewVideosRules.boundary(now, set("c", "d", "e"));
        assertEquals(2, boundary);
        assertTrue(OculoNewVideosRules.isNew(0, boundary, -1, false));
        assertTrue(OculoNewVideosRules.isNew(1, boundary, 0, false));
        assertFalse(OculoNewVideosRules.isNew(2, boundary, -1, false));
        assertFalse(OculoNewVideosRules.isNew(4, boundary, -1, false));
    }

    @Test
    public void firstVisitHasNoDots() {
        assertEquals(-1, OculoNewVideosRules.boundary(list("a", "b"), null));
        assertEquals(-1, OculoNewVideosRules.boundary(list("a", "b"), Collections.<String>emptySet()));
        assertFalse(OculoNewVideosRules.isNew(0, -1, -1, false));
    }

    @Test
    public void nothingNewWhenTheTopIsUnchanged() {
        int boundary = OculoNewVideosRules.boundary(list("c", "d"), set("c", "d"));
        assertEquals(0, boundary);
        assertFalse(OculoNewVideosRules.isNew(0, boundary, -1, false));
    }

    @Test
    public void watchedTopVideosHiddenTheBoundaryFallsOnTheNextMarkedOne() {
        // « Hide watched » on: c (marked, watched since) is gone from the list
        int boundary = OculoNewVideosRules.boundary(list("a", "d", "e"), set("c", "d", "e"));
        assertEquals(1, boundary);
        assertTrue(OculoNewVideosRules.isNew(0, boundary, -1, false));
        assertFalse(OculoNewVideosRules.isNew(1, boundary, -1, false));
    }

    @Test
    public void moreNewVideosThanLoadedAllAreNew() {
        List<String> now = list("x", "y", "z");
        int boundary = OculoNewVideosRules.boundary(now, set("c"));
        assertEquals(3, boundary);
        assertTrue(OculoNewVideosRules.isNew(2, boundary, -1, false));
    }

    @Test
    public void anyProgressOrUpcomingHasNoDot() {
        assertFalse(OculoNewVideosRules.isNew(0, 2, 1, false));
        assertFalse(OculoNewVideosRules.isNew(0, 2, 0.5f, false));
        assertFalse(OculoNewVideosRules.isNew(0, 2, -1, true));
        assertFalse(OculoNewVideosRules.isNew(-1, 2, -1, false));
    }

    @Test
    public void topKeepsOrderDistinctAndLimit() {
        assertEquals(list("a", "b"), OculoNewVideosRules.top(list("a", null, "", "a", "b", "c"), 2));
        assertEquals(list("a"), OculoNewVideosRules.top(list("a", "x,y"), 5));
    }

    @Test
    public void parseAndFormatRoundTrip() {
        Set<String> ids = set("a", "b", "c");
        assertEquals(ids, OculoNewVideosRules.parse(OculoNewVideosRules.format(ids)));
        assertTrue(OculoNewVideosRules.parse(null).isEmpty());
        assertTrue(OculoNewVideosRules.parse("").isEmpty());
        assertEquals("", OculoNewVideosRules.format(null));
    }
}
