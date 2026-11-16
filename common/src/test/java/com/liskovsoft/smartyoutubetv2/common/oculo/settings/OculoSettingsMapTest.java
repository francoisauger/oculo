package com.liskovsoft.smartyoutubetv2.common.oculo.settings;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Oculo (0.11.0): merge net for Oculo's Settings. Reads upstream's settings presenters as text and fails when one of
 * them uses a string (or a literal title) that {@link OculoSettingsMap} does not place and that is not listed below as
 * « not a setting » (values, messages, dialog titles). After a merge with upstream, a new setting therefore fails this
 * test until it gets a place in the map (it still shows meanwhile under Advanced › « Other settings » and in
 * « Classic settings »). Plain JVM test: no Android runtime needed.
 */
public class OculoSettingsMapTest {
    private static final String PRESENTERS = "src/main/java/com/liskovsoft/smartyoutubetv2/common/app/presenters/settings/";
    private static final String[] SOURCES = {
            PRESENTERS + "GeneralSettingsPresenter.java", PRESENTERS + "MainUISettingsPresenter.java",
            PRESENTERS + "PlayerSettingsPresenter.java", PRESENTERS + "AutoFrameRateSettingsPresenter.java",
            PRESENTERS + "SubtitleSettingsPresenter.java", PRESENTERS + "SearchSettingsPresenter.java",
            PRESENTERS + "SponsorBlockSettingsPresenter.java", PRESENTERS + "DeArrowSettingsPresenter.java",
            PRESENTERS + "BackupSettingsPresenter.java", PRESENTERS + "AboutSettingsPresenter.java",
            PRESENTERS + "RemoteControlSettingsPresenter.java", PRESENTERS + "LanguageSettingsPresenter.java",
            PRESENTERS + "AccountSettingsPresenter.java",
            "src/main/java/com/liskovsoft/smartyoutubetv2/common/oculo/OculoSettings.java"};
    private static final String MAP = "src/main/java/com/liskovsoft/smartyoutubetv2/common/oculo/settings/OculoSettingsMap.java";

    /** Strings of these files that are not settings: values, descriptions, messages, dialog titles, links. */
    private static final Set<String> NOT_SETTINGS = new HashSet<>(Arrays.asList(
            "action_channel", "action_debug_info", "action_dislike", "action_like", "action_next", "action_play_pause",
            "action_playback_queue", "action_playlist_add", "action_previous", "action_search", "action_sound_off",
            "action_subscribe", "action_subtitles", "action_video_info", "action_video_speed", "action_video_zoom",
            "add_remove_from_playback_queue", "add_remove_from_recent_playlist", "add_video_to_new_playlist",
            "add_video_to_watch_later", "alt_presets_behavior_desc", "amazon_frame_drop_fix_desc",
            "ambilight_ratio_fix_desc", "amlogic_fix_desc", "app_backup_desc", "app_backup_restore",
            "app_restore_desc", "audio_sync_fix_desc", "btn_confirm", "card_preview_full", "card_preview_muted",
            "card_title_lines_num", "child_mode_desc", "clear_history", "content_block_action_dialog",
            "content_block_action_none", "content_block_action_only_skip", "content_block_action_toast",
            "content_block_alt_server_desc", "content_block_provider", "content_block_provider_url",
            "content_block_status_url", "context_menu_sorting", "create_playlist", "dearrow_provider",
            "dearrow_provider_url", "dearrow_status_url", "default_lang", "default_option", "dialog_add_to_playlist",
            "dialog_block_channel", "dialog_main_ui", "dialog_notification", "dialog_search",
            "disable_network_error_fixing_desc", "disable_ok_long_press_desc", "disable_stream_buffer_desc",
            "disable_vsync_desc", "enable_conscrypt_desc", "enable_voice_search_desc", "force_legacy_codecs_desc",
            "force_sw_codec_desc", "item_postion", "live_stream_fix_4k_desc", "live_stream_fix_desc",
            "lost_setting_warning", "mark_as_watched", "move_section_down", "move_section_up", "msg_done",
            "msg_restart_app", "not_interested", "not_recommend_channel", "nothing_found", "once_a_day",
            "once_a_month", "once_a_week", "open_channel", "open_chat", "open_comments", "open_playlist",
            "option_disabled", "option_never", "pause_history", "pin_unpin_from_sidebar", "play_from_start",
            "play_next", "play_video", "play_video_incognito", "playback_notifications_fix_desc", "playback_settings",
            "player_global_focus_desc", "player_seek_confirmation_pause", "player_seek_confirmation_play",
            "player_seek_preview_carousel_fast", "player_seek_preview_carousel_slow", "player_seek_preview_none",
            "player_seek_preview_single", "player_seek_regular", "player_show_remaining_time",
            "player_time_stretching_desc", "playlist_order", "playlists_style", "playlists_style_grid",
            "playlists_style_rows", "prefer_avc_over_vp9_desc", "prefer_ipv4_desc", "prefer_sabr_live_desc",
            "releases_url", "remote_control_permission", "remove_from_history", "remove_from_subscriptions",
            "rename_playlist", "rename_section", "return_to_background_video", "run_in_background",
            "save_remove_playlist", "screen_dimming_timeout_min", "set_stream_reminder", "settings_general",
            "settings_language_country", "settings_player", "settings_search", "share_embed_link", "share_link",
            "share_qr_link", "sidebar_notification", "skip_codec_profile_check_desc", "sony_frame_drop_fix_desc",
            "sorting_alphabetically", "sorting_by_new_content", "sorting_last_viewed", "sources_url",
            "speech_recognizer_external_1", "speech_recognizer_external_2", "speech_recognizer_system",
            "subscribe_unsubscribe_from_channel", "subtitle_category_title", "thumb_quality_default",
            "thumb_quality_end", "thumb_quality_middle", "thumb_quality_start", "time_format_12",
            "tunneled_video_playback_desc", "ui_hide_timeout_sec", "unlock_all_formats_desc", "video_flip",
            "video_rotate"));

