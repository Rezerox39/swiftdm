# Literal Mochy 8.4.2 Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the ARMz app with a literal GPL-3.0 port of Mochy Music 8.4.2 while preserving ARMz's native download engine as an added Compose screen.

**Architecture:** Vendor Mochy source at exact commit `7e590802345329bb108d74f337af1ea9fe407d2a`, keep Mochy's namespace/UI/branding, set only `applicationId` to `com.abhi.armz`, and extract ARMz's engine/data layer into an `:amz-downloader` Android library consumed by Mochy's `:app`. Add one Mochy-styled Compose route for ARMz downloads and migrate CI to Mochy's Gradle/JDK/SDK stack.

**Tech Stack:** Kotlin 2.3.10, Jetpack Compose 1.10.2, Hilt 2.60.1, Room 2.8.4, OkHttp 4.12.0, AGP 9.0.0, Gradle 9.3.1, JDK 21, Android SDK 37, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-17-literal-mochy-port-design.md`

## Global Constraints

- Upstream source commit: `7e590802345329bb108d74f337af1ea9fe407d2a`.
- APK version string to preserve: `8.4.2 build#1261`.
- Upstream source counts to preserve: 1,247 Kotlin files and 806 XML files, plus ARMz additions.
- Namespace: `com.mochyy.mochymusic`; application id: `com.abhi.armz`.
- No data migration: the old ARMz database may start clean in the ported app.
- License: GPL-3.0 for the repository; preserve Mochy notices and attribution.
- Remove the React/WebView UI and its MIT + Commons Clause notice from the shipping tree.
- Use Mochy Gradle `9.3.1`, JDK 21, `compileSdk = 37`, `minSdk = 24`, NDK `27.0.12077973`.
- Do not run full Android builds on the phone; use GitHub Actions for full verification.
- Do not commit API keys, signing material, Discord credentials, or `local.properties`.

## Review Focus

- Application identity and manifest/package references: expected hybrid identity must remain `namespace = com.mochyy.mochymusic`, `applicationId = com.abhi.armz`; add a grep/REA task proving package-specific actions, widget receivers, FileProvider authorities, and cast provider remain valid.
- License contamination: expected no shipping React Bits/Commons Clause code; add a task that greps the tree and verifies notice files.
- Upstream fidelity: expected literal source at the pinned commit; add a task that verifies counts and a targeted diff against the pinned upstream export.
- Engine preservation: expected ARMz JVM tests to survive; add a task that copies and runs `DownloadEngineTest`, `EngineContractsTest`, `RetryPolicyTest`, and benchmark tests from the new module.
- Hilt/DI graph conflicts: expected one merged Hilt graph; add a task that compiles `:app` and `:amz-downloader` and runs all JVM unit tests.

---

## File Structure

- `docs/superpowers/specs/2026-11-17-literal-mochy-port-design.md` — approved design; update status only.
- `/tmp/opencode/amz-engine-stage/` — temporary staging area for ARMz engine before upstream overlay; never committed.
- `amz-downloader/` — new Android library module for ARMz engine/data/DI/tests.
- `app/` — replaced wholesale by Mochy's app module, then modified for hybrid identity, module dependency, FileProvider, navigation, and the ARMz screen.
- `providers/`, `modules/`, `gradle/`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, wrapper files — replaced by Mochy source.
- `.github/workflows/build-apk.yml` — rewritten for Mochy toolchain and ARMz artifact name; other workflows removed to avoid duplicate builds.
- `LICENSE`, `NOTICE.md`, `THIRD_PARTY_NOTICES.md`, `README.md` — GPL/license/attribution/docs updates.

---

### Task 1: Establish implementation branch and mark design approved

**Files:**
- Modify: `docs/superpowers/specs/2026-11-17-literal-mochy-port-design.md:3`

**Interfaces:**
- Consumes: design document created earlier.
- Produces: branch `feat/literal-mochy-port` containing an approved design doc.

