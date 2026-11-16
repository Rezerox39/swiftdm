import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createArmzBridge } from '../bridge/mock';
import type { ARMzBridge } from '../bridge';
import { defaultSettings, useArmzStore } from '../store/useArmzStore';

describe('ARMz store -> bridge actions', () => {
  let bridge: ARMzBridge;

  beforeEach(() => {
    bridge = createArmzBridge();
    window.ARMz = bridge;
    useArmzStore.setState({
      downloads: [],
      settings: defaultSettings,
      introCompleted: false,
      addOpen: false,
      settingsOpen: false,
      activeFilter: 'All',
    });
  });

  it('serializes download controls as JSON strings', () => {
    const start = vi.spyOn(bridge, 'startDownload');
    const pause = vi.spyOn(bridge, 'pause');
    const remove = vi.spyOn(bridge, 'remove');
    const haptic = vi.spyOn(bridge, 'haptic');

    useArmzStore.getState().startDownload({ url: 'https://example.com/a.zip', segments: 4 });
    useArmzStore.getState().pause('download-1');
    useArmzStore.getState().remove('download-1', true);
    useArmzStore.getState().haptic('light');

    expect(start).toHaveBeenCalledWith(
      JSON.stringify({ url: 'https://example.com/a.zip', segments: 4 }),
    );
    expect(pause).toHaveBeenCalledWith(JSON.stringify({ id: 'download-1' }));
    expect(remove).toHaveBeenCalledWith(JSON.stringify({ id: 'download-1', deleteFile: true }));
    expect(haptic).toHaveBeenCalledWith(JSON.stringify({ kind: 'light' }));
  });

  it('updates settings locally and forwards the patch to Kotlin', () => {
    const setSettings = vi.spyOn(bridge, 'setSettings');

    useArmzStore.getState().setSettings({ wifiOnly: true, visualEffects: 'reduced' });

    expect(setSettings).toHaveBeenCalledWith(
      JSON.stringify({ wifiOnly: true, visualEffects: 'reduced' }),
    );
    expect(useArmzStore.getState().settings.wifiOnly).toBe(true);
    expect(useArmzStore.getState().settings.visualEffects).toBe('reduced');
  });

  it('accepts a settings event pushed by the native shell', () => {
    useArmzStore.getState().applySettings({ ...defaultSettings, defaultSegments: 16 });

    expect(useArmzStore.getState().settings.defaultSegments).toBe(16);
  });
});
