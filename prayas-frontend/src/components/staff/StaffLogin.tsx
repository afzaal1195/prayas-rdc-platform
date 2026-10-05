import { useEffect, useState } from 'react';
import type { MouseEvent } from 'react';
import { loginUrl } from '../../api/staffApi';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { StaffHeader } from './StaffHeader';
import '../../styles/form.css';
import '../../styles/dashboard.css';

export function StaffLogin({ authError }: { authError: string | null }) {
  useDocumentTitle('Sign in');
  const [signingIn, setSigningIn] = useState(false);

  // If the person presses the browser's Back button from Google, the browser
  // may restore this page exactly as it was, with the spinner still showing.
  useEffect(() => {
    const reset = (e: PageTransitionEvent) => {
      if (e.persisted) setSigningIn(false);
    };
    window.addEventListener('pageshow', reset);
    return () => window.removeEventListener('pageshow', reset);
  }, []);

  const handleClick = (e: MouseEvent<HTMLAnchorElement>) => {
    if (signingIn) {
      e.preventDefault(); // ignore extra clicks while we're already redirecting
      return;
    }
    setSigningIn(true); // the link still works: the browser goes on to Google
  };

  return (
    <div className="page page-staff">
      <StaffHeader title="Login" />
      <div className="dashboard-login">
        <p>Sign in with your @iith.ac.in Google account to review campus tour requests.</p>
        {authError && <p className="field-error">{authError}</p>}
        <a
          className={`btn btn-primary${signingIn ? ' is-loading' : ''}`}
          href={loginUrl()}
          onClick={handleClick}
          aria-busy={signingIn}
        >
          {signingIn && <span className="btn-spinner" aria-hidden="true" />}
          {signingIn ? 'Redirecting to Google…' : 'Sign in with Google'}
        </a>
        <p className="login-status" role="status">
          {signingIn ? 'Please wait, this can take a few seconds.' : ''}
        </p>
      </div>
    </div>
  );
}