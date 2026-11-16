import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { HugeiconsIcon } from '@hugeicons/react';
import {
  AlertCircleIcon,
  ArrowUpRight01Icon,
  Cancel01Icon,
  CloudDownloadIcon,
  Copy01Icon,
  Delete01Icon,
  MoreVerticalIcon,
  PauseIcon,
  PlayIcon,
  PlusSignIcon,
  RefreshIcon,
  Settings01Icon,
  Share01Icon,
} from '@hugeicons/core-free-icons';
import { FileGlyph } from '../components/FileGlyph';
import { Sheet } from '../components/Sheet';
import { fileNameFromUrl, formatBytes, formatEta, formatSpeed, hostFromUrl } from '../lib/format';
import { toDownloadRow, type DownloadRow } from '../lib/status';
import { usePerfTier } from '../lib/usePerfTier';
import { useSheetBack } from '../lib/useSheetBack';
import { useArmzDownloads, useArmzSettings, useArmzStore } from '../store/useArmzStore';
import Aurora from '../reactbits/Aurora';
import BlurText from '../reactbits/BlurText';
import CountUp from '../reactbits/CountUp';
import Magnet from '../reactbits/Magnet';
import type { Settings } from '../bridge/types';

export type DownloadFilter = 'All' | 'Active' | 'Done';

interface DownloadsScreenProps {
  activeFilter: DownloadFilter;
  onOpenAdd: () => void;
  onOpenSettings: () => void;
}

const FILTERS: DownloadFilter[] = ['All', 'Active', 'Done'];

type SheetState = { kind: 'actions' | 'delete'; id: string } | null;

function displayName(row: DownloadRow): string {
  return row.fileName || fileNameFromUrl(row.url) || 'Download';
}

function clampFraction(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.max(0, Math.min(1, value));
}

function progressOf(row: DownloadRow): number {
  if (row.status === 'Completed') return 1;
  if (row.status === 'Finalizing') return clampFraction(row.finalizeProgress);
  return clampFraction(row.fraction);
}

function sizeText(row: DownloadRow): string {
  if (row.totalBytes > 0) {
    if (row.isDone) return formatBytes(row.totalBytes);
    return `${formatBytes(row.downloadedBytes)} of ${formatBytes(row.totalBytes)}`;
  }
  if (row.downloadedBytes > 0) return `${formatBytes(row.downloadedBytes)} downloaded`;
  if (row.status === 'Probing') return hostFromUrl(row.url) || 'Resolving file';
  return hostFromUrl(row.url) || 'Unknown size';
}

function statusText(row: DownloadRow): string {
  const percent = Math.round(progressOf(row) * 100);
  switch (row.status) {
    case 'Probing':
      return 'Preparing download';
    case 'Downloading':
      return `${percent}% downloaded`;
    case 'Finalizing':
      return row.finalizeStep ? `Finishing · ${row.finalizeStep}` : 'Finishing up';
    case 'Paused':
      return `Paused · ${percent}%`;
    case 'Completed':
      return 'Complete';
    case 'Failed':
      return 'Download failed';
    case 'Cancelled':
      return 'Cancelled';
    default:
      return 'Queued';
  }
}

function statText(row: DownloadRow): string {
  if (row.status === 'Downloading' && row.bytesPerSecond > 0) {
    const eta = row.etaSeconds != null ? formatEta(row.etaSeconds) : '';
    return eta ? `${formatSpeed(row.bytesPerSecond)} · ${eta}` : formatSpeed(row.bytesPerSecond);
  }
  if (row.status === 'Finalizing') return `${Math.round(progressOf(row) * 100)}%`;
  if (row.isDone) return 'Saved';
  if (row.status === 'Paused') return `${Math.round(progressOf(row) * 100)}%`;
  if (row.status === 'Failed' || row.status === 'Cancelled') return 'Retry';
  return hostFromUrl(row.url) || '';
}

