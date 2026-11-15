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
| P1 | Segmented engine: probe, split, write at offsets, retry, resume, Room | next |
| P2 | Foreground service, queue, notifications, network policy, speed limiter | planned |
| P3 | Downloads list and Add Download sheet | planned |
| P4 | Share/view intents, clipboard capture, per-download headers | planned |
| P5 | Detail screen, Settings, in-app browser | planned |
| P6 | Polish, signed release, GitHub Release | planned |

## Install the APK from CI

1. Open the [latest green build](https://github.com/Rezerox39/swiftdm/actions/workflows/build-apk.yml).
2. Scroll to **Artifacts** and download **SwiftDM-debug**.
3. Unzip it and install `app-debug.apk` on the phone (allow installing from
   this source when Android asks).
4. The app opens on the Downloads placeholder with the floating tab bar.

The APK is debug-signed by the CI runner, so it is installable but not suitable
for a Play Store upload. P6 adds the signed release job.

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
ui/        Compose screens, components, theme, navigation
domain/    use cases and models (P1+)
data/      Room, DataStore, repositories (P1+)
engine/    probe, segmenter, writer, retry, resume, service (P1+)
```

## License

MIT, see [LICENSE](LICENSE). Fonts are covered by their own licenses, see
[licenses/THIRD_PARTY.md](licenses/THIRD_PARTY.md).
