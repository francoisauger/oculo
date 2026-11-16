#!/usr/bin/env python3
"""Generates the Oculo settings map (Java entries) and its EN/FR strings from one spec (0.11.0).
Output in $OUT (default /tmp): entries.java.txt (the add(...) lines of OculoSettingsMap.build()),
strings_en.xml.txt (common/src/main/res/values/oculo_settings.xml) and strings_fr.xml.txt (flavor values-fr).
fr.json = EN -> FR labels from settings-redesign.html. The repo files were edited by hand after generation."""
import json, re, sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
FR = json.load(open(os.path.join(HERE, 'fr.json')))
FR.update({
 'Hide Shorts in…': 'Masquer les Shorts dans…', 'Hide upcoming in…': 'Masquer les vidéos à venir dans…',
 'Button shortcuts': 'Raccourcis des boutons', 'Player screen': 'Écran du lecteur', 'Frame rate': "Fréquence d'image",
 'Network': 'Réseau', 'Video formats': 'Formats vidéo', 'Device fixes': "Correctifs d'appareils", 'Live streams': 'Directs',
 'Display': 'Affichage', 'Exact video format': 'Format vidéo exact', 'Show on progress bar': 'Montrer sur la barre',
 'Advanced mode': 'Mode avancé', 'About Oculo': "À propos d'Oculo", 'Classic settings': 'Réglages classiques',
 'Recent': 'Récents', 'Other settings': 'Autres réglages', 'Account password': 'Mot de passe du compte',
 'Ask which account at start': 'Choisir le compte au démarrage', 'Account': 'Compte',
})
T = {'play':'T_PLAY','av':'T_AV','home':'T_HOME','acct':'T_ACCT','filt':'T_FILT','remote':'T_REMOTE','sys':'T_SYS','adv':'T_ADV'}

def slug(s):
    s = s.replace('«','').replace('»','').replace('…','').replace('&','and').replace('%','pct').replace("'",'')
    s = re.sub(r'[^A-Za-z0-9]+', '_', s).strip('_').lower()
    return ('oculo_set_' + s)[:60].rstrip('_')

labels = {}
def lab(en):
    name = slug(en)
    if name in labels and labels[name] != en:
        raise SystemExit('slug clash: %s %s %s' % (name, labels[name], en))
    labels[name] = en
    if en not in FR:
        raise SystemExit('no FR for: ' + en)
    return 'R.string.' + name

def sel(s):
    if s is None: return None
    kind, _, v = s.partition(':')
    if kind == 'r': return 'res(R.string.%s)' % v
    if kind == 'p': return 'prefix(R.string.%s)' % v
    if kind == 'f': return 'format(R.string.%s)' % v
    if kind == 'l': return 'lit(%s)' % json.dumps(v)
    if kind == 'cs': a, b = v.split('|'); return 'concat(R.string.%s, %s)' % (a, json.dumps(b))
    if kind == 'pc': a, b = v.split('|'); return 'concatPrefix(%s, R.string.%s)' % (json.dumps(a), b)
    if kind == 'c2': a, m, b = v.split('|'); return 'concat2(R.string.%s, %s, R.string.%s)' % (a, json.dumps(m), b)
    raise SystemExit('bad sel ' + s)

out = []
pages = {}
def E(tile, page, label, mode, src, cat=None, item=None, sub=None, flags=None, values=None):
    line = 'add(%s, %s, %s, %s, %s)' % (T[tile], lab(page) if page else '0', lab(label) if label else '0', mode, src)
    if sub: line += '.sub(%s)' % sel(sub)
    if cat: line += '.cat(%s)' % sel(cat)
    if item: line += '.item(%s)' % sel(item)
    if flags: line += '.flags(%s)' % flags
    if values: line += '.values(%s)' % values
    out.append('        ' + line + ';')

def C(text): out.append('\n        // ' + text)

