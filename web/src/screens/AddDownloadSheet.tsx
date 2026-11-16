import { useState } from 'react';
import { HugeiconsIcon } from '@hugeicons/react';
import { AlertCircleIcon, Link01Icon } from '@hugeicons/core-free-icons';
import { FileGlyph } from '../components/FileGlyph';
import { Sheet } from '../components/Sheet';
import { fileNameFromUrl, hostFromUrl, isHttpUrl } from '../lib/format';
import { useArmzSettings, useArmzStore } from '../store/useArmzStore';
import Stepper, { Step } from '../reactbits/Stepper';

interface AddDownloadSheetProps {
  onClose: () => void;
}

const SEGMENT_OPTIONS = [4, 8, 12, 16];

function clampSegments(value: number): number {
  if (!Number.isFinite(value)) return 16;
  return Math.max(1, Math.min(16, Math.round(value)));
}

export function AddDownloadSheet({ onClose }: AddDownloadSheetProps) {
  const settings = useArmzSettings();
  const startDownload = useArmzStore((state) => state.startDownload);
  const setSettings = useArmzStore((state) => state.setSettings);
  const getClipboardUrl = useArmzStore((state) => state.getClipboardUrl);
  const notify = useArmzStore((state) => state.notify);
  const haptic = useArmzStore((state) => state.haptic);

  const [url, setUrl] = useState('');
  const [segments, setSegments] = useState(() => clampSegments(settings.defaultSegments));
  const [step, setStep] = useState(1);
  const [error, setError] = useState<string | null>(null);

  const trimmed = url.trim();
  const valid = isHttpUrl(trimmed);
  const fileName = fileNameFromUrl(trimmed);
  const host = hostFromUrl(trimmed);

  const paste = async () => {
    let candidate = getClipboardUrl();
    if (!candidate && navigator.clipboard?.readText) {
      try {
        const text = await navigator.clipboard.readText();
        if (isHttpUrl(text)) candidate = text.trim();
      } catch {
        // Native clipboard bridge is the primary path; browser reads can be denied.
      }
    }

    if (!candidate) {
      notify('No download link found on the clipboard.', 'error');
      return;
    }

    setUrl(candidate);
    setError(null);
    haptic('light');
  };

  const submit = () => {
    if (!valid) {
      setError('Enter a complete HTTP or HTTPS link first.');
      setStep(1);
      return;
    }

    const started = startDownload({ url: trimmed, segments });
    if (!started) {
      setError('The download could not be started. Check the link and try again.');
      return;
    }

    if (segments !== settings.defaultSegments) setSettings({ defaultSegments: segments });
    haptic('success');
    onClose();
  };

  return (
    <Sheet
      eyebrow="New download"
      title="Add download"
      subtitle="Paste a direct file link. AMz splits the transfer into parallel connections."
      onClose={onClose}
    >
      <Stepper
        step={step}
        onStepChange={(next) => {
          setStep(next);
          setError(null);
          if (next === 2) haptic('light');
        }}
        onFinalStepCompleted={submit}
        width="100%"
        frame={false}
        radius={0}
        indicator="numbers"
        linear
        transition="slide"
        backButtonText="Back"
        nextButtonText="Continue"
        completeButtonText="Start download"
        nextButtonProps={{ disabled: step === 1 && !valid }}
        theme="dark"
        className="pt-1"
      >
        <Step title="Link" description="Direct file URL">
          <div>
            <div className="flex items-center justify-between gap-3">
              <label htmlFor="armz-url" className="text-[11px] font-semibold uppercase tracking-[0.14em] text-white/40">
                File link
              </label>
              <button
                type="button"
                onClick={() => void paste()}
                className="h-8 rounded-full border border-white/[0.1] bg-white/[0.045] px-3 text-[11.5px] font-semibold text-white/65 transition active:scale-95 active:bg-white/[0.09]"
              >
                Paste
              </button>
            </div>

            <div
              className={`mt-2 flex items-center gap-2 rounded-2xl border bg-white/[0.035] px-3 transition ${
                trimmed && !valid ? 'border-amber-300/30' : 'border-white/[0.09] focus-within:border-white/25'
              }`}
            >
              <HugeiconsIcon icon={Link01Icon} size={17} strokeWidth={1.8} className="shrink-0 text-white/30" />
              <input
                id="armz-url"
                type="url"
                inputMode="url"
                autoComplete="off"
                autoCapitalize="none"
                autoCorrect="off"
                spellCheck={false}
                enterKeyHint="next"
                autoFocus
                value={url}
                onChange={(event) => {
                  setUrl(event.target.value);
                  setError(null);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') return;
                  event.preventDefault();
                  if (valid) {
                    setStep(2);
                    haptic('light');
                  } else {
                    setError('Enter a complete HTTP or HTTPS link first.');
                  }
                }}
                placeholder="https://example.com/file.zip"
                className="h-[52px] min-w-0 flex-1 bg-transparent text-[14px] text-white outline-none placeholder:text-white/20"
              />
            </div>

            <p className="mt-2 text-[11.5px] leading-4 text-white/30">
              Direct links only. Regular web pages will not work.
            </p>

            {trimmed && !valid ? (
              <p className="mt-2 flex items-start gap-1.5 text-[11.5px] leading-4 text-amber-300/75">
                <HugeiconsIcon icon={AlertCircleIcon} size={13} strokeWidth={2} className="mt-0.5 shrink-0" />
                Keep typing — the link needs a host and an http or https scheme.
              </p>
            ) : null}

            {valid ? (
              <div className="mt-4 flex items-center gap-3 rounded-2xl border border-white/[0.065] bg-white/[0.025] p-3">
                <FileGlyph fileName={fileName} active />
                <div className="min-w-0">
                  <p className="truncate text-[13px] font-medium text-white/80">{fileName}</p>
                  <p className="mt-0.5 truncate text-[11px] text-white/35">{host}</p>
                </div>
              </div>
            ) : null}

            {error ? (
              <p className="mt-3 flex items-start gap-1.5 text-[12px] leading-4 text-rose-300/80">
                <HugeiconsIcon icon={AlertCircleIcon} size={14} strokeWidth={2} className="mt-0.5 shrink-0" />
                {error}
              </p>
            ) : null}
          </div>
        </Step>

        <Step title="Options" description="Connections">
          <div>
            <div className="rounded-2xl border border-white/[0.065] bg-white/[0.025] p-3.5">
              <div className="flex items-center gap-3">
                <FileGlyph fileName={fileName} active />
                <div className="min-w-0">
                  <p className="truncate text-[13.5px] font-semibold text-white/85">{fileName}</p>
                  <p className="mt-0.5 truncate text-[11.5px] text-white/40">{host || 'Direct download'}</p>
                </div>
              </div>
            </div>

            <div className="mt-5 flex items-end justify-between gap-3">
              <div>
                <p className="text-[13px] font-semibold text-white/85">Parallel connections</p>
                <p className="mt-1 text-[11.5px] leading-4 text-white/40">
                  More connections can improve speed. Lower it if the server throttles.
                </p>
              </div>
              <span className="shrink-0 text-[18px] font-semibold tabular-nums text-white">{segments}</span>
            </div>

            <div className="mt-3 grid grid-cols-4 gap-1.5">
              {SEGMENT_OPTIONS.map((value) => {
                const selected = segments === value;
                return (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setSegments(value)}
                    className={`h-10 rounded-xl border text-[13px] font-semibold transition active:scale-[0.97] ${
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

            <p className="mt-2 text-[11px] leading-4 text-white/30">
              16 is the fastest default on most networks. Your choice becomes the new default.
            </p>

            {error ? (
              <p className="mt-4 flex items-start gap-1.5 text-[12px] leading-4 text-rose-300/80">
                <HugeiconsIcon icon={AlertCircleIcon} size={14} strokeWidth={2} className="mt-0.5 shrink-0" />
                {error}
              </p>
            ) : null}
          </div>
        </Step>
      </Stepper>
    </Sheet>
  );
}
