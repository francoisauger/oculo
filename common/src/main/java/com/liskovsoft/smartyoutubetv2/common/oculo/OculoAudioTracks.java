package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;
import android.util.Log;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.sabr.parser.models.SabrFormatMetadata;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.ExoFormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.TrackSelectorUtil;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.track.AudioTrack;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.track.MediaTrack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Oculo: the original audio track by default, and clear names in the « Audio formats » list.
 * <p>
 * Selection ({@link #filterForSelection}, called by TrackSelectorManager.findBestMatch() after upstream's
 * language filter): when the wanted track is a remembered / default spec (no real format), only the candidates
 * closest to the original are kept (no voice boost, no DRC, original language when there is one); upstream's
 * quality and codec rules then choose among them. When it is a real track picked in the list, the candidates of
 * the same variant are kept, so the pick is honoured for that video. The remembered choice (PlayerData) never
 * carries the variant, so the next video starts on the original again.
 * <p>
 * Flag {@code oculo_audio_original} (false upstream, true in the flavor).
 */
public final class OculoAudioTracks {
    private static final String TAG = "OculoAudio";
    private static final String SEPARATOR = " · ";

    private OculoAudioTracks() {
    }

    public static boolean isEnabled(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_audio_original);
    }

    /** Variant of a format (xtags from the SABR metadata). Null for a null format. */
    public static OculoAudioVariant of(Format format) {
        if (format == null) {
            return null;
        }
        return OculoAudioVariant.of(format.language, xtags(format), TrackSelectorUtil.isDrc(format));
    }

    public static boolean isVoiceBoost(Format format) {
        OculoAudioVariant variant = of(format);
        return variant != null && variant.voiceBoost;
    }

    private static String xtags(Format format) {
        Metadata metadata = format.metadata;
        if (metadata == null) {
            return null;
        }
        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry entry = metadata.get(i);
            if (entry instanceof SabrFormatMetadata) {
                return ((SabrFormatMetadata) entry).xTags;
            }
        }
        return null;
    }

    /** A track of the current media (picked in the list), not a remembered / default spec (no mime type). */
    private static boolean isRealTrack(Format format) {
        return format != null && format.sampleMimeType != null;
    }

    // ---- Selection ----

    public static MediaTrack[][] filterForSelection(Context context, MediaTrack[][] groups, MediaTrack origin) {
        if (groups == null || !(origin instanceof AudioTrack) || origin.format == null || !isEnabled(context)) {
            return groups;
        }

        OculoAudioVariant target = isRealTrack(origin.format) ? of(origin.format) : null;

        int total = 0;
        int minPenalty = Integer.MAX_VALUE;
        boolean hasSameVariant = false;
        for (MediaTrack[] group : groups) {
            if (group == null) {
                continue;
            }
            for (MediaTrack track : group) {
                if (track == null || track.format == null) {
                    continue;
                }
                total++;
                OculoAudioVariant variant = of(track.format);
                minPenalty = Math.min(minPenalty, variant.penalty());
                if (target != null && target.sameVariant(variant)) {
                    hasSameVariant = true;
                }
            }
        }

        if (total == 0) {
            return groups;
        }

        boolean bySameVariant = target != null && hasSameVariant;
        int kept = 0;
        MediaTrack[][] result = new MediaTrack[groups.length][];
        for (int g = 0; g < groups.length; g++) {
            MediaTrack[] group = groups[g];
            if (group == null) {
                continue;
            }
            result[g] = new MediaTrack[group.length];
            for (int t = 0; t < group.length; t++) {
                MediaTrack track = group[t];
                if (track == null || track.format == null) {
                    continue;
                }
                OculoAudioVariant variant = of(track.format);
                boolean keep = bySameVariant ? target.sameVariant(variant) : variant.penalty() == minPenalty;
                if (keep) {
                    result[g][t] = track;
                    kept++;
                }
            }
        }

        Log.d(TAG, String.format("selection: %s, kept %s of %s audio tracks (%s)", origin.format.id,
                kept, total, bySameVariant ? "same variant as the pick" : "closest to the original, penalty " + minPenalty));

        return kept == total ? groups : result;
    }

    // ---- List ----

    /** Relabels and orders the « Audio formats » list: originals first by quality, then the variants. */
    public static List<FormatItem> forList(Context context, List<FormatItem> formats) {
        if (formats == null || !isEnabled(context)) {
            return formats;
        }

        Set<String> languages = new HashSet<>();
        for (FormatItem item : formats) {
            Format format = formatOf(item);
            OculoAudioVariant variant = of(format);
            if (variant != null && variant.langCode != null) {
                languages.add(variant.langCode.toLowerCase());
            }
        }
        boolean showLanguage = languages.size() > 1;
        Locale locale = context.getResources().getConfiguration().locale;

        List<FormatItem> head = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        for (FormatItem item : formats) {
            Format format = formatOf(item);
            if (format == null || !(item instanceof ExoFormatItem)) {
                head.add(item); // « Disabled » option
                continue;
            }
            OculoAudioVariant variant = of(format);
            String languageName = variant.langCode != null ? languageName(variant.langCode, locale) : "";
            ((ExoFormatItem) item).setTitle(label(context, format, variant, showLanguage ? languageName : null, locale));
            entries.add(new Entry(item, variant, format.bitrate, languageName));
        }

        Collections.sort(entries, (a, b) -> {
            int result = Integer.compare(a.variant.penalty(), b.variant.penalty());
            if (result != 0) {
                return result;
            }
            result = a.languageName.compareToIgnoreCase(b.languageName);
            if (result != 0) {
                return result;
            }
            return Integer.compare(b.bitrate, a.bitrate);
        });

        List<FormatItem> result = new ArrayList<>(head);
        for (Entry entry : entries) {
            result.add(entry.item);
        }
        return result;
    }

    private static final class Entry {
        final FormatItem item;
        final OculoAudioVariant variant;
        final int bitrate;
        final String languageName;

        Entry(FormatItem item, OculoAudioVariant variant, int bitrate, String languageName) {
            this.item = item;
            this.variant = variant;
            this.bitrate = bitrate;
            this.languageName = languageName;
        }
    }

    private static Format formatOf(FormatItem item) {
        return item != null && item.getTrack() != null ? item.getTrack().format : null;
    }

    /** « [Language ·] 0.16 Mb/s · Opus [· 5.1] · Original | Dubbed… | Voice boost | Stable volume ». */
    static String label(Context context, Format format, OculoAudioVariant variant, String languageName, Locale locale) {
        List<String> parts = new ArrayList<>();
        if (languageName != null && !languageName.isEmpty()) {
            parts.add(languageName);
        }
        if (format.bitrate != Format.NO_VALUE && format.bitrate > 0) {
            NumberFormat number = NumberFormat.getNumberInstance(locale);
            number.setMinimumFractionDigits(2);
            number.setMaximumFractionDigits(2);
            parts.add(context.getString(R.string.oculo_audio_bitrate, number.format(format.bitrate / 1_000_000d)));
        }
        String codec = codecName(format);
        if (!codec.isEmpty()) {
            parts.add(codec);
        }
        if (TrackSelectorUtil.is51Audio(format)) {
            parts.add("5.1");
        }

        List<String> tail = new ArrayList<>();
        switch (variant.kind) {
            case DUBBED:
                tail.add(context.getString(R.string.oculo_audio_label_dubbed));
                break;
            case AUTO_DUBBED:
                tail.add(context.getString(R.string.oculo_audio_label_auto_dubbed));
                break;
            case DESCRIPTIVE:
                tail.add(context.getString(R.string.oculo_audio_label_descriptive));
                break;
            case SECONDARY:
                tail.add(context.getString(R.string.oculo_audio_label_secondary));
                break;
            default:
                break;
        }
        if (variant.voiceBoost) {
            tail.add(context.getString(R.string.oculo_audio_label_voice_boost));
        }
        if (variant.drc) {
            tail.add(context.getString(R.string.oculo_audio_label_stable_volume));
        }
        if (tail.isEmpty()) {
            tail.add(context.getString(R.string.oculo_audio_label_original));
        }
        parts.addAll(tail);

        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(part);
        }
        return builder.toString();
    }

    static String codecName(Format format) {
        String codec = TrackSelectorUtil.extractCodec(format);
        if (codec == null || codec.isEmpty()) {
            return "";
        }
        switch (codec.toLowerCase()) {
            case "mp4a":
                return "AAC";
            case "opus":
                return "Opus";
            case "vorbis":
                return "Vorbis";
            case "ec-3":
                return "E-AC-3";
            case "ac-3":
                return "AC-3";
            case "flac":
                return "FLAC";
            default:
                return codec;
        }
    }

    static String languageName(String langCode, Locale locale) {
        try {
            String name = Locale.forLanguageTag(langCode).getDisplayLanguage(locale);
            if (name == null || name.isEmpty()) {
                return langCode;
            }
            return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
        } catch (RuntimeException e) {
            return langCode;
        }
    }
}
