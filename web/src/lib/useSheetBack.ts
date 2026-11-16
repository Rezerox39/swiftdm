import { useEffect, useRef } from 'react';

/**
 * Gives a local bottom sheet its own browser-history entry so Android's back
 * gesture closes the sheet instead of leaving the app.
 */
export function useSheetBack(open: boolean, onClose: () => void): void {
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  const pushedRef = useRef(false);

  useEffect(() => {
    if (!open) return;

    if (!pushedRef.current) {
      window.history.pushState({ armzSheet: true }, '');
      pushedRef.current = true;
    }

    const onPopState = () => {
      pushedRef.current = false;
      closeRef.current();
    };

    window.addEventListener('popstate', onPopState);
    return () => {
      window.removeEventListener('popstate', onPopState);
      if (pushedRef.current) {
        pushedRef.current = false;
        const state = window.history.state as { armzSheet?: boolean } | null;
        if (state?.armzSheet) window.history.back();
      }
    };
  }, [open]);
}
