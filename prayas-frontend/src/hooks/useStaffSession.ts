import { useEffect, useState } from 'react';
import { fetchMe } from '../api/staffApi';
import type { Me } from '../api/types';

export interface StaffSession {
  checking: boolean;
  me: Me | null;
  authError: string | null;
}

/**
 * Who is signed in, checked once when a staff page opens. `me` is null when
 * nobody is signed in (a normal state, not an error); `authError` carries
 * either a rejected-login message (the backend redirects back here with
 * ?error=access_denied) or a real failure while checking.
 */
export function useStaffSession(): StaffSession {
  const [checking, setChecking] = useState(true);
  const [me, setMe] = useState<Me | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);

  useEffect(() => {
    if (new URLSearchParams(window.location.search).get('error') === 'access_denied') {
      setAuthError('That Google account isn\u2019t allowed to sign in here. Please use your @iith.ac.in account.');
      window.history.replaceState(null, '', window.location.pathname);
    }

    fetchMe()
      .then(setMe)
      .catch((err) => {
        console.error(err);
        setAuthError(
          err?.response?.data?.detail ?? "Couldn't check your login status. See the console for details.",
        );
      })
      .finally(() => setChecking(false));
  }, []);

  return { checking, me, authError };
}
