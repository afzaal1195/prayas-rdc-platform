import { useEffect } from 'react';

/** Sets the browser tab / installed-app window title while a page is showing, and restores the previous one after. */
export function useDocumentTitle(title: string) {
  useEffect(() => {
    const previous = document.title;
    document.title = title;
    return () => {
      document.title = previous;
    };
  }, [title]);
}
