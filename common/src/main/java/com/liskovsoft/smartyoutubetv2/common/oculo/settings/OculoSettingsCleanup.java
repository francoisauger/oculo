package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Oculo (0.11.0, step 1 of docs/mockups/settings/settings-redesign.html): leaves out of upstream's settings screens
 * what does nothing on the TV and the second copies of duplicated settings. Upstream code stays in place (nothing is
 * deleted): {@code AppDialogPresenter.showDialog()} passes each screen through {@link #apply} before showing it.
 * Flag {@code oculo_settings_cleanup} (false upstream, true in the oculo flavor).
 *
 * Removed (see OCULO.md « Settings »):
 * - General › Misc « Open corresponding section when the app launched from ATV Channels » (Oculo has no ATV channels).
 * - User interface › Misc « Fullscreen mode (without system bars) » (a Google TV has no system bars to hide).
 * - Player › Developer options « Oculus Quest fix » (headset only) and « Hide Settings section (dangerous!) ».
 * - Search « Voice search engine » (only « System » works and Oculo's mic already uses it).
 * - Background playback « Play behind (Android TV 5,6,7) » values (the TV runs Google TV).
 * - Duplicates, one copy kept: « Unlocalized video titles » (kept in DeArrow), « Exit from the player: Double back » and
 *   « Exit from the search: Double back » (kept in General › Misc), « Background play while searching » (kept in
 *   Player › Misc), « Use separate settings per each account » (kept in General › Misc), the thumbnail frame (kept in
 *   User interface).
 * - Color scheme: only « Oculo » (Dark Grey) and « Oculo black (OLED) » (Dark Grey OLED); see {@link #themeIndex}.
 */
public final class OculoSettingsCleanup {
    /** Indexes in {@code MainUIData}'s color scheme list (initColorSchemes()). */
    public static final int THEME_DARK_GREY = 1;
    public static final int THEME_DARK_GREY_OLED = 3;

    private OculoSettingsCleanup() {
    }

    public static boolean isEnabled(@Nullable Context context) {
        return context != null && context.getResources().getBoolean(R.bool.oculo_settings_cleanup);
    }

    /**
     * Color scheme index as read from the stored preferences: the six schemes Oculo dropped fall back to « Oculo »
     * (Dark Grey). The stored value is left as it is until the next save of the UI settings, so going back to an older
     * build keeps the old choice. Indexes 1 and 3 are unchanged.
     */
    public static int themeIndex(@Nullable Context context, int index) {
        if (!isEnabled(context)) {
            return index;
        }

        return index == THEME_DARK_GREY || index == THEME_DARK_GREY_OLED ? index : THEME_DARK_GREY;
    }

    public static boolean isThemeKept(int index) {
        return index == THEME_DARK_GREY || index == THEME_DARK_GREY_OLED;
    }

    /**
     * Removes the dead and duplicated entries from a settings screen about to be shown. {@code categories} is the
     * presenter's own pending list; it is changed in place.
     */
    public static void apply(@Nullable Context context, @Nullable CharSequence dialogTitle, @Nullable List<OptionCategory> categories) {
        if (categories == null || categories.isEmpty() || !isEnabled(context)) {
            return;
        }

        Texts t = new Texts(context);

        for (int i = categories.size() - 1; i >= 0; i--) {
            OptionCategory category = categories.get(i);

            if (category == null) {
                continue;
            }

            if (isRemovedCategory(t, dialogTitle, category)) {
                categories.remove(i);
                continue;
            }

            List<OptionItem> kept = filterItems(t, dialogTitle, category);

            if (kept != null) {
                categories.set(i, OptionCategory.from(category.id, category.type, category.title, kept));
            }
        }
    }

    private static boolean isRemovedCategory(Texts t, CharSequence dialogTitle, OptionCategory category) {
        // Search › « Voice search engine »
        if (category.type == OptionCategory.TYPE_RADIO_LIST && t.eq(category.title, R.string.speech_engine)) {
            return true;
        }

        // DeArrow › thumbnail source: the same preference as User interface › « Where to grab card thumbnails »
        if (category.type == OptionCategory.TYPE_RADIO_LIST && t.eq(category.title, R.string.dearrow_not_submitted_thumbs)) {
            return true;
        }

        // Accounts › « Use separate settings per each account »: same switch as General › Misc
        if (category.type == OptionCategory.TYPE_SINGLE_SWITCH && category.options != null && category.options.size() == 1
                && t.eq(category.options.get(0).getTitle(), R.string.multi_profiles)) {
            return true;
        }

        return false;
    }

    /**
     * @return the items to keep, or null when the category is unchanged
     */
    private static List<OptionItem> filterItems(Texts t, CharSequence dialogTitle, OptionCategory category) {
        if (category.options == null || category.options.isEmpty()) {
            return null;
        }

        boolean changed = false;
        List<OptionItem> kept = new ArrayList<>(category.options.size());

        for (OptionItem item : category.options) {
            if (item != null && isRemovedItem(t, dialogTitle, category, item)) {
                changed = true;
            } else {
                kept.add(item);
            }
        }

        return changed ? kept : null;
    }

    private static boolean isRemovedItem(Texts t, CharSequence dialogTitle, OptionCategory category, OptionItem item) {
        CharSequence title = item.getTitle();

        if (category.type == OptionCategory.TYPE_RADIO_LIST) {
            // « Play behind (Android TV 5,6,7 - …) » values of Background playback (also the player's own dialog)
            if (t.eq(category.title, R.string.category_background_playback) && t.startsWith(title, R.string.option_background_playback_behind)) {
                return true;
            }

            // Color scheme: two Oculo themes
            if (t.eq(category.title, R.string.color_scheme)) {
                return !t.eq(title, R.string.color_scheme_dark_grey) && !t.eq(title, R.string.color_scheme_dark_grey_oled);
            }

            return false;
        }

        if (category.type != OptionCategory.TYPE_CHECKBOX_LIST) {
            return false;
        }

        boolean isMisc = t.eq(category.title, R.string.player_other);

        if (t.eq(dialogTitle, R.string.settings_general) && isMisc) {
            return t.eq(title, R.string.select_channel_section);
        }

        if (t.eq(dialogTitle, R.string.dialog_main_ui)) {
            if (isMisc) {
                return t.eq(title, R.string.card_unlocalized_titles) || t.eq(title, R.string.fullscreen_mode);
            }

            if (t.eq(category.title, R.string.cards_style)) {
                return t.eq(title, R.string.card_unlocalized_titles);
            }

            return false;
        }

        if (t.eq(dialogTitle, R.string.settings_player)) {
            if (isMisc) {
                return t.eqText(title, t.doubleBack(R.string.player_exit_shortcut));
            }

            if (t.eq(category.title, R.string.player_tweaks)) {
                return t.eq(title, R.string.oculus_quest_fix) || t.eq(title, R.string.hide_settings_section);
            }

            return false;
        }

        if (t.eq(dialogTitle, R.string.dialog_search) && isMisc) {
            return t.eqText(title, t.doubleBack(R.string.search_exit_shortcut)) || t.eq(title, R.string.search_background_playback);
        }

        return false;
    }

    /** Resolved strings, compared as plain text (titles may be spans). */
    static final class Texts {
        private final Context mContext;

        Texts(Context context) {
            mContext = context;
        }

        String get(int resId) {
            return mContext.getString(resId);
        }

        boolean eq(@Nullable CharSequence text, int resId) {
            return text != null && TextUtils.equals(text.toString(), get(resId));
        }

        boolean eqText(@Nullable CharSequence text, @Nullable String value) {
            return text != null && value != null && TextUtils.equals(text.toString(), value);
        }

        boolean startsWith(@Nullable CharSequence text, int resId) {
            return text != null && text.toString().startsWith(get(resId));
        }

        /** Upstream's « <shortcut>: Double back » item title (GeneralSettingsPresenter, PlayerSettingsPresenter…). */
        String doubleBack(int shortcutResId) {
            return get(shortcutResId) + ": " + get(R.string.app_double_back_exit);
        }
    }
}