function progressTone(row: DownloadRow): string {
  if (row.status === 'Failed') return 'bg-rose-400/80';
  if (row.isDone) return 'bg-white/85';
  if (row.status === 'Paused') return 'bg-white/35';
  return 'bg-[#6EA8FF]';
}

function dotTone(row: DownloadRow): string {
  if (row.status === 'Failed') return 'bg-rose-400';
  if (row.isDone) return 'bg-emerald-300';
  if (row.status === 'Paused' || row.status === 'Cancelled') return 'bg-white/35';
  return 'bg-[#6EA8FF]';
}

function IconButton({
  label,
  onClick,
  children,
  disabled = false,
}: {
  label: string;
  onClick: () => void;
  children: ReactNode;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-white/[0.09] bg-white/[0.035] text-white/65 transition active:scale-95 active:bg-white/[0.08] disabled:opacity-30"
    >
      {children}
    </button>
  );
}

function DownloadCard({ row, onOpenActions }: { row: DownloadRow; onOpenActions: (id: string) => void }) {
  const openFile = useArmzStore((state) => state.openFile);
  const pause = useArmzStore((state) => state.pause);
  const resume = useArmzStore((state) => state.resume);

  let primary: { label: string; icon: ReactNode; onClick: () => void } | null = null;
  if (row.canOpen) {
    primary = {
      label: 'Open file',
      icon: <HugeiconsIcon icon={ArrowUpRight01Icon} size={17} strokeWidth={1.9} />,
      onClick: () => openFile(row.id),
    };
  } else if (row.canPause) {
    primary = {
      label: 'Pause download',
      icon: <HugeiconsIcon icon={PauseIcon} size={17} strokeWidth={1.9} />,
      onClick: () => pause(row.id),
    };
  } else if (row.canResume) {
    const retry = row.status === 'Failed' || row.status === 'Cancelled';
    primary = {
      label: retry ? 'Retry download' : 'Resume download',
      icon: <HugeiconsIcon icon={retry ? RefreshIcon : PlayIcon} size={17} strokeWidth={1.9} />,
      onClick: () => resume(row.id),
    };
  }

  const percent = Math.round(progressOf(row) * 100);
  const filename = displayName(row);
  const failed = row.status === 'Failed';

  return (
    <li
      className={`relative overflow-hidden rounded-[22px] border p-3.5 ${
        row.isActive
          ? 'border-white/[0.11] bg-[#0B0C10]'
          : 'border-white/[0.065] bg-[#09090B]'
      }`}
    >
      <div className="flex items-start gap-3">
        <FileGlyph fileName={filename} active={row.isActive || row.isDone} />

        <div className="min-w-0 flex-1">
          <div className="flex items-start gap-2">
            <div className="min-w-0 flex-1">
              <p className="line-clamp-2 text-[14.5px] font-semibold leading-[1.28] tracking-[-0.015em] text-white">
                {filename}
              </p>
              <p className="mt-1 truncate text-[11.5px] text-white/40">{sizeText(row)}</p>
            </div>

            <div className="flex shrink-0 items-center gap-1">
              {primary ? (
                <button
                  type="button"
                  onClick={primary.onClick}
                  aria-label={primary.label}
                  title={primary.label}
                  className={`grid h-8 w-8 place-items-center rounded-full border transition active:scale-95 ${
                    row.canOpen
                      ? 'border-white/15 bg-white text-black'
                      : 'border-white/[0.1] bg-white/[0.05] text-white/70'
                  }`}
                >
                  {primary.icon}
                </button>
              ) : null}
              <button
                type="button"
                onClick={() => onOpenActions(row.id)}
                aria-label={`Options for ${filename}`}
                title="Options"
                className="grid h-8 w-8 place-items-center rounded-full text-white/35 transition active:scale-95 active:bg-white/[0.07] active:text-white/70"
              >
                <HugeiconsIcon icon={MoreVerticalIcon} size={17} strokeWidth={1.9} />
              </button>
            </div>
          </div>

          <div className="mt-3">
            <div
              className={`relative h-1.5 overflow-hidden rounded-full bg-white/[0.07] ${
                row.status === 'Probing' ? 'armz-progress-indeterminate' : ''
              }`}
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={row.status === 'Probing' ? undefined : percent}
              aria-label={`${filename} progress`}
            >
              {row.status === 'Probing' ? (
                <div className="armz-progress-sweep absolute inset-y-0 w-1/2 rounded-full bg-[#6EA8FF]/80" />
              ) : (
                <div
                  className={`h-full rounded-full transition-[width] duration-300 ease-out ${progressTone(row)}`}
                  style={{ width: `${percent}%` }}
                />
              )}
            </div>

            <div className="mt-2 flex items-center justify-between gap-2">
              <div className="flex min-w-0 items-center gap-1.5">
                <span className={`h-1.5 w-1.5 shrink-0 rounded-full ${dotTone(row)}`} />
                <span className="truncate text-[11.5px] text-white/45">{statusText(row)}</span>
              </div>
              <span
                className={`shrink-0 text-[11.5px] tabular-nums ${
                  failed ? 'text-rose-300/80' : row.isActive ? 'text-white/65' : 'text-white/40'
                }`}
              >
                {statText(row)}
              </span>
            </div>

            {failed ? (
              <div className="mt-2 flex items-start gap-1.5 text-[11.5px] leading-4 text-rose-300/75">
                <HugeiconsIcon icon={AlertCircleIcon} size={13} strokeWidth={2} className="mt-0.5 shrink-0" />
                <p className="line-clamp-2">{row.error || 'The transfer was interrupted. Retry to continue.'}</p>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </li>
  );
}

function EmptyState({
  filter,
  onAdd,
  onShowAll,
  settings,
  lowPerf,
}: {
  filter: DownloadFilter;
  onAdd: () => void;
  onShowAll: () => void;
  settings: Settings;
  lowPerf: boolean;
}) {
  const isAll = filter === 'All';
  const title = isAll ? 'Nothing here yet' : filter === 'Active' ? 'No active downloads' : 'No downloads finished';
  const body = isAll
    ? 'Paste a direct file link. AMz splits it into parallel connections for a faster start.'
    : filter === 'Active'
      ? 'Everything is settled. New transfers will appear here while they run.'
      : 'Completed files will collect here when they are ready to open or share.';

  return (
    <section className="relative mt-4 overflow-hidden rounded-[26px] border border-white/[0.065] bg-[#08080A] px-6 py-10 text-center">
      {isAll ? (
        <Aurora
          className="pointer-events-none absolute inset-x-0 -top-16 h-64 w-full opacity-40"
          colorStops={['#0A5CFF', '#08080A', '#5B8CFF']}
          amplitude={0.4}
          blend={0.55}
          speed={0.45}
          height={0.5}
          glow={0.5}
          brightness={0.75}
          layers={1}
          lightMode={false}
          paused={settings.visualEffects === 'off' || lowPerf}
          dpr={1}
        />
      ) : null}

      <div className="relative">
        <div className="mx-auto grid h-14 w-14 place-items-center rounded-[18px] border border-white/[0.09] bg-white/[0.04] text-white/70">
          <HugeiconsIcon
            icon={isAll ? CloudDownloadIcon : filter === 'Active' ? AlertCircleIcon : RefreshIcon}
            size={25}
            strokeWidth={1.55}
          />
        </div>

        <BlurText
          text={title}
          className="mt-5 text-[23px] font-semibold tracking-[-0.035em] text-white"
          animateBy="words"
          direction="top"
          effect="blur"
          blur={5}
          distance={16}
          staggerFrom="start"
          trigger="load"
          easing="easeOut"
        />
        <p className="mx-auto mt-2 max-w-[280px] text-[12.5px] leading-5 text-white/40">{body}</p>

        <div className="mt-6 flex justify-center">
          {isAll ? (
            <Magnet
              padding={36}
              disabled={settings.visualEffects === 'off' || lowPerf}
              magnetStrength={0.35}
            >
              <button
                type="button"
                onClick={onAdd}
                className="inline-flex h-11 items-center gap-2 rounded-full bg-white px-5 text-[14px] font-semibold text-black shadow-[0_10px_30px_rgba(255,255,255,0.12)] transition active:scale-[0.98]"
              >
                <HugeiconsIcon icon={PlusSignIcon} size={17} strokeWidth={2.1} />
                Add download
              </button>
            </Magnet>
          ) : (
            <button
              type="button"
              onClick={onShowAll}
              className="inline-flex h-10 items-center rounded-full border border-white/[0.1] bg-white/[0.04] px-4 text-[13px] font-semibold text-white/70 transition active:scale-[0.98] active:bg-white/[0.08]"
            >
              Show all downloads
            </button>
          )}
        </div>
      </div>
    </section>
  );
}

function ActionRow({
  icon,
  label,
  detail,
  onClick,
  destructive = false,
}: {
  icon: ReactNode;
  label: string;
  detail?: string;
  onClick: () => void;
  destructive?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-left transition active:bg-white/[0.055] ${
        destructive ? 'text-rose-300' : 'text-white/80'
      }`}
    >
      <span
        className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl border ${
          destructive
            ? 'border-rose-300/15 bg-rose-400/[0.07] text-rose-300'
            : 'border-white/[0.07] bg-white/[0.04] text-white/60'
        }`}
      >
        {icon}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-[13.5px] font-medium">{label}</span>
        {detail ? <span className="mt-0.5 block truncate text-[11.5px] text-white/35">{detail}</span> : null}
      </span>
    </button>
  );
}

export function DownloadsScreen({ activeFilter, onOpenAdd, onOpenSettings }: DownloadsScreenProps) {
  const downloads = useArmzDownloads();
  const settings = useArmzSettings();
  const setActiveFilter = useArmzStore((state) => state.setActiveFilter);
  const pauseAll = useArmzStore((state) => state.pauseAll);
  const resumeAll = useArmzStore((state) => state.resumeAll);
  const haptic = useArmzStore((state) => state.haptic);
  const openFile = useArmzStore((state) => state.openFile);
  const shareFile = useArmzStore((state) => state.shareFile);
  const copyText = useArmzStore((state) => state.copyText);
  const pause = useArmzStore((state) => state.pause);
  const resume = useArmzStore((state) => state.resume);
  const cancel = useArmzStore((state) => state.cancel);
  const remove = useArmzStore((state) => state.remove);
  const lowPerf = usePerfTier() === 'low';

  const [sheet, setSheet] = useState<SheetState>(null);
  useSheetBack(sheet !== null, () => setSheet(null));

  const rows = useMemo(() => downloads.map(toDownloadRow), [downloads]);
  const activeCount = rows.filter((row) => row.isActive).length;
  const doneCount = rows.filter((row) => row.isDone).length;
  const pausedCount = rows.filter((row) => row.status === 'Paused').length;
  const activeSpeed = rows.reduce((sum, row) => (row.isActive ? sum + row.bytesPerSecond : sum), 0);

  const visible = rows.filter((row) => {
    if (activeFilter === 'Active') return row.isActive;
    if (activeFilter === 'Done') return row.isDone;
    return true;
  });

  const menuRow = sheet ? rows.find((row) => row.id === sheet.id) ?? null : null;

  useEffect(() => {
    if (sheet && !rows.some((row) => row.id === sheet.id)) setSheet(null);
  }, [rows, sheet]);

  const closeSheet = () => setSheet(null);

  const runAndClose = (action: () => void) => {
    action();
    closeSheet();
  };

  return (
    <section className="relative flex h-full w-full flex-col overflow-hidden">
      <header className="shrink-0 px-4 pt-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="grid h-7 w-7 place-items-center rounded-[9px] border border-white/[0.1] bg-white/[0.045] text-[10.5px] font-black tracking-[-0.08em] text-white">
              A
            </span>
            <span className="text-[12px] font-semibold tracking-[0.01em] text-white/55">AMz</span>
          </div>
          <IconButton label="Settings" onClick={onOpenSettings}>
            <HugeiconsIcon icon={Settings01Icon} size={17} strokeWidth={1.8} />
          </IconButton>
        </div>

        <div className="mt-4 flex items-end justify-between gap-3">
          <div className="min-w-0">
            <BlurText
              text="Downloads"
              className="text-[35px] font-semibold leading-none tracking-[-0.055em] text-white"
              animateBy="letters"
              direction="top"
              effect="blur"
              blur={6}
              distance={18}
              staggerFrom="start"
              trigger="load"
              easing="easeOut"
            />
            <p className="mt-2 flex flex-wrap items-center gap-x-1.5 gap-y-1 text-[12px] text-white/40">
              <CountUp
                to={rows.length}
                duration={0.55}
                trigger="load"
                className="font-semibold tabular-nums text-white/70"
              />
              <span>{rows.length === 1 ? 'file' : 'files'}</span>
              {activeCount > 0 ? (
                <>
                  <span className="text-white/20">·</span>
                  <span className="inline-flex items-center gap-1 text-[#8EBAFF]">
                    <span className="h-1 w-1 rounded-full bg-[#6EA8FF]" />
                    {activeCount} active
                  </span>
                  {activeSpeed > 0 ? (
                    <span className="tabular-nums text-white/30">{formatSpeed(activeSpeed)}</span>
                  ) : null}
                </>
              ) : null}
            </p>
          </div>

          <button
            type="button"
            onClick={onOpenAdd}
            aria-label="Add download"
            title="Add download"
            className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-white text-black shadow-[0_10px_28px_rgba(255,255,255,0.13)] transition active:scale-95"
          >
            <HugeiconsIcon icon={PlusSignIcon} size={20} strokeWidth={2.1} />
          </button>
        </div>

        <div className="mt-4 flex items-center gap-2">
          <div className="flex min-w-0 flex-1 items-center rounded-[14px] bg-white/[0.055] p-1">
            {FILTERS.map((filter) => {
              const count = filter === 'All' ? rows.length : filter === 'Active' ? activeCount : doneCount;
              const selected = activeFilter === filter;
              return (
                <button
                  key={filter}
                  type="button"
                  onClick={() => setActiveFilter(filter)}
                  className={`flex h-8 flex-1 items-center justify-center gap-1.5 rounded-[10px] text-[12px] font-semibold transition ${
                    selected ? 'bg-white text-black shadow-sm' : 'text-white/50 active:bg-white/[0.05]'
                  }`}
                >
                  {filter}
                  <span
                    className={`text-[10px] tabular-nums ${selected ? 'text-black/40' : 'text-white/25'}`}
                  >
                    {count}
                  </span>
                </button>
              );
            })}
          </div>

          {activeCount > 0 ? (
            <IconButton
              label="Pause all downloads"
              onClick={() => {
                pauseAll();
                haptic('light');
              }}
            >
              <HugeiconsIcon icon={PauseIcon} size={16} strokeWidth={1.9} />
            </IconButton>
          ) : null}

          {pausedCount > 0 ? (
            <IconButton
              label="Resume paused downloads"
              onClick={() => {
                resumeAll();
                haptic('light');
              }}
            >
              <HugeiconsIcon icon={PlayIcon} size={16} strokeWidth={1.9} />
            </IconButton>
          ) : null}
        </div>
      </header>

      <main className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 pb-6 pt-3">
        {visible.length === 0 ? (
          <EmptyState
            filter={activeFilter}
            onAdd={onOpenAdd}
            onShowAll={() => setActiveFilter('All')}
            settings={settings}
            lowPerf={lowPerf}
          />
        ) : (
          <ul className="space-y-2.5 pb-2">
            {visible.map((row) => (
              <DownloadCard key={row.id} row={row} onOpenActions={(id) => setSheet({ kind: 'actions', id })} />
            ))}
          </ul>
        )}
      </main>

      {sheet && menuRow ? (
        sheet.kind === 'actions' ? (
          <Sheet
            eyebrow="Download options"
            title={displayName(menuRow)}
            subtitle={`${statusText(menuRow)} · ${sizeText(menuRow)}`}
            onClose={closeSheet}
          >
            <div className="space-y-1">
              {menuRow.canOpen ? (
                <ActionRow
                  icon={<HugeiconsIcon icon={ArrowUpRight01Icon} size={18} strokeWidth={1.8} />}
                  label="Open file"
                  detail="Hand off to the app that owns this file type"
                  onClick={() => runAndClose(() => openFile(menuRow.id))}
                />
              ) : null}

              {menuRow.canShare ? (
                <ActionRow
                  icon={<HugeiconsIcon icon={Share01Icon} size={18} strokeWidth={1.8} />}
                  label="Share file"
                  onClick={() => runAndClose(() => shareFile(menuRow.id))}
                />
              ) : null}

              <ActionRow
                icon={<HugeiconsIcon icon={Copy01Icon} size={18} strokeWidth={1.8} />}
                label="Copy source link"
                detail={hostFromUrl(menuRow.url)}
                onClick={() => runAndClose(() => copyText(menuRow.url, 'Source link copied.'))}
              />

              {menuRow.canPause ? (
                <ActionRow
                  icon={<HugeiconsIcon icon={PauseIcon} size={18} strokeWidth={1.8} />}
                  label="Pause download"
                  onClick={() => runAndClose(() => pause(menuRow.id))}
                />
              ) : null}

              {menuRow.canResume || menuRow.canRetry ? (
                <ActionRow
                  icon={<HugeiconsIcon icon={RefreshIcon} size={18} strokeWidth={1.8} />}
                  label={menuRow.status === 'Paused' ? 'Resume download' : 'Retry download'}
                  onClick={() => runAndClose(() => resume(menuRow.id))}
                />
              ) : null}

              {menuRow.isActive || menuRow.status === 'Paused' ? (
                <ActionRow
                  icon={<HugeiconsIcon icon={Cancel01Icon} size={18} strokeWidth={1.8} />}
                  label="Cancel download"
                  detail="Stop this transfer"
                  onClick={() => runAndClose(() => cancel(menuRow.id))}
                />
              ) : null}

              <ActionRow
                icon={<HugeiconsIcon icon={Delete01Icon} size={18} strokeWidth={1.8} />}
                label={menuRow.isDone ? 'Delete file' : 'Delete download'}
                detail={menuRow.isDone ? 'Remove the file and its history' : 'Remove this entry and its partial data'}
                destructive
                onClick={() => setSheet({ kind: 'delete', id: menuRow.id })}
              />
            </div>
          </Sheet>
        ) : (
          <Sheet
            eyebrow="Confirm"
            title={menuRow.isDone ? 'Delete this file?' : 'Delete this download?'}
            subtitle="This removes the entry from AMz and deletes its local data. It cannot be undone."
            onClose={closeSheet}
          >
            <div className="rounded-2xl border border-rose-300/15 bg-rose-400/[0.055] p-4">
              <p className="truncate text-[13px] font-medium text-white/80">{displayName(menuRow)}</p>
              <p className="mt-1 text-[11.5px] text-white/40">{sizeText(menuRow)}</p>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={closeSheet}
                className="h-11 rounded-full border border-white/[0.1] bg-white/[0.04] text-[13.5px] font-semibold text-white/75 transition active:scale-[0.98]"
              >
                Keep
              </button>
              <button
                type="button"
                onClick={() => runAndClose(() => remove(menuRow.id, true))}
                className="h-11 rounded-full bg-rose-500 text-[13.5px] font-semibold text-white transition active:scale-[0.98] active:bg-rose-400"
              >
                Delete
              </button>
            </div>
          </Sheet>
        )
      ) : null}
    </section>
  );
}
