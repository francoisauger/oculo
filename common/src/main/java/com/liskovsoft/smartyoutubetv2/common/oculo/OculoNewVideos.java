package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Nullable;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Oculo (0.13.0): Abricot « new » dot on the thumbnail of each Subscriptions video published since the previous
 * visit to Subscriptions and not watched yet (flag {@code R.bool.oculo_subs_new_video_dot}). Rules in
 * {@link OculoNewVideosRules}.
 *
 * <ul>
 *     <li>A visit starts when Subscriptions is focused in the rail (or when its first card is shown) and ends when
 *     another section is focused. Going to the player and back stays in the same visit, so the dots stay.</li>
 *     <li>The previous visit's mark (top {@link OculoNewVideosRules#MARK_SIZE} ids, {@link #KEY_MARK}) is read
 *     once when a visit starts and kept in memory for the whole visit.</li>
 *     <li>The visit's own top is saved as the next mark when the user first focuses a Subscriptions card (he has
 *     looked at the list; only passing over Subscriptions in the rail does not use up the dots).</li>
 *     <li>Only data already in memory: no request, nothing in the background.</li>
 * </ul>
 */
public final class OculoNewVideos {
    private static final String KEY_MARK = "OculoSubsTop";
    @SuppressLint("StaticFieldLeak")
    private static OculoNewVideos sInstance;
    private final Context mContext;
    private final boolean mEnabled;
    private boolean mInVisit;
    private boolean mCommitted;
    private Set<String> mBaseline = Collections.emptySet();
    // Boundary cache: same group, same size = same boundary
    @Nullable
    private VideoGroup mCachedGroup;
    private int mCachedSize = -1;
    private List<String> mCachedIds = Collections.emptyList();
    private int mCachedBoundary = -1;

    private OculoNewVideos(Context context) {
        mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        mEnabled = mContext.getResources().getBoolean(R.bool.oculo_subs_new_video_dot);
    }

    public static OculoNewVideos instance(Context context) {
        if (sInstance == null) {
            sInstance = new OculoNewVideos(context);
        }
        return sInstance;
    }

    public static boolean isSubscriptionsVideo(@Nullable Video video) {
        if (video == null || video.videoId == null) {
            return false;
        }
        VideoGroup group = video.getGroup();
        return group != null && group.getSection() != null && group.getSection().getId() == MediaGroup.TYPE_SUBSCRIPTIONS;
    }

    /** BrowsePresenter.onSectionFocused(). */
    public void onSectionFocused(int sectionId) {
        if (!mEnabled) {
            return;
        }
        if (sectionId == MediaGroup.TYPE_SUBSCRIPTIONS) {
            beginVisitIfNeeded();
        } else {
            mInVisit = false;
        }
    }

    /** A Subscriptions card is bound: true when it gets the dot. */
    public boolean isNew(@Nullable Video video) {
        if (!mEnabled || !isSubscriptionsVideo(video)) {
            return false;
        }
        beginVisitIfNeeded();
        VideoGroup group = video.getGroup();
        int boundary = boundary(group);
        if (boundary <= 0) {
            return false;
        }
        int index = mCachedIds.indexOf(video.videoId);
        return OculoNewVideosRules.isNew(index, boundary, video.percentWatched, video.isUpcoming);
    }

    /** A card got the focus: the first Subscriptions card focused in a visit saves this visit's top as the mark. */
    public void onCardFocused(@Nullable Video video) {
        if (!mEnabled || mCommitted || !isSubscriptionsVideo(video)) {
            return;
        }
        beginVisitIfNeeded();
        List<String> top = OculoNewVideosRules.top(ids(video.getGroup()), OculoNewVideosRules.MARK_SIZE);
        if (top.isEmpty()) {
            return;
        }
        mCommitted = true;
        OculoStore.write(mContext, KEY_MARK, OculoNewVideosRules.format(top));
    }

    private void beginVisitIfNeeded() {
        if (mInVisit) {
            return;
        }
        mInVisit = true;
        mCommitted = false;
        mCachedGroup = null;
        mBaseline = OculoNewVideosRules.parse(AppPrefs.instance(mContext).getData(KEY_MARK));
    }

    private int boundary(VideoGroup group) {
        List<Video> videos = group.getVideos();
        int size = videos != null ? videos.size() : 0;
        if (group != mCachedGroup || size != mCachedSize) {
            mCachedGroup = group;
            mCachedSize = size;
            mCachedIds = ids(group);
            mCachedBoundary = OculoNewVideosRules.boundary(mCachedIds, mBaseline);
        }
        return mCachedBoundary;
    }

    private static List<String> ids(VideoGroup group) {
        List<String> result = new ArrayList<>();
        List<Video> videos = group != null ? group.getVideos() : null;
        if (videos == null) {
            return result;
        }
        for (Video item : videos) {
            result.add(item != null ? item.videoId : null);
        }
        return result;
    }
}
