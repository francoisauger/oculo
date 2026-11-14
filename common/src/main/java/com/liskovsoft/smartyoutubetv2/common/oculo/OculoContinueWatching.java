package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.State;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Candidate;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.HistoryItem;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Mark;
import com.liskovsoft.smartyoutubetv2.common.oculo.OculoContinueWatchingRules.Pick;
import com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;

/**
 * Oculo: Home's « Continue watching » row (second row, right after the first one).
 * <p>
 * Data (32.74-14): the signed-in account's YouTube history (one request when Home is built: first load and the
 * 10-minute refresh; async, the row is updated in place when it arrives), merged with Oculo's local watch log.
 * Local positions come from SmartTube's VideoStateService (the store the player resumes from); the time a video
 * was last played in Oculo comes from Oculo's own watch log, written on every position save of the player
 * (VideoStateService does not persist its timestamps). History items carry YouTube's progress (percent, resume
 * position) but no date. 32.74-15: the row = unfinished videos among the history's 20 most recent items, plus the
 * unfinished videos played in Oculo in the last 48 h ({@link OculoContinueWatchingRules#selectMerged}); no first-fetch
 * baseline (32.74-14 estimated a 48 h window and left out everything seen on the first fetch). Signed out or request
 * failed: local log only. Each history arrival writes one line to logcat (tag {@code OculoCW}).
 * 32.74-16: the last history result is kept on disk (top 20 items with their cards, per account), so the row is built
 * with Home's first row at launch; the fresh history then updates it with item changes on this row only (never a
 * rows rebuild, and never while the focus is on one of its cards). Videos removed
 * with « Remove from Continue watching » are kept in a hidden list (30 days). Nothing here touches YouTube's
 * history or the watched state.
 */
public class OculoContinueWatching {
    /** Stable id of the row, so that it can be replaced, synced or trimmed in place. */
    public static final int GROUP_ID = 0x0C1A0C1A;
    /** Home row index: right after the first row. */
    public static final int ROW_POSITION = 1;
    private static final String KEY_WATCH_LOG = "OculoWatchLog";
    private static final String KEY_HIDDEN = "OculoContinueHidden";
    /** 32.74-14's store of estimated history dates, no longer used: cleared once. */
    private static final String KEY_HISTORY_SEEN = "OculoHistorySeen";
    /** 32.74-16: the last account history (top items + cards), so that the row shows at launch. */
    private static final String KEY_HISTORY_CACHE = "OculoHistoryCache";
    private static final String TAG = "OculoCW";
    /** Home may be built twice in a row (load, section re-selected): at most one history request per 30 s. */
    private static final long HISTORY_MIN_INTERVAL_MS = 30_000;
    /** New selection not applicable yet (Home rows still loading for a row insert/removal, focus in the row): retry. */
    private static final long APPLY_RETRY_MS = 1_500;
    /** About 5 minutes; afterwards Home's next resume or build applies it. */
    private static final int APPLY_MAX_RETRIES = 200;
    private static final long PERSIST_DELAY_MS = 3_000;
    @SuppressLint("StaticFieldLeak")
    private static OculoContinueWatching sInstance;
    private final Context mContext;
    private final AppPrefs mPrefs;
    private final Map<String, Mark> mWatchLog;
    private final Map<String, Mark> mHidden;
    /** Last history page of the account, most recent first (empty: signed out, error, not loaded yet). */
    private final List<HistoryItem> mHistory = new ArrayList<>();
    private final Map<String, Video> mHistoryVideos = new HashMap<>();
    /** The history above was fetched in this run (false: none yet, or the cached copy from the last run). */
    private boolean mHistoryFresh;
    private boolean mCacheLoaded;
    private Disposable mHistoryAction;
    private long mLastHistoryRequestMs;
    private int mApplyRetries;
    private final Runnable mApplyHistory = this::applyHistory;
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
        if (!TextUtils.isEmpty(mPrefs.getData(KEY_HISTORY_SEEN))) {
            mPrefs.setData(KEY_HISTORY_SEEN, "");
        }
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
        loadHistoryCache();

        List<Video> videos = isEnabled() ? compute() : Collections.emptyList();
        VideoGroup group = createGroup(videos, VideoGroup.ACTION_REPLACE); // also removes an older copy of the row
        view.updateSection(group);

        mShown.addAll(group.getVideos()); // blocked channels may have been filtered out

        requestHistory(); // Home load and 10-minute refresh: the row is updated in place when it arrives

