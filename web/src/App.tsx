import { useEffect } from 'react';
import { LaunchScreen } from './screens/LaunchScreen';
import { DownloadsScreen } from './screens/DownloadsScreen';
import { AddDownloadSheet } from './screens/AddDownloadSheet';
import { SettingsScreen } from './screens/SettingsScreen';
import { installMockBridge } from './bridge/mock';
import { useArmzStore } from './store/useArmzStore';
import type { Settings } from './bridge/types';

export default function App() {
  const introCompleted = useArmzStore((state) => state.introCompleted);
  const addOpen = useArmzStore((state) => state.addOpen);
  const settingsOpen = useArmzStore((state) => state.settingsOpen);
  const activeFilter = useArmzStore((state) => state.activeFilter);
  const setAddOpen = useArmzStore((state) => state.setAddOpen);
  const setSettingsOpen = useArmzStore((state) => state.setSettingsOpen);

  useEffect(() => {
    if (import.meta.env.DEV) {
      installMockBridge();
      return;
    }
    const rawSettings = window.ARMz?.getSettings();
    if (rawSettings) {
      try {
        useArmzStore.getState().applySettings(JSON.parse(rawSettings) as Settings);
      } catch {
        // Keep the U1 defaults if the native payload is malformed.
      }
    }
    window.ARMz?.ready();
  }, []);

  if (!introCompleted) {
    return <LaunchScreen onFinished={() => undefined} />;
  }

  return (
    <div className="relative h-full w-full overflow-hidden bg-black font-sans text-[#F2F2F7]">
      <div className="relative h-full w-full">
        <DownloadsScreen
          activeFilter={activeFilter}
          onOpenAdd={() => setAddOpen(true)}
          onOpenSettings={() => setSettingsOpen(true)}
        />
      </div>
      {addOpen ? <AddDownloadSheet onClose={() => setAddOpen(false)} /> : null}
      {settingsOpen ? <SettingsScreen onClose={() => setSettingsOpen(false)} /> : null}
    </div>
  );
}