PM = 'r:player_other'  # « Misc » checkbox lists
# ---------------- Playback ----------------
C('Playback')
E('play', None, 'When a video ends', 'S', 'PLAYER', cat='r:action_repeat_mode', values='VAL_PLAYBACK_MODE')
E('play', None, 'Video quality', 'S', 'PLAYER', cat='r:title_video_presets', flags='QUALITY')
E('play', None, 'Speed', 'S', 'PLAYER', sub='r:video_speed', cat='r:video_speed')
E('play', None, 'Skip step', 'S', 'PLAYER', cat='r:seek_interval')
E('play', None, 'Sleep timer', 'S', 'PLAYER', cat='r:player_sleep_timer')
E('play', None, 'Keep playing in the background', 'A', 'GENERAL', cat='r:category_background_playback')
E('play', None, 'Remember speed', 'A', 'PLAYER', sub='r:video_speed', cat='r:player_remember_speed')
E('play', None, 'More speeds', 'A', 'PLAYER', sub='r:video_speed', cat=PM, item='r:player_long_speed_list', flags='SPEEDS')
E('play', None, None, 'M', 'PLAYER', sub='r:video_speed', cat=PM, item='r:player_extra_long_speed_list')
E('play', None, 'Classic speed button', 'A', 'PLAYER', cat=PM, item='r:player_speed_button_old_behavior')
E('play', None, 'Buffer size', 'A', 'PLAYER', cat='r:video_buffer')
E('play', None, 'Picture fit', 'A', 'PLAYER', cat='r:video_zoom')
E('play', None, 'How seeking works', 'A', 'PLAYER', cat='r:player_seek_type')
E('play', None, 'Preview while seeking', 'A', 'PLAYER', cat='r:player_seek_preview')
E('play', None, 'Stay at the live edge', 'A', 'PLAYER', cat=PM, item='r:live_edge')
E('play', None, 'Resume live streams', 'A', 'PLAYER', cat=PM, item='r:remember_position_of_live_videos')
E('play', None, 'Resume short videos', 'A', 'PLAYER', cat=PM, item='r:remember_position_of_short_videos')
E('play', None, 'Play the section as a playlist', 'A', 'PLAYER', cat=PM, item='r:player_section_playlist')
E('play', None, 'Notification skips to next chapter', 'A', 'PLAYER', cat=PM, item='r:player_chapter_notification2')
E('play', None, 'Keep playing while searching', 'A', 'PLAYER', cat=PM, item='r:search_background_playback')
E('play', None, 'Loop Shorts', 'A', 'PLAYER', cat=PM, item='r:player_loop_shorts')
E('play', None, 'Queue follows « When a video ends »', 'A', 'PLAYER', cat=PM, item='r:queue_respects_playback_mode')
P = 'Player screen'
E('play', P, 'Player buttons', 'A', 'PLAYER', cat='r:player_buttons')
E('play', P, 'Hide controls after', 'A', 'PLAYER', cat='r:player_ui_hide_behavior')
E('play', P, 'Time shown under the bar', 'A', 'PLAYER', cat='r:player_show_ending_time')
E('play', P, 'Clock in the corner', 'A', 'MAINUI', cat=PM, item='r:player_corner_clock')
E('play', P, 'End time in the corner', 'A', 'MAINUI', cat=PM, item='r:player_corner_ending_time')
for name, label, flags in [
    ('suggestions_horizontally_scrolled', 'Suggestions in one row', None),
    ('player_disable_suggestions', 'Suggestions under the video', 'INVERT'),
    ('dont_resize_video_to_fit_dialog', 'Keep video size behind panels', None),
    ('place_chat_left', 'Live chat on the left', None),
    ('place_comments_left', 'Comments on the left', None),
    ('player_show_clock', 'Clock under the bar', None),
    ('player_show_quality_info', 'Quality under the bar', None),
    ('player_show_quality_info_bitrate', 'Add the bitrate', None),
    ('player_global_focus', 'Keep focus column between rows', None),
    ('player_ui_on_next', 'Show controls on the next video', None),
    ('player_ui_animations', 'Player animations', None),
    ('player_likes_count', 'Like counts', None),
    ('player_show_tooltips', 'Button tooltips', None),
    ('player_button_long_click', 'Long-press hints on buttons', None),
    ('real_channel_icon', 'Channel avatar on its button', None)]:
    E('play', P, label, 'A', 'PLAYER', cat=PM, item='r:' + name, flags=flags)
P = 'Frame rate'
AFR = 'r:auto_frame_rate'
E('play', P, 'Match frame rate', 'A', 'AFR', cat=AFR, item='r:auto_frame_rate')
E('play', P, 'Use 23.976 / 29.97 / 59.94', 'A', 'AFR', cat=AFR, item='f:frame_rate_correction')
E('play', P, 'Also match resolution', 'A', 'AFR', cat=AFR, item='r:resolution_switch')
E('play', P, 'Double refresh rate', 'A', 'AFR', cat=AFR, item='r:double_refresh_rate')
E('play', P, 'Ignore 24 fps', 'A', 'AFR', cat=AFR, item='r:skip_24_rate')
E('play', P, 'Not for Shorts', 'A', 'AFR', cat=AFR, item='r:skip_shorts')
E('play', P, 'Pause while switching', 'A', 'AFR', cat='r:auto_frame_rate_pause')

