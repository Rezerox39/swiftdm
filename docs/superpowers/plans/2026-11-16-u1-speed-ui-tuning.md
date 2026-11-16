# U1 Speed Tuning + UI Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Increase real download throughput with benchmark-backed engine tuning and make the U1 WebView UI more legible and touch-friendly, then publish a patch release.

**Architecture:** Keep the existing segmented download engine and WebView shell. Raise the segment read buffer and segment cap, replace the HEAD+GET probe with a single ranged GET, and keep the React Bits WebView UI with Android-sized touch targets and clearer speed/stability feedback.

**Tech Stack:** Kotlin, OkHttp, coroutines, JUnit, MockWebServer, React 18, TypeScript, Tailwind, Zustand, Vite, Vitest.

**Spec:** `docs/superpowers/specs/2026-11-16-u1-speed-stability-ui-polish-design.md`

## Global Constraints

- Default connections stay **16**; maximum becomes **32**.
- Segment buffer becomes **256 KiB** (`256 * 1024` bytes).
- Bridge validation accepts segment counts **1..32**.
- Web segment presets are **4, 8, 12, 16, 24, 32**.
- Body/meta text must be at least **12 px**; icon/action touch targets at least **44×44 px**; adjacent action gaps at least **8 px**.
- All six React Bits components remain in use: Aurora, BlurText, CountUp, Magnet, SpringCheck, Stepper.
- React Bits attribution remains MIT + Commons Clause.
- Version becomes **0.4.2-u1**, versionCode **6**.
- Android engine changes are verified through CI; web tests/build run locally.

## Review Focus

- **360 dp width with six segment presets** — Settings grid must stay 3 columns × 2 rows; covered by Task 4 manual layout check and build.
- **32 segments × 256 KiB buffers** — peak buffer memory ~8 MiB; bounded by the 32 cap and existing low-perf tier; covered by Task 1 segment-cap tests.
- **Server ignores Range on the new probe** — must fall back to a single stream instead of failing; covered by Task 2 existing fallback test.
- **Pause/cancel during probe** — no HEAD call exists, so tests must wait on GET counters; covered by Task 2 updated pause/cancel tests.
- **Speed readout flicker** — displayed speed is quantized before formatting; covered by Task 4 `stabilizeSpeed` test.

---

### Task 1: Engine limits and larger segment buffer

**Files:**
- Modify: `app/src/main/java/com/abhi/armz/engine/DownloadEngine.kt:883`
- Modify: `app/src/main/java/com/abhi/armz/engine/Models.kt:52-70`
- Modify: `app/src/main/java/com/abhi/armz/engine/HttpClients.kt:12-24`
- Modify: `app/src/main/java/com/abhi/armz/bridge/BridgeValidation.kt:17-24`
- Modify: `app/src/main/java/com/abhi/armz/bridge/WebViewBridge.kt:299-302`
- Test: `app/src/test/java/com/abhi/armz/bridge/BridgeValidationTest.kt`
- Test: `app/src/test/java/com/abhi/armz/engine/DownloadEngineTest.kt`

**Interfaces:**
- Consumes: existing `DownloadRequest.safeSegments`, `SegmentPlanner.plan(total, requestedSegments)`.
- Produces: `DownloadRequest.MaxSegments = 32`, `DownloadEngine.BufferSize = 256 * 1024`, `HttpClients.DefaultMaxSegments = 32`, bridge accepts `1..32`.

- [ ] **Step 1: Write failing tests**

In `BridgeValidationTest.kt`, add:

```kotlin
@Test
fun startPayloadAcceptsThirtyTwoSegmentsAndRejectsThirtyThree() {
    val valid = """{"url":"https://example.com/a.bin","segments":32}"""
    val invalid = """{"url":"https://example.com/a.bin","segments":33}"""
    assertNotNull(BridgeValidation.parseStart(valid))
    assertNull(BridgeValidation.parseStart(invalid))
}
```

In `DownloadEngineTest.kt`, add:

```kotlin
@Test
fun `thirty-two segments produce a byte-identical file`() = runBlocking {
    val content = sampleBytes(4 * 1024 * 1024)
    origin.content = content
    val result = awaitTerminal(engine.start(request(segments = 32)))

    assertEquals(DownloadStatus.Completed, result.status)
    assertEquals(Sha256.of(content), Sha256.of(File(result.filePath)))
    assertTrue("expected at least 32 ranged requests", origin.rangeHeaders.size >= 32)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.abhi.armz.bridge.BridgeValidationTest" --tests "com.abhi.armz.engine.DownloadEngineTest" --stacktrace`

Expected: FAIL because 33 is currently rejected but 32 is also rejected, and 32-segment planning is capped at 16.

- [ ] **Step 3: Implement the limits and buffer**

