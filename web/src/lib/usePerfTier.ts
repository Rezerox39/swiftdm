import { useEffect, useState } from 'react';

export type PerfTier = 'high' | 'low';

function readTier(): PerfTier {
  return document.documentElement.dataset.perf === 'low' ? 'low' : 'high';
}

export function usePerfTier(): PerfTier {
  const [tier, setTier] = useState<PerfTier>(readTier);

  useEffect(() => {
    const observer = new MutationObserver(() => setTier(readTier()));
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-perf'] });
    return () => observer.disconnect();
  }, []);

  return tier;
}