# ---------------- Audio & Subtitles ----------------
C('Audio & Subtitles')
E('av', None, 'Audio language', 'S', 'PLAYER', cat='r:audio_language')
E('av', None, 'Subtitle style', 'S', 'SUBS', cat='r:subtitle_style')
E('av', None, 'Subtitle size', 'S', 'SUBS', cat='r:subtitle_scale')
E('av', None, 'Subtitle position', 'A', 'SUBS', cat='r:subtitle_position')
E('av', None, 'Remember subtitles per channel', 'A', 'SUBS', item='r:subtitle_remember')
E('av', None, 'Audio delay', 'A', 'PLAYER', cat='r:audio_shift')
E('av', None, 'Volume boost', 'A', 'PLAYER', cat='r:player_volume')
E('av', None, 'Pause when another app plays sound', 'A', 'PLAYER', cat=PM, item='r:player_audio_focus')
E('av', None, 'Even out loudness', 'A', 'PLAYER', cat=PM, item='r:player_auto_volume')
E('av', None, 'Natural voice at other speeds', 'A', 'PLAYER', cat=PM, item='r:player_time_stretching')

# ---------------- Home & Display ----------------
C('Home & Display')
HH = 'r:header_home'
E('home', None, 'Start screen', 'S', 'GENERAL', cat='r:boot_to_section')
E('home', None, 'Sidebar sections', 'S', 'GENERAL', cat='r:side_panel_sections')
E('home', None, 'Dim the screen after', 'S', 'GENERAL', sub='r:screen_dimming', cat='r:screen_dimming_timeout')
E('home', None, 'Interface size', 'S', 'MAINUI', cat='r:scale_ui')
E('home', None, 'Hide news and sports rows', 'S', 'MAINUI', cat=HH, item='r:oculo_home_rows_filter')
E('home', None, 'Show Continue watching', 'S', 'MAINUI', cat=HH, item='r:oculo_home_continue_watching')
E('home', None, 'Hide watched in the first row', 'A', 'MAINUI', cat=HH, item='r:oculo_home_hide_watched')
E('home', None, 'Glow from the thumbnail', 'A', 'MAINUI', cat=HH, item='r:oculo_thumbnail_glow')
E('home', None, 'Theme', 'A', 'MAINUI', cat='r:color_scheme')
E('home', None, 'Card size', 'A', 'MAINUI', cat='r:video_grid_scale')
E('home', None, '24-hour clock', 'A', 'MAINUI', cat=PM, item='c2:time_format_24| |time_format')
E('home', None, 'Clock on Home', 'A', 'MAINUI', cat=PM, item='r:app_corner_clock')
E('home', None, 'Dimming level', 'A', 'GENERAL', sub='r:screen_dimming', cat='r:screen_dimming_amount')
E('home', None, 'Block the TV screensaver', 'A', 'GENERAL', cat=PM, item='r:disable_screensaver')
E('home', None, 'Preview on focus', 'A', 'MAINUI', cat='r:card_preview')
E('home', None, 'Title scroll speed', 'A', 'MAINUI', cat='r:card_text_scroll_factor')
CS = 'r:cards_style'
E('home', None, 'Two-line titles', 'A', 'MAINUI', cat=CS, item='r:card_multiline_title')
E('home', None, 'Two-line details', 'A', 'MAINUI', cat=CS, item='r:card_multiline_subtitle')
E('home', None, 'Scroll long titles', 'A', 'MAINUI', cat=CS, item='r:card_auto_scrolled_title')
E('home', None, 'Rounded corners', 'A', 'MAINUI', cat=CS, item='r:rounded_card_corners')
E('home', None, 'Long-press menu items', 'A', 'GENERAL', cat='r:context_menu')
E('home', None, 'Sort channels by', 'A', 'MAINUI', cat='r:channels_section_sorting')
E('home', None, 'Reopen Subscriptions where I left off', 'A', 'GENERAL', cat=PM, item='r:remember_position_subscriptions')
E('home', None, 'Reopen pinned playlists where I left off', 'A', 'GENERAL', cat=PM, item='r:remember_position_pinned')
for name, label in [('channels_old_look', 'Classic Channels layout'), ('pinned_channel_rows', 'Pinned channels as rows'),
        ('playlists_rows', 'Playlists as rows'), ('channels_filter', 'Channel filter field'),
        ('channel_search_bar', 'Search on channel pages'), ('channels_auto_load', 'Load channel videos automatically')]:
    E('home', None, label, 'A', 'MAINUI', cat=PM, item='r:' + name)

