# SwiftDM

[![Build APK](https://github.com/Rezerox39/swiftdm/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Rezerox39/swiftdm/actions/workflows/build-apk.yml)

A native Android download manager with segmented, resumable transfers and an
interface built around the Apple Human Interface Guidelines: large titles,
grouped inset cards, translucent bars, spring motion and full dark mode.

Kotlin · Jetpack Compose · Material 3 · Hilt · Room · DataStore · OkHttp ·
Coroutines/Flow · Coil · Timber.

- Package: `com.abhi.swiftdm`
- minSdk 26, target/compileSdk 35
- APKs are built on GitHub Actions, never on a developer machine

## Status

| Phase | Scope | State |
| --- | --- | --- |
| P0 | Scaffold, Gradle + version catalog, manifest/permissions, Hilt, design tokens, placeholder home, CI | shipped |
| P1 | Segmented engine: probe, split, write at offsets, retry, resume, Room, debug lab | shipped |
| P2 | Foreground service, queue, notifications, network policy, speed limiter | planned |
| P3 | Downloads list and Add Download sheet | planned |
| P4 | Share/view intents, clipboard capture, per-download headers | planned |
| P5 | Detail screen, Settings, in-app browser | planned |
| P6 | Polish, signed release, GitHub Release | planned |

## Install the APK

1. Open the [latest release](https://github.com/Rezerox39/swiftdm/releases) and
   download the APK attached to it, or use the **SwiftDM-debug** artifact from
   the [latest green build](https://github.com/Rezerox39/swiftdm/actions/workflows/build-apk.yml).
2. Install `app-debug.apk` on the phone (allow installing from this source when
   Android asks).
3. The app opens on the Downloads screen with the floating tab bar.

The APK is debug-signed by the CI runner, so it is installable but not suitable
for a Play Store upload. Signing for a store build arrives with P6.

## Engine Lab (P1)

Debug builds expose **Downloads → Engine Lab**, a test bench for the real
engine:

- edit the URL (default: a 100 MB file with ETag and byte ranges),
- choose 1–16 segments with the stepper,
- Start / Pause / Resume / Cancel,
- watch live speed, ETA, per-segment ranges and the final SHA-256.

Paused transfers keep their partial bytes and resume by validating ETag /
Last-Modified; changed content restarts from zero. Files are written to
app-private storage until P2 moves finished downloads into the public
Downloads collection.

The 1-vs-8-segment network benchmark runs from **Actions → Engine benchmark**,
which prints measured MiB/s for both configurations into the workflow log.

## Build

CI runs on every push to `main` and through **Actions → Build APK → Run
workflow**. Locally, with JDK 17 and the Android SDK installed:

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
ui/        Compose screens, components, theme, navigation, debug Engine Lab
data/      Room entities/DAO/database and the DownloadRepository (P1+)
di/        Hilt modules (OkHttp, Room, engine)
engine/    pure-Kotlin/JVM probe, segmenter, writer, retry, resume, speed meter
```

The engine has no Android imports: it is unit-tested on the JVM with
MockWebServer. Run the same command CI uses:

```bash
./gradlew testDebugUnitTest
```

## License

MIT, see [LICENSE](LICENSE). Fonts are covered by their own licenses, see
[licenses/THIRD_PARTY.md](licenses/THIRD_PARTY.md).
