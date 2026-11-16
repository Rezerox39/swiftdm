import { useEffect } from 'react';
import { DownloadsScreen } from './screens/DownloadsScreen';
import { AddDownloadSheet } from './screens/AddDownloadSheet';
import { SettingsScreen } from './screens/SettingsScreen';
import { Toast } from './components/Toast';
import { installMockBridge } from './bridge/mock';
import { useArmzStore } from './store/useArmzStore';
import type { DownloadItem, Settings } from './bridge/types';

function readJson<T>(raw: string | undefined): T | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

export default function App() {
  const addOpen = useArmzStore((state) => state.addOpen);
  const settingsOpen = useArmzStore((state) => state.settingsOpen);
  const activeFilter = useArmzStore((state) => state.activeFilter);
  const setAddOpen = useArmzStore((state) => state.setAddOpen);
  const setSettingsOpen = useArmzStore((state) => state.setSettingsOpen);

  useEffect(() => {
    if (import.meta.env.DEV) installMockBridge();

    const store = useArmzStore.getState();
    const settings = readJson<Settings>(window.ARMz?.getSettings());
    if (settings) store.applySettings(settings);

    const downloadPayload = readJson<DownloadItem[] | { downloads: DownloadItem[] }>(
      window.ARMz?.listDownloads(),
    );
    const downloads = Array.isArray(downloadPayload)
      ? downloadPayload
      : downloadPayload?.downloads;
    if (downloads) store.setDownloads(downloads);

    window.ARMz?.ready();
  }, []);

  useEffect(() => {
    const handlePopState = () => {
      const state = window.history.state as { armzOverlay?: boolean } | null;
      if (!state?.armzOverlay) {
        const store = useArmzStore.getState();
        if (store.addOpen) store.setAddOpen(false);
        if (store.settingsOpen) store.setSettingsOpen(false);
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  useEffect(() => {
    const anyOverlayOpen = addOpen || settingsOpen;
    const hasOverlayEntry = Boolean(
      (window.history.state as { armzOverlay?: boolean } | null)?.armzOverlay,
    );
    if (anyOverlayOpen && !hasOverlayEntry) {
      window.history.pushState({ armzOverlay: true }, '');
    } else if (!anyOverlayOpen && hasOverlayEntry) {
      window.history.back();
    }
  }, [addOpen, settingsOpen]);

  return (
    <div
      className="relative h-full w-full overflow-hidden bg-black font-sans text-white antialiased"
      style={{
        paddingTop: 'var(--sat)',
        paddingBottom: 'var(--sab)',
        paddingLeft: 'var(--sal)',
        paddingRight: 'var(--sar)',
      }}
    >
      <DownloadsScreen
        activeFilter={activeFilter}
        onOpenAdd={() => setAddOpen(true)}
        onOpenSettings={() => setSettingsOpen(true)}
      />

      {addOpen ? <AddDownloadSheet onClose={() => setAddOpen(false)} /> : null}
      {settingsOpen ? <SettingsScreen onClose={() => setSettingsOpen(false)} /> : null}
      <Toast />
    </div>
  );
}
