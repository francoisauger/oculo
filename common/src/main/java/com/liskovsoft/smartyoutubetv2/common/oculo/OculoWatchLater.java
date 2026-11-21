package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.data.PlaylistInfo;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.lang.ref.WeakReference;
import java.util.List;

import io.reactivex.Observable;

/**
 * Oculo (0.14.0): « Add to Watch later » of the long-press menu with a confirmation. Upstream
 * ({@code MediaServiceManager.addToWatchLaterPlaylist()}) adds silently; here the same two calls (the video's playlist
 * list, whose first entry is Watch later, then « add to playlist ») and, when they complete, « Added to Watch later »
 * in Oculo's notice pill at the bottom right of the screen the menu was opened from (OculoSkipNotice), or a toast
 * when that screen has no window focus yet / any more. A failure says so.
 *
 * Add only: upstream notes that YouTube does not mark Watch later as « selected » in the playlist list
 * (MediaServiceManager: « BUG: YT not marked Watch later as selected »), so the menu cannot know cheaply whether the
 * video is already there and offer « Remove from Watch later ». Adding a video already there is harmless (YouTube
 * keeps one copy).
 *
 * Flag {@code R.bool.oculo_watch_later_first}: off = upstream's silent add.
 */
public final class OculoWatchLater {
    private static final String TAG = OculoWatchLater.class.getSimpleName();
    private static final long FOCUS_RETRY_MS = 150;
    private static final int FOCUS_RETRIES = 10; // the menu's activity closes first: up to 1.5 s

    private OculoWatchLater() {
    }

    public static void add(Context context, Video video) {
        if (video == null || video.videoId == null) {
            return;
        }

        if (context == null || !context.getResources().getBoolean(R.bool.oculo_watch_later_first)) {
            MediaServiceManager.instance().addToWatchLaterPlaylist(video);
            return;
        }

        WeakReference<Context> contextRef = new WeakReference<>(context);
        Context appContext = context.getApplicationContext();
        MediaItemService service = YouTubeServiceManager.instance().getMediaItemService();
        String videoId = video.videoId;

        Observable<Void> addObserve = service.getPlaylistsInfoObserve(videoId)
                .flatMap(infos -> {
                    PlaylistInfo watchLater = first(infos);
                    if (watchLater == null || watchLater.getPlaylistId() == null) {
                        return Observable.error(new IllegalStateException("No Watch later playlist"));
                    }
                    return service.addToPlaylistObserve(watchLater.getPlaylistId(), videoId);
                });

        RxHelper.execute(addObserve,
                null,
                error -> {
                    Log.e(TAG, "Add to Watch later error: %s", error.getMessage());
                    notify(contextRef, appContext, appContext.getString(R.string.oculo_watch_later_failed));
                },
                () -> notify(contextRef, appContext, appContext.getString(R.string.oculo_watch_later_added)));
    }

    private static PlaylistInfo first(List<PlaylistInfo> infos) {
        return infos != null && !infos.isEmpty() ? infos.get(0) : null;
    }

    private static void notify(WeakReference<Context> contextRef, Context appContext, String text) {
        new Handler(Looper.getMainLooper()).post(() -> notifyInt(contextRef, appContext, text, FOCUS_RETRIES));
    }

    private static void notifyInt(WeakReference<Context> contextRef, Context appContext, String text, int retries) {
        Context context = contextRef.get();
        Activity activity = context instanceof Activity ? (Activity) context : null;

        if (activity != null && !activity.isFinishing()) {
            if (OculoSkipNotice.showAtBottom(activity, text)) {
                return;
            }
            if (retries > 0 && !activity.hasWindowFocus()) { // the menu is still closing
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> notifyInt(contextRef, appContext, text, retries - 1), FOCUS_RETRY_MS);
                return;
            }
        }

        MessageHelpers.showMessage(appContext, text);
    }
}
