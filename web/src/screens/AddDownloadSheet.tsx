import { useState } from 'react';
import { formatBytes } from '../lib/format';
import Magnet from '../reactbits/Magnet';
import Stepper, { Step } from '../reactbits/Stepper';
import { useArmzStore } from '../store/useArmzStore';
import type { ProbeResult } from '../bridge/types';

interface AddDownloadSheetProps {
  onClose: () => void;
}

export function AddDownloadSheet({ onClose }: AddDownloadSheetProps) {
  const probeUrl = useArmzStore((state) => state.probeUrl);
  const startDownload = useArmzStore((state) => state.startDownload);

  const [url, setUrl] = useState('');
  const [segments] = useState(8);
  const [probing, setProbing] = useState(false);
  const [result, setResult] = useState<ProbeResult | null>(null);

  const submit = () => {
    const trimmed = url.trim();
    if (!trimmed) return;
    setProbing(true);
    setResult(null);
    const probe = probeUrl(trimmed);
    setResult(probe);
    setProbing(false);
    startDownload({
      url: trimmed,
      segments,
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex min-h-screen items-end justify-center bg-black/70 px-4 backdrop-blur-xl"
      style={{ paddingBottom: 'max(var(--sab), var(--kb))' }}
      role="dialog"
      aria-modal="true"
    >
      <div className="w-full max-w-[520px] rounded-t-[32px] border border-white/10 bg-[#0B0B0F] px-6 pb-8 pt-3">
        <div className="mx-auto h-1 w-10 rounded-full bg-white/20" />
        <h2 className="mt-5 text-[22px] font-bold text-white">Add download</h2>
        <p className="mt-2 text-[13px] text-white/60">
          Paste a direct file URL to start a segmented, pauseable download.
        </p>

        <div className="mt-6">
          <Stepper
            step={1}
            onStepChange={() => undefined}
            width="100%"
            className="rounded-[24px]"
          >
            <Step title="Add">
              <div className="space-y-4">
                <input
                  value={url}
                  onChange={(event) => setUrl(event.target.value)}
                  placeholder="https://example.com/file.zip"
                  className="h-12 w-full rounded-2xl border border-white/10 bg-white/5 px-4 text-[15px] text-white outline-none"
                />
                <div className="flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-[13px] text-white/70">
                  <span>Segments</span>
                  <span className="font-semibold text-white">{segments}</span>
                </div>
                {probing ? (
                  <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-[13px] text-white/70">
                    Probing URL…
                  </div>
                ) : null}
                {result ? (
                  <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-[13px] text-white/70">
                    {result.fileName ?? 'Downloaded file'} · {formatBytes(result.contentLength)}
                  </div>
                ) : null}
              </div>
            </Step>
          </Stepper>
        </div>

        <div className="mt-8">
          <Magnet padding={32}>
            <button
              type="button"
              onClick={submit}
              disabled={probing || !url.trim()}
              className="inline-flex h-12 w-full items-center justify-center rounded-full bg-white px-8 text-[15px] font-semibold text-black disabled:opacity-40"
            >
              {probing ? 'Probing…' : 'Start download'}
            </button>
          </Magnet>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="mt-4 h-12 w-full rounded-full border border-white/10 text-[15px] font-semibold text-white"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
