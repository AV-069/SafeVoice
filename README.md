# Project SafeVoice — v1 (offline scam-number shield)

Offline, privacy-first scam-call warning app for senior citizens. v1 detects
calls from a locally bundled blocklist and shows a full-screen warning. No
audio, no cloud, no network calls.

## What changed from the old build

- The old `CallReceiver` had no detection logic at all — it just opened the
  app. This version actually checks the incoming number against
  `android/app/src/main/assets/scam_numbers.json` and, on a match, shows a
  native full-screen warning (`ScamWarningActivity`) that works even if the
  app was killed.
- The old bridge call used `getBridge().eval(...)` with string-concatenated
  JS — fragile and deprecated. This version uses Capacitor's
  `Bridge.triggerJSEvent(...)`, the supported way to fire window events into
  the web layer.
- The old manifest declared permissions but never requested them at runtime.
  `MainActivity` now requests `READ_PHONE_STATE` / `POST_NOTIFICATIONS` at
  startup and sends the user to the overlay-permission settings screen if
  needed, then reports live status back to the React dashboard.
- `CallReceiver` now also handles `EXTRA_STATE_IDLE`, not just `RINGING`, so
  the dashboard doesn't get stuck showing "call in progress" forever.

## Setup — this matters, read before building

This repo ships the **custom app-level pieces** (manifest additions, Java
classes, layout, blocklist asset). It does **not** ship a hand-written Gradle
scaffold, because version-mismatched hand-written Gradle files were the
biggest source of build failures in the old project (see "Pitfalls" below).
Instead, generate the scaffold with Capacitor itself, then merge these files
in:

```bash
npm install
npm run build
npx cap add android          # generates android/ with a scaffold matching your installed Capacitor version
```

Then, into the generated `android/` folder, merge in from this repo:

- `android/app/src/main/AndroidManifest.xml` → merge the `<uses-permission>`
  lines and the `<activity>`/`<receiver>` entries into the generated one
  (don't just overwrite — keep Capacitor's generated `<application>` attributes).
- `android/app/src/main/java/com/safevoice/app/*.java` → copy in as-is.
- `android/app/src/main/res/layout/activity_scam_warning.xml` → copy in.
- `android/app/src/main/res/values/strings.xml` → merge the two extra
  `<string>` entries into the generated file.
- `android/app/src/main/assets/scam_numbers.json` → copy in.

In the generated `android/app/build.gradle`, make sure these are present
(add if missing):

```gradle
dependencies {
    implementation "androidx.appcompat:appcompat:1.7.0"
    implementation "androidx.core:core:1.13.1"
    // ...whatever cap add already put here
}
```

Then sync and build:

```bash
npx cap sync android
cd android
./gradlew assembleDebug
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk` — install this
directly (no Play Store, no review) via `adb install` or by sharing the file.

## Populating the blocklist

`src/data/scamNumbers.json` (mirror it into
`android/app/src/main/assets/scam_numbers.json`) ships empty on purpose — do
not fill it with invented numbers. Real sources to consider:

- Numbers you or people you know have personally received scam calls from.
- India's National Cyber Crime Reporting Portal (cybercrime.gov.in), if it
  exposes any reportable-number data.
- A simple in-app "report this number" feature (safe to add later) that lets
  users flag numbers locally — this is the most sustainable offline source.

`prefixPatterns` lets you flag a known fraudulent range (e.g. a call-center
block) instead of listing every individual number.

## Known pitfalls carried over from the original build log

- **`Unsupported class file major version 70`** — JAVA_HOME pointing at a
  Java version newer than Gradle 8.x supports. Use JDK 17 or 24, not 26+.
- **`onDestroy()` visibility** — must stay `public` to match
  `BridgeActivity`'s superclass signature.
- **`Failed to find target with hash string 'android-36'`** — usually a
  corrupted/partial SDK platform download; delete
  `<sdk>/platforms/android-36` and re-run
  `sdkmanager "platforms;android-36"`.
- **Windows file locks on `app/build`** — run `.\gradlew --stop` first, and
  avoid `clean` if OneDrive or another sync tool is watching the folder.

## Roadmap: v2 (on-device audio detection)

Not built yet — deliberately scoped out of v1 so you have something working
and installable first. When you're ready:

- Requires the user to enable speakerphone (Android does not allow
  third-party apps to tap the actual call-audio stream — Google closed that
  off for all non-default-dialer apps in 2022).
- Mic audio (already-declared `RECORD_AUDIO` permission) → on-device speech
  recognition (Android's offline `SpeechRecognizer`, or a bundled tiny
  Vosk/Whisper model for full offline reliability) → keyword/pattern engine
  for phrases like "digital arrest," "OTP," "account frozen."
- Keep everything on-device — no audio leaves the phone, consistent with the
  original privacy-first philosophy.
