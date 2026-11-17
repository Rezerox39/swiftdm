# Literal Mochy Music 8.4.2 port with AMz downloads preserved

- Date: 2026-11-17
- Status: Approved for implementation
- Target repository: `/root/projects/swiftdm`
- Upstream authority: `/root/projects/mochy-analysis/Mochy-Music` at commit `7e590802345329bb108d74f337af1ea9fe407d2a`

## Intent

Turn the ARMz repository into a literal GPL-3.0 port of Mochy Music 8.4.2 while preserving ARMz's segmented/resumable download engine as an added native screen inside Mochy's UI.

The owner explicitly chose a literal clone/port over an original-inspired restyle. Mochy's Compose screens, navigation, animation systems, theme, string resources, icons, and module structure are therefore the source of truth. ARMz-specific additions must be additive and must not replace or dilute Mochy behavior.

## Goals

1. Vendor Mochy Music source at exact commit `7e59080` into the ARMz repository.
2. Preserve all Mochy screens, routes, player styles, animations, user-facing strings, resources, and module structure as shipped in 8.4.2.
3. Relicense ARMz from MIT to GPL-3.0 and preserve Mochy copyright/license/source obligations.
4. Preserve ARMz's native download engine, Room storage, file publishing, and JVM test suite.
5. Add an ARMz Downloads screen and entry point inside Mochy's Compose shell.
6. Migrate ARMz CI/toolchain to Mochy's Gradle/JDK/SDK requirements and produce a debug APK artifact.
7. Verify the result with source diffing, JVM tests, Gradle build, REA inspection of the built APK, and a documented fidelity checklist.

## Non-goals

- Writing a new design system inspired by Mochy. The literal source is the design system.
- Reproducing behavior from decompiled APK bytecode. Deep DEX analysis was unavailable; the matching source tree is the authority.
- Running the APK on a device or comparing live frames pixel-by-pixel. This environment has no visible display or adb connection.
- Preserving the current React/WebView UI as the shipping UI. It is not part of Mochy 8.4.2.
- Adding provider API keys, Discord credentials, signing secrets, or other secrets to the repository.

## Constraints and verified inputs

### Mochy APK evidence

- Official APK: `app-arm64-foss-release.apk` from the owner-provided release URL.
- SHA-256: `dde23b3886e5ff99e23421e972253ed8b2f20d1fce3fd636d87589e1d8fbb9a7`.
- REA evidence bundle: `/root/projects/mochy-analysis/rea-evidence/mochy-8.4.2-final-bundle.json`.
- Study ledger: `/root/projects/mochy-analysis/MOCHY-8.4.2-STUDY.md`.
- REA found an APK with 1,010 packaged entries, three DEX files, 770 resources, 26 assets, and ARM64 libraries including `libffmpegJNI.so` and `libmochypass.so`.
- Signature is v2 with one signer and a self-consistent declared digest. This is integrity evidence, not proof of authenticity.
- Deep DEX/class analysis is blocked because REA requires a caller-supplied `jadx-headless-mcp` 0.7.1 JAR.

### Source version

- The GitHub release tag `Mochy-8.4.2` points to `77bc1d7`, whose `versionName` is `8.4.1 build#1259`.
- Source HEAD `7e59080` has `versionName = "8.4.2 build#1261"`, and the APK contains the same `8.4.2 build#1261` string.
- Therefore the literal port uses `7e590802345329bb108d74f337af1ea9fe407d2a`, not the stale release tag.

### Mochy UI architecture to preserve

- Four-tab navigation: Home, Search, Library, Settings in `Screens.kt`.
- Custom frosted bottom bar with drag-following pill in `AppNavigation.kt` / `NavPill.kt`.
- Persistent three-anchor player sheet with 11 styles, not a NavHost route.
- Shared-element container transforms, coordinated page recede/return, reveal/stagger, shimmer, marquee text, swipe actions, press tilt, global motion switch.
- Dark-only neutral theme, Inter SemiBold with tight tracking, cover-derived gradients, frosted glass, hairline borders.
- Large route surface: music discovery, library, playlists, settings tree, player pages, lyrics, equalizer, TV graph, module system.

### ARMz baseline

