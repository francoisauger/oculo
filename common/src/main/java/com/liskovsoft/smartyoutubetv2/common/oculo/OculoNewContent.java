package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.Nullable;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.disposables.Disposable;

/**
 * Oculo: Abricot dot on the rail's Subscriptions icon when subscribed channels have new content the user has not
 * acknowledged yet (flag {@code R.bool.oculo_rail_new_content_dot}).
 *
 * <ul>
 *     <li>Signal: the Channels section's own request ({@code getSubscribedChannelsByNewContent}, the channels'
 *     {@code hasNewContent} flags). One request when Home first shows, then at most once per Home refresh interval
 *     ({@link OculoData#getHomeRefreshIntervalMs()}, 10 minutes in Oculo), only when the browse screen starts:
 *     nothing runs in the background or while Oculo is closed. Signed in only.</li>
 *     <li>Opening Subscriptions or Channels marks the currently flagged channels as seen (app prefs,
 *     {@link #KEY_SEEN}); seen ids no longer flagged are forgotten, so a channel that gets new content again brings
 *     the dot back. Rules in {@link OculoNewContentRules}.</li>
 *     <li>Failures are silent: no dot.</li>
 * </ul>
 */
public final class OculoNewContent {
    private static final String KEY_SEEN = "OculoSubsSeen";
    @SuppressLint("StaticFieldLeak")
    private static OculoNewContent sInstance;
    private final Context mContext;
    private final AppPrefs mPrefs;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Set<String> mSeen;
    @Nullable
    private Set<String> mFlagged;
    private long mLastRequestMs;
    private Disposable mAction;
    @Nullable
    private Runnable mListener;

    private OculoNewContent(Context context) {
        mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        mPrefs = AppPrefs.instance(mContext);
        mSeen = OculoNewContentRules.parse(mPrefs.getData(KEY_SEEN));
    }

    public static OculoNewContent instance(Context context) {
        if (sInstance == null) {
            sInstance = new OculoNewContent(context);
        }
        return sInstance;
    }

    public static boolean isEnabled(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_rail_new_content_dot);
    }

    /**
     * The rail: called (main thread) when the dot may have changed.
     */
    public void setListener(@Nullable Runnable listener) {
        mListener = listener;
    }

    /**
     * Called when the browse screen starts (first Home load, return to the app): one request when due.
     */
    public void refreshIfDue() {
        if (!isEnabled(mContext) || RxHelper.isAnyActionRunning(mAction)) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (mLastRequestMs != 0 && now - mLastRequestMs < OculoData.instance(mContext).getHomeRefreshIntervalMs()) {
            return;
        }

        if (MediaServiceManager.instance().getSelectedAccount() == null) {
            setFlagged(null);
            return;
        }

        mLastRequestMs = now;
        try {
            mAction = RxHelper.execute(
                    YouTubeServiceManager.instance().getContentService().getSubscribedChannelsByNewContentObserve(),
                    group -> mHandler.post(() -> setFlagged(collectFlagged(group))),
                    error -> mHandler.post(() -> setFlagged(null)));
        } catch (RuntimeException e) {
            setFlagged(null);
        }
    }

    @Nullable
    private static Set<String> collectFlagged(@Nullable MediaGroup group) {
        if (group == null) {
            return null;
        }
        Set<String> result = new LinkedHashSet<>();
        List<MediaItem> items = group.getMediaItems();
        if (items == null) {
            return result;
        }
        for (MediaItem item : items) {
            if (item != null && item.hasNewContent() && item.getChannelId() != null) {
                result.add(item.getChannelId());
            }
        }
        return result;
    }

    private void setFlagged(@Nullable Set<String> flagged) {
        boolean before = hasDot();
        mFlagged = flagged;
        if (flagged != null) {
            Set<String> pruned = OculoNewContentRules.prune(mSeen, flagged);
            if (!pruned.equals(mSeen)) {
                mSeen = pruned;
                persist();
            }
        }
        if (before != hasDot()) {
            notifyListener();
        }
    }

    public boolean hasDot() {
        return isEnabled(mContext) && OculoNewContentRules.hasUnseen(mFlagged, mSeen);
    }

    /**
     * Subscriptions or Channels was opened: the channels flagged now are acknowledged.
     */
    public void markSeen() {
        if (mFlagged == null || !OculoNewContentRules.hasUnseen(mFlagged, mSeen)) {
            return;
        }
        mSeen.addAll(mFlagged);
        persist();
        notifyListener();
    }

    private void persist() {
        mPrefs.setData(KEY_SEEN, OculoNewContentRules.format(mSeen));
    }

    private void notifyListener() {
        // Posted: the rail restyles its items, which may call markSeen() again
        mHandler.post(() -> {
            if (mListener != null) {
                mListener.run();
            }
        });
    }
}
