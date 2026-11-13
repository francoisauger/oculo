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

| File | Change |
|---|---|
| `smarttubetv/oculo.gradle` | The `oculo` product flavor: applicationId `com.francoisauger.oculo`, minSdk 21, targetSdk = compileSdk, versionCode = upstream × 100 + Oculo revision, versionName = upstream + `-<revision>`, libraries fall back to their `stfdroid` variant (no Firebase/Crashlytics), release signing read from outside the repo, APK names `Oculo_<version>_<abi>.apk`. |
| `smarttubetv/build.gradle` | One line at the end: `apply from: "oculo.gradle"`. |
| `smarttubetv/src/oculo/res/` | Name "Oculo", launcher icon (see « Launcher icon »), Android TV banner, browse logo, global search label and authority (`xml/searchable.xml`, `search_authority`), update manifest URL (`update_urls`), About links and upstream credit (`feedback`, `releases_url`, `sources_url`). |
| `smarttubetv/src/oculo/res/values/colors.xml`, `values/styles.xml`, `drawable/progress_bar_oculo.xml` | Oculo accents (palette from the approved logo, preview in `docs/mockups/colors.html`): the default "Dark Grey" scheme's styles are restated with turquoise `#22D3C8` instead of red (focused card with deep-teal `#0B333A` text, toggles), badges (`dark_red`) and search orb in teal `#0B7F7D`, "Teal" scheme re-tinted to `#0B7F7D`/`#0F4A52`. Dark backgrounds unchanged except the browse glow below; Red and Blue schemes untouched (`scheme_red_background_dark` pinned to `#B31217`). Seek bar, card progress and player buttons: see « Player » below. If upstream changes these styles, re-sync the restated copies. |
| Player (32.74-3) | Option 1 « Sarcelle profonde » of `docs/mockups/player-colors.html`: played part and card watched bars `#0FA3A0` (card track black 55%), buffered `#E0E6E5`, remaining white 25% (`oculo_seek_bar_remaining`), active icons `#12B3AC`, focused button = `#0F4A52` disc with a 3dp `#22D3C8` ring (`drawable-v21/lb_control_button_{primary,secondary}.xml`; the player uses the secondary layout for both rows), focused seek bar 8dp with an 8dp knob radius (`values/dimens.xml`). |
| Loading animation (32.74-4) | Concept B « Balayage » of `docs/mockups/loading.html` exactly as the page shows it by default (frame toggle « Sarcelle foncé »): dark-teal frame (vertical `#145963` → `#0F4A52` → `#0B3A41`), track mint at 10 %, cyan → mint trail (`#2FE3F0` → `#78F5BE`, head 14 % of the lap, tail 40 % at 32 % opacity starting 26 % behind), cyan → turquoise → mint screen with its white 34 % → 6 % gloss, 1.2 s linear lap. `drawable-v24/progress_large_holo.xml` (AnimatedVectorDrawable, gradients; diagonal end points recomputed to match SVG objectBoundingBox gradients) and `drawable/progress_large_holo.xml` (solid mid colors, API 21-23) override SharedModules' spinner (`ProgressBarManager`). Frame-by-frame check against the mockup (Skia render of the AVD vs headless Chrome): `docs/mockups/loading-compare.png`. `layout/view_loading_card.xml` and `layout/widget_preview_card.xml` are copies of the upstream layouts with `indeterminateDrawable` set: re-sync them if upstream changes those layouts. |
| Browse background (32.74-4) | `drawable/oculo_browse_background.xml`: black with the logo board's dark-tile glow (`logos-v3.html`, `radial-gradient(circle at 50% 42%, rgba(50,230,220,.20), transparent 64%)`, radius 67.93%p on 16:9, dithered), set as `shelfBackground` of `App.Theme.DarkGrey.Browse`, which `UriBackgroundManager` draws behind home, search and channel screens. The player keeps its black (`player_background`); `App.Theme.DarkGrey.OLED.Browse` is restated in the flavor with a black `shelfBackground`. Preview: `docs/mockups/background-preview.png`. |
| Top-right logo (32.74-3) | `mipmap-nodpi/app_logo_semi_red.png` and `app_logo_semi_grey.png` (the browse `appLogo` badge, 50dp high) hold the « Oculo » wordmark with white letters (from `docs/mockups/logos-v3/oculo-wordmark-dark.png`, 632×200, content 80% of the height). |
| Home (32.74-3) | `common/.../oculo/OculoData.java` (prefs, `DataSaverBase`) and `OculoSettings.java` (category « Home » in Settings > Main UI): Home reloads on return to the app when older than 10 minutes (upstream 3 h); the news/sports row-title filter is a toggle, off by default; hiding watched videos in Home's first row is a toggle, on by default. Defaults and strings in `common/src/main/res/values/oculo.xml` (upstream behaviour, EN) and `values-fr/oculo.xml` (FR); Oculo defaults in `smarttubetv/src/oculo/res/values/oculo_config.xml`. |
| `common/.../utils/Utils.java` | Adds `com.francoisauger.oculo` to `KNOWN_PACKAGES` so the full About screen (credits, links) is shown. |
| Launcher icon (32.74-5) | Adaptive icon `mipmap-anydpi-v26/app_icon.xml`: background `drawable/oculo_icon_background.xml` = full-bleed logo gradient (`#2FE3F0` → `#22D3C8` → `#78F5BE`, top-left to bottom-right), foreground `drawable/oculo_icon_foreground.xml` = the TV-screen frame alone in `#0F4A52`, interior transparent, farthest point 32dp from center (inside the 66dp safe zone). Before, the icon was a transparent PNG (`mipmap-nodpi` + `mipmap-nodpi-v30`) that Google TV put on a white disc; the v30 PNG is removed and `mipmap-nodpi/app_icon.png` (API < 26) is the same design, full-bleed, opaque. Generated by `docs/mockups/icon/gen_icon.sh` (layers, legacy PNG, preview `docs/mockups/icon-preview.png`). `app_icon_alt` (upstream's optional alternate icon) and the banner are unchanged. |
| QR codes (32.74-5) | `common/.../oculo/OculoQr.java` generates QR codes on the device with ZXing core (`com.google.zxing:core:3.3.3`, Apache 2.0, Java 7 so no desugaring on minSdk 17/21): black modules on white, 4-module quiet zone, integer pixels per module. The sign-in screen loads the bitmap (600 px+); `Utils.toQrCodeLink()` now returns a self-contained `data:text/html` page (PNG inline, 78 % of the screen height, pixelated scaling) for the built-in web view (share QR link of a video, About links and feedback). Nothing goes to api.qrserver.com any more. Test: `common/src/test/.../oculo/OculoQrTest.java` (encodes an activation URL, decodes it back, checks the quiet zone). The app has no open-source licenses screen; ZXing's Apache 2.0 license is noted here. |
| About (32.74-5) | No « Install bridge » button (`oculo_show_install_bridge` = false in `oculo_config.xml`, upstream default true in `common/src/main/res/values/oculo.xml`) and no donations (`smarttubetv/src/oculo/res/values/donations.xml` is an empty `donations` array; `appendDonation()` already skips an empty section, so no header). |
| Player placeholder (32.74-5) | `Video.getBackgroundUrl()` falls back to a 1×1 black PNG `data:` URL (Glide decodes it locally, center-cropped to full screen) instead of `via.placeholder.com`. |
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
| `common/.../app/models/data/VideoGroup.java` | `isWatchedSuggestion()`: Home's first row is filtered only when `OculoData.isHomeHideWatchedEnabled()`. |
| `common/.../app/presenters/settings/MainUISettingsPresenter.java` | `show()`: one call to `OculoSettings.appendHomeCategory()`. |
| `common/build.gradle` | One dependency line: `com.google.zxing:core:3.3.3`. |
| `common/.../utils/Utils.java` (32.74-5) | `toQrCodeLink()` returns `OculoQr.toQrPageUrl(data)`; the `api.qrserver.com` template constant is removed. |
| `smarttubetv/.../ui/signin/SignInFragment.java` | Glide loads `OculoQr.toBitmap(url, 600)` instead of `Utils.toQrCodeLink(url)`. |
| `common/.../app/presenters/settings/AboutSettingsPresenter.java` | `appendInstallBridge()`: returns early when `R.bool.oculo_show_install_bridge` is false. |
| `common/.../app/models/data/Video.java` | `BLACK_PLACEHOLDER_URL` = local 1×1 black PNG `data:` URL instead of `via.placeholder.com`. |
| `smarttubetv/.../ui/mod/leanback/misc/SeekBar.java` | Remaining part color read from `R.color.oculo_seek_bar_remaining` instead of `Color.GRAY` (upstream value kept in `smarttubetv/src/main/res/values/oculo.xml`). |

New files outside the flavor (not upstream): `common/.../oculo/`, `common/src/test/.../oculo/`, `common/src/main/res/values/oculo.xml`,
`common/src/main/res/values-fr/oculo.xml`, `smarttubetv/src/main/res/values/oculo.xml`.

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
