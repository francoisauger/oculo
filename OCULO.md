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
| `smarttubetv/src/oculo/res/` | Name "Oculo", launcher icon, Android TV banner, browse logo, global search label and authority (`xml/searchable.xml`, `search_authority`), update manifest URL (`update_urls`), About links and upstream credit (`feedback`, `releases_url`, `sources_url`). |
| `common/.../utils/Utils.java` | Adds `com.francoisauger.oculo` to `KNOWN_PACKAGES` so the full About screen (credits, links) is shown. |
| `smarttubetv/google-services.json` | Removed (upstream's Firebase project; only used by the `stbeta`/`ststable` flavors). |
| `.gitignore` | Keystores and signing files. |
| `docs/` | Logo work (`docs/mockups/logos-v3/` holds the approved assets). |

Every place tied to the applicationId: the search provider authority comes from
`leanbackassistant/src/stfdroid/AndroidManifest.xml` (`${applicationId}.search_provider`) and must match
`search_authority` and `xml/searchable.xml` in `smarttubetv/src/oculo/res`; the other authorities
(`.androidx-startup`, `.update_provider`) use `${applicationId}` directly.

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
