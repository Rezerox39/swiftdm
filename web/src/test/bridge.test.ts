import { beforeEach, describe, expect, it } from 'vitest';
import { createArmzBridge } from '../bridge/mock';
import { defaultSettings, useArmzStore } from '../store/useArmzStore';
import type { ProbeResult, Settings } from '../bridge/types';

describe('mock ARMz bridge', () => {
  beforeEach(() => {
    useArmzStore.setState({ downloads: [], settings: defaultSettings });
  });

  it('starts, pauses and lists a mock download through JSON strings', () => {
    const bridge = createArmzBridge();
    const started = JSON.parse(
      bridge.startDownload(JSON.stringify({ url: 'https://example.com/archive.zip', segments: 8 })),
    ) as { ok: boolean; id: string };
    expect(started.ok).toBe(true);
    expect(JSON.parse(bridge.listDownloads())).toHaveLength(1);

    expect(JSON.parse(bridge.pause(JSON.stringify({ id: started.id }))).ok).toBe(true);
    expect(JSON.parse(bridge.listDownloads())[0].status).toBe('Paused');
    expect(JSON.parse(bridge.resume(JSON.stringify({ id: started.id }))).ok).toBe(true);
    expect(JSON.parse(bridge.listDownloads())[0].status).toBe('Downloading');
  });

  it('validates start URLs and probes JSON payloads', () => {
    const bridge = createArmzBridge();
    expect(JSON.parse(bridge.startDownload(JSON.stringify({ url: 'file:///etc/passwd' }))).ok).toBe(false);
    expect(bridge.probeUrl(JSON.stringify({ url: '' }))).toBe('null');

    const probe = JSON.parse(
      bridge.probeUrl(JSON.stringify({ url: 'https://example.com/file.bin' })),
    ) as ProbeResult;
    expect(probe.fileName).toBe('file.bin');
    expect(probe.rangeSupported).toBe(true);
  });

  it('round-trips settings payloads', () => {
    const bridge = createArmzBridge();
    expect(JSON.parse(bridge.setSettings(JSON.stringify({ wifiOnly: true, visualEffects: 'off' }))).ok).toBe(true);
    const settings = JSON.parse(bridge.getSettings()) as Settings;
    expect(settings.wifiOnly).toBe(true);
    expect(settings.visualEffects).toBe('off');
  });
});
