package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import android.content.Context;

import androidx.annotation.Nullable;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.oculo.settings.OculoSettingsModel.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Oculo (0.11.0): the Settings section's categories ({@code AppDataSourceManager.getSettingItems()} hook, flag
 * {@code oculo_settings_tiles}) and their screens shown as upstream dialogs (used by the settings grid and from the
 * player). The list-and-detail Settings screen of the TV app ({@code OculoSettingsFragment}) uses the same model.
 */
public final class OculoSettingsTiles {
    public interface Listener {
        void onSettingsChanged();
    }

    /** Category icons (24dp outline set, docs/mockups/settings/settings-redesign.html « Icônes »). */
    public static final int[] TILE_ICONS = {R.drawable.oculo_settings_ic_play, R.drawable.oculo_settings_ic_subs,
            R.drawable.oculo_settings_ic_home, R.drawable.oculo_settings_ic_account, R.drawable.oculo_settings_ic_shield,
            R.drawable.oculo_settings_ic_remote, R.drawable.oculo_settings_ic_info, R.drawable.oculo_settings_ic_tune};

    private static final List<Listener> sListeners = new ArrayList<>();

    private OculoSettingsTiles() {
    }

    public static boolean isEnabled(@Nullable Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_settings_tiles);
    }

    /** The list-and-detail Settings screen replaces the tile grid (OculoSettingsFragment). */
    public static boolean isScreenEnabled(@Nullable Context context) {
        return isEnabled(context) && context.getResources().getBoolean(R.bool.oculo_settings_screen);
    }

    public static void addListener(Listener listener) {
        if (!sListeners.contains(listener)) {
            sListeners.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        sListeners.remove(listener);
    }

    /** Advanced mode turned on or off: the category list changes (Advanced appears / goes). */
    static void onAdvancedModeChanged(Context context) {
        if (!sListeners.isEmpty()) {
            for (Listener listener : new ArrayList<>(sListeners)) {
                listener.onSettingsChanged();
            }
            return;
        }

        // Settings grid: rebuild its tiles (Advanced appears / goes)
        BrowsePresenter browse = BrowsePresenter.instance(context);
        if (browse.isSettingsSection()) {
            browse.refresh(false);
        }
    }

    /** The categories as Settings tiles (the settings grid). */
    public static List<SettingsItem> getSettingItems(Context context) {
        List<SettingsItem> items = new ArrayList<>();

        if (isScreenEnabled(context)) {
            // The Settings screen builds its own model; the section only needs to be non-empty
            for (int i = 0; i < OculoSettingsMap.T_ADV; i++) {
                String id = OculoSettingsModel.TILE_IDS[i];
                items.add(new SettingsItem(context.getString(OculoSettingsModel.TILE_TITLES[i]), () -> showTile(context, id), TILE_ICONS[i]));
            }
            return items;
        }

        OculoSettingsModel model = OculoSettingsModel.build(context);

        if (!getRecentRows(context, model).isEmpty()) {
            items.add(new SettingsItem(context.getString(R.string.oculo_set_recent), () -> showRecent(context), R.drawable.oculo_settings_ic_recent));
        }

        for (Tile tile : model.getVisibleTiles()) {
            String id = tile.getId();
            items.add(new SettingsItem(context.getString(tile.titleRes), () -> showTile(context, id), TILE_ICONS[tile.index]));
        }

        return items;
    }

    /** The rows of « Recent » still visible in the current mode, most recent first. */
    public static List<OculoSettingsNode> getRecentRows(Context context, OculoSettingsModel model) {
        List<OculoSettingsNode> rows = new ArrayList<>();

        for (String key : OculoSettingsRecent.get(context)) {
            OculoSettingsNode node = model.findByKey(key);

            if (node != null) {
                rows.add(node);
            }
        }

        return rows;
    }

    private static void showRecent(Context context) {
        OculoSettingsModel model = OculoSettingsModel.build(context);
        AppDialogPresenter presenter = AppDialogPresenter.instance(context);
        appendRows(context, presenter, getRecentRows(context, model));
        presenter.showDialog(context.getString(R.string.oculo_set_recent), model::runOnFinish);
    }

    /** Top bar gear (OculoTopBar): the Settings section. */
    public static void openSettings(Context context) {
        BrowsePresenter.instance(context).selectSection(MediaGroup.TYPE_SETTINGS);
    }

    /**
     * « More settings » at the end of a dialog: the player's quick settings (Playback), the search filters (Settings
     * section). One line in each upstream dialog.
     */
    public static void appendMoreSettings(Context context, AppDialogPresenter presenter, boolean fromPlayer) {
        if (!isEnabled(context)) {
            return;
        }

        presenter.appendSingleButton(UiOptionItem.from(context.getString(R.string.oculo_set_more_settings), option -> {
            if (fromPlayer) {
                showTile(context, OculoSettingsModel.TILE_IDS[OculoSettingsMap.T_PLAY]);
            } else {
                presenter.closeDialog();
                openSettings(context);
            }
        }));
    }

    /** One category as an upstream dialog: its simple rows, then « Advanced » (advanced mode) opening the others. */
    public static void showTile(Context context, String tileId) {
        OculoSettingsModel model = OculoSettingsModel.build(context);
        Tile tile = model.getTile(tileId);

        if (tile == null) {
            return;
        }

        AppDialogPresenter presenter = AppDialogPresenter.instance(context);
        appendRows(context, presenter, tile.simple);

        if (model.isAdvancedMode() && !tile.advanced.isEmpty()) {
            if (tile.simple.isEmpty()) {
                appendRows(context, presenter, tile.advanced);
            } else {
                List<OculoSettingsNode> advanced = tile.advanced;
                presenter.appendSingleButton(UiOptionItem.from(context.getString(R.string.oculo_set_heading_advanced),
                        option -> showRows(context, context.getString(R.string.oculo_set_heading_advanced), advanced)));
            }
        }

        presenter.showDialog(context.getString(tile.titleRes), model::runOnFinish);
    }

    /** The category holding a row, opened as a dialog (« More settings » from the player). */
    public static void showTileOf(Context context, int tileIndex) {
        if (tileIndex >= 0 && tileIndex < OculoSettingsModel.TILE_IDS.length) {
            showTile(context, OculoSettingsModel.TILE_IDS[tileIndex]);
        }
    }

    static void showRows(Context context, CharSequence title, List<OculoSettingsNode> rows) {
        AppDialogPresenter presenter = AppDialogPresenter.instance(context);
        appendRows(context, presenter, rows);
        presenter.showDialog(title);
    }

    private static void appendRows(Context context, AppDialogPresenter presenter, List<OculoSettingsNode> rows) {
        for (OculoSettingsNode node : rows) {
            switch (node.kind) {
                case OculoSettingsNode.SWITCH:
                    if (node.item != null) {
                        presenter.appendSingleSwitch(node.item);
                    }
                    break;
                case OculoSettingsNode.ACTION:
                    if (node.item != null) {
                        presenter.appendSingleButton(node.item);
                    }
                    break;
                case OculoSettingsNode.PAGE:
                    List<OculoSettingsNode> children = node.children;
                    CharSequence title = node.title;
                    presenter.appendSingleButton(UiOptionItem.from(title, option -> showRows(context, title, children)));
                    break;
                default:
                    OptionCategory category = node.category;
                    if (category != null && category.options != null) {
                        presenter.appendCategory(category);
                    }
                    break;
            }
        }
    }
}
