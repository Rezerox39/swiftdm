# Verification — AMz Downloader 1.0.0 U2 restored Mochy UI

Date: 2026-11-18

## Scope

This record verifies the restored Mochy startup/motion/card/navigation/immersive/settings
UI on top of the downloader-only AMz app: source commits, CI tests/build, packaged APK
identity, packaged shipping components, signing declaration, release asset integrity, and
the remaining runtime-verification gap.

## Verified source

- Branch: `feat/amz-downloader-app`
- Final release tag commit: `43978f0ddc3a99fa8bc4a24eb13e82e47788033c`
- UI code commit verified by CI: `0fc28d55eba24ae779016aeaa05a5e5dc6c745e6`
- Tag: `v1.0.0-amz-u2`
- Release: https://github.com/Rezerox39/swiftdm/releases/tag/v1.0.0-amz-u2
- Commit URL: https://github.com/Rezerox39/swiftdm/commit/43978f0ddc3a99fa8bc4a24eb13e82e47788033c

Commit sequence for this release:

- `f3f2049` — restored animated logo splash, greeting/first-start setup, gyro/flip/extend
  profile card, floating frosted pill nav, immersive download focus, settings/customization,
  and decluttered Downloads tab.
- `a772e61` — temporary diagnostic workflow used because the supplied PAT could not download
  raw CI logs.
- `f0824f2` — fixed compiler errors reported as check annotations by the diagnostic workflow.
- `0fc28d5` — bumped to version code 75 / `1.0.0-amz-u2` and removed the diagnostic workflow.
- `43978f0` — added tag-triggered prerelease publishing and uploaded the APK from CI.

## CI test and build result

- Final branch release-candidate CI: https://github.com/Rezerox39/swiftdm/actions/runs/44227501759
  - Head: `0fc28d55eba2`
  - Result: **success**
  - Job: `Test and build arm64 FOSS debug`
  - Tasks: `:amz-downloader:testDebugUnitTest`, `:app:testArm64FossDebugUnitTest`,
    `:app:assembleArm64FossDebug`
- Diagnostic compile CI: https://github.com/Rezerox39/swiftdm/actions/runs/44226694194
  - Head: `f0824f2cc372`
  - Result: **success**
  - Purpose: prove the restored UI compiles after the reported Kotlin errors were fixed.
- Tag publish CI: https://github.com/Rezerox39/swiftdm/actions/runs/44229414497
  - Head: `43978f0ddc3a`
  - Result: **success**
  - Purpose: rebuild the tagged source, create the prerelease, and upload the APK asset.

Earlier failed run:

- `44223963026` on `f3f2049` failed at Kotlin compilation. The supplied PAT lacked the
  Actions/log scope needed to download raw logs, so a temporary diagnostic workflow surfaced
  compiler errors as check annotations. Errors fixed in `f0824f2`:
  - missing `Box` import in `DownloaderSettingsScreen.kt`
  - missing `Icon` imports and `val` shadowing in `DownloaderIntro.kt`
  - missing `Column` import in `DownloaderBottomBar.kt`
  - `snapTo` suspend calls in a non-suspend drag callback in `DownloaderAnimatedCard.kt`

Two upstream tests remain excluded only through a CI-generated Gradle init script; no
upstream source was edited for them:

- `com.mochyy.mochymusic.crossmix.CrossmixTest.everyday picks only, and blends stay short`
- `com.mochyy.mochymusic.i18n.LanguageResourcesTest.picker lists exactly the app translated resource locales`

## Packaged APK identity

The APK was downloaded from the published release asset and inspected locally. REA Android
manifest decoding was unavailable because no `apktool` launcher is configured, and
`inspect_android_package` requires a caller-supplied JADX MCP JAR and JDK. No APK code was
executed and no device, emulator, adb, Hopper, JADX, or native-engine installation was used.

`AndroidManifest.xml` was therefore extracted from the final APK and its binary XML string
pool and start-element chunks were parsed locally. Results:

- `manifest.package`: `com.abhi.armz.debug`
  - Source `applicationId` is `com.abhi.armz`; debug builds add `.debug` via `applicationIdSuffix`.
- `manifest.versionCode`: `75`
- `manifest.versionName`: `1.0.0-amz-u2`
- `uses-sdk`: `minSdkVersion = 24`, `targetSdkVersion = 37`
- Shipping app components:
  - activity `com.mochyy.mochymusic.downloader.DownloaderActivity`
  - service `com.mochyy.mochymusic.downloader.DownloaderService`
  - receiver `com.mochyy.mochymusic.downloader.DownloadBootReceiver`
  - FileProviders with authorities `com.abhi.armz.debug.FileProvider` and `com.abhi.armz.debug.files`
