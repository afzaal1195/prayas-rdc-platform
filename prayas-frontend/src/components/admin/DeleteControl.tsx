import { useState } from 'react';

interface Props {
  /** What is being deleted, for the confirmation sentence, e.g. "this venue". */
  what: string;
  disabled?: boolean;
  /** Shown beside the button when it is disabled. */
  disabledReason?: string;
  /** Does the delete. Should throw if it fails -- the parent shows the error. */
  onConfirm: () => Promise<void>;
}

/**
 * A delete button that asks "are you sure?" first. Deleting is permanent, so
 * it takes two deliberate clicks, and the second names what's about to go.
 */
export function DeleteControl({ what, disabled, disabledReason, onConfirm }: Props) {
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const run = async () => {
    setDeleting(true);
    try {
      await onConfirm();
    } catch {
      // The parent has already shown why it failed; just step back.
      setConfirming(false);
    } finally {
      setDeleting(false);
    }
  };

  if (!confirming) {
    return (
      <span className="confirm-delete">
        <button type="button" className="btn btn-danger" disabled={disabled} onClick={() => setConfirming(true)}>
          Delete
        </button>
        {disabled && disabledReason && <span className="hint">{disabledReason}</span>}
      </span>
    );
  }

  return (
    <span className="confirm-delete">
      <span>Delete {what} permanently?</span>
      <button type="button" className="btn btn-danger solid" disabled={deleting} onClick={run}>
        {deleting ? 'Deleting…' : 'Yes, delete'}
      </button>
      <button type="button" className="btn btn-secondary" disabled={deleting} onClick={() => setConfirming(false)}>
        Keep
      </button>
    </span>
  );
}