        return !mShown.isEmpty();
    }

    /**
     * BrowsePresenter.onViewResumed() on a loaded Home (e.g. back from the player): updates the row in place
     * when its content changed (see {@link #applyHistory()}).
     */
    public void onHomeResumed(BrowseView view, BrowseSection section, boolean isHomeLoaded) {
        if (view == null || section == null || !isHomeLoaded || mSection == null) {
            return;
        }

        mView = new WeakReference<>(view);
        mSection = section;

        mApplyRetries = 0;
        applyHistory();
    }

    /**
     * Updates the shown row to the current selection with item changes on this row only (32.74-16): progress only
     * (sync), otherwise remove the videos gone and add the new ones before / after the videos kept
     * ({@link OculoContinueWatchingRules#planInPlace}). The row is only inserted or removed as a whole when it
     * appears or becomes empty.
     *
     * @return false when the change has to wait: the row must be inserted or removed while Home's rows are still
     * loading (the next rows' positions depend on it), or the focus is on one of its cards (adding or removing
     * cards there would move it)
     */
    private boolean updateInPlace(BrowseView view, BrowseSection section, BrowsePresenter presenter) {
        List<Video> videos = isEnabled() ? compute() : Collections.<Video>emptyList();
        List<String> newIds = ids(videos);
        List<String> shownIds = ids(mShown);

        if (newIds.equals(shownIds)) {
            if (!videos.isEmpty()) {
                syncProgress(view, videos);
            }
            return true;
        }

        if (mShown.isEmpty() || videos.isEmpty()) {
            // The row appears or disappears: Home's other rows shift by one
            if (presenter.hasPendingActions()) {
                return false;
            }
            if (videos.isEmpty()) {
                if (isFocusInRow(presenter)) {
                    return false;
                }
                mShown.clear();
                view.updateSection(createGroup(Collections.<Video>emptyList(), VideoGroup.ACTION_REPLACE)); // removes the row
            } else {
                showRowNow(view, section, videos);
            }
            return true;
        }

        if (isFocusInRow(presenter)) {
            return false;
        }

        OculoContinueWatchingRules.RowPlan plan = OculoContinueWatchingRules.planInPlace(shownIds, newIds);
        Map<String, Video> byId = new HashMap<>();
        for (Video video : videos) {
            byId.put(video.videoId, video);
        }

        removeShown(view, plan.kept, false); // new cards are added right after: the row stays

        if (!plan.prepend.isEmpty()) {
            VideoGroup group = createGroupOf(pick(plan.prepend, byId), VideoGroup.ACTION_PREPEND);
            view.updateSection(group);
            mShown.addAll(0, group.getVideos());
        }
        if (!plan.append.isEmpty()) {
            VideoGroup group = createGroupOf(pick(plan.append, byId), VideoGroup.ACTION_APPEND);
            view.updateSection(group);
            mShown.addAll(group.getVideos());
        }

        if (mShown.isEmpty()) {
            // Everything filtered out on the way (e.g. blocked channels): no row
            view.updateSection(createGroup(Collections.<Video>emptyList(), VideoGroup.ACTION_REPLACE));
            return true;
        }

        syncProgress(view, videos);
        return true;
    }

    /** First appearance of the row after Home was built (it had nothing to show then). */
    private void showRowNow(BrowseView view, BrowseSection section, List<Video> videos) {
        mShown.clear();
        VideoGroup group = createGroup(videos, VideoGroup.ACTION_REPLACE);
        view.updateSection(group);
        mShown.addAll(group.getVideos());
    }

    /** Progress of the shown cards from the new selection (same videos), one item change each. */
    private void syncProgress(BrowseView view, List<Video> videos) {
        Map<String, Video> byId = new HashMap<>();
        for (Video video : videos) {
            byId.put(video.videoId, video);
        }
        List<Video> changed = new ArrayList<>();
        for (Video shown : mShown) {
            Video fresh = byId.get(shown.videoId);
            if (fresh != null && fresh.percentWatched != shown.percentWatched) {
                shown.percentWatched = fresh.percentWatched;
                shown.startTimeSeconds = fresh.startTimeSeconds;
                changed.add(shown);
            }
        }
        if (!changed.isEmpty()) {
            view.updateSection(createGroupOf(changed, VideoGroup.ACTION_SYNC));
        }
    }

    private static List<Video> pick(List<String> ids, Map<String, Video> byId) {
        List<Video> result = new ArrayList<>();
        for (String id : ids) {
            Video video = byId.get(id);
            if (video != null) {
                result.add(video);
            }
        }
        return result;
    }

    /** The selected card (focus, or the last card focused before the rail) is one of this row's. */
    private static boolean isFocusInRow(BrowsePresenter presenter) {
        return isRowItem(presenter.getCurrentVideo());
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
        removeShown(view, keepIds, true);
    }

    private void removeShown(BrowseView view, List<String> keepIds, boolean dropEmptyRow) {
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

        if (mShown.isEmpty() && dropEmptyRow) {
            view.updateSection(createGroup(Collections.emptyList(), VideoGroup.ACTION_REPLACE)); // removes the row
        } else {
            view.updateSection(createGroupOf(removed, VideoGroup.ACTION_REMOVE));
        }
    }

    // ---- Account history ----

    private void requestHistory() {
        if (!isEnabled() || RxHelper.isAnyActionRunning(mHistoryAction)) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (mLastHistoryRequestMs != 0 && now - mLastHistoryRequestMs < HISTORY_MIN_INTERVAL_MS) {
            return;
        }
        mLastHistoryRequestMs = now;

        if (MediaServiceManager.instance().getSelectedAccount() == null) {
            onSignedOut();
            return;
        }

        try {
            mHistoryAction = RxHelper.execute(
                    YouTubeServiceManager.instance().getContentService().getHistoryObserve(),
                    group -> Utils.post(() -> onHistory(group)),
                    error -> Utils.post(this::onHistoryFailed));
        } catch (RuntimeException e) {
            onHistoryFailed();
        }
    }

    /** Signed out: local log only, and the cached history (another account's, maybe) is dropped. */
    private void onSignedOut() {
        Log.i(TAG, "history: signed out, local log only");
        mPrefs.setData(KEY_HISTORY_CACHE, "");
        boolean changed = !mHistory.isEmpty();
        mHistory.clear();
        mHistoryVideos.clear();
        mHistoryFresh = false;
        if (changed) {
            mApplyRetries = 0;
            applyHistory();
        }
    }

    /** Request failed (network): the history shown so far (cached or older) stays. */
    private void onHistoryFailed() {
        Log.i(TAG, "history: request failed, keeping the last one (" + mHistory.size() + " items, "
                + (mHistoryFresh ? "fetched" : "cached") + ")");
    }

    /**
     * Before Home's row is first built: the history fetched at the last run (same account, 7 days at most), so the
     * row is complete from the start; replaced by the fresh one as soon as it arrives.
     */
    private void loadHistoryCache() {
        if (mCacheLoaded || mHistoryFresh) {
            return;
        }
        mCacheLoaded = true;
        OculoContinueWatchingRules.HistoryCache cache;
        try {
            cache = OculoContinueWatchingRules.parseHistoryCache(mPrefs.getData(KEY_HISTORY_CACHE));
        } catch (RuntimeException e) {
            cache = null;
        }
        if (cache == null || !cache.isUsable(accountKey(), System.currentTimeMillis())) {
            return;
        }
        List<HistoryItem> items = new ArrayList<>();
        Map<String, Video> videos = new HashMap<>();
        for (HistoryItem item : cache.items) {
            Video video = Video.fromString(cache.specs.get(item.videoId));
            if (video == null || TextUtils.isEmpty(video.videoId)) {
                continue;
            }
            video.percentWatched = item.percent;
            video.startTimeSeconds = item.positionMs > 0 ? (int) (item.positionMs / 1_000) : video.startTimeSeconds;
            videos.put(item.videoId, video);
            items.add(item);
        }
        mHistory.clear();
        mHistory.addAll(items);
        mHistoryVideos.clear();
        mHistoryVideos.putAll(videos);
        Log.i(TAG, "history: " + items.size() + " cached items from the last run");
    }

    private void saveHistoryCache() {
        String key = accountKey();
        if (key == null) {
            return;
        }
        Map<String, String> specs = new HashMap<>();
        for (int i = 0; i < mHistory.size() && i < OculoContinueWatchingRules.HISTORY_TOP; i++) {
            Video video = mHistoryVideos.get(mHistory.get(i).videoId);
            if (video != null) {
                specs.put(video.videoId, video.toString());
            }
        }
        mPrefs.setData(KEY_HISTORY_CACHE, OculoContinueWatchingRules.formatHistoryCache(
                new OculoContinueWatchingRules.HistoryCache(key, System.currentTimeMillis(), mHistory, specs)));
    }

    /** The signed-in account, as a hash (no name or e-mail on disk); null when signed out. */
    private static String accountKey() {
        Account account;
        try {
            account = MediaServiceManager.instance().getSelectedAccount();
        } catch (RuntimeException e) {
            return null;
        }
        if (account == null) {
            return null;
        }
        String id = !TextUtils.isEmpty(account.getEmail()) ? account.getEmail()
                : !TextUtils.isEmpty(account.getName()) ? account.getName() : String.valueOf(account.getId());
        return Integer.toHexString(id.hashCode());
    }

    /** Main thread: the account's history arrived. */
    private void onHistory(MediaGroup group) {
        List<HistoryItem> history = new ArrayList<>();
        Map<String, Video> videos = new HashMap<>();
        List<MediaItem> items = group != null ? group.getMediaItems() : null;
        if (items != null) {
            for (MediaItem item : items) {
                Video video = item != null ? Video.from(item) : null;
                if (video == null || TextUtils.isEmpty(video.videoId) || videos.containsKey(video.videoId)) {
                    continue;
                }
                videos.put(video.videoId, video);
                history.add(new HistoryItem(video.videoId, video.percentWatched,
                        video.startTimeSeconds > 0 ? video.startTimeSeconds * 1_000L : -1, video.getDurationMs(),
                        video.isLive || video.isUpcoming, video.isShorts));
            }
        }

        mHistory.clear();
        mHistory.addAll(history);
        mHistoryVideos.clear();
        mHistoryVideos.putAll(videos);
        mHistoryFresh = true;
        saveHistoryCache();

        mLogHistory = true; // the next compute() writes one diagnostic line
        mApplyRetries = 0;
        applyHistory();
    }

    /**
     * Shows the new selection on Home, now if Home is on screen and the change can be made in place, else retried
     * (see {@link #updateInPlace}) or left to Home's next build / resume.
     */
    private void applyHistory() {
        Utils.removeCallbacks(mApplyHistory);
        BrowseView view = mView.get();
        if (view == null || mSection == null) {
            return;
        }
        BrowsePresenter presenter = BrowsePresenter.instance(mContext);
        if (!presenter.isHomeSection() || ViewManager.instance(mContext).getTopView() != BrowseView.class) {
            return; // Home's next build (or onHomeResumed) uses the latest history
        }
        if (!updateInPlace(view, mSection, presenter) && mApplyRetries++ < APPLY_MAX_RETRIES) {
            Utils.postDelayed(mApplyHistory, APPLY_RETRY_MS);
        }
    }

    private boolean mLogHistory;

    private List<Video> compute() {
        VideoStateService stateService = VideoStateService.instance(mContext);
        List<State> states = stateService != null && !stateService.isEmpty()
                ? new ArrayList<>(stateService.getStates()) : Collections.<State>emptyList();
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

        long now = System.currentTimeMillis();
        List<Pick> picks = OculoContinueWatchingRules.selectMerged(candidates, mHistory, mHidden, now);
        if (mLogHistory) {
            mLogHistory = false;
            int local48h = 0;
            for (Candidate c : candidates) {
                if (OculoContinueWatchingRules.isEligible(c, mHidden, now)) {
                    local48h++;
                }
            }
            Log.i(TAG, "history (" + (mHistoryFresh ? "fetched" : "cached") + ") items=" + mHistory.size()
                    + " unfinishedInTop" + OculoContinueWatchingRules.HISTORY_TOP + "=" + OculoContinueWatchingRules.countHistoryUnfinished(mHistory)
                    + " localLogged=" + candidates.size() + " localUnfinished48h=" + local48h
                    + " hidden=" + mHidden.size() + " shown=" + picks);
        }

        List<Video> result = new ArrayList<>();
        for (Pick pick : picks) {
            Video video;
            if (pick.fromHistory) {
                video = toCard(mHistoryVideos.get(pick.videoId));
            } else {
                State state = stateService != null ? stateService.getByVideoId(pick.videoId) : null;
                video = state != null ? toCard(state) : null;
            }
            if (video != null) {
                result.add(video);
            }
        }
        return result;
    }

    /**
     * A fresh card from the account's history: YouTube's progress (percent, resume position) is on the card, and
     * the player resumes there when Oculo has no newer local state (VideoStateController.restorePosition()).
     */
    private static Video toCard(Video historyVideo) {
        if (historyVideo == null) {
            return null;
        }
        Video video = Video.fromString(historyVideo.toString());
        if (video == null) {
            return null;
        }
        video.percentWatched = historyVideo.percentWatched;
        video.startTimeSeconds = historyVideo.startTimeSeconds;
        if (TextUtils.isEmpty(video.cardImageUrl)) {
            video.cardImageUrl = "https://i.ytimg.com/vi/" + video.videoId + "/hqdefault.jpg";
        }
        return video;
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
        group.setPosition(ROW_POSITION); // ACTION_PREPEND resets it to 0; the cards keep the row's position
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
