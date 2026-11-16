package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Oculo (0.12.0): header context of the Oculo panel (every drawer of the app, see OCULO.md « Oculo panel »).
 * A drawer of SmartTube only has a title. The few places François opens often add a grey context label above it and
 * a one-line description below it (« Nature Lab » / video title / « 1.2M views · 3 weeks ago »), keyed by the title
 * the drawer is shown with: the caller puts the context just before {@code showDialog()}, the panel reads it when
 * it binds that title. Nested levels have no entry: their label is the path to them, computed by the panel.
 *
 * A small LRU (titles are not unique forever, but an entry is only looked up right after it is put, or when Back
 * returns to its level), the string helpers are plain Java, see OculoPanelInfoTest.
 */
public final class OculoPanelInfo {
    private static final int MAX = 24;
    /** Segment separator of YouTube's meta lines (« Channel • 1.2M views • 3 weeks ago »). */
    private static final String META_SEPARATOR = "•";
    private static final String DOT = " · ";

    private static final Map<String, Info> sInfo = new LinkedHashMap<String, Info>(MAX, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Info> eldest) {
            return size() > MAX;
        }
    };

    public static final class Info {
        /** Grey line above the title (null: the panel's own, the path or nothing) */
        public final CharSequence label;
        /** Replaces the drawer's title when not null */
        public final CharSequence title;
        /** One line of Roboto below the title (null: none) */
        public final CharSequence desc;

        public Info(CharSequence label, CharSequence title, CharSequence desc) {
            this.label = label;
            this.title = title;
            this.desc = desc;
        }
    }

    private OculoPanelInfo() {
    }

    /**
     * @param key the title the drawer (or one of its categories) is shown with
     */
    public static synchronized void put(CharSequence key, CharSequence label, CharSequence title, CharSequence desc) {
        if (key == null || key.length() == 0) {
            return;
        }
        sInfo.put(key.toString(), new Info(emptyToNull(label), emptyToNull(title), emptyToNull(desc)));
    }

    public static synchronized Info get(CharSequence key) {
        if (key == null || key.length() == 0) {
            return null;
        }
        return sInfo.get(key.toString());
    }

    public static synchronized void clear() {
        sInfo.clear();
    }

    /**
     * Menu of a video card (Home, grids, the player's suggestions) and its « Video description »: label = channel,
     * description = views · date (the menu is shown with the video's title).
     */
    public static void forVideo(CharSequence key, Video video) {
        if (video == null) {
            return;
        }
        String author = video.getAuthor();
        put(key, author, null, metaWithoutAuthor(video.getSecondTitle(), author, 3));
    }

    /**
     * Player « info » button: the drawer's « Title - Channel » becomes the video's title (Roboto Condensed), with
     * « About this video » above and channel · views · date below.
     */
    public static void forVideoInfo(Context context, CharSequence key, Video video) {
        if (context == null || video == null) {
            return;
        }
        String author = video.getAuthor();
        put(key, context.getString(R.string.oculo_panel_about_video), video.getTitleFull(),
                join(author, metaWithoutAuthor(video.getSecondTitle(), author, 2)));
    }

    /** Menu of a channel (Channels section): « Channel » above its name, its meta line below. */
    public static void forChannel(Context context, CharSequence key, Video video) {
        if (context == null || video == null) {
            return;
        }
        put(key, context.getString(R.string.oculo_panel_channel), null, metaWithoutAuthor(video.getSecondTitle(), video.getTitle(), 3));
    }

    /** Account drawer (title: avatar + name): « Account » above, the number of accounts below. */
    public static void forAccount(Context context, CharSequence key, int accountCount) {
        if (context == null) {
            return;
        }
        put(key, context.getString(R.string.oculo_panel_account), null,
                accountCount > 0 ? context.getString(R.string.oculo_panel_accounts, accountCount) : null);
    }

    /** Any drawer that only needs a label above its title (search filters…). */
    public static void forLabel(Context context, CharSequence key, int labelResId) {
        if (context == null) {
            return;
        }
        put(key, context.getString(labelResId), null, null);
    }

    public static String metaWithoutAuthor(CharSequence meta, String author) {
        return metaWithoutAuthor(meta, author, Integer.MAX_VALUE);
    }

    /**
     * « Nature Lab • 1.2M views • 3 weeks ago » → « 1.2M views · 3 weeks ago » (author segment left out, segments
     * joined by a middle dot, bidi marks and blanks trimmed), {@code maxParts} segments at most. Null when nothing
     * is left.
     */
    public static String metaWithoutAuthor(CharSequence meta, String author, int maxParts) {
        if (meta == null) {
            return null;
        }
        String authorKey = author != null ? clean(author) : null;
        List<String> parts = new ArrayList<>();
        for (String raw : meta.toString().split(META_SEPARATOR)) {
            String part = clean(raw);
            if (part.isEmpty() || part.equals(authorKey)) {
                continue;
            }
            if (parts.size() < maxParts) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) {
                result.append(DOT);
            }
            result.append(part);
        }
        return result.toString();
    }

    /**
     * Joins the non empty parts with a middle dot.
     */
    public static String join(CharSequence... parts) {
        StringBuilder result = new StringBuilder();
        for (CharSequence part : parts) {
            if (part == null) {
                continue;
            }
            String text = clean(part.toString());
            if (text.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(DOT);
            }
            result.append(text);
        }
        return result.length() > 0 ? result.toString() : null;
    }

    private static String clean(String text) {
        // Bidi marks around each segment (MediaServiceCore), then blanks
        return text.replaceAll("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069]", "").trim();
    }

    private static CharSequence emptyToNull(CharSequence text) {
        return text == null || text.toString().trim().isEmpty() ? null : text;
    }
}