- [ ] **Step 1: Create the implementation branch**

Run:
```bash
cd /root/projects/swiftdm
git switch -c feat/literal-mochy-port
```

Expected: branch created from current `main`.

- [ ] **Step 2: Mark the design approved**

Change the status line from `- Status: Draft for owner approval` to `- Status: Approved for implementation`.

- [ ] **Step 3: Verify and commit**

Run:
```bash
git diff --check
git status --short
git add docs/superpowers/specs/2026-11-17-literal-mochy-port-design.md
git commit -m "docs: approve literal Mochy port design"
```

Expected: clean commit with only the design doc change.

---

### Task 2: Stage ARMz engine and native resources

**Files:**
- Create staging copies under `/tmp/opencode/amz-engine-stage/`.
- No repository files changed.

**Interfaces:**
- Consumes: current `app/src/main/java/com/abhi/armz/engine`, `data`, `di`, `app/src/test/java/com/abhi/armz/engine`, and `app/src/main/res/xml/armz_file_paths.xml`.
- Produces: staged sources that Task 5 moves into `amz-downloader/` and `app/src/main/res/xml/`.

- [ ] **Step 1: Create staging directories**

Run:
```bash
rm -rf /tmp/opencode/amz-engine-stage
mkdir -p /tmp/opencode/amz-engine-stage/{engine,data,test,res/xml}
```

Expected: empty staging tree exists.

- [ ] **Step 2: Copy ARMz engine/data/DI and tests**

Run:
```bash
cd /root/projects/swiftdm
cp -a app/src/main/java/com/abhi/armz/engine/. /tmp/opencode/amz-engine-stage/engine/
cp -a app/src/main/java/com/abhi/armz/data/. /tmp/opencode/amz-engine-stage/data/
mkdir -p /tmp/opencode/amz-engine-stage/di
cp -a app/src/main/java/com/abhi/armz/di/. /tmp/opencode/amz-engine-stage/di/
cp -a app/src/test/java/com/abhi/armz/engine/. /tmp/opencode/amz-engine-stage/test/
cp -a app/src/main/res/xml/armz_file_paths.xml /tmp/opencode/amz-engine-stage/res/xml/
```

Expected: all five staging subdirectories contain files.

- [ ] **Step 3: Verify staged inventory**

Run:
```bash
find /tmp/opencode/amz-engine-stage -type f | sort
```

Expected: includes `DownloadEngine.kt`, `DownloadRepository.kt`, `ArmzStorage.kt`, `AppModule.kt`, `DownloadEngineTest.kt`, and `armz_file_paths.xml`.

---

### Task 3: Vendor Mochy 8.4.2 source at the pinned commit

**Files:**
- Replace: all repository files except `.git/`, `docs/superpowers/`, and staged ARMz sources.
- Delete: `web/`, old ARMz `app/src/main/java/com/abhi/armz/`, old ARMz native tests, old workflows, old README/LICENSE/catalogs.

**Interfaces:**
- Consumes: `/root/projects/mochy-analysis/Mochy-Music` at pinned commit.
- Produces: literal upstream tree plus preserved `docs/superpowers/`.

- [ ] **Step 1: Confirm upstream commit**

Run:
```bash
git -C /root/projects/mochy-analysis/Mochy-Music rev-parse HEAD
```

Expected: `7e590802345329bb108d74f337af1ea9fe407d2a`.

- [ ] **Step 2: Export the pinned upstream tree**

Run:
```bash
rm -rf /tmp/opencode/mochy-upstream-export
mkdir -p /tmp/opencode/mochy-upstream-export
git -C /root/projects/mochy-analysis/Mochy-Music archive 7e590802345329bb108d74f337af1ea9fe407d2a | tar -x -C /tmp/opencode/mochy-upstream-export
```

Expected: export completes without error.

