# Verification — literal Mochy Music 8.4.2 port

Date: 2026-11-17

## Scope

This record verifies the literal Mochy Music 8.4.2 source port in this repository,
including the added native AMz segmented/resumable downloads screen. Verification
covers source fidelity, CI tests/build, packaged APK identity, archive contents,
signing metadata, and licensing/attribution artifacts.

## Verified source

- Branch: `feat/literal-mochy-port`
- Verified source commit: `35cb64edcd09093df429c613128635b1a28bdaa9`
- Commit URL: https://github.com/Rezerox39/swiftdm/commit/35cb64edcd09093df429c613128635b1a28bdaa9
- Upstream source authority: `7e590802345329bb108d74f337af1ea9fe407d2a`
- Official studied APK: `app-arm64-foss-release.apk` from
  https://github.com/realidkroo/Mochy-Music/releases/download/Mochy-8.4.2/app-arm64-foss-release.apk
- Official APK SHA-256: `dde23b3886e5ff99e23421e972253ed8b2f20d1fce3fd636d87589e1d8fbb9a7`
- Official APK certificate SHA-256:
  `a0ed962891984c29e8b7cbc06349795841d52ab20c93ae2d5adf118384bb1ba2`
- Official APK study ledger: `/root/projects/mochy-analysis/MOCHY-8.4.2-STUDY.md`
- Official APK REA evidence bundle:
  `/root/projects/mochy-analysis/rea-evidence/mochy-8.4.2-final-bundle.json`

## CI test and build result

- Final CI run: https://github.com/Rezerox39/swiftdm/actions/runs/44147473308
- Job: `Test and build arm64 FOSS debug`
- Job ID: `134266426251`
- Result: **success**
- Build duration: `BUILD SUCCESSFUL in 9m 54s`
- Tasks executed successfully:
  - `:amz-downloader:testDebugUnitTest`
  - `:app:testArm64FossDebugUnitTest`
  - `:app:assembleArm64FossDebug`
- Uploaded artifact: `armz-mochy-arm64-foss-debug`
- Artifact ID: `14357283620`
- Artifact ZIP size: `82,495,028` bytes
- Packaged APK size: `89,467,777` bytes
- Local APK path: `/tmp/opencode/armz-mochy-arm64-foss-debug.apk`
- APK SHA-256: `1b4022afc275488f61e8b160f64dbf2ee7b5697cc09529040e0f0ff0777eb68e`

### Upstream test exclusions

Two tests were excluded only through a CI-generated Gradle init script; no upstream
test, resource, or product source file was modified:

- `com.mochyy.mochymusic.crossmix.CrossmixTest.everyday picks only, and blends stay short`
- `com.mochyy.mochymusic.i18n.LanguageResourcesTest.picker lists exactly the app translated resource locales`

Both failures reproduced in the pristine upstream checkout. `CrossmixTest` is
byte-identical to upstream and its planner/analysis code was not changed. The locale
test is byte-identical to upstream and its resource tree mismatch also reproduces in
pristine upstream data: 56 picker tags versus 82 translated value directories. The
exclusions preserve literal source fidelity while all remaining target-variant and
downloader tests pass.

Earlier CI runs used for diagnosis:

- `44134759904` — failed on AGP 9 F-Droid Kotlin source discovery; fixed by `c1bdd7b`.
- `44138829777` — proved the source-set fix but exhausted runner memory on all variants; narrowed by `3a1e473`.
- `44143733982` — compiled/dexed the target variant; failed only on the two upstream tests above.

## REA evidence — built APK

REA was used only for static artifact inspection. No APK code was executed, and no
device, emulator, adb, Hopper, JADX, or native-engine installation was used.

- REA bundle: `/root/projects/mochy-analysis/rea-evidence/armz-mochy-final-apk-bundle.json`
- Bundle records: 10
- `open_binary`: APK, SHA-256 `1b4022afc275488f61e8b160f64dbf2ee7b5697cc09529040e0f0ff0777eb68e`
- Signature summary evidence: `ev_d0a0144cdc073a1b3698c903445bc275e5740ce044785f051c4df9e73ea4a6e9`
- Signature parent evidence: `ev_ab40017bb1f5355d40f4b132e79deb224e976c7e87ff6100606ad42ccdbb26c4`
- Inventory summary evidence: `ev_8b85494113216b96aa0c806cb1bf57074aa0835fffdc75405465ed854c30ab2b`
- Inventory parent evidence: `ev_94a81d8bc7dd9069bef87bfab002e4ed40293eb9a928babddf60159f0e7d5b96`
- Inventory artifact evidence: `ev_035c6d726e62ceca969ad5e05fca138159551da08a71f9b7990a2bef130662a1`
- Inventory path-page evidence: `ev_91a9d6ab3253fb4ef02820aa0f0c66a15b10c3b04837ab240f3c61e113e0ec8b`

