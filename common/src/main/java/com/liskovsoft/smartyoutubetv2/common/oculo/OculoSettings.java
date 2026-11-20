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
    /**
     * « Buttons at the top of the main window » (Search, Language, Accounts) has no effect with Oculo's rail and top
     * bar: NavigateTitleView hides the search and account orbs, and the language orb would sit after the Search pill
     * where the D-pad never reaches it (OculoTopBar), so it is hidden too. The category is left out (32.74-18).
     */
    public static boolean isTopButtonsCategoryHidden(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_hide_top_buttons_setting);
    }

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

        // 0.14.0: placed in Playback by OculoSettingsMap
        settingsPresenter.appendSingleSwitch(UiOptionItem.from(context.getString(R.string.oculo_pause_screen),
                option -> data.setPauseScreenEnabled(option.isSelected()),
                data.isPauseScreenEnabled()));
    }
}
