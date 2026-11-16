import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

function source(relativePath: string): string {
  return readFileSync(new URL(relativePath, import.meta.url), 'utf8');
}

describe('UI contract', () => {
  it('uses 44 px Stepper action buttons', () => {
    const stepper = source('../reactbits/Stepper.tsx');
    expect(stepper).not.toContain('h-[38px]');
    expect(stepper).toContain('h-11');
  });

  it('does not claim stabilized ETA in release notes', () => {
    const workflow = source('../../../.github/workflows/build-apk.yml');
    expect(workflow).not.toContain('stabilized speed/remaining-time readouts');
    expect(workflow).toContain('stabilized speed readouts and computed ETA');
  });
});
