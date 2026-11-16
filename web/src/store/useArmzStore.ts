import { create } from 'zustand';
import type { DownloadItem, ProbeResult, Settings } from '../bridge/types';

export const defaultSettings: Settings = {
  wifiOnly: false,
  maxConcurrentDownloads: 3,
  defaultSegments: 8,
  speedLimitBytesPerSecond: null,
  keepDownloadsAlive: true,
  visualEffects: 'auto',
};

interface ArmzState {
  downloads: DownloadItem[];
  settings: Settings;
  introCompleted: boolean;
  addOpen: boolean;
  settingsOpen: boolean;
  activeFilter: 'All' | 'Active' | 'Done';
  setDownloads: (downloads: DownloadItem[]) => void;
  startDownload: (input: { url: string; headers?: Record<string, string>; segments?: number }) => void;
  pause: (id: string) => void;
  resume: (id: string) => void;
  cancel: (id: string) => void;
  remove: (id: string, deleteFile: boolean) => void;
  pauseAll: () => void;
  resumeAll: () => void;
  probeUrl: (url: string) => ProbeResult | null;
  setSettings: (settings: Partial<Settings>) => void;
  applySettings: (settings: Settings) => void;
  completeIntro: () => void;
  setAddOpen: (open: boolean) => void;
  setSettingsOpen: (open: boolean) => void;
  setActiveFilter: (filter: 'All' | 'Active' | 'Done') => void;
  haptic: (kind: string) => void;
}

export const useArmzStore = create<ArmzState>((set) => ({
  downloads: [],
  settings: defaultSettings,
  introCompleted: false,
  addOpen: false,
  settingsOpen: false,
  activeFilter: 'All',

  setDownloads: (downloads) => set({ downloads }),
  startDownload: (input) => {
    window.ARMz?.startDownload(JSON.stringify(input));
  },
  pause: (id) => {
    window.ARMz?.pause(JSON.stringify({ id }));
  },
  resume: (id) => {
    window.ARMz?.resume(JSON.stringify({ id }));
  },
  cancel: (id) => {
    window.ARMz?.cancel(JSON.stringify({ id }));
  },
  remove: (id, deleteFile) => {
    window.ARMz?.remove(JSON.stringify({ id, deleteFile }));
  },
  pauseAll: () => {
    window.ARMz?.pauseAll();
  },
  resumeAll: () => {
    window.ARMz?.resumeAll();
  },
  probeUrl: (url) => {
    const raw = window.ARMz?.probeUrl(JSON.stringify({ url }));
    if (!raw) return null;
    try {
      return JSON.parse(raw) as ProbeResult | null;
    } catch {
      return null;
    }
  },
  setSettings: (settings) => {
    set((state) => ({ settings: { ...state.settings, ...settings } }));
    window.ARMz?.setSettings(JSON.stringify(settings));
  },
  applySettings: (settings) => set({ settings }),
  completeIntro: () => set({ introCompleted: true }),
  setAddOpen: (open) => set({ addOpen: open }),
  setSettingsOpen: (open) => set({ settingsOpen: open }),
  setActiveFilter: (filter) => set({ activeFilter: filter }),
  haptic: (kind) => {
    window.ARMz?.haptic(JSON.stringify({ kind }));
  },
}));

export const useArmzDownloads = () => useArmzStore((state) => state.downloads);

export const useArmzSettings = () => useArmzStore((state) => state.settings);
