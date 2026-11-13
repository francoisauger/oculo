# Oculo

Oculo is a thin, rebranded fork of [SmartTube](https://github.com/yuliskov/SmartTube) by yuliskov (MIT),
built for Android TV / Google TV. It installs next to the official SmartTube (different package name).
Oculo is not affiliated with YouTube or Google.

## Branches and remotes

- `upstream` = https://github.com/yuliskov/SmartTube (its default branch is `master`).
- `main` tracks `upstream/master` and is never edited.
- `oculo` = `main` + the Oculo changes below. Build from here.

## What the fork changes

Kept untouched: `MediaServiceCore`, `SharedModules` (submodules) and `exoplayer-amzn-2.10.6`.

**MediaServiceCore pin (32.74-10).** Oculo pins `MediaServiceCore` to upstream commit `767a6685` instead of 32.74's `b3d561f5` (both upstream, no Oculo edit). From `66b5a1bc` on (SmartTube 32.73 and 32.74), `FormatInfoWrapper.CLIENT_LIST` starts with the WEB client (Innertube); its WebView poToken is not accepted by the SABR server (`StreamProtectionStatus` stays `ATTESTATION_PENDING`), so YouTube serves about 60 s and then no more media: every video stalls at 0:59 (upstream issues #6370, #6461, #6463, #6439). `767a6685` is the same code with the TV client first, as in 32.71. It lacks two small upstream changes: the "fallback to error result" for unplayable videos and the per-client nonce reset (`b3d561f5`, only matters when a client falls through to the next). After a merge with upstream, move the pin back to upstream's only once upstream fixes the WEB attestation or puts TV first again.

| File | Change |
|---|---|
| `smarttubetv/oculo.gradle` | The `oculo` product flavor: applicationId `com.francoisauger.oculo`, minSdk 21, targetSdk = compileSdk, versionCode = upstream × 100 + Oculo revision, versionName = upstream + `-<revision>`, libraries fall back to their `stfdroid` variant (no Firebase/Crashlytics), release signing read from outside the repo, APK names `Oculo_<version>_<abi>.apk`. |
| `smarttubetv/build.gradle` | One line at the end: `apply from: "oculo.gradle"`. |
| `smarttubetv/src/oculo/res/` | Name "Oculo", launcher icon (see « Launcher icon »), Android TV banner, browse logo, global search label and authority (`xml/searchable.xml`, `search_authority`), update manifest URL (`update_urls`), About links and upstream credit (`feedback`, `releases_url`, `sources_url`). |
| `smarttubetv/src/oculo/res/values/colors.xml`, `values/styles.xml`, `drawable/progress_bar_oculo.xml` | Oculo accents (palette from the approved logo, preview in `docs/mockups/colors.html`): the default "Dark Grey" scheme's styles are restated with turquoise `#22D3C8` instead of red (toggles; until 32.74-7 also the focused card's fill, see « Card focus »), badges (`dark_red`) and search orb in teal `#0B7F7D`, "Teal" scheme re-tinted to `#0B7F7D`/`#0F4A52`. Dark backgrounds unchanged except the browse glow below; Red and Blue schemes untouched (`scheme_red_background_dark` pinned to `#B31217`). Seek bar, card progress and player buttons: see « Player » below. If upstream changes these styles, re-sync the restated copies. |
| Player (32.74-3) | Option 1 « Sarcelle profonde » of `docs/mockups/player-colors.html`: played part and card watched bars `#0FA3A0` (card track black 55%), buffered `#E0E6E5`, remaining white 25% (`oculo_seek_bar_remaining`), active icons `#12B3AC`, focused button = `#0F4A52` disc with a 3dp `#22D3C8` ring (`drawable-v21/lb_control_button_{primary,secondary}.xml`; the player uses the secondary layout for both rows), focused seek bar 8dp with an 8dp knob radius (`values/dimens.xml`). |
| Loading animation (32.74-4) | Concept B « Balayage » of `docs/mockups/loading.html` exactly as the page shows it by default (frame toggle « Sarcelle foncé »): dark-teal frame (vertical `#145963` → `#0F4A52` → `#0B3A41`), track mint at 10 %, cyan → mint trail (`#2FE3F0` → `#78F5BE`, head 14 % of the lap, tail 40 % at 32 % opacity starting 26 % behind), cyan → turquoise → mint screen with its white 34 % → 6 % gloss, 1.2 s linear lap. `drawable-v24/progress_large_holo.xml` (AnimatedVectorDrawable, gradients; diagonal end points recomputed to match SVG objectBoundingBox gradients) and `drawable/progress_large_holo.xml` (solid mid colors, API 21-23) override SharedModules' spinner (`ProgressBarManager`). Frame-by-frame check against the mockup (Skia render of the AVD vs headless Chrome): `docs/mockups/loading-compare.png`. `layout/view_loading_card.xml` and `layout/widget_preview_card.xml` are copies of the upstream layouts with `indeterminateDrawable` set: re-sync them if upstream changes those layouts. |
| Browse background (32.74-6) | `drawable/oculo_browse_background.xml`: black with one turquoise glow (`#32E6DC` at 20 %, the logo board's dark-tile glow strength) at the top right, below the clock and wordmark: center 78 % / 25 %, radius 47.5 % of the screen width (84.44%p of the 1080 px side), dithered. Before 32.74-6 it was centered (50 % / 42 %). Set as `shelfBackground` of `App.Theme.DarkGrey.Browse`, which `UriBackgroundManager` draws behind home, search and channel screens. The player keeps its black (`player_background`); `App.Theme.DarkGrey.OLED.Browse` is restated in the flavor with a black `shelfBackground`. Preview: `docs/mockups/background-glow.png` (TV screenshot). |
| Top-right logo (32.74-3) | `mipmap-nodpi/app_logo_semi_red.png` and `app_logo_semi_grey.png` (the browse `appLogo` badge, 50dp high) hold the « Oculo » wordmark with white letters (from `docs/mockups/logos-v3/oculo-wordmark-dark.png`, 632×200, content 80% of the height). |
| Home (32.74-3) | `common/.../oculo/OculoData.java` (prefs, `DataSaverBase`) and `OculoSettings.java` (category « Home » in Settings > Main UI): Home reloads on return to the app when older than 10 minutes (upstream 3 h); the news/sports row-title filter is a toggle, off by default; hiding watched videos in Home's first row is a toggle, on by default. Defaults and strings in `common/src/main/res/values/oculo.xml` (upstream behaviour, EN) and `values-fr/oculo.xml` (FR); Oculo defaults in `smarttubetv/src/oculo/res/values/oculo_config.xml`. |
| `common/.../utils/Utils.java` | Adds `com.francoisauger.oculo` to `KNOWN_PACKAGES` so the full About screen (credits, links) is shown. |
| Launcher icon (32.74-6) | Built from the Android TV banner. Adaptive icon `mipmap-anydpi-v26/app_icon.xml`: background `drawable/oculo_icon_background.xml` = the banner's dark-teal gradient (sampled from `oculo-banner@2x.png`: `#0F5E67` → `#0B4952` → `#062A31`, top-left to bottom-right, soft white light top-left), full bleed; foreground `drawable-nodpi/oculo_icon_foreground.png` (432 px = 108dp at xxxhdpi) = the banner's TV mark alone, drawn from the logo's vector source in `logos-v3.html` (white → `#D4F1F1` frame, cyan → turquoise → mint screen, diagonal gloss) with a soft white/cyan glow. Frame 46dp wide (about 64 % of the visible 72dp disc), centered, well inside the 66dp safe zone. `mipmap-nodpi/app_icon.png` (API < 26) is the same design, opaque. Generated by `docs/mockups/icon/gen_icon.sh` (runs `gen_icon.py`; layers, legacy PNG, preview `docs/mockups/icon-preview.png` next to the banner crop). `app_icon_alt` and the banner are unchanged. |
| QR codes (32.74-5) | `common/.../oculo/OculoQr.java` generates QR codes on the device with ZXing core (`com.google.zxing:core:3.3.3`, Apache 2.0, Java 7 so no desugaring on minSdk 17/21): black modules on white, 4-module quiet zone, integer pixels per module. The sign-in screen loads the bitmap (600 px+); `Utils.toQrCodeLink()` now returns a self-contained `data:text/html` page (PNG inline, 78 % of the screen height, pixelated scaling) for the built-in web view (share QR link of a video, About links and feedback). Nothing goes to api.qrserver.com any more. Test: `common/src/test/.../oculo/OculoQrTest.java` (encodes an activation URL, decodes it back, checks the quiet zone). The app has no open-source licenses screen; ZXing's Apache 2.0 license is noted here. |
| About (32.74-5) | No « Install bridge » button (`oculo_show_install_bridge` = false in `oculo_config.xml`, upstream default true in `common/src/main/res/values/oculo.xml`) and no donations (`smarttubetv/src/oculo/res/values/donations.xml` is an empty `donations` array; `appendDonation()` already skips an empty section, so no header). |
| Player placeholder (32.74-5) | `Video.getBackgroundUrl()` falls back to a 1×1 black PNG `data:` URL (Glide decodes it locally, center-cropped to full screen) instead of `via.placeholder.com`. |
| Secondary color « Abricot » (32.74-6) | Option A of `docs/mockups/couleur-secondaire.html`, for signals only (teal stays the identity: logo, focus, played part, loader). Colors in `values/colors.xml` (`oculo_abricot_light` `#FFC896`, `oculo_abricot` `#FFA25C`, `oculo_abricot_dark` `#B0541A`, `oculo_abricot_ink` `#3A1A06`) and roles in `values/oculo_config.xml`: LIVE / NEW / upcoming card badges = `dark_red` → Abricot with ink text (`oculo_badge_accent_text`, 7.9:1); channel cards with new content = dark Abricot with white text (`oculo_channel_new_content`, 5.1:1); sign-in code Abricot and URL light Abricot (`oculo_signin_code`, `oculo_signin_link`); SponsorBlock segments (`oculo_sb_*`): sponsor Abricot, intro and outro light, self-promo dark, highlight light, interaction / music off-topic / preview / filler in greys (`#7A7A7A`, `#9A9A9A`, `#BDBDBD`, `#5E5E5E`). SmartTube does not let users pick segment colors (only toggle markers), so these are the colors. Toasts are system toasts and the subscribe button has no themable accent: unchanged. Upstream defaults in `common/src/main/res/values/oculo.xml` and `smarttubetv/src/main/res/values/oculo.xml`. Palette files: `docs/mockups/logos-v3/oculo-palette.{css,json}`, section « Secondaire : Abricot » of `logos-v3.html`. |
| Left rail (32.74-6, reworked in 32.74-7) | YouTube TV style navigation (`smarttubetv/.../tv/oculo/OculoRail.java`, flag `oculo_left_rail`, true in the flavor). Collapsed (focus in the content): the headers stay on screen as an icon column, current section turquoise `#22D3C8` (32.74-11, `docs/mockups/couleur-secondaire.html` `.side .ic.cur`; white `#F1F1F1` from 32.74-7 to 32.74-10), the others grey `#B8B8B8` (YouTube's values). Expanded (D-pad left): labels appear and the content moves right (leanback's own headers transition), icons and labels white (current section's icon turquoise when not focused, 32.74-11), focused item = YouTube's white rounded rectangle (`#F1F1F1`, 7.5dp corners, 222 × 39dp) with dark `#0F0F0F` icon and text (`layout/icon_header_item.xml` flavor copy, `drawable/oculo_rail_item_background.xml`, `color/oculo_rail_icon.xml` collapsed, `color/oculo_rail_icon_expanded.xml`, `color/oculo_rail_label.xml`; turquoise until 32.74-7), no zoom (`lb_browse_header_select_scale` 1.0), no dimming (`oculo_header_unselect_alpha` 100 %). 32.74-7, measured on YouTube TV (`docs/mockups/youtube-ref/yt-*.png`, 1080p, 2 px = 1dp; dimens in `smarttubetv/src/main/res/values/oculo.xml`): the profile avatar (27dp, click: next account or account dialog, long click: account settings) stays fixed at the top, centered 50.75dp down; the section list is clipped 77.5dp from the top and Search (outlined magnifier `drawable/oculo_ic_search.xml`) is its first slot, so Search scrolls away with the sections while the avatar stays, as in YouTube (`updateSearchPosition()` follows the grid's scroll; a 22dp fade softens items scrolling under the avatar). Icons 20dp centered 38.75dp from the edge, labels from 61dp, 39dp pitch, pill 222dp (32.74-11: 46.75dp and 69dp, see below). Collapsed: Search 116.75dp, Home 155.75dp; expanded: the list moves up 19.5dp (Search 97.25dp, Home 136.25dp), like YouTube's. Content starts 78dp from the edge collapsed (`oculo_rail_collapsed_width` 22dp + the rows' 56dp padding; 32.74-11: François found the icon column narrow, so it is 20 % wider than YouTube's: 94dp, `oculo_rail_collapsed_width` 38dp, icons and avatar recentered at 46.75dp with `oculo_rail_list_padding_start` 24dp, content clip `oculo_rail_content_clip_start` 26dp; the top bar follows the content start; expanded content start unchanged, its icons and pill 8dp further right) and 267dp expanded (`lb_browse_rows_margin_start` 211dp in the flavor); scrolled cards are clipped right of the icons (`oculo_rail_content_clip_start`). Not copied: YouTube's group separator and Settings pinned at the bottom (SmartTube's sections are user-ordered). D-pad: left from the content opens the rail on the current section; up from the first section goes to Search, then the avatar; right goes back to the content. Back keeps upstream's behaviour. Screenshots: `docs/mockups/oculo-326/`, `docs/mockups/oculo-327/`. |
| Top bar (32.74-7) | `smarttubetv/.../tv/oculo/OculoTopBar.java`, built by OculoRail inside leanback's title view (no layout change): a round mic button (36dp, `drawable/oculo_ic_mic.xml`) and a 315 × 36dp « Search » pill at the content's start (they slide with the content when the rail opens), white 15 % at rest, white `#F1F1F1` with dark `#0F0F0F` text and icon when focused, as in YouTube (`oculo_white_focus`, `oculo_white_focus_ink`; turquoise until 32.74-7); clock 18sp, date 11sp and the wordmark 28dp high (YouTube's logo: 44 px) at the top right, all centered 50.75dp from the top. Up from the first row lands on the mic (as in YouTube), right goes to the pill, down back to the rows, left from the mic opens the rail. Mic = `SearchPresenter.startVoice()` (the search screen opens listening, « Speak to search »: the TV has a system recognizer, `com.google.android.tts` / katniss, and Oculo's RECORD_AUDIO is granted; no bridge app needed), pill = `startSearch(null)`. As in YouTube the bar stays while the rows scroll: `BrowseFragment` keeps the title shown and the content is clipped 75dp from the top (`oculo_top_bar_content_clip`). The first row title sits 18dp higher (`lb_browse_rows_margin_top` 112dp in the flavor; upstream SmartTube 130dp). Comparison: `docs/mockups/oculo-327/compare-youtube.png`. |
| Card focus (32.74-8, ring in 32.74-9) | No turquoise fill on the focused card: `cardSelectedBackground` = the card's own background (`scheme_grey_card_background_dark`) in the flavor's Dark Grey styles, and the title/meta text stays white (`OculoUi.cardSelectedTextColor()`: when the theme gives no focus fill, focused text = default text; schemes with a white fill keep upstream's dark text). Focus = a thin white ring set slightly apart from the thumbnail, as on the Google TV home screen and in Plex (refs `docs/mockups/plex-ref/`, measured at 1080p: Google TV 4.5 px ring + 5 px gap, Plex 4 px + ~1 px): `OculoUi.setCardFocusFrame()` draws, as a View foreground (API 23+) over the thumbnail's edges, a 2dp `#F1F1F1` ring (`oculo_card_focus_ring`, `oculo_card_focus_ring_width`) and a 2.5dp dark gap in the card's background color (`oculo_card_focus_ring_gap`), outer corners = the card's 12dp rounded corners, inner corners concentric; the duration badge and watched bar move inside. The card keeps leanback's 1.1 focus zoom, so the picture stays about its unfocused size. Channel cards get the same ring around the whole card. (32.74-8 had a 7dp dark-teal `#224850` frame from `docs/mockups/ref-francois/focus-contour.png`; François rejected it.) Settings tiles and search tags keep upstream's white focus fill, with YouTube's dark ink `#0F0F0F` as text (`card_selected_text_grey`; deep teal until 32.74-7). Upstream defaults (transparent, 0dp) in `smarttubetv/src/main/res/values/oculo.xml`. Screenshots: `docs/mockups/oculo-328/` (rail, top bar, 32.74-8 frame), `docs/mockups/oculo-329/` (ring). |
| Continue watching (32.74-11) | Home's second row « Continue watching » / « Continuer à regarder » (`common/.../oculo/OculoContinueWatching.java`, rules in `OculoContinueWatchingRules.java`, test `OculoContinueWatchingRulesTest`), right after the first row (Recommended), toggle « Show Continue watching » / « Afficher Continuer à regarder » in Settings > User interface > Home (`oculo_home_continue_watching_default`: on in the flavor, off upstream). **Data: local only, no network call.** Positions come from SmartTube's `VideoStateService` (the store the player resumes from, so a card resumes at its saved position and shows the usual watched bar); the time a video was last played comes from Oculo's own watch log (`OculoWatchLog` in the app prefs files, `id:timeMs:flags`, written by `VideoStateController.savePosition()`, kept 7 days / 300 entries), because `VideoStateService.State.timestamp` is not persisted (it is reset to the restore time). YouTube's account history was not merged: it only has coarse day groups (« Today », « Yesterday ») and needs a network call; a video started on another device therefore does not appear. Consequence: the row starts empty after installing 32.74-11 and fills as videos are played in Oculo. **Rules:** played in the last 48 h; started = position ≥ 30 s; unfinished = < 90 % and more than 60 s left; duration > 60 s; no live streams, no Shorts (flags recorded at play time); not hidden; most recent first; at most 20. No row when empty. **Long press** on a card of this row (and only this row: `GROUP_ID`) adds « Remove from Continue watching » / « Ne plus proposer ici » at the top of SmartTube's video menu: the id goes to `OculoContinueHidden` (kept 30 days) and the card is removed in place (the row is removed when it becomes empty); the watched state and YouTube's history are not touched. Back on Home (e.g. from the player), the row updates in place: progress only = sync, videos gone = remove, otherwise the row is rebuilt at index 1. Home's 10-minute refresh rebuilds it with the rest. The « hide watched videos in the first row » filter only applies to row 0, so it never removes items from this row; blocked channels are still filtered. |
| Settings tiles (32.74-11) | Smaller tiles so the 13 Settings tiles fit on two rows of 7 at 1080p (`settings_card_width` 104dp, upstream 128dp → 6 columns), icons 30dp (upstream 70dp) with a 20dp margin, two-line 14sp title so « Auto Frame Rate », « Language/Country » stay whole (`layout/settings_card.xml` flavor copy of upstream's, `oculo_settings_title_size`; re-sync if upstream changes that layout). Focus = the video cards' ring (`OculoUi.setCardFocusFrame()`, 2dp `#F1F1F1`, 2.5dp gap) instead of the white title fill (`OculoUi.hasCardFocusRing()`). Screenshot: `docs/mockups/oculo-3211/03-settings-tile-focused.png`. Search tags keep upstream's white fill. |
| `smarttubetv/google-services.json` | Removed (upstream's Firebase project; only used by the `stbeta`/`ststable` flavors). |
| `.gitignore` | Keystores and signing files. |
| `docs/` | Logo work (`docs/mockups/logos-v3/` holds the approved assets). |

Every place tied to the applicationId: the search provider authority comes from
`leanbackassistant/src/stfdroid/AndroidManifest.xml` (`${applicationId}.search_provider`) and must match
`search_authority` and `xml/searchable.xml` in `smarttubetv/src/oculo/res`; the other authorities
(`.androidx-startup`, `.update_provider`) use `${applicationId}` directly.

## Upstream files touched

Keep this list current: these are the only upstream files Oculo edits (each change is a small hook, marked
`Oculo` where it is not obvious). Check them first after a merge with upstream.

| File | Hook |
|---|---|
| `smarttubetv/build.gradle` | `apply from: "oculo.gradle"` at the end. |
| `common/.../utils/Utils.java` | `com.francoisauger.oculo` in `KNOWN_PACKAGES`. |
| `common/.../app/presenters/BrowsePresenter.java` | `refreshIfNeeded()`: interval from `OculoData.getHomeRefreshIntervalMs()` instead of 3 h; `filterHomeIfNeeded()`: returns early when `OculoData.isHomeRowsFilterEnabled()` is false. |
| `common/.../app/presenters/BrowsePresenter.java` (32.74-11) | `updateVideoRows()`: after Home's first row, `OculoContinueWatching.showRow()` inserts « Continue watching » at index 1 (the next rows shift by one); `onViewResumed()`: one call to `onHomeResumed()`. 6 lines, import. |
| `common/.../app/models/playback/controllers/VideoStateController.java` (32.74-11) | `savePosition()`: one call to `OculoContinueWatching.onPositionSaved(video)` (watch time), import. CRLF file. |
| `common/.../app/presenters/dialogs/menu/VideoMenuPresenter.java` (32.74-11) | `prepareAndShowDialogSigned()` / `Unsigned()`: one call each to `OculoContinueWatching.appendMenuItem()`, import. CRLF file. |
| `smarttubetv/.../presenter/SettingsCardPresenter.java` (32.74-11) | Focus listener: `OculoUi.setCardFocusFrame()` and no white fill when `OculoUi.hasCardFocusRing()` (4 lines), import. |
| `common/.../app/models/data/VideoGroup.java` | `isWatchedSuggestion()`: Home's first row is filtered only when `OculoData.isHomeHideWatchedEnabled()`. |
| `common/.../app/presenters/settings/MainUISettingsPresenter.java` | `show()`: one call to `OculoSettings.appendHomeCategory()`. |
| `common/build.gradle` | One dependency line: `com.google.zxing:core:3.3.3`. |
| `common/.../utils/Utils.java` (32.74-5) | `toQrCodeLink()` returns `OculoQr.toQrPageUrl(data)`; the `api.qrserver.com` template constant is removed. |
| `smarttubetv/.../ui/signin/SignInFragment.java` | Glide loads `OculoQr.toBitmap(url, 600)` instead of `Utils.toQrCodeLink(url)`. |
| `common/.../app/presenters/settings/AboutSettingsPresenter.java` | `appendInstallBridge()`: returns early when `R.bool.oculo_show_install_bridge` is false. |
| `common/.../app/models/data/Video.java` | `BLACK_PLACEHOLDER_URL` = local 1×1 black PNG `data:` URL instead of `via.placeholder.com`. |
| `smarttubetv/.../ui/mod/leanback/misc/SeekBar.java` | Remaining part color read from `R.color.oculo_seek_bar_remaining` instead of `Color.GRAY` (upstream value kept in `smarttubetv/src/main/res/values/oculo.xml`). |
| `leanback-1.0.0/.../app/BrowseSupportFragment.java` (32.74-6) | Two protected hooks: `getCollapsedHeadersWidth()` (default 0; used by `setHeadersOnScreen()` to keep the headers on screen and by `expandMainFragment()` as the content's start margin) and `onHeadersShown(boolean)` (called at the end of `showHeaders()`). About 20 lines. |
| `leanback-1.0.0/.../app/HeadersSupportFragment.java` (32.74-6) | `setItemsAlwaysVisible(boolean)`: `updateListViewVisibility()` keeps the items visible while the headers are disabled. About 10 lines. |
| `smarttubetv/.../ui/browse/BrowseFragment.java` (32.74-6, 32.74-7) | `OculoRail.attach()` in `onCreateView()`, overrides of the two leanback hooks, `onStart()` and `onDestroyView()` calls (about 30 lines); 32.74-7: `showTitle(boolean)` / `showTitle(int)` overrides keep the title (top bar) shown with all its parts while the rail is on (12 lines). |
| `smarttubetv/.../ui/widgets/browse/NavigateTitleView.java` (32.74-6) | `updateButtonsVisibility()`: hides the title's search and account orbs when `OculoRail.isEnabled()`. |
| `smarttubetv/.../presenter/IconHeaderItemPresenter.java` (32.74-6) | Unfocused header opacity read from `R.fraction.oculo_header_unselect_alpha` (upstream value: leanback's fraction). |
| `smarttubetv/.../widgets/complexcardview/ComplexImageView.java` (32.74-6) | `setBadgeColor()`: one call to `OculoUi.tintBadgeText()` (ink text on the accent badge). |
| `smarttubetv/.../presenter/ChannelCardPresenter.java` (32.74-6, 32.74-8) | New-content background from `R.color.oculo_channel_new_content` instead of `dark_red`; 32.74-8: focused text color through `OculoUi.cardSelectedTextColor()` (2 lines), `OculoUi.setCardFocusFrame()` in the focus listener (1 line), import. |
| `smarttubetv/.../presenter/VideoCardPresenter.java` (32.74-8) | Focused text color through `OculoUi.cardSelectedTextColor()` (2 lines), `OculoUi.setCardFocusFrame()` in `updateCardBackgroundColor()` (1 line), import. |
| `smarttubetv/.../ui/signin/SignInFragment.java` (32.74-6) | Code color via `OculoUi.setTextColorIfSet(R.color.oculo_signin_code)`; URL color `R.color.oculo_signin_link` instead of `red`. |
| `common/.../prefs/SponsorBlockData.java` (32.74-6) | `initColorMapping()`: segment colors `R.color.oculo_sb_*` (upstream values in `common/src/main/res/values/oculo.xml`). |

New files outside the flavor (not upstream): `common/.../oculo/`, `common/src/test/.../oculo/`, `common/src/main/res/values/oculo.xml`,
`common/src/main/res/values-fr/oculo.xml`, `smarttubetv/src/main/res/values/oculo.xml`, `smarttubetv/.../tv/oculo/` (`OculoRail`, `OculoTopBar`, `OculoUi`), `smarttubetv/src/main/res/drawable/oculo_*.xml` and `color/oculo_top_bar_content.xml`. The flavor's `layout/settings_card.xml` (32.74-11) is a copy of upstream's.

## Build

Requirements: JDK 17 (Gradle 7.5 / AGP 7.4.2 do not run on JDK 21), Android SDK with platform 34 and
build-tools 30.0.3, `local.properties` with `sdk.dir=...` (gitignored).

```bash
git submodule update --init --recursive
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :smarttubetv:assembleOculoRelease
# APKs: smarttubetv/build/outputs/apk/oculo/release/Oculo_<version>_{arm64-v8a,armeabi-v7a,x86,universal}.apk
```

## Signing

The release key is never in the repo. `oculo.gradle` reads a JSON file
(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) from `-PoculoSigningConfig=<path>`,
the `OCULO_SIGNING_CONFIG` environment variable, or by default `~/.config/chuck/oculo/signing.json`.
On François's Mac the keystore is `~/.config/chuck/oculo/oculo-release.jks`.
Losing that key means existing installs can no longer be updated in place: keep a backup.

## In-app updates

`update_urls` points to `https://github.com/francoisauger/oculo/releases/latest/download/oculo_stable.json`.
The manifest uses the SmartTube format (`SharedModules/appupdatechecker2/.../AppVersionChecker.java`):

```json
{
  "package": {
    "downloadUrlList": ["https://github.com/francoisauger/oculo/releases/download/<tag>/Oculo_<version>_armeabi-v7a.apk"],
    "downloadUrlList_arm64-v8a": ["https://github.com/francoisauger/oculo/releases/download/<tag>/Oculo_<version>_arm64-v8a.apk"],
    "downloadUrlList_x86": ["https://github.com/francoisauger/oculo/releases/download/<tag>/Oculo_<version>_x86.apk"]
  },
  "32.74-1": { "versionCode": 246401, "changelog": ["..."] }
}
```

Version keys are versionNames; the updater compares `versionCode` with the installed one and picks
`downloadUrlList_<primary ABI>`, then `downloadUrlList`, then `downloadUrl`.

## Sync with upstream

```bash
git fetch upstream
git checkout main && git merge --ff-only upstream/master
git checkout oculo && git merge main
git submodule update --init --recursive
```

When upstream bumps `versionCode`, reset `oculoRevision` to 1 in `smarttubetv/oculo.gradle`;
for an Oculo-only rebuild of the same upstream version, increment it.
