package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OculoNewContentRulesTest {
    private static Set<String> set(String... ids) {
        return new LinkedHashSet<>(Arrays.asList(ids));
    }

    @Test
    public void dotWhenAFlaggedChannelIsNotSeen() {
        assertTrue(OculoNewContentRules.hasUnseen(set("a", "b"), set("a")));
        assertFalse(OculoNewContentRules.hasUnseen(set("a", "b"), set("a", "b", "c")));
    }

    @Test
    public void noDotWithoutFlags() {
        assertFalse(OculoNewContentRules.hasUnseen(null, set("a")));
        assertFalse(OculoNewContentRules.hasUnseen(Collections.<String>emptySet(), null));
        assertTrue(OculoNewContentRules.hasUnseen(set("a"), null));
    }

    @Test
    public void pruneForgetsChannelsNoLongerFlagged() {
        Set<String> seen = OculoNewContentRules.prune(set("a", "b", "c"), set("b", "d"));
        assertEquals(set("b"), seen);
        // "a" gets new content again later: dot again
        assertTrue(OculoNewContentRules.hasUnseen(set("a", "b"), seen));
    }

    @Test
    public void formatAndParseRoundTrip() {
        Set<String> ids = set("UC123", "UCabc_-x");
        assertEquals(ids, OculoNewContentRules.parse(OculoNewContentRules.format(ids)));
        assertEquals(Collections.emptySet(), OculoNewContentRules.parse(""));
        assertEquals(Collections.emptySet(), OculoNewContentRules.parse(null));
        assertEquals("a", OculoNewContentRules.format(Arrays.asList("a", "", null, "x,y")));
    }
}
