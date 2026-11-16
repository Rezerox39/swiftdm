import type { ARMzBridge } from './index';
import type { DownloadItem, ProbeResult, Settings } from './types';
import { useArmzStore } from '../store/useArmzStore';

function createMockDownload(url: string, segments: number): DownloadItem {
  const now = Date.now();
  return {
    id: `mock-${now}-${Math.random().toString(36).slice(2, 8)}`,
    url,
    fileName: decodeURIComponent(url.split('/').pop()?.split('?')[0] || 'download.bin'),
    filePath: '',
    contentUri: null,
    mimeType: 'application/octet-stream',
    totalBytes: 128 * 1024 * 1024,
    downloadedBytes: 0,
    status: 'Downloading',
    bytesPerSecond: 0,
    etaSeconds: null,
    error: null,
    finalizeStep: null,
    finalizeProgress: 0,
    segmentCount: segments,
    createdAt: now,
    live: true,
    fraction: 0,
  };
}

export function createArmzBridge(): ARMzBridge {
  const bridge: ARMzBridge = {
    startDownload: (input) => {
      const request = JSON.parse(input) as { url?: string; segments?: number };
      if (!request.url || !/^https?:\/\//.test(request.url)) {
        return JSON.stringify({ ok: false, error: 'invalid_url' });
      }
      const download = createMockDownload(request.url, request.segments ?? 8);
      useArmzStore.setState((state) => ({ downloads: [download, ...state.downloads] }));
      return JSON.stringify({ ok: true, id: download.id });
    },
    pause: (input) => {
      const request = JSON.parse(input) as { id?: string };
      if (!request.id) return JSON.stringify({ ok: false, error: 'invalid_id' });
      useArmzStore.setState((state) => ({
        downloads: state.downloads.map((download) =>
          download.id === request.id
            ? { ...download, status: 'Paused', live: false, bytesPerSecond: 0 }
            : download,
        ),
      }));
      return JSON.stringify({ ok: true });
    },
    resume: (input) => {
      const request = JSON.parse(input) as { id?: string };
      if (!request.id) return JSON.stringify({ ok: false, error: 'invalid_id' });
      useArmzStore.setState((state) => ({
        downloads: state.downloads.map((download) =>
          download.id === request.id
            ? { ...download, status: 'Downloading', live: true }
            : download,
        ),
      }));
      return JSON.stringify({ ok: true });
    },
    cancel: (input) => {
      const request = JSON.parse(input) as { id?: string };
      if (!request.id) return JSON.stringify({ ok: false, error: 'invalid_id' });
      useArmzStore.setState((state) => ({
        downloads: state.downloads.map((download) =>
          download.id === request.id
            ? { ...download, status: 'Cancelled', live: false, bytesPerSecond: 0 }
            : download,
        ),
      }));
      return JSON.stringify({ ok: true });
    },
    remove: (input) => {
      const request = JSON.parse(input) as { id?: string };
      if (!request.id) return JSON.stringify({ ok: false, error: 'invalid_id' });
      useArmzStore.setState((state) => ({
        downloads: state.downloads.filter((download) => download.id !== request.id),
      }));
      return JSON.stringify({ ok: true });
    },
    pauseAll: () => {
      useArmzStore.setState((state) => ({
        downloads: state.downloads.map((download) =>
          download.status === 'Downloading' || download.status === 'Finalizing'
            ? { ...download, status: 'Paused', live: false, bytesPerSecond: 0 }
            : download,
        ),
      }));
      return JSON.stringify({ ok: true });
    },
    resumeAll: () => {
      useArmzStore.setState((state) => ({
        downloads: state.downloads.map((download) =>
          download.status === 'Paused' ? { ...download, status: 'Downloading', live: true } : download,
        ),
      }));
      return JSON.stringify({ ok: true });
    },
    listDownloads: () => JSON.stringify(useArmzStore.getState().downloads),
    probeUrl: (urlJson) => {
      const request = JSON.parse(urlJson) as { url?: string };
      if (!request.url) return 'null';
      const result: ProbeResult = {
        finalUrl: request.url,
        contentLength: 128 * 1024 * 1024,
        rangeSupported: true,
        etag: 'mock-etag',
        lastModified: 'mock-last-modified',
        fileName: decodeURIComponent(request.url.split('/').pop()?.split('?')[0] || 'ARMz-mock.bin'),
        mimeType: 'application/octet-stream',
      };
      return JSON.stringify(result);
    },
    getSettings: (): string => JSON.stringify(useArmzStore.getState().settings),
    setSettings: (settingsJson) => {
      const settings = JSON.parse(settingsJson) as Partial<Settings>;
      useArmzStore.setState((state) => ({ settings: { ...state.settings, ...settings } }));
      return JSON.stringify({ ok: true });
    },
    openFile: () => JSON.stringify({ ok: true }),
    shareFile: () => JSON.stringify({ ok: true }),
    copyText: () => JSON.stringify({ ok: true }),
    haptic: () => JSON.stringify({ ok: true }),
    requestNotificationPermission: () => JSON.stringify({ ok: true }),
    openBatterySettings: () => JSON.stringify({ ok: true }),
    openAutostartSettings: () => JSON.stringify({ ok: true }),
    getClipboardUrl: () => JSON.stringify({ url: '' }),
    getInsets: () => JSON.stringify({ top: 0, bottom: 0, left: 0, right: 0 }),
    ready: () => JSON.stringify({ ok: true }),
  };
  return bridge;
}

export function installMockBridge(): void {
  if (typeof window !== 'undefined') {
    window.ARMz = createArmzBridge();
  }
}
