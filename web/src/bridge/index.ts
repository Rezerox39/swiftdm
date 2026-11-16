import type { DownloadItem, ProbeResult, Settings } from './types';

export interface ARMzBridge {
  startDownload: (inputJson: string) => string;
  pause: (inputJson: string) => string;
  resume: (inputJson: string) => string;
  cancel: (inputJson: string) => string;
  remove: (inputJson: string) => string;
  pauseAll: () => string;
  resumeAll: () => string;
  listDownloads: () => string;
  probeUrl: (urlJson: string) => string;
  getSettings: () => string;
  setSettings: (settingsJson: string) => string;
  openFile: (idJson: string) => string;
  shareFile: (idJson: string) => string;
  copyText: (textJson: string) => string;
  haptic: (kindJson: string) => string;
  requestNotificationPermission: () => string;
  openBatterySettings: () => string;
  openAutostartSettings: () => string;
  getClipboardUrl: () => string;
  getInsets: () => string;
  ready: () => string;
}

export interface ParsedSettings extends Settings {
  [key: string]: unknown;
}

export type ParsedProbeResult = ProbeResult | null;

export type ParsedDownloads = DownloadItem[];

declare global {
  interface Window {
    ARMz?: ARMzBridge;
    __armz_emit?: (json: string) => void;
    __armz_perf?: (tier: 'high' | 'low') => void;
  }
}