# ---------------- Account & History ----------------
C('Account & History (the « Account » row, « Account password » and « Ask which account at start » are built by OculoSettingsModel)')
E('acct', None, 'YouTube language', 'S', 'LANG', cat='p:dialog_select_language')
E('acct', None, 'Content country', 'S', 'LANG', cat='p:dialog_select_country')
E('acct', None, 'Watch history', 'S', 'GENERAL', cat='r:header_history', values='VAL_HISTORY')
E('acct', None, 'Search history', 'S', 'SEARCH', cat=PM, item='r:disable_search_history', flags='INVERT')
E('acct', None, 'Pause history while casting', 'A', 'REMOTE', cat=PM, item='r:disable_remote_history')
E('acct', None, 'Separate settings per account', 'A', 'GENERAL', cat=PM, item='r:multi_profiles')
E('acct', None, 'Settings password', 'A', 'GENERAL', cat=PM, item='r:protect_settings_with_password')
E('acct', None, 'Master password', 'A', 'GENERAL', cat=PM, item='r:enable_master_password')
E('acct', None, 'Child mode', 'A', 'GENERAL', cat=PM, item='r:child_mode')
E('acct', None, 'Import subscriptions', 'A', 'BACKUP', item='p:import_subscriptions_group')

# ---------------- SponsorBlock & Filters ----------------
C('SponsorBlock & Filters')
HC = 'r:hide_unwanted_content'
E('filt', None, 'Hide Shorts everywhere', 'S', 'GENERAL', cat=HC, item='r:hide_shorts_everywhere')
E('filt', None, 'SponsorBlock', 'S', 'SB', item='cs:enable| SponsorBlock')
E('filt', None, 'Segments', 'S', 'SB', cat='r:content_block_action_type')
E('filt', None, 'Show on progress bar', 'A', 'SB', cat='r:sponsor_color_markers')
E('filt', None, 'Ignore segments shorter than', 'A', 'SB', cat='r:ignore_short_segments')
E('filt', None, 'Paid promotion notice', 'A', 'SB', cat=PM, item='r:paid_content_notification')
E('filt', None, 'Skip each segment only once', 'A', 'SB', cat=PM, item='r:skip_each_segment_once')
E('filt', None, 'Not for this channel', 'A', 'SB', item='r:content_block_exclude_channel')
E('filt', None, 'Not for this channel', 'A', 'SB', item='r:content_block_stop_excluding_channel')
E('filt', None, 'Server status', 'A', 'SB', item='r:content_block_status')
E('filt', None, 'About SponsorBlock', 'A', 'SB', item='r:about_sponsorblock')
E('filt', None, 'Hide Mixes', 'A', 'GENERAL', cat=HC, item='r:hide_mixes')
E('filt', None, 'Hide live streams in Subscriptions', 'A', 'GENERAL', cat=HC, item='r:hide_streams')
E('filt', None, 'Remove watched from Watch later', 'A', 'GENERAL', cat=HC, item='r:hide_watched_from_watch_later')
E('filt', None, 'Hide watched on Home', 'A', 'GENERAL', cat=HC, item='r:hide_watched_from_home')
E('filt', None, 'Hide watched in Subscriptions', 'A', 'GENERAL', cat=HC, item='r:hide_watched_from_subscriptions')
E('filt', None, 'Hide watched in Notifications', 'A', 'GENERAL', cat=HC, item='r:hide_watched_from_notifications')
P = 'Hide Shorts in…'
for name, label in [('hide_shorts', 'Subscriptions'), ('hide_shorts_from_search', 'Search results'), ('hide_shorts_from_home', 'Home'),
        ('hide_shorts_channel', 'Channel pages'), ('hide_shorts_from_history', 'History'), ('hide_shorts_from_trending', 'Trending')]:
    E('filt', P, label, 'A', 'GENERAL', cat=HC, item='r:' + name)
