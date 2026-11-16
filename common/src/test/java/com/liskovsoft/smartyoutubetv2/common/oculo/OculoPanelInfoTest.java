package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OculoPanelInfoTest {
    private static String wrap(String item) {
        return '‪' + item + '‬';
    }

    @Before
    public void setUp() {
        OculoPanelInfo.clear();
    }

    @Test
    public void metaWithoutAuthor() {
        String meta = wrap("Nature Lab") + " • " + wrap("1.2M views") + " • " + wrap("3 weeks ago");
        assertEquals("1.2M views · 3 weeks ago", OculoPanelInfo.metaWithoutAuthor(meta, "Nature Lab"));
        assertEquals("Nature Lab · 1.2M views · 3 weeks ago", OculoPanelInfo.metaWithoutAuthor(meta, null));
        assertNull(OculoPanelInfo.metaWithoutAuthor(wrap("Nature Lab"), "Nature Lab"));
        assertNull(OculoPanelInfo.metaWithoutAuthor(null, "Nature Lab"));
    }

    @Test
    public void join() {
        assertEquals("a · b", OculoPanelInfo.join("a", null, " ", "b"));
        assertNull(OculoPanelInfo.join(null, ""));
    }

    @Test
    public void putAndGet() {
        OculoPanelInfo.put("Title", "Label", null, " ");
        OculoPanelInfo.Info info = OculoPanelInfo.get("Title");
        assertNotNull(info);
        assertEquals("Label", info.label);
        assertNull(info.title);
        assertNull(info.desc); // blank is no description
        assertNull(OculoPanelInfo.get("Other"));
        assertNull(OculoPanelInfo.get(null));
    }

    @Test
    public void lruKeepsTheRecentOnes() {
        for (int i = 0; i < 40; i++) {
            OculoPanelInfo.put("t" + i, "l" + i, null, null);
        }
        assertNull(OculoPanelInfo.get("t0"));
        assertNotNull(OculoPanelInfo.get("t39"));
    }
}
