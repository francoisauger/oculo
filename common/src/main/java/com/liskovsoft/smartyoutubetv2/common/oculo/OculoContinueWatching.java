package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.State;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Candidate;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Mark;
import com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Oculo: Home's « Continue watching » row (second row, right after the first one).
 * <p>
 * Data: local only, no network call. Positions come from SmartTube's VideoStateService (the same store the
 * player resumes from); the time a video was last played comes from Oculo's own watch log, written on every
 * position save of the player (VideoStateService does not persist its timestamps). Selection rules:
 * {@link OculoContinueWatchingRules}. Videos removed with « Remove from Continue watching » are kept in a
 * hidden list (30 days). Nothing here touches YouTube's history or the watched state.
 */
public class OculoContinueWatching {
    /** Stable id of the row, so that it can be replaced, synced or trimmed in place. */
    public static final int GROUP_ID = 0x0C1A0C1A;
    /** Home row index: right after the first row. */
    public static final int ROW_POSITION = 1;
    private static final String KEY_WATCH_LOG = "OculoWatchLog";
    private static final String KEY_HIDDEN = "OculoContinueHidden";
    private static final long PERSIST_DELAY_MS = 3_000;
    @SuppressLint("StaticFieldLeak")
    private static OculoContinueWatching sInstance;
    private final Context mContext;
    private final AppPrefs mPrefs;
    private final Map<String, Mark> mWatchLog;
    private final Map<String, Mark> mHidden;
    private final Runnable mPersist = this::persistNow;
    private WeakReference<BrowseView> mView = new WeakReference<>(null);
    private BrowseSection mSection;
    /** Videos currently shown in the row (empty: no row). */
    private final List<Video> mShown = new ArrayList<>();

    private OculoContinueWatching(Context context) {
        mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        mPrefs = AppPrefs.instance(mContext);
        long now = System.currentTimeMillis();
        mWatchLog = OculoContinueWatchingRules.parse(mPrefs.getData(KEY_WATCH_LOG));
        OculoContinueWatchingRules.prune(mWatchLog, now, OculoContinueWatchingRules.LOG_RETENTION_MS, OculoContinueWatchingRules.LOG_MAX_SIZE);
        mHidden = OculoContinueWatchingRules.parse(mPrefs.getData(KEY_HIDDEN));
        OculoContinueWatchingRules.prune(mHidden, now, OculoContinueWatchingRules.HIDDEN_RETENTION_MS, OculoContinueWatchingRules.HIDDEN_MAX_SIZE);
    }

    public static OculoContinueWatching instance(Context context) {
        if (sInstance == null && context != null) {
            sInstance = new OculoContinueWatching(context);
        }

        return sInstance;
    }

    private boolean isEnabled() {
        return OculoData.instance(mContext).isHomeContinueWatchingEnabled();
    }

    // ---- Hooks ----

    /**
     * VideoStateController.savePosition(): the player just saved this video's position.
     */
    public void onPositionSaved(Video video) {
        if (video == null || TextUtils.isEmpty(video.videoId)) {
            return;
        }

        int flags = (video.isLive ? OculoContinueWatchingRules.FLAG_LIVE : 0) | (video.isShorts ? OculoContinueWatchingRules.FLAG_SHORTS : 0);
        mWatchLog.remove(video.videoId);
        mWatchLog.put(video.videoId, new Mark(System.currentTimeMillis(), flags));
        schedulePersist();
    }

    /**
     * BrowsePresenter, right after Home's first row was added: (re)builds the row at {@link #ROW_POSITION}.
     * @return true when the row was added (the caller then shifts the next rows by one)
     */
    public boolean showRow(BrowseView view, BrowseSection section) {
        if (view == null || section == null) {
            return false;
        }

        mView = new WeakReference<>(view);
        mSection = section;
        mShown.clear();

        List<Video> videos = isEnabled() ? compute() : Collections.emptyList();
        VideoGroup group = createGroup(videos, VideoGroup.ACTION_REPLACE); // also removes an older copy of the row
        view.updateSection(group);

        if (!videos.isEmpty()) {
            mShown.addAll(group.getVideos());
        }

        return !videos.isEmpty();
    }

    /**
     * BrowsePresenter.onViewResumed() on a loaded Home (e.g. back from the player): updates the row in place
     * when its content changed: progress only (sync), videos gone (remove) or anything else (replace).
     */
    public void onHomeResumed(BrowseView view, BrowseSection section, boolean isHomeLoaded) {
        if (view == null || section == null || !isHomeLoaded || mSection == null) {
            return;
        }

        mView = new WeakReference<>(view);
        mSection = section;

        List<Video> videos = isEnabled() ? compute() : Collections.emptyList();
        List<String> newIds = ids(videos);
        List<String> shownIds = ids(mShown);

        if (newIds.equals(shownIds)) {
            if (!videos.isEmpty()) {
                for (int i = 0; i < videos.size(); i++) {
                    mShown.get(i).percentWatched = videos.get(i).percentWatched;
                }
                view.updateSection(createGroupOf(new ArrayList<>(mShown), VideoGroup.ACTION_SYNC));
            }
        } else if (!newIds.isEmpty() && isSubsequence(newIds, shownIds)) {
            removeShown(view, newIds);
            for (int i = 0; i < videos.size(); i++) {
                mShown.get(i).percentWatched = videos.get(i).percentWatched;
            }
            view.updateSection(createGroupOf(new ArrayList<>(mShown), VideoGroup.ACTION_SYNC));
        } else {
            showRow(view, section);
        }
    }

