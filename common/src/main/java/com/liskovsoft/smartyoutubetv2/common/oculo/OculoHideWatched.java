package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData;

/**
 * Oculo (0.13.0): upstream's « Hide watched videos from Subscriptions » (Settings › General › hide content,
 * {@code MediaServiceData.CONTENT_WATCHED_SUBSCRIPTIONS}, applied by MediaServiceCore when the feed is fetched) in
 * one step: the first item of the long-press menu of a Subscriptions card is « Hide watched videos » or « Show
 * watched videos », and Subscriptions reloads right after (flag {@code R.bool.oculo_subs_hide_watched_menu}).
 * The same setting is in Oculo's Settings, simple mode (SponsorBlock &amp; Filters). Nothing is reimplemented.
 */
public final class OculoHideWatched {
    private OculoHideWatched() {
    }

    public static boolean isHidden() {
        return MediaServiceData.instance().isContentHidden(MediaServiceData.CONTENT_WATCHED_SUBSCRIPTIONS);
    }

    /** VideoMenuPresenter: first item when the long press is on a Subscriptions card. */
    public static void appendMenuItem(Context context, AppDialogPresenter dialogPresenter, Video video) {
        if (context == null || dialogPresenter == null || !context.getResources().getBoolean(R.bool.oculo_subs_hide_watched_menu)
                || !OculoNewVideos.isSubscriptionsVideo(video)) {
            return;
        }

        boolean hidden = isHidden();
        dialogPresenter.appendSingleButton(UiOptionItem.from(
                context.getString(hidden ? R.string.oculo_subs_show_watched : R.string.oculo_subs_hide_watched),
                optionItem -> {
                    MediaServiceData.instance().setContentHidden(MediaServiceData.CONTENT_WATCHED_SUBSCRIPTIONS, !hidden);
                    dialogPresenter.closeDialog();
                    BrowsePresenter.instance(context).refresh(true);
                }));
    }
}
