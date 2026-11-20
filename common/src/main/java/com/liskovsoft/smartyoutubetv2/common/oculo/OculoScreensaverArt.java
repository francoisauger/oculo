package com.liskovsoft.smartyoutubetv2.common.oculo;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Oculo (0.13.0): the thumbnails the Oculo screensaver may show ({@link OculoScreensaverView}): the video cards
 * already shown in the app (VideoCardPresenter → OculoUi.onVideoCardBound), most recent first, Continue watching's
 * cards first. Only URLs are kept (no bitmap); the screensaver loads them from Glide's caches only, so no request
 * is made. Main thread only.
 */
public final class OculoScreensaverArt {
    static final int MAX_ENTRIES = 40;
    private static final Map<String, OculoScreensaverRules.Art> sRecent = new LinkedHashMap<>();

    private OculoScreensaverArt() {
    }

    /**
     * A video card was bound with {@code url} as its thumbnail (the card's first choice); {@code fallbackUrl} is
     * the card's error fallback.
     */
    public static void offer(@Nullable Video video, @Nullable String url, @Nullable String fallbackUrl) {
        if (video == null || video.videoId == null || video.isShorts || video.isUpcoming || (url == null && fallbackUrl == null)) {
            return;
        }
        boolean priority = OculoContinueWatching.isRowItem(video);
        sRecent.remove(video.videoId);
        sRecent.put(video.videoId, new OculoScreensaverRules.Art(video.videoId, url, fallbackUrl, priority));
        if (sRecent.size() > MAX_ENTRIES) {
            Iterator<String> oldest = sRecent.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /** Most recent first. */
    static List<OculoScreensaverRules.Art> recent() {
        List<OculoScreensaverRules.Art> result = new ArrayList<>(sRecent.values());
        java.util.Collections.reverse(result);
        return result;
    }
}
