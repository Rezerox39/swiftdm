import { useArmzStore } from '../store/useArmzStore';
import Magnet from '../reactbits/Magnet';
import SpringCheck from '../reactbits/SpringCheck';

interface SettingsScreenProps {
  onClose: () => void;
}

function SettingsRow({
  title,
  detail,
  checked,
  onCheckedChange,
  disabled,
}: {
  title: string;
  detail?: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
}) {
  return (
    <li className="flex items-center justify-between gap-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
      <div className="min-w-0">
        <p className="text-[14px] font-semibold text-white">{title}</p>
        {detail ? <p className="mt-0.5 text-[12px] text-white/55">{detail}</p> : null}
      </div>
      <SpringCheck
        checked={checked}
        onChange={onCheckedChange}
        disabled={disabled}
        ariaLabel={title}
        className="shrink-0"
      />
    </li>
  );
}

export function SettingsScreen({ onClose }: SettingsScreenProps) {
  const settings = useArmzStore((state) => state.settings);
  const setSettings = useArmzStore((state) => state.setSettings);

  const update = (patch: Partial<typeof settings>) => {
    setSettings(patch);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex min-h-screen items-end justify-center bg-black/70 px-4 backdrop-blur-xl"
      role="dialog"
      aria-modal="true"
    >
      <div className="max-h-[85vh] w-full max-w-[520px] overflow-y-auto rounded-t-[32px] border border-white/10 bg-[#0B0B0F] px-6 pb-8 pt-3">
        <div className="mx-auto h-1 w-10 rounded-full bg-white/20" />
        <h2 className="mt-5 text-[22px] font-bold text-white">Settings</h2>

        <div className="mt-6 space-y-3">
          <SettingsRow
            title="Wi-Fi only"
            detail="Prefer Wi-Fi for background transfers"
            checked={settings.wifiOnly}
            onCheckedChange={(checked) => update({ wifiOnly: checked })}
          />
          <SettingsRow
            title="Keep downloads alive"
            detail="Resume interrupted downloads after reboot"
            checked={settings.keepDownloadsAlive}
            onCheckedChange={(checked) => update({ keepDownloadsAlive: checked })}
          />
          <SettingsRow
            title="Visual effects"
            detail={`Mode: ${settings.visualEffects}`}
            checked={settings.visualEffects !== 'off'}
            onCheckedChange={(checked) => update({ visualEffects: checked ? 'auto' : 'off' })}
          />
        </div>

        <div className="mt-6 space-y-3">
          <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
            <p className="text-[14px] font-semibold text-white">Concurrent downloads</p>
            <p className="mt-1 text-[12px] text-white/55">Maximum active transfers</p>
            <div className="mt-3 flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => update({ maxConcurrentDownloads: value })}
                  className={`h-9 flex-1 rounded-full border text-[13px] font-semibold ${
                    settings.maxConcurrentDownloads === value
                      ? 'border-white/40 bg-white text-black'
                      : 'border-white/10 bg-white/5 text-white/70'
                  }`}
                >
                  {value}
                </button>
              ))}
            </div>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
            <p className="text-[14px] font-semibold text-white">Default segments</p>
            <p className="mt-1 text-[12px] text-white/55">1–16 ranged segments</p>
            <div className="mt-3 flex items-center gap-2">
              {[1, 4, 8, 12, 16].map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => update({ defaultSegments: value })}
                  className={`h-9 flex-1 rounded-full border text-[13px] font-semibold ${
                    settings.defaultSegments === value
                      ? 'border-white/40 bg-white text-black'
                      : 'border-white/10 bg-white/5 text-white/70'
                  }`}
                >
                  {value}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="mt-6 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
          <p className="text-[14px] font-semibold text-white">About</p>
          <p className="mt-1 text-[12px] leading-5 text-white/55">
            ARMz 0.4.0-u1 · React Bits motion components are used under the MIT + Commons Clause
            license.
          </p>
        </div>

        <div className="mt-8">
          <Magnet padding={32}>
            <button
              type="button"
              onClick={onClose}
              className="inline-flex h-12 w-full items-center justify-center rounded-full bg-white px-8 text-[15px] font-semibold text-black"
            >
              Done
            </button>
          </Magnet>
        </div>
      </div>
    </div>
  );
}
