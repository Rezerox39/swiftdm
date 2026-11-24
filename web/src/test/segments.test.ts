import { describe, expect, it } from 'vitest';
import { SEGMENT_PRESETS, clampSegments } from '../lib/segments';

describe('segment presets', () => {
  it('uses the six supported presets', () => {
    expect(SEGMENT_PRESETS).toEqual([4, 8, 16, 24, 32, 48]);
  });

  it('clamps and rounds segment counts', () => {
    expect(clampSegments(0)).toBe(1);
    expect(clampSegments(49)).toBe(48);
    expect(clampSegments(12.6)).toBe(13);
    expect(clampSegments(Number.NaN)).toBe(24);
  });
});
