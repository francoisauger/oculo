# Oculo device support

Back to the [README](../README.md).

## Requirements

- **Android 5.0 (API 21) or later** (Oculo's `minSdkVersion`).
- An **Android TV or Google TV** device, or another Android device with a large screen and a remote, game controller or keyboard. Touch and mouse input are not supported.
- A processor matching one of the APKs: `armeabi-v7a` (32-bit ARM), `arm64-v8a` (64-bit ARM) or `x86`. There is no `x86_64` build: on x86_64 devices that can run 32-bit apps, use the `x86` APK.

Oculo is developed and tested on a TCL Google TV (`armeabi-v7a`) and the Android TV emulator (`x86`). Other Android TV and Google TV devices that meet the Android version above should run it as well.

## Supported

- Android TVs and Google TVs (Sony, Philips, TCL, Hisense…), and TVs with Google TV built in.
- Google TV Streamer, Chromecast with Google TV (see the extra step below).
- NVIDIA Shield (all models).
- Amazon Fire TV devices released before October 2025 that run Fire OS (Android based).
- Xiaomi Mi Box and other Android TV boxes (see the note on Chinese firmware below).

## Partial support

The app may install and run on these, but it is not designed for them:

- Android tablets, car head units and Android based VR headsets, with a remote, controller or keyboard.
- Virtual machines such as Waydroid.

## Not supported

- **Phones.** Oculo is a TV app: no portrait mode, no touch navigation, and no plan for a phone version. You can still cast to Oculo from the YouTube app on a phone (see [Using Oculo](../README.md#using-oculo) in the README).
- **Non-Android TVs and boxes:** Samsung (Tizen), LG (webOS), Roku, Apple TV.
- **Fire TV devices running Vega OS:** since October 2025, new Amazon devices such as the Fire TV Stick 4K Select no longer run Android.

On an unsupported TV, use an Android TV or Google TV stick or box.

## Installing

Get the APK from [GitHub Releases](https://github.com/francoisauger/oculo/releases/latest) only. Which APK to pick and the ways to copy it to the TV are in the README's [Installation](../README.md#installation) section. Android must allow the app you install from (downloader, file manager) to install unknown apps.

### Chromecast with Google TV and other Google TV devices

Installing apps from outside the Play Store is blocked until developer options are turned on:

1. **Turn on developer options.** Settings › System › About, scroll to _Android TV OS build_ and select it repeatedly until a message says you are now a developer.
2. **Allow the installing app.** Settings › Apps › Security & Restrictions › Unknown sources, and turn on the app you will use (your downloader or file manager).

Menu names vary slightly between devices and Android versions. Then follow the README's installation steps. For in-app updates, Android asks the same permission for Oculo itself the first time you install an update.

### Xiaomi devices with Chinese firmware

Xiaomi's Chinese firmware has been reported to block installing some sideloaded apps; the international firmware is not affected. Oculo has not been tested on it. If installation is refused: use the international firmware for your device, or install with ADB.

## Troubleshooting

- **Installation fails:** check free space, download the APK again (compare it with `SHA256SUMS.txt`), and make sure you picked the APK for your processor (`armeabi-v7a` if unsure).
- **The app crashes at launch:** install it to internal storage, not to an SD card or external storage.
- **An update fails:** usually free space or a download cut off; try again from Settings › System › Check for updates. An update must be signed with the same key as the installed app: a build you made yourself cannot be updated by Oculo's releases.
- **Updates never show:** if a DNS filter is in use, allow `github.com` and `release-assets.githubusercontent.com` (also `objects.githubusercontent.com`).