- `DownloadEngine.BufferSize = 256 * 1024`
- `DownloadRequest.MaxSegments = 32`
- `HttpClients.DefaultMaxSegments = 32`
- `BridgeValidation.parseStart`: `segments !in 1..32` is rejected.
- `WebViewBridge.mergeSettings`: `value in 1..32` for `defaultSegments`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.abhi.armz.bridge.BridgeValidationTest" --tests "com.abhi.armz.engine.DownloadEngineTest" --stacktrace`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abhi/armz/engine/DownloadEngine.kt app/src/main/java/com/abhi/armz/engine/Models.kt app/src/main/java/com/abhi/armz/engine/HttpClients.kt app/src/main/java/com/abhi/armz/bridge/BridgeValidation.kt app/src/main/java/com/abhi/armz/bridge/WebViewBridge.kt app/src/test/java/com/abhi/armz/bridge/BridgeValidationTest.kt app/src/test/java/com/abhi/armz/engine/DownloadEngineTest.kt
git commit -m "perf: raise segment buffer and connection cap"
```

---

### Task 2: Single ranged-GET probe

**Files:**
- Modify: `app/src/main/java/com/abhi/armz/engine/HttpProbe.kt`
- Modify: `app/src/test/java/com/abhi/armz/engine/EngineTestServer.kt`
- Test: `app/src/test/java/com/abhi/armz/engine/DownloadEngineTest.kt`

**Interfaces:**
- Consumes: `HttpProbe.probe(url, headers, registerCall, unregisterCall): ProbeResult`.
- Produces: probe uses exactly one GET request; `headCount` remains 0 for normal downloads.

- [ ] **Step 1: Write failing tests**

In `DownloadEngineTest.kt`, change the probe test to assert no HEAD call:

```kotlin
val probe = HttpProbe(HttpClients.downloadClient()).probe(url())

assertEquals(0, origin.headCount.get())
assertEquals(1, origin.getCount.get())
assertEquals(origin.content.size.toLong(), probe.contentLength)
assertTrue(probe.rangeSupported)
assertEquals("\"probe-tag\"", probe.etag)
assertEquals("sample.bin", probe.fileName)
```

Change the two probe-interruption tests to use `origin.getDelayMs = 2_000L` and wait for `origin.getCount.get() > 0` instead of `headCount`/`headDelayMs`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.abhi.armz.engine.DownloadEngineTest" --stacktrace`

Expected: FAIL on `headCount == 0`.

- [ ] **Step 3: Implement GET-only probe**

In `HttpProbe.probeOnce`, remove the HEAD request block. Issue only the existing `Range: bytes=0-0` GET:
- 206 → `ProbeResult(rangeSupported = true, contentLength = Content-Range total, metadata from response)`.
- 200 → `ProbeResult(rangeSupported = false, contentLength = Content-Length)`.
- 416, 4xx, 5xx handling unchanged.

In `EngineTestServer.kt`, add `Content-Disposition: attachment; filename="sample.bin"` to the 206 and full 200 responses so filename metadata survives the probe change.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.abhi.armz.engine.DownloadEngineTest" --stacktrace`

Expected: PASS, including range fallback, pause/cancel during probe, filename, retry, and checksum tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abhi/armz/engine/HttpProbe.kt app/src/test/java/com/abhi/armz/engine/EngineTestServer.kt app/src/test/java/com/abhi/armz/engine/DownloadEngineTest.kt
git commit -m "perf: probe with a single ranged GET"
```

---

### Task 3: Benchmark 8 / 16 / 32 segments

**Files:**
- Modify: `app/src/test/java/com/abhi/armz/engine/EngineBenchmarkTest.kt`
- Modify: `.github/workflows/benchmark.yml`

**Interfaces:**
- Consumes: `DownloadEngine`, `DownloadRequest`, `Sha256`.
- Produces: benchmark output lines for `segments=8`, `segments=16`, `segments=32`, plus ratios.

- [ ] **Step 1: Update the benchmark**

Replace `oneVersusEightSegments()` with `segmentCountComparison()` that:
- measures `8`, `16`, and `32` segments,
- verifies all three outputs have the same SHA-256,
- prints `ARMz_BENCH segments=<n> bytes=... millis=... mib_per_s=...` for each,
- prints `ARMz_BENCH ratio16v8` and `ARMz_BENCH ratio32v8`.

Keep `assumeTrue` gating and the 15-minute per-run timeout.

- [ ] **Step 2: Verify benchmark compiles**

Run: `./gradlew compileDebugUnitTestKotlin --stacktrace`

Expected: PASS.

- [ ] **Step 3: Update the benchmark workflow**

Rename the job to `8 / 16 / 32 segments` and keep the dispatched URL input and artifact upload.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/abhi/armz/engine/EngineBenchmarkTest.kt .github/workflows/benchmark.yml
git commit -m "perf: benchmark 8, 16 and 32 segments"
```

---

### Task 4: Web segment presets and UI polish

