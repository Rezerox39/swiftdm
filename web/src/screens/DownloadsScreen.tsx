import { useArmzDownloads, useArmzStore } from '../store/useArmzStore';
import { formatBytes, formatEta, formatSpeed } from '../lib/format';
import { toDownloadRow, type DownloadRow } from '../lib/status';
import Aurora from '../reactbits/Aurora';
import BlurText from '../reactbits/BlurText';
import CountUp from '../reactbits/CountUp';
import Magnet from '../reactbits/Magnet';
import SpringCheck from '../reactbits/SpringCheck';
import type { DownloadStatus } from '../bridge/types';

export type DownloadFilter = 'All' | 'Active' | 'Done';

interface DownloadsScreenProps {
  activeFilter: DownloadFilter;
  onOpenAdd: () => void;
  onOpenSettings: () => void;
}

const FILTERS: DownloadFilter[] = ['All', 'Active', 'Done'];

function statusLabel(status: DownloadStatus): string {
  return status;
}

function RowActions({ row }: { row: DownloadRow }) {
  const startDownload = useArmzStore((state) => state.startDownload);
  const pause = useArmzStore((state) => state.pause);
  const resume = useArmzStore((state) => state.resume);
  const cancel = useArmzStore((state) => state.cancel);
  const remove = useArmzStore((state) => state.remove);

  if (row.action === 'retry') {
    return (
      <button
        type="button"
        onClick={() => startDownload({ url: row.url, segments: row.segmentCount })}
        className="rounded-full bg-white px-3 py-1 text-[12px] font-semibold text-black"
      >
        Retry
      </button>
    );
  }

  if (row.action === 'pause') {
    return (
      <button
        type="button"
        onClick={() => pause(row.id)}
        className="rounded-full border border-white/15 px-3 py-1 text-[12px] font-semibold text-white"
      >
        Pause
      </button>
    );
  }

  if (row.action === 'resume') {
    return (
      <button
        type="button"
        onClick={() => resume(row.id)}
        className="rounded-full bg-white px-3 py-1 text-[12px] font-semibold text-black"
      >
        Resume
      </button>
    );
  }

  if (row.action === 'cancel') {
    return (
      <button
        type="button"
        onClick={() => cancel(row.id)}
        className="rounded-full border border-white/15 px-3 py-1 text-[12px] font-semibold text-white"
      >
        Cancel
      </button>
    );
  }

  if (row.action === 'remove') {
    return (
      <button
        type="button"
        onClick={() => remove(row.id, true)}
        className="rounded-full border border-white/15 px-3 py-1 text-[12px] font-semibold text-white"
      >
        Delete
      </button>
    );
  }

  return null;
}

function DownloadRowCard({ row }: { row: DownloadRow }) {
  const pause = useArmzStore((state) => state.pause);
  const resume = useArmzStore((state) => state.resume);

  const active = row.status === 'Probing' || row.status === 'Downloading' || row.status === 'Finalizing';
  const progress = row.status === 'Finalizing' ? row.finalizeProgress : row.fraction;
  const canPause = row.canPause;

  return (
    <li className="rounded-3xl border border-white/10 bg-white/5 p-4 backdrop-blur-xl">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-[15px] font-semibold text-white">{row.fileName}</p>
          <p className="mt-1 truncate text-[12px] text-white/55">{row.url}</p>
        </div>
        <SpringCheck
          checked={row.status === 'Completed'}
          onChange={() => undefined}
          disabled={!row.isDone}
          ariaLabel={`${row.fileName} completed`}
          className="shrink-0"
        />
      </div>

      <div className="mt-4">
        <div className="h-2 overflow-hidden rounded-full bg-white/10">
          <div
            className="h-full rounded-full bg-white"
            style={{ width: `${Math.max(0, Math.min(1, progress)) * 100}%` }}
          />
        </div>
        <div className="mt-2 flex items-center justify-between text-[11px] text-white/55">
          <span>{statusLabel(row.status)}</span>
          <span>{formatBytes(row.downloadedBytes)} / {formatBytes(row.totalBytes)}</span>
          <span>{formatSpeed(row.bytesPerSecond)}</span>
          <span>{formatEta(row.etaSeconds)}</span>
        </div>
      </div>

      {row.status === 'Finalizing' ? (
        <p className="mt-3 text-[12px] text-white/70">Assembling download… {row.finalizeStep ?? 'Working'}</p>
      ) : null}

      {row.status === 'Failed' ? (
        <div className="mt-3 flex justify-end">
          <RowActions row={row} />
        </div>
      ) : null}

      {row.status === 'Paused' ? (
        <div className="mt-3 flex justify-end">
          <button
            type="button"
            onClick={() => resume(row.id)}
            className="rounded-full bg-white px-3 py-1 text-[12px] font-semibold text-black"
          >
            Resume
          </button>
        </div>
      ) : null}

      {row.status === 'Downloading' || row.status === 'Probing' ? (
        <div className="mt-3 flex justify-end">
          {canPause ? (
            <button
              type="button"
              onClick={() => pause(row.id)}
              className="rounded-full border border-white/15 px-3 py-1 text-[12px] font-semibold text-white"
            >
              Pause
            </button>
          ) : null}
        </div>
      ) : null}

      {active || row.status === 'Downloading' || row.status === 'Finalizing' ? null : null}
    </li>
  );
}