- [ ] **Step 3: Overlay the upstream tree into the repository**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
rsync -a --delete \
  --exclude='.git' \
  --exclude='.git/' \
  --exclude='.superpowers/' \
  --exclude='.worktrees/' \
  --exclude='docs/superpowers/' \
  /tmp/opencode/mochy-upstream-export/ ./
```

Expected: repository root now matches upstream Mochy source, except `.git`, `.superpowers/`, and `docs/superpowers/`.

- [ ] **Step 4: Verify source counts**

Run:
```bash
cd /root/projects/swiftdm
echo "Kotlin: $(find . -path ./.git -prune -o -path ./docs/superpowers -prune -o -type f -name '*.kt' -print | wc -l)"
echo "XML: $(find . -path ./.git -prune -o -path ./docs/superpowers -prune -o -type f -name '*.xml' -print | wc -l)"
```

Expected: `Kotlin: 1247` and `XML: 806` before ARMz additions.

- [ ] **Step 5: Verify removed web shell**

Run:
```bash
cd /root/projects/swiftdm
test ! -e web && test ! -e app/src/main/assets/web && echo 'web UI removed'
```

Expected: `web UI removed`.

- [ ] **Step 6: Verify literal upstream files**

Run:
```bash
diff -q /tmp/opencode/mochy-upstream-export/app/src/main/kotlin/com/mochyy/mochymusic/MainActivity.kt app/src/main/kotlin/com/mochyy/mochymusic/MainActivity.kt
diff -q /tmp/opencode/mochy-upstream-export/app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/NavigationBuilder.kt app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/NavigationBuilder.kt
diff -q /tmp/opencode/mochy-upstream-export/gradle/libs.versions.toml gradle/libs.versions.toml
```

Expected: all three files identical before later tasks modify them.

- [ ] **Step 7: Commit the vendored upstream tree**

Run:
```bash
git add -A
git commit -m "feat: vendor Mochy Music 8.4.2 source"
```

Expected: large commit containing Mochy source and removal of old ARMz UI files.

---

### Task 4: Apply GPL-3.0 licensing and attribution

**Files:**
- Create: `NOTICE.md`
- Create/Modify: `THIRD_PARTY_NOTICES.md`
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-11-17-literal-mochy-port-design.md` if status was not updated in Task 1.

**Interfaces:**
- Consumes: Mochy `LICENSE` from vendored source; staged ARMz engine.
- Produces: GPL-compliant repository metadata.

- [ ] **Step 1: Verify GPL license file**

Run:
```bash
cd /root/projects/swiftdm
head -3 LICENSE
```

Expected: `GNU GENERAL PUBLIC LICENSE`, `Version 3, 29 June 2007`.

- [ ] **Step 2: Create `NOTICE.md`**

Write a file containing:
- `Mochy Music 8.4.2 port`
- Upstream repository: `https://github.com/realidkroo/Mochy-Music`
- Upstream commit: `7e590802345329bb108d74f337af1ea9fe407d2a`
- Statement that Mochy source is GPL-3.0 and upstream notices are preserved.
- Statement that ARMz-derived download engine code is relicensed GPL-3.0.
- Statement that this is a modified port with application id `com.abhi.armz`.

- [ ] **Step 3: Create `THIRD_PARTY_NOTICES.md`**

Write a concise notice listing:
- Mochy Music, GPL-3.0, upstream URL and commit.
- GPL-3.0 repository license.
- Major bundled dependency families from Mochy's version catalog: Compose, Media3, Hilt, Room, Ktor, Coil, Lottie, and license note that each dependency retains its own terms.
- Explicit statement that the former React Bits web UI is not shipped.

- [ ] **Step 4: Rewrite `README.md` header and build section**

Replace the old ARMz description with a Mochy-port description that includes:
- App name: Mochy with ARMz downloads.
- GPL-3.0 license.
- Build: JDK 21, Gradle wrapper 9.3.1.
- Assemble command: `./gradlew assembleArm64FossDebug`.
- Test command: `./gradlew test`.
- Link to `NOTICE.md` and the design doc.
- Note that `applicationId` is `com.abhi.armz` and namespace is Mochy.

