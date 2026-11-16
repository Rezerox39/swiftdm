# U1 speed/stability tuning + UI polish — design

## Intent
The user wants one more patch after `v0.4.1-u1`, focused on making real downloads faster while keeping the app stable, plus UI/UX polish. They chose the tuned-engine approach: stay inside the current architecture, but raise buffer size, allow more connections, remove the extra probe round trip, and apply UI fixes.

## Goals
- Improve real throughput with benchmark-backed engine tuning:
  - segment read buffer 64 KiB → 256 KiB
  - maximum segments 16 → 32, default stays 16
  - probe via one ranged GET instead of HEAD + ranged GET, saving one round trip
- Extend the benchmark to compare 8 / 16 / 32 segments so the default and cap have evidence.
- Make the WebView UI meet Android touch and spacing guidance: 44–48 dp targets and 8 px gaps.
- Raise body/meta text to a legible minimum and keep a single restrained AMOLED-black visual system.
- Make speed and stability legible: live speed, ETA, finalizing stage, failure reason, retry/pause/resume affordances, aggregate header speed.
- Keep all six React Bits components in purposeful use and preserve MIT + Commons Clause attribution.
- Ship a patch release with CI evidence.

## Non-goals
- No adaptive throughput manager (dynamic add/drop segments, network-aware backoff).
- No P3 or U2 work.
- No changes to queueing, foreground service, or notification architecture.

## Constraints
- Android WebView; safe-area/IME CSS variables must keep working.
- 360 dp-wide screens must not overflow.
- Phone memory is limited; 32 segments × 256 KiB buffers is about 8 MiB, acceptable but must be bounded.
- No local JDK/Android SDK; web tests/build run locally, JVM/APK via CI.
- Native engine has been unchanged since P2; `v0.4.1-u1` already removed duplicate web probe, set 16 default connections, and added ETA.

## Approaches considered
1. **UI-only polish.** Lowest risk but no additional real throughput gain. Rejected: user wants faster real downloads.
2. **Tuned engine + UI polish (chosen).** Benchmark and adjust buffer/segment cap/probe, plus UI fixes. Best speed upside without a new architecture.
3. **Adaptive segment manager.** Highest ceiling but new architecture and stability risk. Rejected for this patch.

## Design
### Engine tuning
- `DownloadEngine.BufferSize`: 64 KiB → 256 KiB.
- `DownloadRequest.MaxSegments` and `HttpClients.DefaultMaxSegments`: 16 → 32; dispatcher/connection-pool limits scale with the cap.
- Bridge validation and settings merge accept `1..32` segments; web presets become `4, 8, 12, 16, 24, 32`.
- Default stays 16; Settings notes that 32 is the maximum and uses more memory/battery.
- `HttpProbe`: remove the HEAD attempt; issue a single `Range: bytes=0-0` GET. 206 → segmented probe result with metadata; 200 → single-stream result; retry/terminal statuses unchanged. This removes one network round trip before transfer starts.
- `EngineBenchmarkTest`: compare 8 / 16 / 32 segments, print MiB/s and ratios, verify identical SHA-256, and keep the 15-minute per-run timeout.

### UI/UX polish
- Touch targets: every icon/action button at least 44×44 px; primary add and sheet footer 48 px; adjacent action gap 8 px.
- Typography: body/meta text never below 12 px; line-height at least 1.4; uppercase labels stay 10.5–11 px.
- Rows: increase card padding, give file names two lines, keep a clear two-line size/status/speed/ETA footer, progress bar 6 px.
- Speed display: aggregate speed in header; per-row speed + ETA; neutral “Connecting…” until bytes per second is nonzero; round speeds to reduce flicker.
- Stability display: explicit preparing/finalizing/failed/paused/cancelled states with retry affordance; destructive actions behind confirmation; hardware back closes sheets before leaving.

## Verification
- Web tests and build locally.
- UI Pro Max pre-delivery checklist (touch, contrast, safe areas, accessibility).
- CI JVM tests, engine benchmark (when a benchmark URL is configured), and APK assembly.
- Commit, push, tag, and publish a patch release.

## Left to the builder
Implement engine and UI changes, add/adjust tests, run verification, commit, push, tag, and publish the new APK.