P = 'Hide upcoming in…'
for name, label in [('hide_upcoming', 'Subscriptions'), ('hide_upcoming_home', 'Home'), ('hide_upcoming_channel', 'Channel pages')]:
    E('filt', P, label, 'A', 'GENERAL', cat=HC, item='r:' + name)
E('filt', None, 'Original titles', 'A', 'DEARROW', item='r:card_unlocalized_titles')
E('filt', None, 'Better titles (DeArrow)', 'A', 'DEARROW', item='r:crowdsoursed_titles')
E('filt', None, 'Better thumbnails (DeArrow)', 'A', 'DEARROW', item='r:crowdsourced_thumbnails')
E('filt', None, 'Thumbnail frame', 'A', 'MAINUI', cat='r:card_content')
E('filt', None, 'DeArrow server status', 'A', 'DEARROW', item='r:dearrow_status')
E('filt', None, 'About DeArrow', 'A', 'DEARROW', item='r:about_dearrow')

# ---------------- Remote & Devices ----------------
C('Remote & Devices')
E('remote', None, 'Phone remote', 'S', 'REMOTE', item='r:settings_remote_control')
E('remote', None, 'Link a phone', 'S', 'REMOTE', item='r:dialog_add_device')
E('remote', None, 'Show in the cast list', 'A', 'REMOTE', item='r:remote_control_dial')
E('remote', None, 'Unlink all phones', 'A', 'REMOTE', cat='r:dialog_remove_all_devices')
E('remote', None, 'Close Oculo when the phone disconnects', 'A', 'REMOTE', cat=PM, item='r:finish_on_disconnect')
E('remote', None, 'Back on Home leaves Oculo', 'A', 'GENERAL', cat='r:app_exit_shortcut', values='VAL_EXIT')
E('remote', None, 'Press Back twice to leave a video', 'A', 'GENERAL', cat=PM, item='c2:player_exit_shortcut|: |app_double_back_exit')
E('remote', None, 'Press Back twice to leave search', 'A', 'GENERAL', cat=PM, item='c2:search_exit_shortcut|: |app_double_back_exit')
E('remote', None, 'Back returns to the TV home after a Google TV search', 'A', 'GENERAL', cat=PM, item='r:return_to_launcher')
E('remote', None, 'OK button during a video', 'A', 'PLAYER', cat='r:player_ok_button_behavior', values='VAL_OK')
E('remote', None, 'OK long press', 'A', 'GENERAL', cat=PM, item='r:disable_ok_long_press', flags='INVERT')
E('remote', None, 'Number keys jump (1 = 10 %)', 'A', 'PLAYER', cat=PM, item='r:player_number_key_seek')
P = 'Button shortcuts'
KR = 'r:key_remapping'
E('remote', P, None, 'M', 'GENERAL', cat=KR, item='pc:OK -> |player_toggle_speed')
for name, label in [('player_quick_shorts_skip_alt', 'Up/Down skips Shorts'), ('player_quick_shorts_skip', 'Left/Right skips Shorts'),
        ('player_quick_skip_videos_alt', 'Up/Down skips videos'), ('player_quick_skip_videos', 'Left/Right skips videos')]:
    E('remote', P, label, 'A', 'GENERAL', cat=KR, item='r:' + name)
for lit, label in [('Play/Pause -> OK', 'Play/Pause acts as OK'), ('DPAD RIGHT/LEFT -> Volume Up/Down', 'Left/Right = volume'),
        ('DPAD UP/DOWN -> Volume Up/Down', 'Up/Down = volume'), ('DPAD UP/DOWN -> Speed Up/Down', 'Up/Down = speed'),
        ('Numbers 3/1 -> Speed Up/Down', '3 / 1 = speed'), ('Next/Previous -> Fast Forward/Rewind', 'Next/Previous = seek'),
        ('Next/Previous -> Speed Up/Down', 'Next/Previous = speed'), ('Fast Forward/Rewind -> Next/Previous', 'Fast forward/Rewind = next/previous'),
        ('Fast Forward/Rewind -> Speed Up/Down', 'Fast forward/Rewind = speed'), ('Fast Forward/Rewind -> Speed Toggle', 'Fast forward/Rewind = speed toggle'),
        ('S -> Speed Toggle', 'S key = speed toggle'), ('Page Up/Down -> Next/Previous', 'Page Up/Down = next/previous'),
        ('Page Up/Down -> Like/Dislike', 'Page Up/Down = like/dislike'), ('Page Up/Down -> Speed Up/Down', 'Page Up/Down = speed'),
        ('Page Up/Down -> Speed Down/Up', 'Page Up/Down = speed (reversed)'), ('Channel Up/Down -> Volume Up/Down', 'Channel +/- = volume'),
        ('Channel Up/Down -> Next/Previous', 'Channel +/- = next/previous'), ('Channel Up/Down -> Like/Dislike', 'Channel +/- = like/dislike'),
        ('Channel Up/Down -> Speed Up/Down', 'Channel +/- = speed'), ('Channel Up/Down -> Speed Toggle', 'Channel +/- = speed toggle'),
        ('Channel Up/Down -> Search', 'Channel +/- = search')]:
    E('remote', P, label, 'A', 'GENERAL', cat=KR, item='l:' + lit)
