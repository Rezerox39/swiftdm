export const SEGMENT_PRESETS = [4, 8, 12, 16, 24, 32] as const;

export function clampSegments(value: number): number {
  if (!Number.isFinite(value)) return 16;
  return Math.max(1, Math.min(32, Math.round(value)));
}
