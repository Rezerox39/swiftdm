import { describe, expect, it } from 'vitest';
import type { NativeEvent, Settings } from '../bridge/types';

describe('ARMz WebView bridge contract', () => {
  it('accepts the U1 native event envelope', () => {
    const event: NativeEvent = {
      kind: 'downloads',
      downloads: [],
    };
    expect(event.kind).toBe('downloads');
  });

  it('accepts partial settings updates', () => {
    const settings: Settings = {
      wifiOnly: false,
      maxConcurrentDownloads: 3,
      defaultSegments: 8,
      speedLimitBytesPerSecond: null,
      keepDownloadsAlive: true,
      visualEffects: 'auto',
    };
    expect(settings.maxConcurrentDownloads).toBe(3);
  });
});
