# Verification — AMz Downloader 1.0.0 U4 canvas UI

Date: 2026-11-18

## Scope

This record verifies the eight-screen canvas-specified AMz Downloader UI, the
theme-only visual-constant cleanup, CI compilation/build, packaged APK identity,
signing declaration, release asset integrity, and the remaining runtime gap.
The download engine was not changed.

## Verified source

- Branch: `feat/amz-downloader-app`
- Final release tag commit: `3ff2cb59ede38b31856e092fc14936690c74173b`
- Tag: `v1.0.0-amz-u4`
- Remote tag ref: `3ff2cb59ede38b31856e092fc14936690c74173b refs/tags/v1.0.0-amz-u4`
- Release: https://github.com/Rezerox39/swiftdm/releases/tag/v1.0.0-amz-u4

Commit sequence for this release:

- `a7e4295` — added the eight AMz UI screens, navigation, ViewModel, fake
  repository, reusable components, theme, fonts, permissions, and activity wiring.
- `4a47ceb` — fixed Compose import/API errors found by CI.
- `eb7d666` — removed the temporary compile-diagnostic workflow.
- `3ff2cb5` — moved all remaining screen/component literals into `amzui/theme`,
  centralized motion durations, and bumped to version code 77 / `1.0.0-amz-u4`.

`v1.0.0-amz-u3` was published earlier on `eb7d666` but is superseded by u4 because
u3 still contained a small number of literal dp/color/duration values in screens
and components. It was left in place rather than deleted.

## CI result

- Final branch release-candidate CI: https://github.com/Rezerox39/swiftdm/actions/runs/44279347855
  - Head: `3ff2cb5`
  - Result: **success**
  - Job: `Test and build arm64 FOSS debug`
- Tag publish CI: https://github.com/Rezerox39/swiftdm/actions/runs/44279347630
  - Head: `3ff2cb5`
  - Result: **success**
  - Purpose: rebuild tagged source, create prerelease, and upload APK asset.
- Earlier compile diagnostic: https://github.com/Rezerox39/swiftdm/actions/runs/44273448219
  - Head: `4a47ceb`
  - Result: **success**, `AMz Kotlin diagnostic errors: 0`
- Earlier branch build on the same fix commit: https://github.com/Rezerox39/swiftdm/actions/runs/44273448186
  - Result: **success**

The temporary diagnostic workflow was removed before the final tag and is not in
the shipped source.

## Theme-only source checks

At the final commit, the following scans found no literal dp/sp/color values in
`amzui/screens` or `amzui/components` outside theme-owned references:

- `rg '[0-9]+\.dp|0xFF|Color\(' app/src/main/kotlin/.../amzui`
  - matches only occur inside `amzui/theme/AmzTheme.kt`, except the intentional
    `AmzColors.Background.toArgb()` WebView background call and a zero-offset
    `0.dp`.
- `rg 'tween\([0-9]|delay\([0-9]|durationMs = [0-9]' ...`
  - no literal animation durations remain outside `AmzDuration`.

Engine preservation:

- `git diff --name-only v1.0.0-amz-u2..HEAD -- app/src/main/kotlin/.../downloader`
  reports only `DownloaderActivity.kt`, which now hosts `AmzUiApp`.
- `DownloaderService`, `DownloadBootReceiver`, download engine classes, and
  repository wiring were not edited.

## Packaged APK identity

The APK was downloaded from the published release asset and parsed locally with
`pyaxmlparser`. No APK code was executed and no device, emulator, or adb was used.

- `manifest.package`: `com.abhi.armz.debug`
  - Source `applicationId` is `com.abhi.armz`; the debug build adds `.debug`.
- `manifest.versionCode`: `77`
- `manifest.versionName`: `1.0.0-amz-u4`
- `uses-sdk`: `minSdkVersion = 26`, `targetSdkVersion = 37`
- First-party launcher activity: `com.mochyy.mochymusic.downloader.DownloaderActivity`
- First-party service: `com.mochyy.mochymusic.downloader.DownloaderService`
- First-party receiver: `com.mochyy.mochymusic.downloader.DownloadBootReceiver`
- Requested permissions include INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK,
  FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, POST_NOTIFICATIONS,
  RECEIVE_BOOT_COMPLETED, REQUEST_INSTALL_PACKAGES, READ_MEDIA_IMAGES,
  READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, and READ_EXTERNAL_STORAGE.