    /** Accounts: OculoSettingsModel's own « Account », « Account password » and « Ask which account at start » rows. */
    private static final Set<String> ACCOUNT_ROWS = new HashSet<>(Arrays.asList(
            "dialog_account_list", "dialog_account_none", "dialog_remove_account", "enter_account_password",
            "protect_account_with_password", "select_account_on_boot", "settings_accounts"));

    /** Literal titles that are values or dead code (DNS values, « OK -> » prefix, a commented-out switch). */
    private static final Set<String> NOT_SETTING_LITERALS = new HashSet<>(Arrays.asList(
            "IPv4 DNS", "Google DNS", "OK -> ", "Unlock more subtitles"));

    /**
     * Category builders the presenters call. Their categories are placed by title in the map; a new builder means a
     * new setting: place it, then add it here.
     */
    private static final Set<String> KNOWN_BUILDERS = new HashSet<>(Arrays.asList(
            "appendSeekIntervalDialogItems", "createAudioDelayCategory", "createAudioLanguageCategory",
            "createAudioVolumeCategory", "createBackgroundPlaybackCategory", "createExcludeFromContentBlockButton",
            "createIgnoreShortSegmentsCategory", "createNetworkEngineCategory", "createPlaybackModeCategory",
            "createPlayerScreenOffTimeoutCategory", "createRememberSpeedCategory", "createSleepTimerCategory",
            "createSpeedListCategory", "createSpeedMiscCategory", "createSubscriptionsBackupButton",
            "createSubtitleChannelOption", "createSubtitlePositionCategory", "createSubtitleSizeCategory",
            "createSubtitleStylesCategory", "createVideoBufferCategory", "createVideoPresetsCategory",
            "createVideoZoomCategory", "showConfirmationDialog", "createAutoFrameRateCategory",
            "createAutoFrameRateModesCategory", "createAutoFrameRatePauseCategory", "appendHomeCategory",
            "isTopButtonsCategoryHidden"));

    private static final Pattern STRING = Pattern.compile("R\\.string\\.(\\w+)");
    private static final Pattern LITERAL_TITLE = Pattern.compile("UiOptionItem\\.from\\(\\s*\"([^\"]+)\"");
    private static final Pattern BUILDER = Pattern.compile("\\b(?:AppDialogUtil|AutoFrameRateController|OculoSettings)\\.(\\w+)\\(");
    private static final Pattern MAP_LITERAL = Pattern.compile("(?:lit|concatPrefix)\\(\"([^\"]+)\"");

