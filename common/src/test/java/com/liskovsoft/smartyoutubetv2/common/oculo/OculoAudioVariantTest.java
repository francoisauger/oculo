package com.liskovsoft.smartyoutubetv2.common.oculo;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OculoAudioVariantTest {
    // Seen on the TV (wAbVAAGwZFo, itag 251): {key "vb", value "1"}
    private static final String XTAGS_VOICE_BOOST = "CgcKAnZiEgEx";
    // {key "drc", value "1"}
    private static final String XTAGS_DRC = "CggKA2RyYxIBMQ==";
    // URL-safe, no padding: {acont dubbed-auto}, {lang fr}
    private static final String XTAGS_AUTO_DUBBED_FR = "ChQKBWFjb250EgtkdWJiZWQtYXV0bwoKCgRsYW5nEgJmcg";

    @Test
    public void parsesXtags() {
        Map<String, String> tags = OculoAudioVariant.parseXtags(XTAGS_VOICE_BOOST);
        assertEquals(1, tags.size());
        assertEquals("1", tags.get("vb"));

        assertEquals("1", OculoAudioVariant.parseXtags(XTAGS_DRC).get("drc"));

        Map<String, String> dubbed = OculoAudioVariant.parseXtags(XTAGS_AUTO_DUBBED_FR);
        assertEquals("dubbed-auto", dubbed.get("acont"));
        assertEquals("fr", dubbed.get("lang"));
    }

    @Test
    public void malformedXtagsAreIgnored() {
        assertTrue(OculoAudioVariant.parseXtags(null).isEmpty());
        assertTrue(OculoAudioVariant.parseXtags("").isEmpty());
        assertTrue(OculoAudioVariant.parseXtags("!!!").isEmpty());
        assertTrue(OculoAudioVariant.parseXtags("CgcKAnZi").isEmpty()); // truncated entry
        assertTrue(OculoAudioVariant.of(null, "%%%", false).isOriginal());
    }

    @Test
    public void originalVoiceBoostAndDrc() {
        OculoAudioVariant original = OculoAudioVariant.of(null, null, false);
        OculoAudioVariant voiceBoost = OculoAudioVariant.of(null, XTAGS_VOICE_BOOST, false);
        OculoAudioVariant drc = OculoAudioVariant.of(null, null, true);
        OculoAudioVariant drcByXtags = OculoAudioVariant.of(null, XTAGS_DRC, false);

        assertTrue(original.isOriginal());
        assertEquals(0, original.penalty());

        assertTrue(voiceBoost.voiceBoost);
        assertFalse(voiceBoost.isOriginal());
        assertEquals(4, voiceBoost.penalty());

        assertTrue(drc.drc);
        assertTrue(drcByXtags.drc);
        assertFalse(drc.isOriginal());
        assertEquals(2, drc.penalty());

        assertTrue(original.penalty() < drc.penalty() && drc.penalty() < voiceBoost.penalty());
        assertFalse(original.sameVariant(voiceBoost));
        assertFalse(original.sameVariant(drc));
        assertTrue(drc.sameVariant(drcByXtags));
    }

    @Test
    public void languageFromSabrForm() {
        // MediaServiceCore: "%s (%s)", hyphens replaced by U+2010
        OculoAudioVariant original = OculoAudioVariant.of("en‐US (original)", null, false);
        assertEquals("en-US", original.langCode);
        assertEquals(OculoAudioVariant.Kind.ORIGINAL, original.kind);
        assertTrue(original.isOriginal());

        OculoAudioVariant dubbed = OculoAudioVariant.of("fr (dubbed-auto)", null, false);
        assertEquals("fr", dubbed.langCode);
        assertEquals(OculoAudioVariant.Kind.AUTO_DUBBED, dubbed.kind);
        assertFalse(dubbed.isOriginal());
        assertEquals(1, dubbed.penalty());

        assertEquals(OculoAudioVariant.Kind.DESCRIPTIVE, OculoAudioVariant.of("en (descriptive)", null, false).kind);
        assertEquals(OculoAudioVariant.Kind.DUBBED, OculoAudioVariant.of("es (dubbed)", null, false).kind);

        // A clean dubbed track comes before any variant of the original
        assertTrue(dubbed.penalty() < OculoAudioVariant.of("en (original)", null, true).penalty());
        // Voice boost of the original language is still not the original
        assertFalse(OculoAudioVariant.of("en (original)", XTAGS_VOICE_BOOST, false).isOriginal());
    }

    @Test
    public void languageFromXtagsWhenMissing() {
        OculoAudioVariant variant = OculoAudioVariant.of(null, XTAGS_AUTO_DUBBED_FR, false);
        assertEquals("fr", variant.langCode);
        assertEquals(OculoAudioVariant.Kind.AUTO_DUBBED, variant.kind);
    }

    @Test
    public void plainLanguage() {
        OculoAudioVariant variant = OculoAudioVariant.of("en", null, false);
        assertEquals("en", variant.langCode);
        assertEquals(OculoAudioVariant.Kind.UNKNOWN, variant.kind);
        assertTrue(variant.isOriginal());
        assertNull(OculoAudioVariant.of("und", null, false).langCode);
    }
}
