import { useEffect } from 'react';
import { HugeiconsIcon } from '@hugeicons/react';
import { AlertCircleIcon, InformationCircleIcon, Tick02Icon } from '@hugeicons/core-free-icons';
import { useArmzStore } from '../store/useArmzStore';

const TONE = {
  info: {
    icon: InformationCircleIcon,
    shell: 'border-white/10 bg-[#141418] text-white/85',
    iconColor: 'text-white/65',
  },
  success: {
    icon: Tick02Icon,
    shell: 'border-emerald-300/20 bg-[#0D1712] text-emerald-50/90',
    iconColor: 'text-emerald-300',
  },
  error: {
    icon: AlertCircleIcon,
    shell: 'border-rose-300/20 bg-[#1A0D10] text-rose-50/90',
    iconColor: 'text-rose-300',
  },
} as const;

export function Toast() {
  const notice = useArmzStore((state) => state.notice);
  const dismissNotice = useArmzStore((state) => state.dismissNotice);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(dismissNotice, 3200);
    return () => window.clearTimeout(timer);
  }, [notice, dismissNotice]);

  if (!notice) return null;
  const tone = TONE[notice.tone];

  return (
    <div
      className="pointer-events-none fixed inset-x-0 top-0 z-[80] flex justify-center px-4"
      style={{ paddingTop: 'calc(var(--sat) + 12px)' }}
      role="status"
      aria-live="polite"
    >
      <div
        key={notice.id}
        className={`flex max-w-[420px] items-center gap-2.5 rounded-2xl border px-4 py-2.5 shadow-[0_16px_40px_rgba(0,0,0,0.45)] ${tone.shell} armz-toast-in`}
      >
        <HugeiconsIcon icon={tone.icon} size={17} strokeWidth={1.9} className={tone.iconColor} />
        <p className="text-[12.5px] font-medium leading-5">{notice.message}</p>
      </div>
    </div>
  );
}
