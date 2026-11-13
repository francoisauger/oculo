package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OculoCardMetaTest {
    // MediaServiceCore wraps each item with BidiFormatter.unicodeWrap() (LRE ... PDF) and joins with " • "
    private static String meta(String... items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append('‪').append(item).append('‬');
        }
        return builder.toString();
    }

    @Test
    public void recentEnglish() {
        assertTrue(OculoCardMeta.isLessThanADayAgo("3 hours ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("1 hour ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("an hour ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("23 hours ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("25 minutes ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("1 minute ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("5 min ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("3h ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("30 seconds ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("Streamed 2 hours ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("Premiered 7 hours ago"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("Just now"));
    }

    @Test
    public void olderEnglish() {
        assertFalse(OculoCardMeta.isLessThanADayAgo("24 hours ago"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("1 day ago"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("2 days ago"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("3 weeks ago"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("5 months ago"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("1 year ago"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("Streamed 3 days ago"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("1.2M views"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("Mar 3, 2026"));
    }

    @Test
    public void recentFrench() {
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a 3 heures"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a 1 heure"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a une heure"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a 3 h"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a 3 h"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a 23 heures"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a 25 minutes"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a 5 min"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("il y a 40 secondes"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("Diffusé il y a 3 heures"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("Il y a 2 heures"));
        assertTrue(OculoCardMeta.isLessThanADayAgo("À l'instant"));
    }

    @Test
    public void olderFrench() {
        assertFalse(OculoCardMeta.isLessThanADayAgo("il y a 24 heures"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("il y a 1 jour"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("il y a 2 jours"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("il y a 3 semaines"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("il y a 5 mois"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("il y a 1 an"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("Diffusé il y a 2 jours"));
        assertFalse(OculoCardMeta.isLessThanADayAgo("1,2 M de vues"));
    }

    @Test
    public void liveViewers() {
        assertTrue(OculoCardMeta.isLiveViewers("1.4K watching"));
        assertTrue(OculoCardMeta.isLiveViewers("12 watching"));
        assertTrue(OculoCardMeta.isLiveViewers("1,4 k spectateurs"));
        assertTrue(OculoCardMeta.isLiveViewers("6,2 k spectateurs"));
        assertTrue(OculoCardMeta.isLiveViewers("1 spectateur"));
        assertFalse(OculoCardMeta.isLiveViewers("1.4K views"));
        assertFalse(OculoCardMeta.isLiveViewers("Café Django"));
        assertFalse(OculoCardMeta.isLiveViewers("123 waiting"));
    }

    @Test
    public void rangesCoverOnlyTheSegment() {
        String text = meta("Café Django", "1.2M views", "3 hours ago");
        List<OculoCardMeta.Range> ranges = OculoCardMeta.findAccents(text);
        assertEquals(1, ranges.size());
        assertEquals(OculoCardMeta.KIND_RECENT, ranges.get(0).kind);
        assertEquals("3 hours ago", text.substring(ranges.get(0).start, ranges.get(0).end));

        text = meta("1,4 k spectateurs", "Café Django");
        ranges = OculoCardMeta.findAccents(text);
        assertEquals(1, ranges.size());
        assertEquals(OculoCardMeta.KIND_LIVE_VIEWERS, ranges.get(0).kind);
        assertEquals("1,4 k spectateurs", text.substring(ranges.get(0).start, ranges.get(0).end));

        assertTrue(OculoCardMeta.findAccents(meta("Café Django", "1.2M views", "2 days ago")).isEmpty());
        assertTrue(OculoCardMeta.findAccents("Café Django • il y a 1 jour").isEmpty());
        assertTrue(OculoCardMeta.findAccents(null).isEmpty());
        assertTrue(OculoCardMeta.findAccents("").isEmpty());

        // Plain text, no bidi marks
        text = "Néon FM • 3,4 k vues • il y a 5 heures";
        ranges = OculoCardMeta.findAccents(text);
        assertEquals(1, ranges.size());
        assertEquals("il y a 5 heures", text.substring(ranges.get(0).start, ranges.get(0).end));
    }
}
