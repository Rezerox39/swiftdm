import { useEffect, type ReactNode } from 'react';
import { HugeiconsIcon } from '@hugeicons/react';
import { Cancel01Icon } from '@hugeicons/core-free-icons';

interface SheetProps {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  ariaLabel?: string;
}

export function Sheet({ eyebrow, title, subtitle, onClose, children, footer, ariaLabel }: SheetProps) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/75"
      role="dialog"
      aria-modal="true"
      aria-label={ariaLabel ?? title}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div
        className="flex w-full justify-center"
        style={{ paddingBottom: 'max(var(--sab), var(--kb))' }}
      >
        <div
          className="flex w-full max-w-[560px] flex-col overflow-hidden rounded-t-[30px] border-t border-white/10 bg-[#070708] shadow-[0_-24px_80px_rgba(0,0,0,0.8)]"
          style={{ maxHeight: 'calc(100dvh - max(var(--sab), var(--kb)) - 12px)' }}
        >
          <div className="shrink-0 px-5 pb-4 pt-2.5">
            <div className="mx-auto mb-4 h-1 w-10 rounded-full bg-white/15" />
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                {eyebrow ? (
                  <p className="mb-1 text-[10.5px] font-semibold uppercase tracking-[0.16em] text-white/35">
                    {eyebrow}
                  </p>
                ) : null}
                <h2 className="truncate text-[24px] font-semibold leading-tight tracking-[-0.025em] text-white">
                  {title}
                </h2>
                {subtitle ? (
                  <p className="mt-1.5 max-w-[360px] text-[12.5px] leading-5 text-white/45">{subtitle}</p>
                ) : null}
              </div>
              <button
                type="button"
                onClick={onClose}
                aria-label="Close"
                className="grid h-9 w-9 shrink-0 place-items-center rounded-full border border-white/10 bg-white/[0.05] text-white/65 transition active:scale-95 active:bg-white/10"
              >
                <HugeiconsIcon icon={Cancel01Icon} size={17} strokeWidth={1.8} />
              </button>
            </div>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 pb-5">{children}</div>

          {footer ? (
            <div className="shrink-0 border-t border-white/[0.07] px-5 py-4">{footer}</div>
          ) : null}
        </div>
      </div>
    </div>
  );
}
