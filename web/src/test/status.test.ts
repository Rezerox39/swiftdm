import { describe, expect, it } from 'vitest';
import { toDownloadRow } from '../lib/status';
import type { DownloadItem, DownloadStatus } from '../bridge/types';

function item(status: DownloadStatus, overrides: Partial<DownloadItem> = {}): DownloadItem {
  return {
    id: `id-${status}`,
    url: 'https://example.com/file.bin',
    fileName: 'file.bin',
    filePath: '',
    contentUri: null,
    mimeType: null,
    totalBytes: 100,
    downloadedBytes: 25,
    status,
    bytesPerSecond: 0,
    etaSeconds: null,
    error: null,
    finalizeStep: null,
    finalizeProgress: 0,
    segmentCount: 8,
    createdAt: 1,
    live: false,
    fraction: 0.25,
    ...overrides,
  };
}

describe('download row mapping', () => {
  it('maps every native status to a row action', () => {
    expect(toDownloadRow(item('Idle')).action).toBeNull();
    expect(toDownloadRow(item('Probing', { live: true })).action).toBe('pause');
    expect(toDownloadRow(item('Downloading', { live: true })).action).toBe('pause');
    expect(toDownloadRow(item('Finalizing', { live: true })).action).toBe('pause');
    expect(toDownloadRow(item('Paused')).action).toBe('resume');
    expect(toDownloadRow(item('Completed')).action).toBeNull();
    expect(toDownloadRow(item('Failed')).action).toBe('retry');
    expect(toDownloadRow(item('Cancelled')).action).toBeNull();
  });

  it('exposes pause, resume and completion flags', () => {
    const active = toDownloadRow(item('Downloading', { live: true }));
    expect(active.canPause).toBe(true);
    expect(active.canResume).toBe(false);
    expect(active.isDone).toBe(false);

    const orphaned = toDownloadRow(item('Downloading', { live: false }));
    expect(orphaned.canPause).toBe(false);
    expect(orphaned.canResume).toBe(true);

    expect(toDownloadRow(item('Paused')).canResume).toBe(true);
    expect(toDownloadRow(item('Failed')).canResume).toBe(true);
    expect(toDownloadRow(item('Completed')).isDone).toBe(true);
  });

  it('preserves finalizing progress as the visible fraction', () => {
    const row = toDownloadRow(item('Finalizing', { live: true, finalizeProgress: 0.7, finalizeStep: 'Hashing' }));
    expect(row.finalizeStep).toBe('Hashing');
    expect(row.fraction).toBe(0.25);
    expect(row.action).toBe('pause');
  });
});
