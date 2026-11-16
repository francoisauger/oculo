package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import android.content.Context;

import androidx.annotation.Nullable;

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
    public static final int[] TILE_ICONS = {R.drawable.settings_player, R.drawable.settings_subtitles,
            R.drawable.settings_main_ui, R.drawable.settings_account, R.drawable.settings_block,
            R.drawable.settings_cast, R.drawable.settings_about, R.drawable.settings_app};

    private static final List<Listener> sListeners = new ArrayList<>();

    private OculoSettingsTiles() {
    }

    public static boolean isEnabled(@Nullable Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_settings_tiles);
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
        OculoSettingsModel model = OculoSettingsModel.build(context);
        List<SettingsItem> items = new ArrayList<>();

        for (Tile tile : model.getVisibleTiles()) {
            String id = tile.getId();
            items.add(new SettingsItem(context.getString(tile.titleRes), () -> showTile(context, id), TILE_ICONS[tile.index]));
        }

        return items;
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
