package com.liskovsoft.smartyoutubetv2.common.oculo;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Oculo: what kind of audio track a YouTube format is (pure Java, no Android, see OculoAudioVariantTest).
 * <p>
 * YouTube serves the same itag several times: the original mix, a « voice boost » mix (xtags vb=1: voices up,
 * music and ambience strongly reduced), a « stable volume » mix (DRC: xtags drc=1 or isDrc) and, on videos with
 * several audio languages, dubbed / auto-dubbed / descriptive tracks. Upstream SmartTube only tells DRC apart,
 * so the voice-boost mix could be picked as the default (same itag, same bitrate as the original).
 * <p>
 * xtags: base64 (standard or URL-safe) of a protobuf message whose repeated field 1 is an entry
 * {1: key (string), 2: value (string)}, e.g. "CgcKAnZiEgEx" = {key "vb", value "1"}.
 */
public final class OculoAudioVariant {
    public enum Kind { ORIGINAL, UNKNOWN, DUBBED, AUTO_DUBBED, DESCRIPTIVE, SECONDARY }

    public final boolean voiceBoost;
    public final boolean drc;
    public final Kind kind;
    /** Language code (e.g. "en", "fr-CA") or null when the format has no language. */
    public final String langCode;

    public OculoAudioVariant(boolean voiceBoost, boolean drc, Kind kind, String langCode) {
        this.voiceBoost = voiceBoost;
        this.drc = drc;
        this.kind = kind == null ? Kind.UNKNOWN : kind;
        this.langCode = langCode;
    }

    /**
     * @param language exo language of the format: null, "en", or MediaServiceCore's SABR form "en (original)"
     *                 (hyphens replaced by U+2010)
     * @param xtags    raw xtags of the format or null
     * @param isDrc    upstream DRC flag (Format.isDrc, or an id ending with "drc")
     */
    public static OculoAudioVariant of(String language, String xtags, boolean isDrc) {
        Map<String, String> tags = parseXtags(xtags);

        boolean voiceBoost = "1".equals(tags.get("vb"));
        boolean drc = isDrc || "1".equals(tags.get("drc"));

        String langCode = null;
        String acont = null;

        if (language != null && !language.isEmpty() && !"und".equals(language)) {
            String lang = language.replace('‐', '-').trim();
            int open = lang.indexOf('(');
            int close = lang.lastIndexOf(')');
            if (open != -1 && close > open) {
                acont = lang.substring(open + 1, close).trim();
                lang = lang.substring(0, open).trim();
            }
            langCode = lang.isEmpty() ? null : lang;
        }

        if (acont == null) {
            acont = tags.get("acont");
        }
        if (langCode == null) {
            langCode = tags.get("lang");
        }

        return new OculoAudioVariant(voiceBoost, drc, kindOf(acont), langCode);
    }

    static Kind kindOf(String acont) {
        if (acont == null || acont.isEmpty()) {
            return Kind.UNKNOWN;
        }
        switch (acont.toLowerCase()) {
            case "original":
                return Kind.ORIGINAL;
            case "dubbed":
                return Kind.DUBBED;
            case "dubbed-auto":
                return Kind.AUTO_DUBBED;
            case "descriptive":
                return Kind.DESCRIPTIVE;
            case "secondary":
                return Kind.SECONDARY;
            default:
                return Kind.UNKNOWN;
        }
    }

    /** Original language (or no language info). */
    public boolean isOriginalLanguage() {
        return kind == Kind.ORIGINAL || kind == Kind.UNKNOWN;
    }

    /** The original mix in the original language: no voice boost, no DRC. */
    public boolean isOriginal() {
        return !voiceBoost && !drc && isOriginalLanguage();
    }

    /**
     * Distance from the original, used to choose the default track and order the list: 0 = original;
     * a dubbed / descriptive track (clean mix) 1, stable volume 2, voice boost 4 (added up).
     */
    public int penalty() {
        return (voiceBoost ? 4 : 0) + (drc ? 2 : 0) + (isOriginalLanguage() ? 0 : 1);
    }

