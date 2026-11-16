export type DownloadStatus =
  | 'Idle'
  | 'Probing'
  | 'Downloading'
  | 'Finalizing'
  | 'Paused'
  | 'Completed'
  | 'Failed'
  | 'Cancelled';

export type FinalizeStep = 'Verifying' | 'Syncing' | 'Hashing' | 'Publishing';

export interface SegmentSnapshot {
  index: number;
  start: number;
  end: number;
  downloaded: number;
  state: 'Pending' | 'Active' | 'Done' | 'Failed';
}

export interface DownloadSnapshot {
  id: string;
  url: string;
  fileName: string;
  filePath: string;
  totalBytes: number;
  downloadedBytes: number;
  status: DownloadStatus;
  bytesPerSecond: number;
  etaSeconds: number | null;
  rangeSupported: boolean;
  etag: string | null;
  lastModified: string | null;
  segmentCount: number;
  segments: SegmentSnapshot[];
  sha256: string | null;
  error: string | null;
  finalizeStep: FinalizeStep | null;
  finalizeProgress: number;
  contentUri: string | null;
  mimeType: string | null;
}

export interface DownloadItem {
  id: string;
  url: string;
  fileName: string;
  filePath: string;
  contentUri: string | null;
  mimeType: string | null;
  totalBytes: number;
  downloadedBytes: number;
  status: DownloadStatus;
  bytesPerSecond: number;
  etaSeconds: number | null;
  error: string | null;
  finalizeStep: FinalizeStep | null;
  finalizeProgress: number;
  segmentCount: number;
  createdAt: number;
  live: boolean;
  fraction: number;
}

export interface ProbeResult {
  finalUrl: string;
  contentLength: number;
  rangeSupported: boolean;
  etag: string | null;
  lastModified: string | null;
  fileName: string | null;
  mimeType: string | null;
}

export interface StartDownloadInput {
  url: string;
  headers?: Record<string, string>;
  segments?: number;
}

export interface Settings {
  wifiOnly: boolean;
  maxConcurrentDownloads: number;
  defaultSegments: number;
  speedLimitBytesPerSecond: number | null;
  keepDownloadsAlive: boolean;
  visualEffects: 'auto' | 'reduced' | 'off';
}

export type NativeEvent =
  | { kind: 'downloads'; downloads: DownloadItem[] }
  | { kind: 'settings'; settings?: Settings }
  | { kind: 'network'; online?: boolean }
  | { kind: 'permission'; granted?: boolean };
