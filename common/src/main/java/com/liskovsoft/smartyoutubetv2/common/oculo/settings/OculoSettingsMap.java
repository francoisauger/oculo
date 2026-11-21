package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Oculo (0.11.0): where each upstream setting goes in Oculo's Settings (docs/mockups/settings/settings-redesign.html,
 * table « Ancien → nouveau »). One entry = one upstream setting, found in the screen an upstream presenter builds
 * ({@link OculoSettingsCapture}), by its title: the upstream string (same resource, so any language) or, for the key
 * remapping items that upstream does not translate, the literal title.
 *
 * Mode: {@link #S} shown in simple mode, {@link #A} only in advanced mode, {@link #M} merged into another entry (not
 * shown), {@link #X} left out by {@link OculoSettingsCleanup} (dead on the TV or a second copy). Entries of a page
 * (second argument) are grouped in one row that opens them. Order = order on screen.
 *
 * A setting upstream adds later and that no entry finds still shows, under Advanced › « Other settings », and in
 * « Classic settings »; {@code OculoSettingsMapTest} fails until it is placed here.
 * Generated from the proposal's table by docs/mockups/settings/gen_map.py (labels EN in values/oculo_settings.xml,
 * FR in the flavor's values-fr/strings.xml), then edited by hand.
 */
public final class OculoSettingsMap {
    // Tiles, in screen order
    public static final int T_PLAY = 0;
    public static final int T_AV = 1;
    public static final int T_HOME = 2;
    public static final int T_ACCT = 3;
    public static final int T_FILT = 4;
    public static final int T_REMOTE = 5;
    public static final int T_SYS = 6;
    public static final int T_ADV = 7;
    public static final int TILE_COUNT = 8;

    // Modes
    public static final char S = 'S';
    public static final char A = 'A';
    public static final char M = 'M';
    public static final char X = 'X';

    // Upstream screens (sources)
    public static final int GENERAL = 0;
    public static final int MAINUI = 1;
    public static final int PLAYER = 2;
    public static final int AFR = 3;
    public static final int SUBS = 4;
    public static final int SEARCH = 5;
    public static final int SB = 6;
    public static final int DEARROW = 7;
    public static final int BACKUP = 8;
    public static final int ABOUT = 9;
    public static final int REMOTE = 10;
    public static final int LANG = 11;
    public static final int SOURCE_COUNT = 12;

    // Flags
    /** Shown in the positive (« Disable search history » → « Search history », switch inverted). */
    public static final int INVERT = 1;
    /** « Video quality »: Auto, then one value per resolution (OculoSettingsModel.qualityChoices()). */
    public static final int QUALITY = 2;
    /** « More speeds »: Short / Long / Extra long instead of two switches. */
    public static final int SPEEDS = 4;

    // Value renames: {upstream value, Oculo value}
    public static final int[][] VAL_HISTORY = {
            {R.string.auto_history, R.string.oculo_val_follow_account},
            {R.string.enable_history, R.string.oculo_val_on},
            {R.string.disable_history, R.string.oculo_val_off}};
    public static final int[][] VAL_EXIT = {
            {R.string.app_exit_none, R.string.oculo_val_never},
            {R.string.app_double_back_exit, R.string.oculo_val_press_twice},
            {R.string.app_single_back_exit, R.string.oculo_val_press_once}};
    public static final int[][] VAL_PLAYBACK_MODE = {
            {R.string.repeat_mode_all, R.string.oculo_val_play_next},
            {R.string.repeat_mode_one, R.string.oculo_val_repeat_it},
            {R.string.repeat_mode_shuffle, R.string.oculo_val_shuffle},
            {R.string.repeat_mode_pause_alt, R.string.oculo_val_playlist_only},
            {R.string.repeat_mode_reverse_list, R.string.oculo_val_playlist_reverse},
            {R.string.repeat_mode_pause, R.string.oculo_val_pause},
            {R.string.repeat_mode_none, R.string.oculo_val_stop}};
    public static final int[][] VAL_OK = {
            {R.string.player_only_ui, R.string.oculo_val_show_controls},
            {R.string.player_ui_and_pause, R.string.oculo_val_controls_pause},
            {R.string.player_only_pause, R.string.oculo_val_pause},
            {R.string.player_toggle_speed, R.string.oculo_val_speed_toggle}};

    /** Resolved strings (cached by the caller: matching runs a few hundred thousand comparisons). */
    public interface Strings {
        String get(int resId);
    }

    /** Finds a title: an upstream string (equal, prefix…) or a literal. */
    public static final class Sel {
        static final int RES = 0;
        static final int PREFIX = 1;
        static final int FORMAT = 2;
        static final int LIT = 3;
        static final int CONCAT = 4;
        static final int CONCAT_PREFIX = 5;
        static final int CONCAT2 = 6;
        final int kind;
        final int res;
        final int res2;
        final String text;

        Sel(int kind, int res, int res2, String text) {
            this.kind = kind;
            this.res = res;
            this.res2 = res2;
            this.text = text;
        }

        public boolean matches(Strings strings, @Nullable CharSequence title) {
            if (title == null) {
                return false;
            }

            String t = title.toString();

            switch (kind) {
                case RES:
                    return TextUtils.equals(t, strings.get(res));
                case PREFIX:
                    return t.startsWith(strings.get(res));
                case FORMAT:
                    String raw = strings.get(res);
                    int cut = raw.indexOf('%');
                    return t.startsWith(cut >= 0 ? raw.substring(0, cut) : raw);
                case LIT:
                    return TextUtils.equals(t, text);
                case CONCAT:
                    return TextUtils.equals(t, strings.get(res) + text);
                case CONCAT_PREFIX:
                    return TextUtils.equals(t, text + strings.get(res));
                case CONCAT2:
                    return TextUtils.equals(t, strings.get(res) + text + strings.get(res2));
            }

            return false;
        }

        public boolean same(Sel other) {
            return other == this || (other != null && kind == other.kind && res == other.res && res2 == other.res2
                    && TextUtils.equals(text, other.text));
        }
    }

    static Sel res(int res) {
        return new Sel(Sel.RES, res, 0, null);
    }

    static Sel prefix(int res) {
        return new Sel(Sel.PREFIX, res, 0, null);
    }

    static Sel format(int res) {
        return new Sel(Sel.FORMAT, res, 0, null);
    }

    static Sel lit(String text) {
        return new Sel(Sel.LIT, 0, 0, text);
    }

    static Sel concat(int res, String suffix) {
        return new Sel(Sel.CONCAT, res, 0, suffix);
    }

    static Sel concatPrefix(String prefix, int res) {
        return new Sel(Sel.CONCAT_PREFIX, res, 0, prefix);
    }

    static Sel concat2(int res, String middle, int res2) {
        return new Sel(Sel.CONCAT2, res, res2, middle);
    }

    public static final class Entry {
        public final int tile;
        /** Label of the page (row that opens a group) this entry belongs to, or 0. */
        public final int page;
        /** Oculo label, or 0 for merged / left out entries. */
        public final int label;
        public final char mode;
        public final int source;
        /** Title of the upstream button whose sub-screen holds the setting (Screen dimming, Network…), or null. */
        public Sel sub;
        /** Upstream category (list) title, or null for a single switch / button. */
        public Sel cat;
        /** Item title inside the category (or the single switch / button), or null for the whole category. */
        public Sel item;
        public int flags;
        public int[][] values;

        Entry(int tile, int page, int label, char mode, int source) {
            this.tile = tile;
            this.page = page;
            this.label = label;
            this.mode = mode;
            this.source = source;
        }

        Entry sub(Sel sel) {
            sub = sel;
            return this;
        }

        Entry cat(Sel sel) {
            cat = sel;
            return this;
        }

        Entry item(Sel sel) {
            item = sel;
            return this;
        }

        Entry flags(int flags) {
            this.flags = flags;
            return this;
        }

        Entry values(int[][] values) {
            this.values = values;
            return this;
        }

        public boolean isShown() {
            return mode == S || mode == A;
        }

        public boolean has(int flag) {
            return (flags & flag) != 0;
        }
    }

    private static List<Entry> sEntries;

    private OculoSettingsMap() {
    }

    public static synchronized List<Entry> entries() {
        if (sEntries == null) {
            sEntries = Collections.unmodifiableList(build());
        }

        return sEntries;
    }

    private static List<Entry> sBuilding;

    private static Entry add(int tile, int page, int label, char mode, int source) {
        Entry entry = new Entry(tile, page, label, mode, source);
        sBuilding.add(entry);
        return entry;
    }

    private static List<Entry> build() {
        sBuilding = new ArrayList<>();

        // Playback
        add(T_PLAY, 0, R.string.oculo_set_when_a_video_ends, S, PLAYER).cat(res(R.string.action_repeat_mode)).values(VAL_PLAYBACK_MODE);
        add(T_PLAY, 0, R.string.oculo_set_video_quality, S, PLAYER).cat(res(R.string.title_video_presets)).flags(QUALITY);
        add(T_PLAY, 0, R.string.oculo_set_speed, S, PLAYER).sub(res(R.string.video_speed)).cat(res(R.string.video_speed));
        add(T_PLAY, 0, R.string.oculo_set_skip_step, S, PLAYER).cat(res(R.string.seek_interval));
        add(T_PLAY, 0, R.string.oculo_set_sleep_timer, S, PLAYER).cat(res(R.string.player_sleep_timer));
        add(T_PLAY, 0, R.string.oculo_set_pause_screen, S, MAINUI).item(res(R.string.oculo_pause_screen)); // 0.14.0, OculoSettings
        add(T_PLAY, 0, R.string.oculo_set_keep_playing_in_the_background, A, GENERAL).cat(res(R.string.category_background_playback));
        add(T_PLAY, 0, R.string.oculo_set_remember_speed, A, PLAYER).sub(res(R.string.video_speed)).cat(res(R.string.player_remember_speed));
        add(T_PLAY, 0, R.string.oculo_set_more_speeds, A, PLAYER).sub(res(R.string.video_speed)).cat(res(R.string.player_other)).item(res(R.string.player_long_speed_list)).flags(SPEEDS);
        add(T_PLAY, 0, 0, M, PLAYER).sub(res(R.string.video_speed)).cat(res(R.string.player_other)).item(res(R.string.player_extra_long_speed_list));
        add(T_PLAY, 0, R.string.oculo_set_classic_speed_button, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_speed_button_old_behavior));
        add(T_PLAY, 0, R.string.oculo_set_buffer_size, A, PLAYER).cat(res(R.string.video_buffer));
        add(T_PLAY, 0, R.string.oculo_set_picture_fit, A, PLAYER).cat(res(R.string.video_zoom));
        add(T_PLAY, 0, R.string.oculo_set_how_seeking_works, A, PLAYER).cat(res(R.string.player_seek_type));
        add(T_PLAY, 0, R.string.oculo_set_preview_while_seeking, A, PLAYER).cat(res(R.string.player_seek_preview));
        add(T_PLAY, 0, R.string.oculo_set_stay_at_the_live_edge, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.live_edge));
        add(T_PLAY, 0, R.string.oculo_set_resume_live_streams, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.remember_position_of_live_videos));
        add(T_PLAY, 0, R.string.oculo_set_resume_short_videos, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.remember_position_of_short_videos));
        add(T_PLAY, 0, R.string.oculo_set_play_the_section_as_a_playlist, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_section_playlist));
        add(T_PLAY, 0, R.string.oculo_set_notification_skips_to_next_chapter, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_chapter_notification2));
        add(T_PLAY, 0, R.string.oculo_set_keep_playing_while_searching, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.search_background_playback));
        add(T_PLAY, 0, R.string.oculo_set_loop_shorts, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_loop_shorts));
        add(T_PLAY, 0, R.string.oculo_set_queue_follows_when_a_video_ends, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.queue_respects_playback_mode));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_player_buttons, A, PLAYER).cat(res(R.string.player_buttons));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_hide_controls_after, A, PLAYER).cat(res(R.string.player_ui_hide_behavior));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_time_shown_under_the_bar, A, PLAYER).cat(res(R.string.player_show_ending_time));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_clock_in_the_corner, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.player_corner_clock));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_end_time_in_the_corner, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.player_corner_ending_time));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_suggestions_in_one_row, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.suggestions_horizontally_scrolled));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_suggestions_under_the_video, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_disable_suggestions)).flags(INVERT);
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_keep_video_size_behind_panels, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.dont_resize_video_to_fit_dialog));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_live_chat_on_the_left, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.place_chat_left));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_comments_on_the_left, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.place_comments_left));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_clock_under_the_bar, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_show_clock));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_quality_under_the_bar, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_show_quality_info));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_add_the_bitrate, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_show_quality_info_bitrate));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_keep_focus_column_between_rows, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_global_focus));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_show_controls_on_the_next_video, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_ui_on_next));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_player_animations, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_ui_animations));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_like_counts, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_likes_count));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_button_tooltips, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_show_tooltips));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_long_press_hints_on_buttons, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_button_long_click));
        add(T_PLAY, R.string.oculo_set_player_screen, R.string.oculo_set_channel_avatar_on_its_button, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.real_channel_icon));
        add(T_PLAY, R.string.oculo_set_frame_rate, R.string.oculo_set_match_frame_rate, A, AFR).cat(res(R.string.auto_frame_rate)).item(res(R.string.auto_frame_rate));
        add(T_PLAY, R.string.oculo_set_frame_rate, R.string.oculo_set_use_23_976_29_97_59_94, A, AFR).cat(res(R.string.auto_frame_rate)).item(format(R.string.frame_rate_correction));
        add(T_PLAY, R.string.oculo_set_frame_rate, R.string.oculo_set_also_match_resolution, A, AFR).cat(res(R.string.auto_frame_rate)).item(res(R.string.resolution_switch));
        add(T_PLAY, R.string.oculo_set_frame_rate, R.string.oculo_set_double_refresh_rate, A, AFR).cat(res(R.string.auto_frame_rate)).item(res(R.string.double_refresh_rate));
        add(T_PLAY, R.string.oculo_set_frame_rate, R.string.oculo_set_ignore_24_fps, A, AFR).cat(res(R.string.auto_frame_rate)).item(res(R.string.skip_24_rate));
        add(T_PLAY, R.string.oculo_set_frame_rate, R.string.oculo_set_not_for_shorts, A, AFR).cat(res(R.string.auto_frame_rate)).item(res(R.string.skip_shorts));
        add(T_PLAY, R.string.oculo_set_frame_rate, R.string.oculo_set_pause_while_switching, A, AFR).cat(res(R.string.auto_frame_rate_pause));

        // Audio & Subtitles
        add(T_AV, 0, R.string.oculo_set_audio_language, S, PLAYER).cat(res(R.string.audio_language));
        add(T_AV, 0, R.string.oculo_set_subtitle_style, S, SUBS).cat(res(R.string.subtitle_style));
        add(T_AV, 0, R.string.oculo_set_subtitle_size, S, SUBS).cat(res(R.string.subtitle_scale));
        add(T_AV, 0, R.string.oculo_set_subtitle_position, A, SUBS).cat(res(R.string.subtitle_position));
        add(T_AV, 0, R.string.oculo_set_remember_subtitles_per_channel, A, SUBS).item(res(R.string.subtitle_remember));
        add(T_AV, 0, R.string.oculo_set_audio_delay, A, PLAYER).cat(res(R.string.audio_shift));
        add(T_AV, 0, R.string.oculo_set_volume_boost, A, PLAYER).cat(res(R.string.player_volume));
        add(T_AV, 0, R.string.oculo_set_pause_when_another_app_plays_sound, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_audio_focus));
        add(T_AV, 0, R.string.oculo_set_even_out_loudness, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_auto_volume));
        add(T_AV, 0, R.string.oculo_set_natural_voice_at_other_speeds, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_time_stretching));

        // Home & Display
        add(T_HOME, 0, R.string.oculo_set_start_screen, S, GENERAL).cat(res(R.string.boot_to_section));
        add(T_HOME, 0, R.string.oculo_set_sidebar_sections, S, GENERAL).cat(res(R.string.side_panel_sections));
        add(T_HOME, 0, R.string.oculo_set_dim_the_screen_after, S, GENERAL).sub(res(R.string.screen_dimming)).cat(res(R.string.screen_dimming_timeout));
        add(T_HOME, 0, R.string.oculo_set_interface_size, S, MAINUI).cat(res(R.string.scale_ui));
        add(T_HOME, 0, R.string.oculo_set_hide_news_and_sports_rows, S, MAINUI).cat(res(R.string.header_home)).item(res(R.string.oculo_home_rows_filter));
        add(T_HOME, 0, R.string.oculo_set_show_continue_watching, S, MAINUI).cat(res(R.string.header_home)).item(res(R.string.oculo_home_continue_watching));
        add(T_HOME, 0, R.string.oculo_set_hide_watched_in_the_first_row, A, MAINUI).cat(res(R.string.header_home)).item(res(R.string.oculo_home_hide_watched));
        add(T_HOME, 0, R.string.oculo_set_glow_from_the_thumbnail, A, MAINUI).cat(res(R.string.header_home)).item(res(R.string.oculo_thumbnail_glow));
        add(T_HOME, 0, R.string.oculo_set_theme, A, MAINUI).cat(res(R.string.color_scheme));
        add(T_HOME, 0, R.string.oculo_set_card_size, A, MAINUI).cat(res(R.string.video_grid_scale));
        add(T_HOME, 0, R.string.oculo_set_24_hour_clock, A, MAINUI).cat(res(R.string.player_other)).item(concat2(R.string.time_format_24, " ", R.string.time_format));
        add(T_HOME, 0, R.string.oculo_set_clock_on_home, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.app_corner_clock));
        add(T_HOME, 0, R.string.oculo_set_dimming_level, A, GENERAL).sub(res(R.string.screen_dimming)).cat(res(R.string.screen_dimming_amount));
        add(T_HOME, 0, R.string.oculo_set_block_the_tv_screensaver, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.disable_screensaver));
        add(T_HOME, 0, R.string.oculo_set_preview_on_focus, A, MAINUI).cat(res(R.string.card_preview));
        add(T_HOME, 0, R.string.oculo_set_title_scroll_speed, A, MAINUI).cat(res(R.string.card_text_scroll_factor));
        add(T_HOME, 0, R.string.oculo_set_two_line_titles, A, MAINUI).cat(res(R.string.cards_style)).item(res(R.string.card_multiline_title));
        add(T_HOME, 0, R.string.oculo_set_two_line_details, A, MAINUI).cat(res(R.string.cards_style)).item(res(R.string.card_multiline_subtitle));
        add(T_HOME, 0, R.string.oculo_set_scroll_long_titles, A, MAINUI).cat(res(R.string.cards_style)).item(res(R.string.card_auto_scrolled_title));
        add(T_HOME, 0, R.string.oculo_set_rounded_corners, A, MAINUI).cat(res(R.string.cards_style)).item(res(R.string.rounded_card_corners));
        add(T_HOME, 0, R.string.oculo_set_long_press_menu_items, A, GENERAL).cat(res(R.string.context_menu));
        add(T_HOME, 0, R.string.oculo_set_sort_channels_by, A, MAINUI).cat(res(R.string.channels_section_sorting));
        add(T_HOME, 0, R.string.oculo_set_reopen_subscriptions_where_i_left_off, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.remember_position_subscriptions));
        add(T_HOME, 0, R.string.oculo_set_reopen_pinned_playlists_where_i_left_off, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.remember_position_pinned));
        add(T_HOME, 0, R.string.oculo_set_classic_channels_layout, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.channels_old_look));
        add(T_HOME, 0, R.string.oculo_set_pinned_channels_as_rows, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.pinned_channel_rows));
        add(T_HOME, 0, R.string.oculo_set_playlists_as_rows, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.playlists_rows));
        add(T_HOME, 0, R.string.oculo_set_channel_filter_field, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.channels_filter));
        add(T_HOME, 0, R.string.oculo_set_search_on_channel_pages, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.channel_search_bar));
        add(T_HOME, 0, R.string.oculo_set_load_channel_videos_automatically, A, MAINUI).cat(res(R.string.player_other)).item(res(R.string.channels_auto_load));

        // Account & History (the « Account » row, « Account password » and « Ask which account at start » are built by OculoSettingsModel)
        add(T_ACCT, 0, R.string.oculo_set_youtube_language, S, LANG).cat(prefix(R.string.dialog_select_language));
        add(T_ACCT, 0, R.string.oculo_set_content_country, S, LANG).cat(prefix(R.string.dialog_select_country));
        add(T_ACCT, 0, R.string.oculo_set_watch_history, S, GENERAL).cat(res(R.string.header_history)).values(VAL_HISTORY);
        add(T_ACCT, 0, R.string.oculo_set_search_history, S, SEARCH).cat(res(R.string.player_other)).item(res(R.string.disable_search_history)).flags(INVERT);
        add(T_ACCT, 0, R.string.oculo_set_pause_history_while_casting, A, REMOTE).cat(res(R.string.player_other)).item(res(R.string.disable_remote_history));
        add(T_ACCT, 0, R.string.oculo_set_separate_settings_per_account, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.multi_profiles));
        add(T_ACCT, 0, R.string.oculo_set_settings_password, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.protect_settings_with_password));
        add(T_ACCT, 0, R.string.oculo_set_master_password, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.enable_master_password));
        add(T_ACCT, 0, R.string.oculo_set_child_mode, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.child_mode));
        add(T_ACCT, 0, R.string.oculo_set_import_subscriptions, A, BACKUP).item(prefix(R.string.import_subscriptions_group));

        // SponsorBlock & Filters
        add(T_FILT, 0, R.string.oculo_set_hide_shorts_everywhere, S, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_shorts_everywhere));
        add(T_FILT, 0, R.string.oculo_set_hide_watched_in_subscriptions, S, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_watched_from_subscriptions)); // 0.13.0: simple mode
        add(T_FILT, 0, R.string.oculo_set_sponsorblock, S, SB).item(concat(R.string.enable, " SponsorBlock"));
        add(T_FILT, 0, R.string.oculo_set_segments, S, SB).cat(res(R.string.content_block_action_type));
        add(T_FILT, 0, R.string.oculo_set_show_on_progress_bar, A, SB).cat(res(R.string.sponsor_color_markers));
        add(T_FILT, 0, R.string.oculo_set_ignore_segments_shorter_than, A, SB).cat(res(R.string.ignore_short_segments));
        add(T_FILT, 0, R.string.oculo_set_paid_promotion_notice, A, SB).cat(res(R.string.player_other)).item(res(R.string.paid_content_notification));
        add(T_FILT, 0, R.string.oculo_set_skip_each_segment_only_once, A, SB).cat(res(R.string.player_other)).item(res(R.string.skip_each_segment_once));
        add(T_FILT, 0, R.string.oculo_set_not_for_this_channel, A, SB).item(res(R.string.content_block_exclude_channel));
        add(T_FILT, 0, R.string.oculo_set_not_for_this_channel, A, SB).item(res(R.string.content_block_stop_excluding_channel));
        add(T_FILT, 0, R.string.oculo_set_server_status, A, SB).item(res(R.string.content_block_status));
        add(T_FILT, 0, R.string.oculo_set_about_sponsorblock, A, SB).item(res(R.string.about_sponsorblock));
        add(T_FILT, 0, R.string.oculo_set_hide_mixes, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_mixes));
        add(T_FILT, 0, R.string.oculo_set_hide_live_streams_in_subscriptions, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_streams));
        add(T_FILT, 0, R.string.oculo_set_remove_watched_from_watch_later, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_watched_from_watch_later));
        add(T_FILT, 0, R.string.oculo_set_hide_watched_on_home, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_watched_from_home));
        add(T_FILT, 0, R.string.oculo_set_hide_watched_in_notifications, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_watched_from_notifications));
        add(T_FILT, R.string.oculo_set_hide_shorts_in, R.string.oculo_set_subscriptions, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_shorts));
        add(T_FILT, R.string.oculo_set_hide_shorts_in, R.string.oculo_set_search_results, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_shorts_from_search));
        add(T_FILT, R.string.oculo_set_hide_shorts_in, R.string.oculo_set_home, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_shorts_from_home));
        add(T_FILT, R.string.oculo_set_hide_shorts_in, R.string.oculo_set_channel_pages, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_shorts_channel));
        add(T_FILT, R.string.oculo_set_hide_shorts_in, R.string.oculo_set_history, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_shorts_from_history));
        add(T_FILT, R.string.oculo_set_hide_shorts_in, R.string.oculo_set_trending, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_shorts_from_trending));
        add(T_FILT, R.string.oculo_set_hide_upcoming_in, R.string.oculo_set_subscriptions, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_upcoming));
        add(T_FILT, R.string.oculo_set_hide_upcoming_in, R.string.oculo_set_home, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_upcoming_home));
        add(T_FILT, R.string.oculo_set_hide_upcoming_in, R.string.oculo_set_channel_pages, A, GENERAL).cat(res(R.string.hide_unwanted_content)).item(res(R.string.hide_upcoming_channel));
        add(T_FILT, 0, R.string.oculo_set_original_titles, A, DEARROW).item(res(R.string.card_unlocalized_titles));
        add(T_FILT, 0, R.string.oculo_set_better_titles_dearrow, A, DEARROW).item(res(R.string.crowdsoursed_titles));
        add(T_FILT, 0, R.string.oculo_set_better_thumbnails_dearrow, A, DEARROW).item(res(R.string.crowdsourced_thumbnails));
        add(T_FILT, 0, R.string.oculo_set_thumbnail_frame, A, MAINUI).cat(res(R.string.card_content));
        add(T_FILT, 0, R.string.oculo_set_dearrow_server_status, A, DEARROW).item(res(R.string.dearrow_status));
        add(T_FILT, 0, R.string.oculo_set_about_dearrow, A, DEARROW).item(res(R.string.about_dearrow));

        // Remote & Devices
        add(T_REMOTE, 0, R.string.oculo_set_phone_remote, S, REMOTE).item(res(R.string.settings_remote_control));
        add(T_REMOTE, 0, R.string.oculo_set_link_a_phone, S, REMOTE).item(res(R.string.dialog_add_device));
        add(T_REMOTE, 0, R.string.oculo_set_show_in_the_cast_list, A, REMOTE).item(res(R.string.remote_control_dial));
        add(T_REMOTE, 0, R.string.oculo_set_unlink_all_phones, A, REMOTE).cat(res(R.string.dialog_remove_all_devices));
        add(T_REMOTE, 0, R.string.oculo_set_close_oculo_when_the_phone_disconnects, A, REMOTE).cat(res(R.string.player_other)).item(res(R.string.finish_on_disconnect));
        add(T_REMOTE, 0, R.string.oculo_set_back_on_home_leaves_oculo, A, GENERAL).cat(res(R.string.app_exit_shortcut)).values(VAL_EXIT);
        add(T_REMOTE, 0, R.string.oculo_set_press_back_twice_to_leave_a_video, A, GENERAL).cat(res(R.string.player_other)).item(concat2(R.string.player_exit_shortcut, ": ", R.string.app_double_back_exit));
        add(T_REMOTE, 0, R.string.oculo_set_press_back_twice_to_leave_search, A, GENERAL).cat(res(R.string.player_other)).item(concat2(R.string.search_exit_shortcut, ": ", R.string.app_double_back_exit));
        add(T_REMOTE, 0, R.string.oculo_set_back_returns_to_the_tv_home_after_a_google_tv_sear, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.return_to_launcher));
        add(T_REMOTE, 0, R.string.oculo_set_ok_button_during_a_video, A, PLAYER).cat(res(R.string.player_ok_button_behavior)).values(VAL_OK);
        add(T_REMOTE, 0, R.string.oculo_set_ok_long_press, A, GENERAL).cat(res(R.string.player_other)).item(res(R.string.disable_ok_long_press)).flags(INVERT);
        add(T_REMOTE, 0, R.string.oculo_set_number_keys_jump_1_10_pct, A, PLAYER).cat(res(R.string.player_other)).item(res(R.string.player_number_key_seek));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, 0, M, GENERAL).cat(res(R.string.key_remapping)).item(concatPrefix("OK -> ", R.string.player_toggle_speed));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_up_down_skips_shorts, A, GENERAL).cat(res(R.string.key_remapping)).item(res(R.string.player_quick_shorts_skip_alt));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_left_right_skips_shorts, A, GENERAL).cat(res(R.string.key_remapping)).item(res(R.string.player_quick_shorts_skip));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_up_down_skips_videos, A, GENERAL).cat(res(R.string.key_remapping)).item(res(R.string.player_quick_skip_videos_alt));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_left_right_skips_videos, A, GENERAL).cat(res(R.string.key_remapping)).item(res(R.string.player_quick_skip_videos));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_play_pause_acts_as_ok, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Play/Pause -> OK"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_left_right_volume, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("DPAD RIGHT/LEFT -> Volume Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_up_down_volume, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("DPAD UP/DOWN -> Volume Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_up_down_speed, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("DPAD UP/DOWN -> Speed Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_3_1_speed, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Numbers 3/1 -> Speed Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_next_previous_seek, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Next/Previous -> Fast Forward/Rewind"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_next_previous_speed, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Next/Previous -> Speed Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_fast_forward_rewind_next_previous, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Fast Forward/Rewind -> Next/Previous"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_fast_forward_rewind_speed, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Fast Forward/Rewind -> Speed Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_fast_forward_rewind_speed_toggle, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Fast Forward/Rewind -> Speed Toggle"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_s_key_speed_toggle, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("S -> Speed Toggle"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_page_up_down_next_previous, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Page Up/Down -> Next/Previous"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_page_up_down_like_dislike, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Page Up/Down -> Like/Dislike"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_page_up_down_speed, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Page Up/Down -> Speed Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_page_up_down_speed_reversed, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Page Up/Down -> Speed Down/Up"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_channel_volume, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Channel Up/Down -> Volume Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_channel_next_previous, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Channel Up/Down -> Next/Previous"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_channel_like_dislike, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Channel Up/Down -> Like/Dislike"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_channel_speed, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Channel Up/Down -> Speed Up/Down"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_channel_speed_toggle, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Channel Up/Down -> Speed Toggle"));
        add(T_REMOTE, R.string.oculo_set_button_shortcuts, R.string.oculo_set_channel_search, A, GENERAL).cat(res(R.string.key_remapping)).item(lit("Channel Up/Down -> Search"));
        add(T_REMOTE, 0, R.string.oculo_set_auto_correction, A, SEARCH).cat(res(R.string.player_other)).item(res(R.string.typing_corrections)).flags(INVERT);
        add(T_REMOTE, 0, R.string.oculo_set_search_as_soon_as_i_stop_talking, A, SEARCH).cat(res(R.string.player_other)).item(res(R.string.instant_voice_search));
        add(T_REMOTE, 0, R.string.oculo_set_jump_to_results, A, SEARCH).cat(res(R.string.player_other)).item(res(R.string.focus_on_search_results));
        add(T_REMOTE, 0, R.string.oculo_set_open_the_keyboard_automatically, A, SEARCH).cat(res(R.string.player_other)).item(res(R.string.keyboard_auto_show));
        add(T_REMOTE, 0, R.string.oculo_set_ok_doesnt_open_the_keyboard_g20s_remotes, A, SEARCH).cat(res(R.string.player_other)).item(res(R.string.keyboard_fix));

        // System (« Advanced mode » and « About Oculo » are built by OculoSettingsModel)
        add(T_SYS, 0, R.string.oculo_set_back_up_settings, S, BACKUP).cat(res(R.string.local_backup)).item(prefix(R.string.app_backup));
        add(T_SYS, 0, R.string.oculo_set_restore_settings, S, BACKUP).cat(res(R.string.local_backup)).item(prefix(R.string.app_restore));
        add(T_SYS, 0, R.string.oculo_set_check_for_updates, S, ABOUT).item(res(R.string.check_for_updates));
        add(T_SYS, 0, R.string.oculo_set_whats_new, S, ABOUT).cat(prefix(R.string.update_changelog));
        add(T_SYS, 0, R.string.oculo_set_automatic_backup, A, BACKUP).cat(res(R.string.local_backup)).item(res(R.string.auto_backup_category));
        add(T_SYS, 0, R.string.oculo_set_back_up_to_google_drive, A, BACKUP).sub(lit("Google Drive")).item(res(R.string.app_backup));
        add(T_SYS, 0, R.string.oculo_set_restore_from_google_drive, A, BACKUP).sub(lit("Google Drive")).item(res(R.string.app_restore));
        add(T_SYS, 0, R.string.oculo_set_automatic_drive_backup, A, BACKUP).sub(lit("Google Drive")).item(res(R.string.auto_backup_category));
        add(T_SYS, 0, R.string.oculo_set_drive_backup_for_this_tv_only, A, BACKUP).sub(lit("Google Drive")).item(res(R.string.device_specific_backup));
        add(T_SYS, 0, R.string.oculo_set_google_drive_account, A, BACKUP).sub(lit("Google Drive")).item(res(R.string.dialog_add_account));
        add(T_SYS, 0, R.string.oculo_set_update_notices, A, ABOUT).cat(res(R.string.check_updates_auto));
        add(T_SYS, 0, R.string.oculo_set_update_source, A, ABOUT).cat(res(R.string.preferred_update_source));
        add(T_SYS, 0, R.string.oculo_set_feedback, A, ABOUT).cat(res(R.string.feedback));
        add(T_SYS, 0, R.string.oculo_set_releases, A, ABOUT).item(res(R.string.releases));
        add(T_SYS, 0, R.string.oculo_set_source_code, A, ABOUT).item(res(R.string.sources));

        // Advanced (also « Other settings » and « Classic settings », built by OculoSettingsModel)
        add(T_ADV, R.string.oculo_set_network, R.string.oculo_set_automatic_network_repair, A, GENERAL).sub(res(R.string.network_settings)).item(res(R.string.disable_network_error_fixing)).flags(INVERT);
        add(T_ADV, R.string.oculo_set_network, R.string.oculo_set_web_proxy, A, GENERAL).sub(res(R.string.network_settings)).item(res(R.string.enable_web_proxy));
        add(T_ADV, R.string.oculo_set_network, R.string.oculo_set_conscrypt_tls, A, GENERAL).sub(res(R.string.network_settings)).item(res(R.string.enable_conscrypt));
        add(T_ADV, R.string.oculo_set_network, R.string.oculo_set_dns, A, GENERAL).sub(res(R.string.network_settings)).cat(res(R.string.preferred_dns));
        add(T_ADV, R.string.oculo_set_network, R.string.oculo_set_network_engine, A, GENERAL).sub(res(R.string.network_settings)).cat(res(R.string.player_network_stack));
        add(T_ADV, R.string.oculo_set_network, R.string.oculo_set_sponsorblock_alternative_server, A, SB).cat(res(R.string.player_other)).item(res(R.string.content_block_alt_server));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_exact_video_format, A, PLAYER).cat(res(R.string.title_video_presets));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_high_bitrate_1080p, A, PLAYER).cat(res(R.string.player_tweaks)).item(prefix(R.string.unlock_high_bitrate_formats));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_high_bitrate_aac_audio, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.unlock_high_bitrate_audio_formats));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_legacy_codecs_360p_max, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.force_legacy_codecs));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_unlock_all_formats, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.unlock_all_formats));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_quality_by_bandwidth, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.alt_presets_behavior));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_prefer_avc_1080p_max, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.prefer_avc_over_vp9));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_skip_codec_check, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.skip_codec_profile_check));
        add(T_ADV, R.string.oculo_set_video_formats, R.string.oculo_set_software_decoder, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.force_sw_codec));
        add(T_ADV, R.string.oculo_set_live_streams, R.string.oculo_set_sabr_live_streams, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.prefer_sabr_live));
        add(T_ADV, R.string.oculo_set_live_streams, R.string.oculo_set_live_fix_hls_1080p, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.live_stream_fix));
        add(T_ADV, R.string.oculo_set_live_streams, R.string.oculo_set_live_fix_dash_4k, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.live_stream_fix_4k));
        add(T_ADV, R.string.oculo_set_live_streams, R.string.oculo_set_no_buffer_on_live_streams, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.disable_stream_buffer));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_playback_notifications, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.playback_notifications_fix)).flags(INVERT);
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_audio_sync_fix, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.audio_sync_fix));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_ambilight_and_aspect_fix, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.ambilight_ratio_fix));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_amlogic_1080p60_fix, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.amlogic_fix));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_tunneled_playback, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.tunneled_video_playback));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_no_vsync_alignment, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.disable_vsync));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_frame_drop_fix_sony, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.sony_frame_drop_fix));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_frame_drop_fix_amazon, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.amazon_frame_drop_fix));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_keep_finished_activities, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.keep_finished_activities));
        add(T_ADV, R.string.oculo_set_device_fixes, R.string.oculo_set_fix_empty_subscriptions, A, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.fix_empty_subs_and_channels));
        add(T_ADV, R.string.oculo_set_display, R.string.oculo_set_pixel_ratio, A, PLAYER).cat(res(R.string.player_pixel_ratio));
        add(T_ADV, R.string.oculo_set_display, R.string.oculo_set_display_modes, A, AFR).cat(res(R.string.auto_frame_rate_modes));

        // Left out by OculoSettingsCleanup (dead on the TV or second copies); listed so the mapping test knows them
        add(T_ADV, 0, 0, X, GENERAL).cat(res(R.string.player_other)).item(res(R.string.select_channel_section));
        add(T_ADV, 0, 0, X, MAINUI).cat(res(R.string.various_buttons));
        add(T_ADV, 0, 0, X, MAINUI).cat(res(R.string.player_other)).item(res(R.string.fullscreen_mode));
        add(T_ADV, 0, 0, X, MAINUI).cat(res(R.string.player_other)).item(res(R.string.card_unlocalized_titles));
        add(T_ADV, 0, 0, X, MAINUI).cat(res(R.string.cards_style)).item(res(R.string.card_unlocalized_titles));
        add(T_ADV, 0, 0, X, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.oculus_quest_fix));
        add(T_ADV, 0, 0, X, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.hide_settings_section));
        add(T_ADV, 0, 0, X, PLAYER).cat(res(R.string.player_tweaks)).item(res(R.string.disable_channels_service));
        add(T_ADV, 0, 0, X, PLAYER).cat(res(R.string.player_other)).item(concat2(R.string.player_exit_shortcut, ": ", R.string.app_double_back_exit));
        add(T_ADV, 0, 0, X, SEARCH).cat(res(R.string.speech_engine));
        add(T_ADV, 0, 0, X, SEARCH).cat(res(R.string.player_other)).item(concat2(R.string.search_exit_shortcut, ": ", R.string.app_double_back_exit));
        add(T_ADV, 0, 0, X, SEARCH).cat(res(R.string.player_other)).item(res(R.string.search_background_playback));
        add(T_ADV, 0, 0, X, DEARROW).cat(res(R.string.dearrow_not_submitted_thumbs));
        add(T_ADV, 0, 0, X, ABOUT).item(res(R.string.install_bridge));
        add(T_ADV, 0, 0, X, ABOUT).cat(res(R.string.donation));

        List<Entry> result = sBuilding;
        sBuilding = null;
        return result;
    }
}