    /**
     * VideoMenuPresenter (long press): « Remove from Continue watching », only for cards of this row.
     */
    public void appendMenuItem(AppDialogPresenter dialogPresenter, Video video) {
        if (dialogPresenter == null || !isRowItem(video)) {
            return;
        }

        dialogPresenter.appendSingleButton(UiOptionItem.from(mContext.getString(R.string.oculo_continue_watching_remove), optionItem -> {
            hide(video.videoId);
            dialogPresenter.closeDialog();
        }));
    }

    public static boolean isRowItem(Video video) {
        return video != null && video.videoId != null && video.getGroup() != null && video.getGroup().getId() == GROUP_ID;
    }

    // ---- Internals ----

    private void hide(String videoId) {
        mHidden.remove(videoId);
        mHidden.put(videoId, new Mark(System.currentTimeMillis(), 0));
        OculoContinueWatchingRules.prune(mHidden, System.currentTimeMillis(), OculoContinueWatchingRules.HIDDEN_RETENTION_MS, OculoContinueWatchingRules.HIDDEN_MAX_SIZE);
        schedulePersist();

        BrowseView view = mView.get();
        if (view == null || mSection == null) {
            return;
        }

        List<String> keep = ids(mShown);
        keep.remove(videoId);
        removeShown(view, keep);
    }

    /** Removes from the row every shown video not in {@code keepIds}; drops the row when it becomes empty. */
    private void removeShown(BrowseView view, List<String> keepIds) {
        List<Video> removed = new ArrayList<>();
        for (Video video : mShown) {
            if (!keepIds.contains(video.videoId)) {
                removed.add(video);
            }
        }
        if (removed.isEmpty()) {
            return;
        }

        mShown.removeAll(removed);

        if (mShown.isEmpty()) {
            view.updateSection(createGroup(Collections.emptyList(), VideoGroup.ACTION_REPLACE)); // removes the row
        } else {
            view.updateSection(createGroupOf(removed, VideoGroup.ACTION_REMOVE));
        }
    }

    private List<Video> compute() {
        VideoStateService stateService = VideoStateService.instance(mContext);
        if (stateService == null || stateService.isEmpty()) {
            return Collections.emptyList();
        }

        List<State> states = new ArrayList<>(stateService.getStates());
        List<Candidate> candidates = new ArrayList<>();
        for (State state : states) {
            if (state == null || state.video == null) {
                continue;
            }
            Mark mark = mWatchLog.get(state.video.videoId);
            if (mark == null) {
                continue; // never played since the watch log exists: no reliable date
            }
            long durationMs = state.durationMs > 0 ? state.durationMs : state.video.getDurationMs();
            candidates.add(new Candidate(state.video.videoId, mark.timeMs, state.positionMs, durationMs,
                    state.video.isLive || (mark.flags & OculoContinueWatchingRules.FLAG_LIVE) != 0,
                    state.video.isShorts || (mark.flags & OculoContinueWatchingRules.FLAG_SHORTS) != 0));
        }

        List<Video> result = new ArrayList<>();
        for (String videoId : OculoContinueWatchingRules.select(candidates, mHidden, System.currentTimeMillis())) {
            State state = stateService.getByVideoId(videoId);
            Video video = state != null ? toCard(state) : null;
            if (video != null) {
                result.add(video);
            }
        }
        return result;
    }

    /** A fresh card (the stored Video stays untouched): progress from the saved position, resumes there. */
    private static Video toCard(State state) {
        Video video = Video.fromString(state.video.toString());
        if (video == null) {
            return null;
        }
        video.sync(state); // percentWatched from the saved position (the player resumes from the same state)
        if (TextUtils.isEmpty(video.cardImageUrl)) {
            video.cardImageUrl = "https://i.ytimg.com/vi/" + video.videoId + "/hqdefault.jpg";
        }
        return video;
    }

    private VideoGroup createGroup(List<Video> videos, int action) {
        VideoGroup group = VideoGroup.from(mSection, ROW_POSITION);
        group.setId(GROUP_ID);
        group.setTitle(mContext.getString(R.string.oculo_continue_watching));
        group.setAction(action);
        for (Video video : videos) {
            group.add(video); // blocked channels are still filtered; Home's watched filter only applies to row 0
        }
        return group;
    }

    /** Same row, already shown videos (keeps their group). */
    private VideoGroup createGroupOf(List<Video> videos, int action) {
        VideoGroup group = VideoGroup.from(mSection, ROW_POSITION);
        group.setId(GROUP_ID);
        group.setAction(action);
        for (Video video : videos) {
            group.add(video);
        }
        return group;
    }

    private static List<String> ids(List<Video> videos) {
        List<String> result = new ArrayList<>();
        for (Video video : videos) {
            result.add(video.videoId);
        }
        return result;
    }

    /** {@code sub} keeps the order of {@code full} with some items removed. */
    static boolean isSubsequence(List<String> sub, List<String> full) {
        int j = 0;
        for (String id : full) {
            if (j < sub.size() && sub.get(j).equals(id)) {
                j++;
            }
        }
        return j == sub.size();
    }

    private void schedulePersist() {
        Utils.removeCallbacks(mPersist);
        Utils.postDelayed(mPersist, PERSIST_DELAY_MS);
    }

    public void persistNow() {
        mPrefs.setData(KEY_WATCH_LOG, OculoContinueWatchingRules.format(mWatchLog));
        mPrefs.setData(KEY_HIDDEN, OculoContinueWatchingRules.format(mHidden));
    }
}