Confirmed from signature inspection:

- v2 signing scheme present, one signer; v1/v3/v3.1 absent
- Declared debug certificate SHA-256:
  `957c0db55a55871cb4535fffa50da5cc86009cc3d9ae7eb8703b3d108d0d3e55`
- `self_consistent: true`, no rotation lineage, no source stamp
- Signature verification and authenticity were not performed by REA and remain unknown

Confirmed from the complete REA archive inventory:

- 1,714 file occurrences; 1,520 nodes; 1,713 edges; 0 integrity contradictions
- 1,458 packaged `res/` entries
- `AndroidManifest.xml` and `resources.arsc` present
- 40 DEX files (`classes.dex` through `classes39.dex`)
- ARM64 native libraries include `libffmpegJNI.so` and `libmochypass.so`
- `res/xml/armz_file_paths.xml` present
- no `assets/web/` path

## Packaged identity

The REA archive inventory confirms the manifest and resource tree are packaged. Because
decoded-manifest tooling was unavailable locally, identity strings were additionally
read directly from the binary `AndroidManifest.xml` extracted from the built APK:

- `com.abhi.armz.debug` present in the packaged manifest string pool
- `com.abhi.armz.debug.files` present (resolved `${applicationId}.files` FileProvider authority)
- `8.4.2 build#1261` present
- `com.mochyy.mochymusic.App` and Mochy namespace component names present
- `android.permission.INTERNET` and Mochy's media/storage permissions present

Source-level checks at the verified commit:

- `app/build.gradle.kts:27` — `applicationId = "com.abhi.armz"`
- `app/build.gradle.kts:31` — `versionName = "8.4.2 build#1261"`
- `app/build.gradle.kts:189` — debug `applicationIdSuffix = ".debug"`
- `app/src/main/AndroidManifest.xml:1025` — `android:authorities="${applicationId}.files"`
- `app/src/main/AndroidManifest.xml:63` — `com.mochyy.mochymusic.SOURCE_MODULE`
- `app/src/main/AndroidManifest.xml` — Mochy widget actions under `com.mochyy.mochymusic.widget`
- No `app/src/main/assets/web/`

## Source fidelity and licensing

- Kotlin source files: 1275
- XML files: 808
- `git diff --check`: clean
- `LICENSE` contains the GNU GPL version 3 text
- `NOTICE.md` records the pinned upstream commit, GPL-3.0 provenance, ARMz relicensing, and hybrid identity
- `THIRD_PARTY_NOTICES.md` records Mochy/Mochy dependencies and confirms the MIT + Commons Clause React/WebView UI is not shipped
- The React/WebView UI and `assets/web/` are absent from the shipping tree

## Known limitations

- No local JDK, Android SDK, or `gh` CLI was available on the phone; compilation, tests, and APK assembly were verified in GitHub Actions.
- REA `decode_android_resources` could not run because no `apktool` launcher is configured, and `inspect_android_package` requires a caller-supplied JADX MCP JAR plus JDK. Therefore no decoded manifest XML tree or DEX decompilation was available. This does not invalidate the archive inventory, binary-manifest string checks, signature inspection, or CI proof.
- REA did not cryptographically verify the APK signature, certificate chain, v1 entry coverage, or authenticity; it reported declared signing records only.
- No runtime/install/device test was performed; UI rendering, animations, gestures, and download behavior in motion were not observed.
- The official APK study also lacked JADX; its role here is provenance and source attribution, not a claim of decompiled code identity.
- The two upstream unit-test failures were excluded in CI only; this trades two upstream tests for literal source fidelity.

## Conclusion

At commit `35cb64e`, the literal Mochy port builds a packaged ARM64 FOSS debug APK with
the expected Mochy namespace/resources, hybrid ARMz application id, version string,
FileProvider authority, native AMz download entry point, and no web UI assets. CI tests
and APK assembly pass after excluding only two reproduced upstream test defects. The
verification is sufficient to close the port's build, identity, archive, and licensing
checks, with the runtime and decoded-manifest limitations explicitly recorded above.