- [ ] **Step 5: Verify no Commons Clause in shipping tree**

Run:
```bash
cd /root/projects/swiftdm
find . -path ./.git -prune -o -path ./.superpowers -prune -o -path ./docs/superpowers -prune -o -name THIRD_PARTY_NOTICES.md -prune -o -type f -print0 | xargs -0 grep -niE 'commons clause|react bits' || echo 'no Commons Clause references'
grep -n 'GNU GENERAL PUBLIC LICENSE' LICENSE
grep -n '7e590802345329bb108d74f337af1ea9fe407d2a' NOTICE.md THIRD_PARTY_NOTICES.md README.md
```

Expected: no Commons Clause/React Bits matches outside the notice that explicitly says the old UI is not shipped; GPL text and pinned commit found.

- [ ] **Step 6: Commit**

Run:
```bash
git add LICENSE NOTICE.md THIRD_PARTY_NOTICES.md README.md docs/superpowers/specs/2026-11-17-literal-mochy-port-design.md
git commit -m "docs: apply GPL licensing and Mochy attribution"
```

Expected: clean commit.

---

### Task 5: Create the `:amz-downloader` library module

**Files:**
- Create: `amz-downloader/build.gradle.kts`
- Create: `amz-downloader/src/main/kotlin/com/abhi/armz/**` from staged engine/data/DI.
- Create: `amz-downloader/src/test/kotlin/com/abhi/armz/engine/**` from staged tests.
- Modify: `settings.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/armz_file_paths.xml`

**Interfaces:**
- Consumes: staged ARMz sources in `/tmp/opencode/amz-engine-stage/`.
- Produces: `DownloadRepository` injectable by `@HiltViewModel`; `DownloadHandle` control surface; `DownloadEntity` rows for the Compose screen.

- [ ] **Step 1: Create module directories**

Run:
```bash
cd /root/projects/swiftdm
mkdir -p amz-downloader/src/main/kotlin/com/abhi/armz
mkdir -p amz-downloader/src/test/kotlin/com/abhi/armz/engine
mkdir -p app/src/main/res/xml
```

Expected: directories exist.

- [ ] **Step 2: Copy staged ARMz sources into the module**

Run:
```bash
cp -a /tmp/opencode/amz-engine-stage/engine amz-downloader/src/main/kotlin/com/abhi/armz/
cp -a /tmp/opencode/amz-engine-stage/data amz-downloader/src/main/kotlin/com/abhi/armz/
cp -a /tmp/opencode/amz-engine-stage/di amz-downloader/src/main/kotlin/com/abhi/armz/
cp -a /tmp/opencode/amz-engine-stage/test/. amz-downloader/src/test/kotlin/com/abhi/armz/engine/
cp -a /tmp/opencode/amz-engine-stage/res/xml/armz_file_paths.xml app/src/main/res/xml/
```

Expected: module source files and XML exist.

- [ ] **Step 3: Write `amz-downloader/build.gradle.kts`**

Use this exact plugin block and dependency set:
```kotlin
plugins {
    id("com.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.abhi.armz"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(libs.timber)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
```

Expected: file created; all aliases exist in Mochy's catalog.

- [ ] **Step 4: Add the module to Gradle**

Modify `settings.gradle.kts` by adding this line after `include(":app")`:
```kotlin
include(":amz-downloader")
```

- [ ] **Step 5: Merge ARMz FileProvider into Mochy manifest**

In `app/src/main/AndroidManifest.xml`, add this `<provider>` inside `<application>` next to Mochy's existing FileProvider:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.files"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/armz_file_paths" />
</provider>
```

Expected: no authority collision with Mochy's `${applicationId}.FileProvider`.

- [ ] **Step 6: Verify ARMz test inventory**

Run:
```bash
find amz-downloader/src/test/kotlin -type f | sort
```

Expected: includes `DownloadEngineTest.kt`, `EngineContractsTest.kt`, `EngineBenchmarkTest.kt`, `EngineTestServer.kt`, `RetryPolicyTest.kt`.

- [ ] **Step 7: Commit**

Run:
```bash
git add -A
git commit -m "feat: add ARMz downloader library module"
```

Expected: clean commit.

---

### Task 6: Configure hybrid app identity and module dependency

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/build.gradle.kts` dependency block.

