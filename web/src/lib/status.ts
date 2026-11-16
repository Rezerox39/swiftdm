import type { DownloadItem, DownloadStatus } from '../bridge/types';

export type ArmzAction = 'retry' | 'pause' | 'resume' | 'cancel' | 'remove' | null;

export interface DownloadRow {
  id: string;
  fileName: string;
  url: string;
  filePath: string;
  contentUri: string | null;
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
  createdAt: number;
  live: boolean;
  fraction: number;
  canPause: boolean;
  canResume: boolean;
  canRetry: boolean;
  canOpen: boolean;
  canShare: boolean;
  isActive: boolean;
  isDone: boolean;
  action: ArmzAction;
}

function isActiveStatus(status: DownloadStatus): boolean {
  return status === 'Probing' || status === 'Downloading' || status === 'Finalizing';
}

export function toDownloadRow(item: DownloadItem): DownloadRow {
  const isActive = isActiveStatus(item.status);
  const isDone = item.status === 'Completed';
  const isFailed = item.status === 'Failed';
  const hasFile = isDone && Boolean(item.contentUri || item.filePath);

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
    canRetry: isFailed || item.status === 'Cancelled',
    canOpen: hasFile,
    canShare: hasFile,
    isActive,
    isDone,
    action,
  };
}

export function formatDownloadStatus(status: DownloadStatus): string {
  switch (status) {
    case 'Probing':
      return 'Preparing';
    case 'Finalizing':
      return 'Finishing up';
    case 'Cancelled':
      return 'Cancelled';
    default:
      return status;
  }
}
