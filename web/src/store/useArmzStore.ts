import { create } from 'zustand';
import type { DownloadItem, ProbeResult, Settings } from '../bridge/types';

export const defaultSettings: Settings = {
  wifiOnly: false,
  maxConcurrentDownloads: 3,
  defaultSegments: 16,
  speedLimitBytesPerSecond: null,
  keepDownloadsAlive: true,
  visualEffects: 'auto',
};

export type NoticeTone = 'info' | 'success' | 'error';

export interface Notice {
  id: number;
  message: string;
  tone: NoticeTone;
}

type DownloadFilter = 'All' | 'Active' | 'Done';

interface ArmzState {
  downloads: DownloadItem[];
  settings: Settings;
  introCompleted: boolean;
  addOpen: boolean;
  settingsOpen: boolean;
  activeFilter: DownloadFilter;
  notice: Notice | null;
  setDownloads: (downloads: DownloadItem[]) => void;
  startDownload: (input: { url: string; headers?: Record<string, string>; segments?: number }) => boolean;
  pause: (id: string) => void;
  resume: (id: string) => void;
  cancel: (id: string) => void;
  remove: (id: string, deleteFile: boolean) => void;
  pauseAll: () => void;
  resumeAll: () => void;
  probeUrl: (url: string) => ProbeResult | null;
  openFile: (id: string) => void;
  shareFile: (id: string) => void;
  copyText: (text: string, successMessage?: string) => void;
  getClipboardUrl: () => string;
  setSettings: (settings: Partial<Settings>) => void;
  applySettings: (settings: Settings) => void;
  completeIntro: () => void;
  setAddOpen: (open: boolean) => void;
  setSettingsOpen: (open: boolean) => void;
  setActiveFilter: (filter: DownloadFilter) => void;
  notify: (message: string, tone?: NoticeTone) => void;
  dismissNotice: () => void;
  haptic: (kind: string) => void;
}

interface BridgeReply {
  ok?: boolean;
  error?: string;
  url?: string;
}

const ERROR_COPY: Record<string, string> = {
  invalid_start_input: 'That is not a valid HTTP or HTTPS link.',
  invalid_url: 'That is not a valid HTTP or HTTPS link.',
  invalid_id: 'That download is no longer available.',
  unknown_id: 'That download is no longer available.',
  file_unavailable: 'The file is not available on this device yet.',
  invalid_text: 'There is nothing to copy.',
  no_clipboard: 'The clipboard is unavailable on this device.',
  invalid_settings: 'That setting could not be saved.',
  bridge_unavailable: 'The native download bridge is unavailable.',
};

function parseReply(raw: string | undefined): BridgeReply {
  if (!raw) return { ok: false, error: 'bridge_unavailable' };
  try {
    const parsed = JSON.parse(raw) as BridgeReply | null;
    if (parsed && typeof parsed === 'object') return parsed;
  } catch {
    // Some commands return a bare success value; treat anything else as success.
  }
  return { ok: true };
}

function messageFor(reply: BridgeReply): string {
  return (reply.error && ERROR_COPY[reply.error]) || 'Something went wrong. Please try again.';
}

let noticeSequence = 0;

export const useArmzStore = create<ArmzState>((set, get) => ({
  downloads: [],
  settings: defaultSettings,
  introCompleted: true,
  addOpen: false,
  settingsOpen: false,
  activeFilter: 'All',
  notice: null,

  setDownloads: (downloads) => set({ downloads }),

  startDownload: (input) => {
    const reply = parseReply(window.ARMz?.startDownload(JSON.stringify(input)));
    if (reply.ok === false) {
      get().notify(messageFor(reply), 'error');
      return false;
    }
    return true;
  },

  pause: (id) => {
    const reply = parseReply(window.ARMz?.pause(JSON.stringify({ id })));
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
  },

  resume: (id) => {
    const reply = parseReply(window.ARMz?.resume(JSON.stringify({ id })));
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
  },

  cancel: (id) => {
    const reply = parseReply(window.ARMz?.cancel(JSON.stringify({ id })));
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
  },

  remove: (id, deleteFile) => {
    const reply = parseReply(window.ARMz?.remove(JSON.stringify({ id, deleteFile })));
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
  },

  pauseAll: () => {
    const reply = parseReply(window.ARMz?.pauseAll());
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
  },

  resumeAll: () => {
    const reply = parseReply(window.ARMz?.resumeAll());
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
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

  openFile: (id) => {
    const reply = parseReply(window.ARMz?.openFile(JSON.stringify({ id })));
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
  },

  shareFile: (id) => {
    const reply = parseReply(window.ARMz?.shareFile(JSON.stringify({ id })));
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
  },

  copyText: (text, successMessage = 'Copied to clipboard.') => {
    const reply = parseReply(window.ARMz?.copyText(JSON.stringify({ text })));
    get().notify(reply.ok === false ? messageFor(reply) : successMessage, reply.ok === false ? 'error' : 'success');
  },

  getClipboardUrl: () => {
    const reply = parseReply(window.ARMz?.getClipboardUrl());
    return typeof reply.url === 'string' ? reply.url : '';
  },

  setSettings: (settings) => {
    set((state) => ({ settings: { ...state.settings, ...settings } }));
    const reply = parseReply(window.ARMz?.setSettings(JSON.stringify(settings)));
    if (reply.ok === false) get().notify(messageFor(reply), 'error');
  },

  applySettings: (settings) => set({ settings }),
  completeIntro: () => set({ introCompleted: true }),
  setAddOpen: (open) => set({ addOpen: open }),
  setSettingsOpen: (open) => set({ settingsOpen: open }),
  setActiveFilter: (filter) => set({ activeFilter: filter }),

  notify: (message, tone = 'info') => {
    noticeSequence += 1;
    set({ notice: { id: noticeSequence, message, tone } });
  },

  dismissNotice: () => set({ notice: null }),

  haptic: (kind) => {
    window.ARMz?.haptic(JSON.stringify({ kind }));
  },
}));

export const useArmzDownloads = () => useArmzStore((state) => state.downloads);

export const useArmzSettings = () => useArmzStore((state) => state.settings);