**Interfaces:**
- Consumes: Mochy app module and `:amz-downloader`.
- Produces: APK with Mochy namespace/branding and ARMz install identity.

- [ ] **Step 1: Set application id**

In `app/build.gradle.kts`, change only:
```kotlin
applicationId = "com.mochyy.mochymusic"
```
to:
```kotlin
applicationId = "com.abhi.armz"
```

Keep `namespace = "com.mochyy.mochymusic"` unchanged.

- [ ] **Step 2: Add module dependency**

Add to the dependencies block:
```kotlin
implementation(project(":amz-downloader"))
```

- [ ] **Step 3: Verify identity constraints**

Run:
```bash
cd /root/projects/swiftdm
grep -nE 'namespace =|applicationId =|versionName =' app/build.gradle.kts
grep -n 'amz-downloader' app/build.gradle.kts settings.gradle.kts
```

Expected: namespace Mochy, application id ARMz, version name `8.4.2 build#1261`, module dependency present in both files.

- [ ] **Step 4: Commit**

Run:
```bash
git add app/build.gradle.kts settings.gradle.kts
git commit -m "feat: configure hybrid Mochy/ARMz identity"
```

Expected: clean commit.

---

### Task 7: Add the ARMz Downloads Compose screen and route

**Files:**
- Create: `app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/amz/AmzDownloadsRoute.kt`
- Create: `app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/amz/AmzDownloadsUiMapper.kt`
- Create: `app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/amz/AmzDownloadsViewModel.kt`
- Create: `app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/amz/AmzDownloadsScreen.kt`
- Create: `app/src/main/res/values/amz_strings.xml`
- Modify: `app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/NavigationBuilder.kt`
- Modify: `app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/settings/MochySettingsScreen.kt`
- Modify: `app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/settings/SettingsPane.kt`
- Test: `app/src/test/kotlin/com/mochyy/mochymusic/ui/screens/amz/AmzDownloadsUiMapperTest.kt`

**Interfaces:**
- Consumes: `DownloadRepository.observeDownloads(): Flow<List<DownloadEntity>>`, `startNew(url: String, segments: Int, fileName: String?, mimeType: String?): DownloadHandle`, `resume(id: String): DownloadHandle?`, and `DownloadHandle.pause()/resume()/cancel()`.
- Produces: route string `amz_downloads`; a `@HiltViewModel` `AmzDownloadsViewModel`; `AmzDownloadsUiMapper.toRow(entity): AmzDownloadRow`.

- [ ] **Step 1: Write the failing mapper test**

Create `AmzDownloadsUiMapperTest.kt`:
```kotlin
package com.mochyy.mochymusic.ui.screens.amz

import com.abhi.armz.data.local.DownloadEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AmzDownloadsUiMapperTest {
    @Test
    fun mapsEntityToVisibleRow() {
        val entity = DownloadEntity(
            id = "d1",
            url = "https://example.com/file.bin",
            fileName = "file.bin",
            filePath = "",
            totalBytes = 200,
            downloadedBytes = 50,
            status = "Downloading",
            etag = null,
            lastModified = null,
            segmentCount = 8,
            bytesPerSecond = 1024,
            sha256 = null,
            errorMessage = null,
            createdAt = 1,
            updatedAt = 2,
        )

        val row = AmzDownloadsUiMapper.toRow(entity)

        assertEquals("d1", row.id)
        assertEquals("file.bin", row.fileName)
        assertEquals("Downloading", row.status)
        assertEquals(0.25f, row.fraction, 0.001f)
        assertEquals(1024L, row.bytesPerSecond)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:
```bash
cd /root/projects/swiftdm
./gradlew :app:testArm64FossDebugUnitTest --tests '*AmzDownloadsUiMapperTest*' --no-configuration-cache --console=plain
```

Expected: compile failure because `AmzDownloadsUiMapper` does not exist.

- [ ] **Step 3: Implement the route constant and mapper**

Create `AmzDownloadsRoute.kt`:
```kotlin
package com.mochyy.mochymusic.ui.screens.amz