for name, label, flags in [('typing_corrections', 'Auto-correction', 'INVERT'), ('instant_voice_search', 'Search as soon as I stop talking', None),
        ('focus_on_search_results', 'Jump to results', None), ('keyboard_auto_show', 'Open the keyboard automatically', None),
        ('keyboard_fix', "OK doesn't open the keyboard (G20s remotes)", None)]:
    E('remote', None, label, 'A', 'SEARCH', cat=PM, item='r:' + name, flags=flags)

# ---------------- System ----------------
C('System (« Advanced mode » and « About Oculo » are built by OculoSettingsModel)')
LB = 'r:local_backup'
E('sys', None, 'Back up settings', 'S', 'BACKUP', cat=LB, item='p:app_backup')
E('sys', None, 'Restore settings', 'S', 'BACKUP', cat=LB, item='p:app_restore')
E('sys', None, 'Check for updates', 'S', 'ABOUT', item='r:check_for_updates')
E('sys', None, "What's new", 'S', 'ABOUT', cat='p:update_changelog')
E('sys', None, 'Automatic backup', 'A', 'BACKUP', cat=LB, item='r:auto_backup_category')
GD = 'l:Google Drive'
E('sys', None, 'Back up to Google Drive', 'A', 'BACKUP', sub=GD, item='r:app_backup')
E('sys', None, 'Restore from Google Drive', 'A', 'BACKUP', sub=GD, item='r:app_restore')
E('sys', None, 'Automatic Drive backup', 'A', 'BACKUP', sub=GD, item='r:auto_backup_category')
E('sys', None, 'Drive backup for this TV only', 'A', 'BACKUP', sub=GD, item='r:device_specific_backup')
E('sys', None, 'Google Drive account', 'A', 'BACKUP', sub=GD, item='r:dialog_add_account')
E('sys', None, 'Update notices', 'A', 'ABOUT', cat='r:check_updates_auto')
E('sys', None, 'Update source', 'A', 'ABOUT', cat='r:preferred_update_source')
E('sys', None, 'Feedback', 'A', 'ABOUT', cat='r:feedback')
E('sys', None, 'Releases', 'A', 'ABOUT', item='r:releases')
E('sys', None, 'Source code', 'A', 'ABOUT', item='r:sources')

# ---------------- Advanced ----------------
C('Advanced (also « Other settings » and « Classic settings », built by OculoSettingsModel)')
P = 'Network'
NS = 'r:network_settings'
E('adv', P, 'Automatic network repair', 'A', 'GENERAL', sub=NS, item='r:disable_network_error_fixing', flags='INVERT')
E('adv', P, 'Web proxy', 'A', 'GENERAL', sub=NS, item='r:enable_web_proxy')
E('adv', P, 'Conscrypt (TLS)', 'A', 'GENERAL', sub=NS, item='r:enable_conscrypt')
E('adv', P, 'DNS', 'A', 'GENERAL', sub=NS, cat='r:preferred_dns')
E('adv', P, 'Network engine', 'A', 'GENERAL', sub=NS, cat='r:player_network_stack')
E('adv', P, 'SponsorBlock: alternative server', 'A', 'SB', cat=PM, item='r:content_block_alt_server')
P = 'Video formats'
PT = 'r:player_tweaks'
E('adv', P, 'Exact video format', 'A', 'PLAYER', cat='r:title_video_presets')
for kind, name, label in [('p', 'unlock_high_bitrate_formats', 'High-bitrate 1080p'), ('r', 'unlock_high_bitrate_audio_formats', 'High-bitrate AAC audio'),
        ('r', 'force_legacy_codecs', 'Legacy codecs (360p max)'), ('r', 'unlock_all_formats', 'Unlock all formats'),
        ('r', 'alt_presets_behavior', 'Quality by bandwidth'), ('r', 'prefer_avc_over_vp9', 'Prefer AVC (1080p max)'),
        ('r', 'skip_codec_profile_check', 'Skip codec check'), ('r', 'force_sw_codec', 'Software decoder')]:
    E('adv', P, label, 'A', 'PLAYER', cat=PT, item=kind + ':' + name)
