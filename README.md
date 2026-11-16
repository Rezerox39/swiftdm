# ARMz

[![Build APK](https://github.com/Rezerox39/swiftdm/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Rezerox39/swiftdm/actions/workflows/build-apk.yml)

A native Android download manager with segmented, resumable transfers and a
React Bits interface rendered inside a single edge-to-edge WebView. The native
engine, foreground service, notifications, queue and network layers stay in
Kotlin; the U1 UI is React 18 + TypeScript + Tailwind built with Vite.

Kotlin · React 18 · TypeScript · Tailwind CSS · Vite · Hilt · Room · OkHttp ·
Coroutines/Flow · Timber.


- minSdk 26, target/compileSdk 35
- APKs are built on GitHub Actions, never on a developer machine

## Status

| Phase | Scope | State |
| --- | --- | --- |
| P0 | Scaffold, Gradle + version catalog, manifest/permissions, Hilt, design tokens, placeholder home, CI | shipped |
| P1 | Segmented engine: probe, split, write at offsets, retry, resume, Room, debug lab | shipped |
| P2 | Finalization, public `Downloads/ARMz`, real Downloads UI, splash/intro, Engine Lab diagnostics | shipped |
| U1 | React Bits web UI, Vite build into Android assets, typed JSON WebView bridge, U1 splash/insets/IME | in review |
| P3 | Foreground service, queue, notifications, network policy, speed limiter | in progress |
| P4 | Share/view intents, clipboard capture, per-download headers | planned |
| P5 | Detail screen, Settings, in-app browser | planned |
| P6 | Polish, signed release, GitHub Release | planned |

## Install the APK

1. Open the [latest release](https://github.com/Rezerox39/swiftdm/releases) and
   download the APK attached to it, or use the **ARMz-debug** artifact from
   the [latest green build](https://github.com/Rezerox39/swiftdm/actions/workflows/build-apk.yml).
2. Install `ARMz-debug.apk` on the phone (allow installing from this source when
   Android asks).
3. The app opens on the Downloads screen after the short ARMz intro.

The APK is debug-signed by the CI runner, so it is installable but not suitable
for a Play Store upload. Signing for a store build arrives with P6.

## U1 web interface

The U1 UI lives in `web/` and is built into `app/src/main/assets/web/` before
Gradle runs. It uses React Bits motion components (Aurora, BlurText, CountUp,
Magnet, SpringCheck and Stepper) under MIT + Commons Clause; attribution is in
`THIRD_PARTY_NOTICES.md` and in the in-app Settings About row.

The WebView loads the built app from
`https://appassets.androidplatform.net/assets/web/index.html`. JavaScript talks
to Kotlin only through JSON strings on `window.ARMz`; Kotlin validates every
input, and native snapshots/settings arrive through `window.__armz_emit` at a
200 ms maximum cadence. The Android 12 splash is held until the web app calls
`ARMz.ready()` with a 2 second fallback.

Paused transfers keep their partial bytes and resume by validating ETag /
Last-Modified; changed content restarts from zero. Finished files are assembled,
coverage-verified, fsynced, SHA-256 checked and published into the public
`Downloads/ARMz` collection. Only after the publish succeeds does the row become
`Completed`.

## Build

CI runs the web build and Vitest suite first, then the JVM tests and Android
assembly. Locally, with Node 20:

```bash
cd web
npm ci
npm run build
npm run test
```

With JDK 17 and the Android SDK installed:

```bash
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

The Gradle wrapper (8.9), version catalog and workflow are committed so a
clean checkout builds without extra setup. Signing material is never committed;
release signing reads `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
`KEY_PASSWORD` from Actions secrets only.

## Design system

| Token | Value |
| --- | --- |
| Blue | `#007AFF` light / `#0A84FF` dark |
| Green / Red / Orange | `#34C759` / `#FF3B30` / `#FF9500`, dark variants per HIG |
| Backgrounds | `#F2F2F7` with white cards; `#000000` with `#1C1C1E` cards |
| Type scale | Inter bundled; Large Title 34 bold → Caption 12 |
| Radii | Cards 12–14dp, sheets 28dp, capsules fully round |
| Separators | 0.5dp hairlines |
| Motion | spring damping 0.8 / stiffness 300, press-scale 0.97, no ripple |
| Icons | Material Symbols Rounded, weight 400 |

All tokens live in `ui/theme` (`Color.kt`, `Type.kt`, `Shape.kt`, `Motion.kt`).
Inter and the Material Symbols subset are bundled with their licenses in
`licenses/`; no Apple fonts or assets are used.

## Architecture

Single `app` module, clean layering:

```
web/       Vite + React 18 + TypeScript + Tailwind UI and Vitest tests
bridge/    JSON DTOs, Kotlin validation and the WebView JavascriptInterface
data/      Room entities/DAO/database and the DownloadRepository (P1+)
di/        Hilt modules (OkHttp, Room, engine)
engine/    pure-Kotlin/JVM probe, segmenter, writer, retry, resume, speed meter
ui/theme/  retained native design tokens for the splash/fallback surface
```

The engine has no Android imports: it is unit-tested on the JVM with
MockWebServer. Run the same command CI uses:

```bash
./gradlew testDebugUnitTest
```

## License

MIT, see [LICENSE](LICENSE). Fonts are covered by their own licenses, see
[licenses/THIRD_PARTY.md](licenses/THIRD_PARTY.md).