export function DownloadsScreen({ activeFilter, onOpenAdd, onOpenSettings }: DownloadsScreenProps) {
  const downloads = useArmzDownloads();
  const setActiveFilter = useArmzStore((state) => state.setActiveFilter);
  const pauseAll = useArmzStore((state) => state.pauseAll);
  const resumeAll = useArmzStore((state) => state.resumeAll);

  const visible = downloads.filter((download) => {
    const row = toDownloadRow(download);
    if (activeFilter === 'Active') {
      return row.status === 'Probing' || row.status === 'Downloading' || row.status === 'Finalizing';
    }
    if (activeFilter === 'Done') {
      return row.status === 'Completed';
    }
    return true;
  });

  const rows = visible.map(toDownloadRow);

  return (
    <section className="relative flex h-full w-full flex-col">
      <div className="relative z-10 flex h-full w-full flex-col">
        <header className="px-5 pt-8">
          <BlurText
            text="Downloads"
            className="text-[34px] font-bold tracking-tight text-white"
            animateBy="words"
            direction="top"
            effect="blur"
            blur={8}
            distance={24}
            staggerFrom="start"
            trigger="load"
            easing="easeOut"
          />
          <p className="mt-1 flex items-baseline gap-2 text-[12px] text-white/45">
            <CountUp
              to={downloads.length}
              duration={0.6}
              trigger="load"
              className="text-[18px] font-semibold text-white/80"
            />
            <span>in library</span>
          </p>
          <div className="mt-5 flex items-center justify-between gap-3">
            <div className="flex rounded-full bg-white/10 p-1">
              {FILTERS.map((filter) => (
                <button
                  key={filter}
                  type="button"
                  onClick={() => setActiveFilter(filter)}
                  className={`rounded-full px-4 py-1.5 text-[13px] font-semibold ${
                    activeFilter === filter ? 'bg-white text-black' : 'text-white/70'
                  }`}
                >
                  {filter}
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={resumeAll}
                className="rounded-full border border-white/15 px-3 py-1.5 text-[12px] font-semibold text-white"
              >
                Resume all
              </button>
              <button
                type="button"
                onClick={pauseAll}
                className="rounded-full border border-white/15 px-3 py-1.5 text-[12px] font-semibold text-white"
              >
                Pause all
              </button>
              <button
                type="button"
                onClick={onOpenSettings}
                className="rounded-full border border-white/15 px-3 py-1.5 text-[12px] font-semibold text-white"
              >
                Settings
              </button>
            </div>
          </div>
        </header>

        <main className="min-h-0 flex-1 overflow-y-auto px-5 pb-8">
          {rows.length === 0 ? (
            <section className="relative mt-10 flex min-h-[420px] flex-1 flex-col items-center justify-center overflow-hidden rounded-[32px] border border-white/10 bg-white/5 px-6 text-center">
              <Aurora
                className="absolute inset-0 h-full w-full"
                colorStops={['#0A84FF', '#5E5CE6', '#0A84FF']}
                amplitude={0.45}
                blend={0.5}
                speed={0.8}
                height={0.6}
                glow={0.6}
                brightness={0.9}
                layers={2}
                lightMode={false}
                paused={false}
                dpr={1.5}
              />
              <div className="relative z-10">
                <BlurText
                  text="Nothing downloading"
                  className="text-[24px] font-bold text-white"
                  animateBy="words"
                  direction="top"
                  effect="blur"
                  blur={6}
                  distance={18}
                  staggerFrom="start"
                  trigger="load"
                  easing="easeOut"
                />
                <Magnet padding={32}>
                  <button
                    type="button"
                    onClick={onOpenAdd}
                    className="inline-flex h-12 min-w-[180px] items-center justify-center rounded-full bg-white px-8 text-[15px] font-semibold text-black"
                  >
                    Paste Link
                  </button>
                </Magnet>
              </div>
            </section>
          ) : (
            <ul className="space-y-3">
              {rows.map((row) => (
                <DownloadRowCard key={row.id} row={row} />
              ))}
            </ul>
          )}
        </main>
      </div>
    </section>
  );
}
