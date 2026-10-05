import { useState } from 'react';
import type { TourRequestCreated } from '../api/types';

interface Props {
  result: TourRequestCreated;
}

export function SuccessScreen({ result }: Props) {
  const statusUrl = `${window.location.origin}/status/${result.trackingToken}`;
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(statusUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard API can be blocked (permissions, non-HTTPS context, etc.)
      // -- the link is still fully visible and selectable, so this isn't fatal.
    }
  };

  return (
    <div className="success-screen">
      <div className="success-mark" aria-hidden="true">✓</div>
      <h1>Request sent</h1>
      <p>
        Thank you — your campus tour request has been received and is now{' '}
        <strong>under review</strong>. We'll be in touch on the phone number you provided once a
        decision is made.
      </p>

      <div className="tracking-box">
        <label htmlFor="tracking-link" style={{ fontWeight: 500 }}>
          Save this link to check your request's status later
        </label>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginTop: '0.4rem' }}>
          <span className="token" id="tracking-link" style={{ flex: 1 }}>
            {statusUrl}
          </span>
          <button type="button" className="btn btn-secondary" onClick={handleCopy} style={{ whiteSpace: 'nowrap' }}>
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>
      </div>

      <p className="hint">Request reference: #{result.programmeId}</p>
    </div>
  );
}