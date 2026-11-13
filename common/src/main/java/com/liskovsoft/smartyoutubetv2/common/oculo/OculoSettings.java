package com.liskovsoft.smartyoutubetv2.common.oculo;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;

import java.util.ArrayList;
import java.util.List;

/**
 * Oculo: settings added to the "Main UI" screen (MainUISettingsPresenter). Changes apply the next time Home loads
 * (the thumbnail glow: on the next focused card).
 */
public class OculoSettings {
    public static void appendHomeCategory(Context context, AppDialogPresenter settingsPresenter) {
        OculoData data = OculoData.instance(context);
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(context.getString(R.string.oculo_home_rows_filter),
                option -> data.setHomeRowsFilterEnabled(option.isSelected()),
                data.isHomeRowsFilterEnabled()));

        options.add(UiOptionItem.from(context.getString(R.string.oculo_home_hide_watched),
                option -> data.setHomeHideWatchedEnabled(option.isSelected()),
                data.isHomeHideWatchedEnabled()));

        options.add(UiOptionItem.from(context.getString(R.string.oculo_home_continue_watching),
                option -> data.setHomeContinueWatchingEnabled(option.isSelected()),
                data.isHomeContinueWatchingEnabled()));
        options.add(UiOptionItem.from(context.getString(R.string.oculo_thumbnail_glow),
                option -> data.setThumbnailGlowEnabled(option.isSelected()),
                data.isThumbnailGlowEnabled()));

        settingsPresenter.appendCheckedCategory(context.getString(R.string.header_home), options);
    }
}
