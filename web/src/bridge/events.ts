import type { NativeEvent, Settings } from './types';
import { useArmzStore } from '../store/useArmzStore';

function readSettings(): Settings | null {
  const raw = window.ARMz?.getSettings();
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Settings;
  } catch {
    return null;
  }
}

/**
 * Native events are always JSON strings. The shell batches them on a 200 ms
 * cadence and calls this one global function from the WebView.
 */
export function installNativeEventReceiver(): void {
  if (typeof window === 'undefined') return;

  window.__armz_emit = (json: string) => {
    try {
      const event = JSON.parse(json) as NativeEvent;
      if (event.kind === 'downloads') {
        useArmzStore.getState().setDownloads(event.downloads);
        return;
      }
      if (event.kind === 'settings') {
        const settings = event.settings ?? readSettings();
        if (settings) useArmzStore.getState().applySettings(settings);
        return;
      }
      if (event.kind === 'network') {
        document.documentElement.dataset.network = event.online === false ? 'offline' : 'online';
      }
    } catch (error) {
      console.warn('ARMz: ignored malformed native event', error);
    }
  };

  window.__armz_perf = (tier: 'high' | 'low') => {
    document.documentElement.dataset.perf = tier;
  };
}