**Files:**
- Create: `web/src/lib/segments.ts`
- Create: `web/src/test/segments.test.ts`
- Modify: `web/src/lib/format.ts`
- Modify: `web/src/test/format.test.ts`
- Modify: `web/src/screens/DownloadsScreen.tsx`
- Modify: `web/src/screens/AddDownloadSheet.tsx`
- Modify: `web/src/screens/SettingsScreen.tsx`
- Modify: `web/src/components/Sheet.tsx`
- Modify: `web/src/components/Toast.tsx`

**Interfaces:**
- Consumes: `Settings`, `DownloadRow`, `useArmzStore`.
- Produces: `SEGMENT_PRESETS = [4, 8, 12, 16, 24, 32]`, `clampSegments(value: number): number`, `stabilizeSpeed(bytesPerSecond: number): number`.

- [ ] **Step 1: Write failing web tests**

`web/src/test/segments.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { SEGMENT_PRESETS, clampSegments } from '../lib/segments';

describe('segment presets', () => {
  it('uses the six supported presets', () => {
    expect(SEGMENT_PRESETS).toEqual([4, 8, 12, 16, 24, 32]);
  });

  it('clamps and rounds segment counts', () => {
    expect(clampSegments(0)).toBe(1);
    expect(clampSegments(33)).toBe(32);
    expect(clampSegments(12.6)).toBe(13);
  });
});
```

Add to `web/src/test/format.test.ts`:

```ts
it('quantises live speed to reduce flicker', () => {
  expect(stabilizeSpeed(0)).toBe(0);
  expect(stabilizeSpeed(1_530_000)).toBe(1_500_000);
  expect(stabilizeSpeed(18_400_000)).toBe(18_000_000);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd web && npm run test`

Expected: FAIL because `../lib/segments` and `stabilizeSpeed` do not exist.

- [ ] **Step 3: Implement the helpers**

Create `web/src/lib/segments.ts` exporting `SEGMENT_PRESETS` and `clampSegments` with bounds `1..32`. Add `stabilizeSpeed` to `web/src/lib/format.ts` using a 100 KiB step below 10 MiB/s and a 1 MiB step at or above 10 MiB/s.

- [ ] **Step 4: Apply UI polish**

- Use `SEGMENT_PRESETS` in `AddDownloadSheet.tsx` and `SettingsScreen.tsx`; render as a 3-column × 2-row grid so 360 dp screens do not overflow.
- Use `stabilizeSpeed()` before `formatSpeed()` in `DownloadsScreen.tsx` for header aggregate speed and row speed.
- Raise all icon-only buttons in `DownloadsScreen.tsx`, `Sheet.tsx`, and action rows to `h-11 w-11` (44 px); primary add and footer buttons to `h-12` (48 px).
- Change adjacent action gaps from `gap-1` to `gap-2`.
- Raise meta text from `11px`/`11.5px` to at least `12px`; keep uppercase labels at 10.5–11 px.
- Keep "Until bytes per second is nonzero, show Connecting…" behavior in `statText()`.

- [ ] **Step 5: Run web tests and build**

Run: `cd web && npm run test && npm run build`

Expected: all tests pass; build succeeds.

- [ ] **Step 6: Commit**

```bash
git add web/src/lib/segments.ts web/src/test/segments.test.ts web/src/lib/format.ts web/src/test/format.test.ts web/src/screens/DownloadsScreen.tsx web/src/screens/AddDownloadSheet.tsx web/src/screens/SettingsScreen.tsx web/src/components/Sheet.tsx web/src/components/Toast.tsx
git commit -m "feat: polish U1 speed UI and touch targets"
```

---

### Task 5: Version bump, CI verification, and release

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `web/package.json`
- Modify: `web/package-lock.json`
- Modify: `web/src/lib/app.ts`
- Modify: `.github/workflows/build-apk.yml`

**Interfaces:**
- Consumes: all prior tasks.
- Produces: `v0.4.2-u1` release with APK.

- [ ] **Step 1: Bump version metadata**

Set `versionCode = 6`, `versionName = "0.4.2-u1"`, web package version `0.4.2-u1`, `APP_VERSION = '0.4.2-u1'`, and update release notes to mention engine tuning and UI polish.

- [ ] **Step 2: Run local verification**

Run: `cd web && npm run test && npm run build`

Expected: PASS.

- [ ] **Step 3: Push and verify CI**

Run: `git push origin main`, then poll the GitHub Actions run until `completed`/`success`.

Expected: web tests, JVM tests, and APK assembly all pass.

- [ ] **Step 4: Tag and publish**

Run: `git tag v0.4.2-u1 && git push origin v0.4.2-u1`.

Expected: release published with `ARMz-debug.apk`.

- [ ] **Step 5: Commit version bump**

```bash
git add app/build.gradle.kts web/package.json web/package-lock.json web/src/lib/app.ts .github/workflows/build-apk.yml
git commit -m "chore: release v0.4.2-u1"
```