const val AmzDownloadsRoute = "amz_downloads"
```

Create `AmzDownloadsUiMapper.kt`:
```kotlin
package com.mochyy.mochymusic.ui.screens.amz

import com.abhi.armz.data.local.DownloadEntity

data class AmzDownloadRow(
    val id: String,
    val fileName: String,
    val status: String,
    val fraction: Float,
    val bytesPerSecond: Long,
    val errorMessage: String?,
)

object AmzDownloadsUiMapper {
    fun toRow(entity: DownloadEntity): AmzDownloadRow {
        val fraction = if (entity.totalBytes <= 0L) {
            0f
        } else {
            (entity.downloadedBytes.toDouble() / entity.totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
        }
        return AmzDownloadRow(
            id = entity.id,
            fileName = entity.fileName.ifBlank { entity.url },
            status = entity.status,
            fraction = fraction,
            bytesPerSecond = entity.bytesPerSecond,
            errorMessage = entity.errorMessage,
        )
    }
}
```

- [ ] **Step 4: Implement the ViewModel**

Create `AmzDownloadsViewModel.kt` with:
- `@HiltViewModel class AmzDownloadsViewModel @Inject constructor(private val repository: DownloadRepository) : ViewModel()`.
- `private val handles = mutableMapOf<String, DownloadHandle>()`.
- `val downloads: StateFlow<List<AmzDownloadRow>>` from `repository.observeDownloads().map { list -> list.map(AmzDownloadsUiMapper::toRow) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())`.
- `private val _message = MutableStateFlow<String?>(null)` and `val message: StateFlow<String?>`.
- `fun setUrl(value: String)`, `fun setSegments(value: Int)`, `fun addDownload()`, `fun pause(id: String)`, `fun resume(id: String)`, `fun cancel(id: String)`, `fun remove(id: String)`, `fun clearMessage()`.
- URL validation: accept only trimmed values starting with `http://` or `https://`; otherwise set message from `R.string.armz_downloads_invalid_url` via string resource passed into the ViewModel constructor as `@ApplicationContext context: Context`.
- Segment clamp: `value.coerceIn(1, 32)`.
- `addDownload()` calls `repository.startNew(url, segments, null, null)`, stores the returned handle, and clears the URL.
- `resume(id)` reuses an existing handle or calls `repository.resume(id)` and stores non-null result.
- `pause(id)` and `cancel(id)` use stored handles; `cancel(id)` also removes the handle from the map.
- `remove(id)` cancels any stored handle, then calls `repository.delete(id)`.

- [ ] **Step 5: Implement the screen**

Create `AmzDownloadsScreen.kt` with:
- `@Composable fun AmzDownloadsScreen(navController: NavController, viewModel: AmzDownloadsViewModel = hiltViewModel())`.
- Root shell: `SubSettingsScaffold(title = stringResource(R.string.armz_downloads_title), onBack = { navController.navigateUp() }, backLabel = stringResource(R.string.settings)) { ... }`.
- URL input using `TextField`/`OutlinedTextField` bound to `viewModel.url`.
- Segment selector using `FilterChip` values `1, 4, 8, 16, 32`.
- Start button using Mochy's `SettingsItem` row inside `SettingsGroup` for `R.string.armz_downloads_add`.
- Download list using `LazyColumn`; each row uses `SettingsItem` with title = file name, description = status plus bytes/s, trailing content = pause/resume/cancel/remove text buttons.
- Progress uses Mochy theme color and `LinearProgressIndicator(progress = { row.fraction })`.
- Empty state text: `R.string.armz_downloads_empty`.
- Apply `Modifier.revealOnAppear(key = "armz-download-${row.id}", seenState = rememberRevealSeenState())` to each row if that helper exists in the current source; otherwise use `AnimatedVisibility` with Mochy's standard fade/slide durations.
- Show validation/action errors with `TopToast` if its signature is straightforward; otherwise use a `Text` in the scaffold.

- [ ] **Step 6: Add strings**

Create `app/src/main/res/values/amz_strings.xml` with these exact names:
- `armz_downloads_title` = `AMz downloads`
- `armz_downloads_url` = `Download URL`
- `armz_downloads_add` = `Start download`
- `armz_downloads_segments` = `Segments`
- `armz_downloads_empty` = `No downloads yet`
- `armz_downloads_pause` = `Pause`
- `armz_downloads_resume` = `Resume`
- `armz_downloads_cancel` = `Cancel`
- `armz_downloads_remove` = `Remove`
- `armz_downloads_invalid_url` = `Enter an http(s) URL`

- [ ] **Step 7: Register the route**

Modify `NavigationBuilder.kt`:
- Add import for `AmzDownloadsRoute` and `AmzDownloadsScreen`.
- Add a `navigationComposable(AmzDownloadsRoute) { AmzDownloadsScreen(navController) }` next to the settings routes.
- Do not add it to `MainScreens`.

- [ ] **Step 8: Add the Settings entry**

Modify `MochySettingsScreen.kt`:
- Import `AmzDownloadsRoute`.
- Add `SettingsEntry("AMz downloads", "App behaviour", AmzDownloadsRoute, "download manager segmented resumable armz")` to `SettingsIndex` in the App behaviour section.
- No new category or fifth tab.

Modify `SettingsPane.kt`:
- Import `AmzDownloadsRoute`.
- Add `AmzDownloadsRoute` to `SettingsPaneRoutes`.

- [ ] **Step 9: Run the focused mapper test**

Run:
```bash
./gradlew :app:testArm64FossDebugUnitTest --tests '*AmzDownloadsUiMapperTest*' --no-configuration-cache --console=plain
```

Expected: PASS.

- [ ] **Step 10: Commit**

Run:
```bash
git add -A
git commit -m "feat: add ARMz downloads screen to Mochy port"
```

Expected: clean commit.

---

### Task 8: Migrate CI to the Mochy toolchain

**Files:**
- Delete: `.github/workflows/build.yml`, `build_pr.yml`, `pages.yml`, `stale.yml`, `benchmark.yml` if present.
- Modify: `.github/workflows/build-apk.yml`.

**Interfaces:**
- Consumes: Mochy Gradle wrapper and toolchain.
- Produces: one green CI workflow that runs all JVM tests and builds `arm64FossDebug`.

- [ ] **Step 1: Remove duplicate workflows**

Run:
```bash
cd /root/projects/swiftdm
rm -f .github/workflows/build.yml .github/workflows/build_pr.yml .github/workflows/pages.yml .github/workflows/stale.yml .github/workflows/benchmark.yml
```

Expected: only `build-apk.yml` remains.

- [ ] **Step 2: Rewrite `build-apk.yml`**

Write a workflow that:
- Triggers on push to `main`, tags `v*`, and `workflow_dispatch`.
- Uses `contents: read` and concurrency group `build-${{ github.ref }}`.
- Checks out with `actions/checkout@v5`.
- Sets up JDK 21 with `actions/setup-java@v5`, Temurin.
- Sets up Gradle with `gradle/actions/setup-gradle@v4`, `cache-disabled: true`.
- Generates `~/.android/debug.keystore` with the same keytool command as Mochy's `build.yml`.
- Exposes Last.fm/Libre.fm/Discord env vars from secrets with empty-safe values.
- Runs:
```bash
./gradlew test assembleArm64FossDebug --no-configuration-cache --console=plain --warning-mode summary
```
- Uploads artifact name `armz-mochy-arm64-foss-debug` from `app/build/outputs/apk/arm64Foss/debug/*.apk`.

- [ ] **Step 3: Validate workflow basics**

Run:
```bash
grep -nE 'java-version|assembleArm64FossDebug|armz-mochy-arm64-foss-debug' .github/workflows/build-apk.yml
```

Expected: JDK 21, assemble task, and artifact name present.

- [ ] **Step 4: Commit**

Run:
```bash
git add -A .github/workflows
git commit -m "ci: build literal Mochy port"
```

Expected: clean commit.

---

### Task 9: Full verification and REA check of built APK

**Files:**
- No source changes unless verification finds a defect.

**Interfaces:**
- Consumes: all previous tasks.
- Produces: evidence that tests, build, source fidelity, identity, and licensing constraints hold.

- [ ] **Step 1: Run all JVM tests locally if JDK 21 is available**

Run:
```bash
cd /root/projects/swiftdm
java -version
./gradlew test --no-configuration-cache --console=plain --warning-mode summary
```

Expected: if JDK 21 is available, all JVM tests pass. If not, record the client limitation and run in CI instead.

- [ ] **Step 2: Push branch to trigger CI**

Run:
```bash
git push -u origin feat/literal-mochy-port
```

Expected: branch pushed; `Build APK` workflow starts.

- [ ] **Step 3: Confirm CI success**

Check the workflow run. Expected:
- `./gradlew test assembleArm64FossDebug ...` exits 0.
- artifact `armz-mochy-arm64-foss-debug` is uploaded.

- [ ] **Step 4: Download and inspect the APK with REA**

Uploaded APK path after download: `/tmp/opencode/armz-mochy-arm64-foss-debug.apk`.

Run through REA tools:
- `inspect_apk_signature` summary.
- `inventory_artifact` summary.
- APK manifest/resource views where available without JADX.

Expected:
- application id evidence contains `com.abhi.armz.debug` for the debug artifact, with base id `com.abhi.armz`.
- version evidence contains `8.4.2 build#1261`.
- merged manifest has `${applicationId}.files` FileProvider and Mochy permissions.
- packaged tree contains Mochy resources and no `assets/web/`.

- [ ] **Step 5: Verify identity and package-specific references**

Run:
```bash
cd /root/projects/swiftdm
grep -n 'applicationId = "com.abhi.armz"' app/build.gradle.kts
grep -n 'android:authorities="\${applicationId}.files"' app/src/main/AndroidManifest.xml
grep -n 'com.mochyy.mochymusic.SOURCE_MODULE' app/src/main/AndroidManifest.xml
grep -n 'com.mochyy.mochymusic.widget' app/src/main/AndroidManifest.xml
```

Expected: hybrid identity and Mochy namespace-based action/receiver names remain intact.

- [ ] **Step 6: Verify source fidelity**

Run:
```bash
cd /root/projects/swiftdm
echo "Kotlin: $(find . -path ./.git -prune -o -path ./docs/superpowers -prune -o -type f -name '*.kt' -print | wc -l)"
echo "XML: $(find . -path ./.git -prune -o -path ./docs/superpowers -prune -o -type f -name '*.xml' -print | wc -l)"
git diff --check
```

Expected: Kotlin/XML counts exceed upstream baseline by only ARMz additions; no whitespace errors.

- [ ] **Step 7: Record verification results**

Create `docs/superpowers/verification/2026-11-17-literal-mochy-port.md` with:
- commit SHA,
- CI run URL,
- artifact SHA-256,
- REA evidence IDs,
- test/build results,
- known limitations.

- [ ] **Step 8: Commit verification record**

Run:
```bash
git add docs/superpowers/verification/2026-11-17-literal-mochy-port.md
git commit -m "docs: record literal Mochy port verification"
```

Expected: clean commit.
