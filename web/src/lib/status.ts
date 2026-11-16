import type { DownloadItem, DownloadStatus } from '../bridge/types';

export type ArmzAction = 'retry' | 'pause' | 'resume' | 'cancel' | 'remove' | null;

export interface DownloadRow {
  id: string;
  fileName: string;
  url: string;
  mimeType: string | null;
  totalBytes: number;
  downloadedBytes: number;
  status: DownloadStatus;
  bytesPerSecond: number;
  etaSeconds: number | null;
  error: string | null;
  finalizeStep: string | null;
  finalizeProgress: number;
  segmentCount: number;
  fraction: number;
  canPause: boolean;
  canResume: boolean;
  isDone: boolean;
  action: ArmzAction;
}

export function toDownloadRow(item: DownloadItem): DownloadRow {
  const isActive = item.status === 'Probing' || item.status === 'Downloading' || item.status === 'Finalizing';
  const isDone = item.status === 'Completed';
  const isFailed = item.status === 'Failed';

  const action: ArmzAction = isDone
    ? null
    : isFailed
      ? 'retry'
      : item.status === 'Paused'
        ? 'resume'
        : isActive
          ? 'pause'
          : null;

  return {
    ...item,
    fraction: item.fraction,
    canPause: Boolean(item.live && isActive),
    canResume: item.status === 'Paused' || isFailed || (!item.live && isActive),
    isDone,
    action,
  };
}

export function formatDownloadStatus(status: DownloadStatus): string {
  return status;
}
