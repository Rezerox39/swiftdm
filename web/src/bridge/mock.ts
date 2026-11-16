import type { ARMzBridge } from './index';
import type { DownloadItem, ProbeResult, Settings } from './types';
import { fileNameFromUrl, hostFromUrl } from '../lib/format';
import { useArmzStore } from '../store/useArmzStore';

let mockTicker: number | null = null;

function createMockDownload(url: string, segments: number): DownloadItem {
  const now = Date.now();
  return {
    id: `mock-${now}-${Math.random().toString(36).slice(2, 8)}`,
    url,
    fileName: fileNameFromUrl(url),
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
      const download = createMockDownload(
        request.url,
        request.segments ?? useArmzStore.getState().settings.defaultSegments,
      );
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
        fileName: fileNameFromUrl(request.url) || hostFromUrl(request.url) || 'ARMz-mock.bin',
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

function startMockTicker(): void {
  if (mockTicker !== null) return;
  mockTicker = window.setInterval(() => {
    useArmzStore.setState((state) => {
      let changed = false;
      const downloads = state.downloads.map((download) => {
        if (download.status !== 'Downloading' || !download.live) return download;
        const total = download.totalBytes > 0 ? download.totalBytes : 128 * 1024 * 1024;
        const step = Math.max(512 * 1024, Math.round(total / 120));
        const next = Math.min(total, download.downloadedBytes + step);
        const speed = 14 * 1024 * 1024 + Math.round(Math.random() * 5 * 1024 * 1024);
        const finished = next >= total;
        changed = true;
        return {
          ...download,
          totalBytes: total,
          downloadedBytes: next,
          bytesPerSecond: finished ? 0 : speed,
          etaSeconds: finished ? null : Math.ceil((total - next) / speed),
          fraction: next / total,
          status: finished ? 'Completed' as const : 'Downloading' as const,
          live: !finished,
          filePath: finished ? `/tmp/${fileNameFromUrl(download.url)}` : download.filePath,
          contentUri: finished ? `content://armz.mock/${download.id}` : download.contentUri,
        };
      });
      return changed ? { downloads } : {};
    });
  }, 600);
}

export function installMockBridge(): void {
  if (typeof window === 'undefined') return;
  if (!window.ARMz) window.ARMz = createArmzBridge();
  startMockTicker();
}
