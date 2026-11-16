export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const exponent = bytes === 0
    ? 0
    : Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** exponent;
  return `${value >= 100 || exponent === 0 ? Math.round(value) : value.toFixed(1)} ${units[exponent]}`;
}

export function stabilizeSpeed(bytesPerSecond: number): number {
  if (!Number.isFinite(bytesPerSecond) || bytesPerSecond <= 0) return 0;
  const step = bytesPerSecond >= 10 * 1024 * 1024 ? 1024 * 1024 : 100 * 1024;
  return Math.round(bytesPerSecond / step) * step;
}

export function formatSpeed(bytesPerSecond: number): string {
  return `${formatBytes(bytesPerSecond)}/s`;
}

export function formatEta(seconds: number | null): string {
  if (seconds == null || !Number.isFinite(seconds) || seconds < 0) return '--:--';
  const total = Math.ceil(seconds);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const secs = total % 60;
  const mm = String(minutes).padStart(2, '0');
  const ss = String(secs).padStart(2, '0');
  return hours > 0 ? `${hours}:${mm}:${ss}` : `${mm}:${ss}`;
}

export function isHttpUrl(value: string): boolean {
  const trimmed = value.trim();
  if (!trimmed) return false;
  try {
    const url = new URL(trimmed);
    return (url.protocol === 'http:' || url.protocol === 'https:') && Boolean(url.hostname);
  } catch {
    return false;
  }
}

export function fileNameFromUrl(value: string): string {
  try {
    const url = new URL(value.trim());
    const pathName = url.pathname.split('/').filter(Boolean).pop();
    const decoded = pathName ? decodeURIComponent(pathName) : '';
    return decoded || url.hostname || 'Download';
  } catch {
    return 'Download';
  }
}

export function hostFromUrl(value: string): string {
  try {
    return new URL(value.trim()).hostname.replace(/^www\./i, '');
  } catch {
    return '';
  }
}

export function extensionOf(fileName: string): string {
  const clean = fileName.split('?')[0].split('#')[0].trim();
  const dot = clean.lastIndexOf('.');
  return dot > 0 && dot < clean.length - 1 ? clean.slice(dot + 1).toLowerCase() : '';
}