- Current visible UI: React 18 + TypeScript + Tailwind in `web/`, rendered in a WebView.
- Native engine is pure-Kotlin/JVM at `app/src/main/java/com/abhi/armz/engine/` with tests under `app/src/test/java/com/abhi/armz/engine/`.
- Hilt/Room/OkHttp glue is in `di/`, `data/`, and `bridge/`.
- Baseline at time of study: `npm test` 18/18 passed and `npm run build` succeeded.
- Current LICENSE is MIT; `THIRD_PARTY_NOTICES.md` includes React Bits MIT + Commons Clause.

## Approaches considered

1. **Original-inspired craft layer** — rebuild existing ARMz screens with Mochy-inspired motion and visuals. Rejected by owner.
2. **Expanded ARMz UX** — keep ARMz architecture and add Mochy-like screens. Rejected by owner.
3. **Literal GPL port** — vendor Mochy source and add ARMz downloads. Chosen by owner.
4. **Pure Mochy fork with downloads dropped** — rejected by owner; downloads must survive.

## Chosen approach

### 1. Repository and license foundation

- Replace root `LICENSE` with the full GPL-3.0 text from Mochy's `LICENSE`.
- Update `README.md`, `THIRD_PARTY_NOTICES.md`, and add `NOTICE.md` with:
  - Mochy Music copyright and GPL-3.0 attribution.
  - Upstream repository URL and exact source commit.
  - Statement that ARMz-derived engine code is relicensed GPL-3.0.
  - Preservation requirement for upstream notices in source files.
- Keep Mochy's root `LICENSE` and all upstream source headers intact.
- Remove the React/WebView UI and its Commons Clause notice from the current source tree because MIT + Commons Clause is not GPL-compatible. Its history remains in git for provenance; it is no longer built or shipped.
- Update font/asset notices to reflect what is actually shipped after the port.

### 2. Vendor Mochy source

- Copy Mochy source at `7e59080` into `/root/projects/swiftdm`, including:
  - `app/`, `providers/`, `modules/`, `gradle/`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, wrapper files, resources, scripts, and workflow files needed to build.
  - Exact Kotlin/XML/raw resources and string files; no renaming or rewriting for style.
- Use a **hybrid identity**: preserve Mochy's package namespace `com.mochyy.mochymusic`, source references, app label, icons, and branding, but set `applicationId = "com.abhi.armz"` so the result remains an in-place ARMz install.
- Keep app label and user-facing text from Mochy. Add only ARMz-specific strings prefixed `armz_`.
- Audit and adjust package-specific manifest entries, intent filters, deep links, module/source actions, provider authorities, and widget/configuration references so they remain correct with the ARMz application id. Preserve Mochy-visible behavior rather than rewriting user-facing identity.
- Record the vendored commit in `NOTICE.md` and build metadata.

### 3. Preserve and integrate the ARMz download engine

- Move ARMz native engine sources into a dedicated Android library module, proposed path `amz-downloader/`, package `com.abhi.armz`.
- Include:
  - `engine/` pure-Kotlin classes and JVM tests.
  - `data/` Room entities/DAO/database/repository/publishing.
  - `di/AppModule.kt`, adjusted to avoid duplicate OkHttpClient bindings with Mochy's Hilt graph.
  - FileProvider configuration and download permissions merged into Mochy's manifest.
- Apply Hilt/KSP/Room/OkHttp/Timber dependencies to the new module.
- Keep existing engine test cases and failing-test behavior unchanged except for DI qualification changes required by the merged graph.
- Add an ARMz repository wrapper exposing downloads as `StateFlow`/suspend calls suitable for a Compose screen.

### 4. Add the ARMz Downloads screen to Mochy

- Add a new Mochy Compose route such as `amz_downloads` in `NavigationBuilder.kt`.
- Build the screen with Mochy's existing design primitives, not the old WebView UI:
  - `SettingsGroup`/`SubSettingsScaffold` or equivalent shells.
  - Mochy typography, spacing, glass surfaces, reveal animations, toast, and press feedback.
  - Add/download, pause, resume, cancel, remove, speed/segments settings, and storage location.
- Add the entry point from Mochy Settings under a clearly labeled ARMz section, plus a secondary entry from Home if a native Home card boundary exists without altering existing cards.
- Do not add a fifth main tab; that would break the literal four-tab structure.
- Add navigation/UI tests where feasible and a small JVM test for the repository wrapper.

