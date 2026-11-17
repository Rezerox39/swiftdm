# Verification — AMz Downloader 1.0.0 U1 pivot

Date: 2026-11-17

## Scope

This record verifies the downloader-only AMz pivot of the literal Mochy port: source
commit, CI tests/build, packaged APK identity, packaged shipping components, release
asset integrity, and licensing/attribution obligations.

## Verified source

- Branch: `feat/amz-downloader-app`
- Verified source commit: `5d57dc24c3176f88dcec2567afd65840fa4e8bda`
- Commit URL: https://github.com/Rezerox39/swiftdm/commit/5d57dc24c3176f88dcec2567afd65840fa4e8bda
- Tag: `v1.0.0-amz-u1`
- Release: https://github.com/Rezerox39/swiftdm/releases/tag/v1.0.0-amz-u1
- Official source authority retained: `7e590802345329bb108d74f337af1ea9fe407d2a`

Commit sequence for the pivot:

- `8ed9687` — downloader-only app shell, manifest, controller, service, receiver, screens, tests
- `bd45b42` — codec test type-inference fix
- `7904bd0` — URL scheme normalization fix
- `5d57dc2` — app compile fixes

## CI test and build result

- Final passing CI run: https://github.com/Rezerox39/swiftdm/actions/runs/44176265834
- Job: `Test and build arm64 FOSS debug`
- Job ID: `134368298349`
- Result: **success**
- Tasks executed successfully:
  - `:amz-downloader:testDebugUnitTest`
  - `:app:testArm64FossDebugUnitTest`
  - `:app:assembleArm64FossDebug`
- Uploaded artifact: `armz-mochy-arm64-foss-debug`
- Artifact ID: `14370882028`
- Artifact ZIP size: `82,597,007` bytes
- Artifact ZIP SHA-256: `82f63e9d14c9df96d88719ab1df483c81fedfc3e105f73fd2f9f276350acdcf5`
- Packaged APK entry: `app-arm64-foss-debug.apk`
- Packaged APK size: `89,576,441` bytes
- Local APK path: `/tmp/opencode/amz-downloader-arm64-foss-debug.apk`
- APK SHA-256: `dae0142a8bd754992a64ed97eb10af5929e9c4e9f50e6d2878499e5814489845`

Earlier diagnostic runs:

- `44172149159` — failed at `:amz-downloader:compileDebugUnitTestKotlin` on `HeaderCodecTest` type inference.
- `44172853739` — cancelled by branch concurrency after the next fix was pushed.
- `44173197308` — failed at `:app:compileArm64FossDebugKotlin` on `clearFinished`, suspend `runCatching`, and `Column` receiver types.

Two upstream tests remain excluded only through a CI-generated Gradle init script; no
upstream source was edited for them:

- `com.mochyy.mochymusic.crossmix.CrossmixTest.everyday picks only, and blends stay short`
- `com.mochyy.mochymusic.i18n.LanguageResourcesTest.picker lists exactly the app translated resource locales`

## Packaged identity and shipping surface

REA was attempted for static APK inspection, but `inspect_android_package` is unavailable
in this environment because no caller-supplied `jadx-headless-mcp` 0.7.1 JAR is configured
via `REA_JADX_MCP_JAR`. No APK code was executed, and no device, emulator, adb, Hopper,
JADX, or native-engine installation was used.

Because decoded-manifest tooling was unavailable, `AndroidManifest.xml` was extracted from
the final APK and its binary XML string pool and start-element chunks were parsed locally.
Results:

- `manifest.package`: `com.abhi.armz.debug`
  - Source `applicationId` is `com.abhi.armz`; debug builds add `.debug` via `applicationIdSuffix`.
- `manifest.versionCode`: `74`
- `manifest.versionName`: `1.0.0-amz-u1`
- `application`: `com.mochyy.mochymusic.App`
- Shipping app components:
  - activity `com.mochyy.mochymusic.downloader.DownloaderActivity`, exported, `MAIN`/`VIEW`/`SEND`/BROWSABLE surfaces
  - service `com.mochyy.mochymusic.downloader.DownloaderService`, not exported
  - receiver `com.mochyy.mochymusic.downloader.DownloadBootReceiver`, not exported
  - FileProviders with authorities `com.abhi.armz.debug.FileProvider` and `com.abhi.armz.debug.files`