    @Test
    public void everyUpstreamSettingIsPlaced() throws IOException {
        Set<String> placed = matches(STRING, read(MAP));
        Set<String> placedLiterals = matches(MAP_LITERAL, read(MAP));
        Set<String> unplaced = new TreeSet<>();
        Set<String> unplacedLiterals = new TreeSet<>();
        Set<String> unknownBuilders = new TreeSet<>();

        for (String source : SOURCES) {
            String code = stripComments(read(source));

            for (String name : matches(STRING, code)) {
                if (!placed.contains(name) && !NOT_SETTINGS.contains(name) && !ACCOUNT_ROWS.contains(name)) {
                    unplaced.add(name + " (" + new File(source).getName() + ")");
                }
            }

            for (String literal : matches(LITERAL_TITLE, code)) {
                if (!placedLiterals.contains(literal) && !NOT_SETTING_LITERALS.contains(literal)) {
                    unplacedLiterals.add(literal + " (" + new File(source).getName() + ")");
                }
            }

            for (String builder : matches(BUILDER, code)) {
                if (!KNOWN_BUILDERS.contains(builder)) {
                    unknownBuilders.add(builder + " (" + new File(source).getName() + ")");
                }
            }
        }

        if (!unplaced.isEmpty() || !unplacedLiterals.isEmpty() || !unknownBuilders.isEmpty()) {
            fail("Upstream settings without a place in OculoSettingsMap (or not listed as « not a setting » in this test):"
                    + "\nstrings: " + unplaced + "\nliteral titles: " + unplacedLiterals + "\ncategory builders: " + unknownBuilders);
        }
    }

    @Test
    public void mapIsConsistent() {
        Map<Integer, Integer> simplePerTile = new HashMap<>();

        for (OculoSettingsMap.Entry entry : OculoSettingsMap.entries()) {
            assertTrue("tile " + entry.tile, entry.tile >= 0 && entry.tile < OculoSettingsMap.TILE_COUNT);
            assertTrue("source " + entry.source, entry.source >= 0 && entry.source < OculoSettingsMap.SOURCE_COUNT);
            assertTrue("an entry needs a category or an item", entry.cat != null || entry.item != null);

            if (entry.isShown()) {
                assertTrue("shown entry without label", entry.label != 0);
            } else {
                assertEquals("merged / left out entries have no label", 0, entry.label);
            }

            if (entry.page != 0) {
                assertTrue("pages are advanced", entry.mode != OculoSettingsMap.S);
            }

            if (entry.mode == OculoSettingsMap.S) {
                simplePerTile.put(entry.tile, simplePerTile.containsKey(entry.tile) ? simplePerTile.get(entry.tile) + 1 : 1);
                assertTrue("the Advanced category has no simple rows", entry.tile != OculoSettingsMap.T_ADV);
            }
        }

        for (int tile = 0; tile < OculoSettingsMap.T_ADV; tile++) {
            int simple = simplePerTile.containsKey(tile) ? simplePerTile.get(tile) : 0;
            // Account and System also get Oculo-made simple rows (Account; Advanced mode, About Oculo)
            assertTrue("tile " + tile + " has " + simple + " simple rows", simple >= 1 && simple <= 6);
        }
    }

    @Test
    public void qualityPrefersSdrVp9HighFrameRate() {
        assertTrue(OculoSettingsModel.qualityScore("1080p    60fps    vp9") > OculoSettingsModel.qualityScore("1080p    30fps    vp9"));
        assertTrue(OculoSettingsModel.qualityScore("1080p    30fps    vp9") > OculoSettingsModel.qualityScore("1080p    60fps    avc"));
        assertTrue(OculoSettingsModel.qualityScore("1080p    60fps    avc") > OculoSettingsModel.qualityScore("1080p    60fps    av01"));
        assertTrue(OculoSettingsModel.qualityScore("(4K) 2160p    30fps    vp9") > OculoSettingsModel.qualityScore("(4K) 2160p    60fps    vp9+hdr"));
    }

    private static Set<String> matches(Pattern pattern, String text) {
        Set<String> result = new HashSet<>();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            result.add(matcher.group(1));
        }

        return result;
    }

    private static String stripComments(String code) {
        return code.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    private static String read(String path) throws IOException {
        List<File> candidates = new ArrayList<>();
        candidates.add(new File(path));
        candidates.add(new File("common", path));

        for (File file : candidates) {
            if (file.isFile()) {
                return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            }
        }

        throw new IOException("Not found: " + path + " (from " + new File(".").getAbsolutePath() + ")");
    }
}
