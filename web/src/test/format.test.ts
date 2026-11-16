import { describe, expect, it } from 'vitest';
import { formatBytes, formatEta, formatSpeed } from '../lib/format';

describe('formatters', () => {
  it('formats byte sizes with binary units', () => {
    expect(formatBytes(0)).toBe('0 B');
    expect(formatBytes(1024)).toBe('1.0 KB');
    expect(formatBytes(128 * 1024 * 1024)).toBe('128 MB');
    expect(formatBytes(-1)).toBe('0 B');
  });

  it('formats speed and ETA', () => {
    expect(formatSpeed(2 * 1024 * 1024)).toBe('2.0 MB/s');
    expect(formatEta(65)).toBe('01:05');
    expect(formatEta(3661)).toBe('1:01:01');
    expect(formatEta(null)).toBe('--:--');
  });
});