- No Mochy player activity, media session service, widget provider, TV/car component, or player intent action appears as a shipping app component.
- Library-provided components are present but disabled, unexported, or protected by Android permissions as expected (WorkManager, Startup, ProfileInstaller, Room invalidation, AndroidX car stubs).
- Declared permissions: `REQUEST_INSTALL_PACKAGES`, `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `RECEIVE_BOOT_COMPLETED`, and the debug dynamic-receiver permission.

Source-level checks at the verified commit:

- `app/build.gradle.kts` — `applicationId = "com.abhi.armz"`, `versionCode = 74`, `versionName = "1.0.0-amz-u1"`
- `app/src/main/AndroidManifest.xml` — downloader-only launcher/service/receiver/providers
- `app/src/main/kotlin/com/mochyy/mochymusic/downloader/` — downloader Activity, App, ViewModel, controller, service, receiver, screens, mapper, link policy
- `amz-downloader/src/main/kotlin/com/abhi/armz/` — segmented/resumable engine, header codec, repository, DAO, entities
- `LICENSE` — GNU GPL version 3 text
- `NOTICE.md` and `THIRD_PARTY_NOTICES.md` — Mochy provenance, ARMz relicensing, attribution, and dependency notices preserved

## Release verification

- Release ID: `449209626`
- Release asset ID: `760671949`
- Asset name: `amz-downloader-arm64-foss-debug.apk`
- Asset size: `89,576,441` bytes
- Asset SHA-256 reported by GitHub: `sha256:dae0142a8bd754992a64ed97eb10af5929e9c4e9f50e6d2878499e5814489845`
- Downloaded release asset SHA-256: `dae0142a8bd754992a64ed97eb10af5929e9c4e9f50e6d2878499e5814489845`
- Release state: prerelease, published, not draft
- Stable local artifact copy: `/root/projects/mochy-analysis/artifacts/amz-downloader-arm64-foss-debug.apk`

## Functional scope implemented but not runtime-verified

- Downloader-only UX with Mochy visual language, user/profile card, and existing animation style retained.
- Direct link entry and browser/share capture; cookies, user agent, and referer persisted as request headers.
- Resume after process restart through controller, foreground service, and boot receiver.
- Segmented engine: probe, 1–32 segments, dynamic splitting, gap repair, SHA-256 verification, MediaStore publishing, 256 KiB buffers, speed meter, benchmarks.
- Default UI segment count is 32; actual throughput depends on server range support and network conditions.
- Clean start: Room schema bumped `2 -> 3` with `fallbackToDestructiveMigration()`; old download history is intentionally wiped.

## Known limitations

- No local JDK, Android SDK, or `gh` CLI was available on the phone; compilation, tests, and APK assembly were verified in GitHub Actions.
- REA Android-package inspection could not run because no JADX MCP JAR is configured; no decoded manifest XML tree or DEX decompilation was available. This does not invalidate the CI proof, binary-manifest parse, archive hash, or release-asset hash.
- APK signature verification was not performed by REA for this final artifact; the APK is debug-signed by the CI-generated debug keystore.
- No runtime/install/device test was performed; UI rendering, animations, gestures, live browser capture, and real download throughput were not observed in motion.

## Conclusion

At commit `5d57dc2`, the AMz downloader pivot passes all targeted CI tests and builds an
ARM64 FOSS debug APK whose packaged identity is `com.abhi.armz.debug` / version code 74 /
version name `1.0.0-amz-u1`. The shipping manifest exposes the downloader Activity,
service, boot receiver, and FileProviders without Mochy player, widget, TV/car, or media
intent surfaces. The published prerelease asset hash matches the CI-built APK exactly,
with runtime behavior and motion animations explicitly left as unverified limitations.