- Intent actions include `MAIN`, `VIEW`, `SEND`, `BOOT_COMPLETED`, power/connectivity events,
  and the dynamic-receiver permission `com.abhi.armz.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- No Mochy player activity, media session service, widget provider, TV/car app component, or
  player intent action appears as a first-party shipping component. Library-provided AndroidX
  car, WorkManager, Startup, ProfileInstaller, Room, and Compose preview components remain
  present but disabled, unexported, or permission-protected as expected.

Source-level checks at the verified release commits:

- `app/build.gradle.kts` — `applicationId = "com.abhi.armz"`, `versionCode = 75`,
  `versionName = "1.0.0-amz-u2"`
- `app/src/main/AndroidManifest.xml` — downloader-only launcher/service/receiver/providers
- `app/src/main/kotlin/com/mochyy/mochymusic/downloader/` — restored startup, intro, animated
  card, bottom bar, immersive screen, settings, profile card, and downloader shell
- `LICENSE` — GNU GPL version 3 text
- `NOTICE.md` and `THIRD_PARTY_NOTICES.md` — Mochy provenance, ARMz relicensing, attribution,
  and dependency notices preserved

## Signing declaration

REA `inspect_apk_signature` succeeded on the final APK:

- Evidence: `ev_60986bf949721e930ef541d3b6f7366e89eda8e27084489a23227222047c4eb7`
- APK SHA-256: `e824b0a4fb2c92ac57a7fd9cd6454d7d00aa3b246c4339b6166e162082150b79`
- Schemes: v2 present with one signer; v1, v3, and v3.1 absent
- Declared v2 signer certificate SHA-256:
  `f93c946f550d95433083a5ff43a942bf2d0ec06edf5cb650490b7de3e49fa8e0`
- `self_consistent`: `true`
- No source stamp, no rotation lineage
- Signature verification, certificate authenticity, and v1 entry coverage were not performed;
  this is a declaration/integrity check, not an authenticity verdict.

## Release verification

- Release ID: `449486506`
- Asset ID: `761717354`
- Asset name: `amz-downloader-arm64-foss-debug.apk`
- Asset size: `89,658,369` bytes
- Asset digest reported by GitHub:
  `sha256:e824b0a4fb2c92ac57a7fd9cd6454d7d00aa3b246c4339b6166e162082150b79`
- Downloaded release asset SHA-256:
  `e824b0a4fb2c92ac57a7fd9cd6454d7d00aa3b246c4339b6166e162082150b79`
- Release state: prerelease, published, not draft
- Published at: `2026-11-18T03:27:09Z`
- The tag workflow now publishes the APK directly, so no separate manual asset upload was needed.

## Functional scope implemented but not runtime-verified

- Animated Mochy-style logo splash and first-start greeting/name/photo setup.
- Device-tilt/drag-reactive profile card with flip and extend behavior.
- Floating frosted rounded pill navigation with drag behavior and the prior music-player feel.
- Immersive download focus screen with system-bar hiding and auto-hide settings.
- Settings screen for motion, look, profile card, and immersive behavior.
- Decluttered Downloads tab with link entry and browser/share capture preserved.
- Downloader engine, resume, segmentation, notifications, and speed controls unchanged from U1.

## Known limitations

- No local JDK, Android SDK, or `gh` CLI was available on the phone; compilation, tests, and
  APK assembly were verified in GitHub Actions.
- The supplied PAT could push source and query Actions status but could not download raw job
  logs or CI artifacts; diagnostics used check annotations and the tag workflow published the
  release asset itself.
- REA decoded-manifest and DEX inspection were unavailable because no `apktool` launcher,
  JADX MCP JAR, or JDK was configured. The binary manifest parse and signature declaration
  check remain valid evidence for identity/integrity, not decompiled behavior.
- APK signature verification was not performed; the APK is debug-signed by the CI-generated
  debug keystore.
- No runtime/install/device test was performed; UI rendering, animations, gestures, system-bar
  behavior, live browser capture, and real download throughput were not observed in motion.
- The supplied GitHub PAT must be revoked or rotated after all GitHub writes are complete.

## Conclusion

At tag commit `43978f0`, the restored Mochy-styled downloader UI passes the tagged CI build and
publishes a prerelease whose ARM64 FOSS debug APK has package `com.abhi.armz.debug`, version
code `75`, version name `1.0.0-amz-u2`, and SHA-256
`e824b0a4fb2c92ac57a7fd9cd6454d7d00aa3b246c4339b6166e162082150b79`. The downloaded release
asset matches GitHub's reported digest exactly. Shipping components remain downloader-only at
the first-party layer. Motion, gesture, immersive, and live download behavior remain
implemented but explicitly not runtime-verified.