    /** Same mix and same language kind (the itag / bitrate is compared elsewhere). */
    public boolean sameVariant(OculoAudioVariant other) {
        return other != null && voiceBoost == other.voiceBoost && drc == other.drc
                && isOriginalLanguage() == other.isOriginalLanguage() && (isOriginalLanguage() || kind == other.kind);
    }

    // ---- xtags ----

    /** Decodes xtags into key/value pairs; empty on null or malformed input. */
    public static Map<String, String> parseXtags(String xtags) {
        if (xtags == null || xtags.isEmpty()) {
            return Collections.emptyMap();
        }

        byte[] data = decodeBase64(xtags);
        if (data == null) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        try {
            int[] pos = {0};
            while (pos[0] < data.length) {
                long tag = readVarint(data, pos);
                int field = (int) (tag >>> 3);
                int wire = (int) (tag & 7);
                if (wire == 2) {
                    int len = (int) readVarint(data, pos);
                    if (len < 0 || pos[0] + len > data.length) {
                        break;
                    }
                    if (field == 1) {
                        parseEntry(data, pos[0], pos[0] + len, result);
                    }
                    pos[0] += len;
                } else if (!skip(data, pos, wire)) {
                    break;
                }
            }
        } catch (RuntimeException e) {
            // Malformed: keep what was read
        }
        return result;
    }

    private static void parseEntry(byte[] data, int start, int end, Map<String, String> result) {
        String key = null;
        String value = null;
        int[] pos = {start};
        while (pos[0] < end) {
            long tag = readVarint(data, pos);
            int field = (int) (tag >>> 3);
            int wire = (int) (tag & 7);
            if (wire == 2) {
                int len = (int) readVarint(data, pos);
                if (len < 0 || pos[0] + len > end) {
                    return;
                }
                String text = new String(data, pos[0], len, Charset.forName("UTF-8"));
                if (field == 1) {
                    key = text;
                } else if (field == 2) {
                    value = text;
                }
                pos[0] += len;
            } else if (!skip(data, pos, wire)) {
                return;
            }
        }
        if (key != null) {
            result.put(key, value);
        }
    }

    private static boolean skip(byte[] data, int[] pos, int wire) {
        switch (wire) {
            case 0:
                readVarint(data, pos);
                return true;
            case 1:
                pos[0] += 8;
                return pos[0] <= data.length;
            case 5:
                pos[0] += 4;
                return pos[0] <= data.length;
            default:
                return false;
        }
    }

    private static long readVarint(byte[] data, int[] pos) {
        long result = 0;
        int shift = 0;
        while (pos[0] < data.length && shift < 64) {
            byte b = data[pos[0]++];
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new IllegalArgumentException("truncated varint");
    }

    /** Standard or URL-safe base64, padding optional (java.util.Base64 needs API 26). */
    static byte[] decodeBase64(String input) {
        String s = input.trim();
        int len = s.length();
        while (len > 0 && s.charAt(len - 1) == '=') {
            len--;
        }
        byte[] out = new byte[len * 3 / 4];
        int buffer = 0;
        int bits = 0;
        int index = 0;
        for (int i = 0; i < len; i++) {
            int value = base64Value(s.charAt(i));
            if (value < 0) {
                return null;
            }
            buffer = (buffer << 6) | value;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                if (index < out.length) {
                    out[index++] = (byte) ((buffer >> bits) & 0xFF);
                }
            }
        }
        if (index < out.length) {
            byte[] trimmed = new byte[index];
            System.arraycopy(out, 0, trimmed, 0, index);
            return trimmed;
        }
        return out;
    }

    private static int base64Value(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+' || c == '-') return 62;
        if (c == '/' || c == '_') return 63;
        return -1;
    }
}