- No Mochy player activity or media-session service appears as a first-party
  shipping component. Library-provided AndroidX car, WorkManager, Startup,
  ProfileInstaller, and Compose preview components remain present as expected.

## Signing declaration

REA `inspect_apk_signature` succeeded on the final APK:

- Evidence: `ev_ef2b806a599c4c87061955a96bba7f72c8720ce47626b0545fee619d9b4f99b1`
- APK SHA-256: `b6df46aa86cd05c0420819a51d3388880273c0e7bda68957cb83b49bad0f5ff7`
- Schemes: v2 present with one signer; v1, v3, and v3.1 absent
- Declared v2 signer certificate SHA-256:
  `7e8cc507ae7857544361073e936c37b1670d503ed67c89c7f5df07defec49e76`
- `self_consistent`: `true`
- No source stamp, no rotation lineage
- Signature verification, certificate authenticity, and v1 entry coverage were
  not performed; this is a declaration/integrity check, not an authenticity verdict.

The CI-generated debug keystore is recreated per build, so the signer fingerprint
is expected to differ from earlier prereleases.

## Release verification

- Release ID: `449790450`
- Asset ID: `762845261`
- Asset name: `amz-downloader-arm64-foss-debug.apk`
- Asset size: `88,975,310` bytes
- Asset digest reported by GitHub:
  `sha256:b6df46aa86cd05c0420819a51d3388880273c0e7bda68957cb83b49bad0f5ff7`
- Downloaded release asset SHA-256:
  `b6df46aa86cd05c0420819a51d3388880273c0e7bda68957cb83b49bad0f5ff7`
- Release state: prerelease, published, not draft
- Published at: `2026-11-18T09:57:18Z`

## Functional scope implemented but not runtime-verified

- Splash, Import, Setup, Home, Details, Library, Browser, and Profile screens at
  the 390x844 canvas target.
- Minimal Stats and Settings destinations.
- Pure-black page background; Inter 400/500/600/700/800 bundled; mono stats font.
- Reusable component kit: `FileTile`, `RowCard`, `HeroCard`, `PillBar`,
  `GlassNavBar`, `Chip`, `AmzSwitch`, `IdCard`, `ProgressFill`, `Sparkline`,
  `Sheet`, `RoundIconButton`, `StatCard`, `SectionHeader`.
- 300 ms fake tick, clamped speed, hero/queue progress behavior, queue cycling,
  and first-add PillBar state change.
- Tab navigation without back-stack growth, >=44dp touch targets, icon content
  descriptions, gesture-nav insets, and reduced-motion final states.
- Engine remains on the existing `AmzDownloadsRepository` wiring boundary; the
  shipped build uses the fake repository.

## Known limitations

- No local JDK, Android SDK, device, or emulator was available on the phone;
  compilation, tests, and APK assembly were verified in GitHub Actions.
- The supplied PAT could push source/tags and query Actions status, but could not
  download CI artifacts; the tag workflow published the release asset directly.
- APK signature verification was not performed; the APK is debug-signed by the
  CI-generated debug keystore.
- No runtime/install/device test was performed. Rendering, animations, gestures,
  immersive behavior, WebView capture, permissions, and live download motion
  remain implemented but explicitly not observed in motion.
- The supplied GitHub PAT must be revoked or rotated after all GitHub writes are
  complete.

## Conclusion

At tag commit `3ff2cb5`, the canvas-specified AMz Downloader UI passes the tagged
CI build and publishes a prerelease whose ARM64 FOSS debug APK has package
`com.abhi.armz.debug`, version code `77`, version name `1.0.0-amz-u4`, and SHA-256
`b6df46aa86cd05c0420819a51d3388880273c0e7bda68957cb83b49bad0f5ff7`. The
downloaded release asset matches GitHub's reported digest exactly. The engine
remains untouched, visual constants are centralized in `amzui/theme`, and
runtime motion remains unverified.