### 5. Toolchain and CI migration

- Adopt Mochy's Gradle `9.3.1`, JDK 21, `compileSdk = 37`, NDK `27.0.12077973`, and version catalog.
- Replace or rewrite `.github/workflows/build-apk.yml` based on Mochy's `build.yml`:
  - Set up JDK 21.
  - Generate debug keystore.
  - Build `arm64FossDebug` or a documented equivalent.
  - Run JVM tests.
  - Upload a consistently named debug APK artifact.
- Keep release tagging compatible with the existing ARMz release process if possible.
- Remove Node/npm steps and web build from CI once the web source is removed.
- Do not run the full Android build on the phone; GitHub Actions is the build environment.

### 6. Documentation and release metadata

- Rewrite `README.md` to describe the literal Mochy port plus ARMz downloads.
- Document build commands, flavors, license, attribution, source commit, and known differences from the official Mochy release.
- Preserve Mochy docs and fastlane assets where they are part of the source tree.
- Keep the current version scheme or set a new fork version; this is a builder decision unless the owner specifies otherwise.

## Phases

1. **Baseline and license** — snapshot current ARMz state, replace LICENSE, add NOTICE/third-party updates, remove web UI from current tree.
2. **Upstream vendoring** — copy exact Mochy source commit and verify file counts/commit metadata.
3. **Engine module extraction** — create `amz-downloader`, move engine/data/DI, resolve duplicate bindings, keep tests green.
4. **Compose integration** — add route, screen, entry points, strings, and repository wrapper.
5. **Build/CI migration** — Gradle/JDK/SDK/workflow, artifact naming, local commands.
6. **Verification and cleanup** — run tests/build, REA inspect built APK, fidelity checklist, final docs.

## Verification

- `git diff --stat 7e59080..vendored-tree` shows only ARMz-additive files and intentionally adjusted build/license/docs files.
- Upstream Kotlin/XML counts remain 1,247/806 plus added ARMz files.
- `./gradlew testDebugUnitTest` passes all Mochy and ARMz JVM tests.
- `./gradlew assembleArm64FossDebug` succeeds in CI.
- Built APK is opened with REA and checked for:
  - expected hybrid identity: application id `com.abhi.armz`, Mochy namespace/branding, and ported version string,
  - merged ARMz permissions/FileProvider,
  - packaged resources/routes/string evidence where statically available,
  - no accidental web assets in the shipping APK.
- Fidelity checklist confirms the four-tab nav, persistent player sheet, 11 styles, motion system, dark theme/type, and source-preserved strings from the study report.
- ARMz download engine behavior is verified by existing JVM tests and the new Compose route build.

## Risks and explicit limitations

- **GPL compatibility:** the current React Bits Commons Clause web UI cannot remain in the GPL source tree; removal is part of the design.
- **Hybrid identity audit:** the ARMz application id preserves install identity and access to existing ARMz sandbox data, but package-specific Mochy manifest/intent/deep-link/module references must be audited. Skipping the audit could silently break callbacks or module discovery.
- **No data migration:** the owner chose a clean start. The download engine is preserved, but old ARMz rows/settings are not imported or transformed. Because the application id remains `com.abhi.armz`, an optional future DB mapping is possible without a file-based import.
- **Build cost:** full Mochy build is large for GitHub Actions; local device builds should be avoided.
- **Source vs APK:** the APK and source version string match, but without JADX we cannot prove every bytecode instruction matches. Literal source vendoring is the strongest available equivalence.
- **Secrets:** Mochy integrations compile with empty keys or generated placeholders; no owner secrets are required for the debug port.
- All owner-level questions from the builder check are resolved: hybrid identity with no data migration.

## Decision record

- Literal port chosen over inspired restyle: owner decision.
- ARMz relicensed to GPL-3.0 with Mochy notices preserved: owner authorization.
- ARMz download engine preserved and exposed inside Mochy UI: owner decision.
- Use source commit `7e59080`, not stale release tag `77bc1d7`: verified from APK version string and source `versionName`.
- Hybrid identity chosen: Mochy namespace/branding with `com.abhi.armz` application id: owner decision.
- No one-time old-data import/migration: owner decision; clean start is acceptable.
