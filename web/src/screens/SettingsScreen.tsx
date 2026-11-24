import { HugeiconsIcon } from '@hugeicons/react';
import { Download01Icon } from '@hugeicons/core-free-icons';
import { Sheet } from '../components/Sheet';
import { APP_VERSION } from '../lib/app';
import { SEGMENT_PRESETS } from '../lib/segments';
import { useArmzSettings, useArmzStore } from '../store/useArmzStore';
import Magnet from '../reactbits/Magnet';
import SpringCheck from '../reactbits/SpringCheck';

interface SettingsScreenProps {
  onClose: () => void;
}

function SectionLabel({ children }: { children: string }) {
  return (
    <p className="mb-2 px-1 text-[10.5px] font-semibold uppercase tracking-[0.16em] text-white/30">
      {children}
    </p>
  );
}

export function SettingsScreen({ onClose }: SettingsScreenProps) {
  const settings = useArmzSettings();
  const setSettings = useArmzStore((state) => state.setSettings);
  const haptic = useArmzStore((state) => state.haptic);
  const reduceMotion = settings.visualEffects === 'off';

  const chooseSegments = (value: number) => {
    if (value === settings.defaultSegments) return;
    setSettings({ defaultSegments: value });
    haptic('light');
  };

  return (
    <Sheet
      eyebrow="Preferences"
      title="Settings"
      subtitle="Tune how AMz splits transfers and how the interface moves."
      onClose={onClose}
      footer={
        <Magnet
          padding={28}
          disabled={reduceMotion}
          magnetStrength={0.3}
        >
          <button
            type="button"
            onClick={onClose}
            className="h-11 w-full rounded-full bg-white text-[14px] font-semibold text-black transition active:scale-[0.98]"
          >
            Done
          </button>
        </Magnet>
      }
    >
      <div className="space-y-6 pb-1">
        <section>
          <SectionLabel>Downloads</SectionLabel>
          <div className="overflow-hidden rounded-[20px] border border-white/[0.07] bg-white/[0.025]">
            <div className="p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex min-w-0 items-start gap-3">
                  <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-white/[0.07] bg-white/[0.04] text-white/55">
                    <HugeiconsIcon icon={Download01Icon} size={17} strokeWidth={1.8} />
                  </span>
                  <div className="min-w-0">
                    <p className="text-[13.5px] font-semibold text-white/85">Parallel connections</p>
                    <p className="mt-1 text-[12px] leading-4 text-white/38">
                      More connections can improve speed. Use fewer on weak servers.
                    </p>
                  </div>
                </div>
                <span className="mt-0.5 shrink-0 rounded-full bg-white/[0.08] px-2.5 py-1 text-[12px] font-semibold tabular-nums text-white/70">
                  {settings.defaultSegments}
                </span>
              </div>

              <div className="mt-4 grid grid-cols-3 gap-2">
                {SEGMENT_PRESETS.map((value) => {
                  const selected = settings.defaultSegments === value;
                  return (
                    <button
                      key={value}
                      type="button"
                      onClick={() => chooseSegments(value)}
                      className={`h-11 rounded-xl border text-[12.5px] font-semibold transition active:scale-[0.97] ${
                        selected
                          ? 'border-white bg-white text-black'
                          : 'border-white/[0.08] bg-white/[0.035] text-white/55 active:bg-white/[0.07]'
                      }`}
                    >
                      {value}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
          <p className="mt-2 px-1 text-[12px] leading-4 text-white/30">
            New downloads use this value. 48 is the maximum and uses more memory and battery; files smaller than 1 MB still use one connection.
          </p>
        </section>

        <section>
          <SectionLabel>Appearance</SectionLabel>
          <div className="rounded-[20px] border border-white/[0.07] bg-white/[0.025] px-4 py-3.5">
            <div className="flex items-center justify-between gap-4">
              <div className="min-w-0">
                <p className="text-[13.5px] font-semibold text-white/85">Reduce motion</p>
                <p className="mt-1 text-[12px] leading-4 text-white/38">
                  Pause the ambient glow and soften interface transitions.
                </p>
              </div>
              <SpringCheck
                label=""
                checked={reduceMotion}
                onChange={(checked) => {
                  setSettings({ visualEffects: checked ? 'off' : 'auto' });
                  haptic('light');
                }}
                boxSize={26}
                boxRadius={8}
                strike="none"
                bounce={0.12}
                doneOpacity={1}
                ariaLabel="Reduce motion"
                className="shrink-0"
              />
            </div>
          </div>
        </section>

        <section>
          <SectionLabel>About</SectionLabel>
          <div className="overflow-hidden rounded-[20px] border border-white/[0.07] bg-white/[0.025]">
            <div className="flex items-center gap-3 p-4">
              <div className="grid h-11 w-11 shrink-0 place-items-center rounded-[14px] border border-white/10 bg-white/[0.045] text-[12px] font-black tracking-[-0.08em] text-white">
                A
              </div>
              <div>
                <p className="text-[14px] font-semibold text-white/90">AMz</p>
                <p className="mt-0.5 text-[12px] text-white/38">Version {APP_VERSION} · com.abhi.armz</p>
              </div>
            </div>
            <p className="border-t border-white/[0.06] px-4 py-3 text-[12px] leading-4 text-white/30">
              Motion components by React Bits, used under the MIT + Commons Clause license.
            </p>
          </div>
        </section>
      </div>
    </Sheet>
  );
}
