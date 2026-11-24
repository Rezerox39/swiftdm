export const SEGMENT_PRESETS = [4, 8, 16, 24, 32, 48] as const;

export function clampSegments(value: number): number {
  if (!Number.isFinite(value)) return 24;
  return Math.max(1, Math.min(48, Math.round(value)));
}