P = 'Live streams'
for name, label in [('prefer_sabr_live', 'SABR live streams'), ('live_stream_fix', 'Live fix, HLS (1080p)'),
        ('live_stream_fix_4k', 'Live fix, DASH (4K)'), ('disable_stream_buffer', 'No buffer on live streams')]:
    E('adv', P, label, 'A', 'PLAYER', cat=PT, item='r:' + name)
P = 'Device fixes'
E('adv', P, 'Playback notifications', 'A', 'PLAYER', cat=PT, item='r:playback_notifications_fix', flags='INVERT')
for name, label in [('audio_sync_fix', 'Audio sync fix'), ('ambilight_ratio_fix', 'Ambilight & aspect fix'),
        ('amlogic_fix', 'Amlogic 1080p60 fix'), ('tunneled_video_playback', 'Tunneled playback'), ('disable_vsync', 'No vsync alignment'),
        ('sony_frame_drop_fix', 'Frame drop fix (Sony)'), ('amazon_frame_drop_fix', 'Frame drop fix (Amazon)'),
        ('keep_finished_activities', 'Keep finished activities'), ('fix_empty_subs_and_channels', 'Fix empty Subscriptions')]:
    E('adv', P, label, 'A', 'PLAYER', cat=PT, item='r:' + name)
P = 'Display'
E('adv', P, 'Pixel ratio', 'A', 'PLAYER', cat='r:player_pixel_ratio')
E('adv', P, 'Display modes', 'A', 'AFR', cat='r:auto_frame_rate_modes')

# ---------------- Left out (OculoSettingsCleanup removes them before Oculo reads the screens) ----------------
C('Left out by OculoSettingsCleanup (dead on the TV or second copies); listed so the mapping test knows them')
for src, cat, item in [('GENERAL', PM, 'r:select_channel_section'), ('MAINUI', 'r:various_buttons', None),
        ('MAINUI', PM, 'r:fullscreen_mode'), ('MAINUI', PM, 'r:card_unlocalized_titles'), ('MAINUI', CS, 'r:card_unlocalized_titles'),
        ('PLAYER', PT, 'r:oculus_quest_fix'), ('PLAYER', PT, 'r:hide_settings_section'), ('PLAYER', PT, 'r:disable_channels_service'),
        ('PLAYER', PM, 'c2:player_exit_shortcut|: |app_double_back_exit'), ('SEARCH', 'r:speech_engine', None),
        ('SEARCH', PM, 'c2:search_exit_shortcut|: |app_double_back_exit'), ('SEARCH', PM, 'r:search_background_playback'),
        ('DEARROW', 'r:dearrow_not_submitted_thumbs', None), ('ABOUT', None, 'r:install_bridge'), ('ABOUT', 'r:donation', None)]:
    E('adv', None, None, 'X', src, cat=cat, item=item)

# --- extra labels built in code ---
for en in ['Account', 'Account password', 'Ask which account at start', 'Advanced mode', 'About Oculo', 'Classic settings',
           'Recent', 'Other settings']:
    lab(en)

OUTDIR = os.environ.get('OUT', '/tmp')
open(os.path.join(OUTDIR, 'entries.java.txt'), 'w').write('\n'.join(out) + '\n')

def x(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace("'", "\\'").replace('"', '\\"')
en = ['    <string name="%s">%s</string>' % (k, x(v)) for k, v in sorted(labels.items())]
fr = ['    <string name="%s">%s</string>' % (k, x(FR[v])) for k, v in sorted(labels.items())]
open(os.path.join(OUTDIR, 'strings_en.xml.txt'), 'w').write('\n'.join(en) + '\n')
open(os.path.join(OUTDIR, 'strings_fr.xml.txt'), 'w').write('\n'.join(fr) + '\n')
print(len(out), 'lines,', len(labels), 'labels')
