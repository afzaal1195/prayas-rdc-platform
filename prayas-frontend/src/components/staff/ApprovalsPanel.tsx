import { useEffect, useMemo, useState } from 'react';
import { fetchApprovalQueue, updateApproval } from '../../api/staffApi';
import { extractErrorMessage } from '../../api/errors';
import type { ApprovalStatus, ApprovalStep } from '../../api/types';
import { formatDateDMY } from '../DateInput';

const STATUS_LABEL: Record<ApprovalStatus, string> = {
  WAITING: 'Waiting for step 1',
  PENDING: 'Not asked yet',
  REQUESTED: 'Asked, waiting for reply',
  APPROVED: 'Approved',
  DENIED: 'Denied',
};

const STATUS_CLASS: Record<ApprovalStatus, string> = {
  WAITING: 'neutral',
  PENDING: 'pending',
  REQUESTED: 'pending',
  APPROVED: 'approved',
  DENIED: 'rejected',
};

/** The buttons offered for a step, depending on where it has got to. */
const ACTIONS: Record<ApprovalStatus, { status: ApprovalStatus; label: string; primary?: boolean }[]> = {
  WAITING: [],
  PENDING: [
    { status: 'REQUESTED', label: 'Mark as asked' },
    { status: 'APPROVED', label: 'Approved', primary: true },
    { status: 'DENIED', label: 'Denied' },
  ],
  REQUESTED: [
    { status: 'APPROVED', label: 'Approved', primary: true },
    { status: 'DENIED', label: 'Denied' },
    { status: 'PENDING', label: 'Not asked yet' },
  ],
  APPROVED: [{ status: 'PENDING', label: 'Undo' }],
  DENIED: [
    { status: 'REQUESTED', label: 'Ask again' },
    { status: 'PENDING', label: 'Reset' },
  ],
};

function StepCard({ step, onChanged }: { step: ApprovalStep; onChanged: () => void }) {
  const [note, setNote] = useState('');
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const act = async (status: ApprovalStatus) => {
    setWorking(true);
    setError(null);
    try {
      await updateApproval(step.id, status, note.trim() || undefined);
      setNote('');
      onChanged();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setWorking(false);
    }
  };

  const contact = step.contact;
  return (
    <li className="approval-step">
      <div className="approval-step-head">
        <span className="approval-step-title">{step.title}</span>
        <span className={`status-badge ${STATUS_CLASS[step.status]}`}>{STATUS_LABEL[step.status]}</span>
      </div>
      {step.detail && <div className="date-change-hint">{step.detail}</div>}

      {contact ? (
        <div className="approval-contact">
          <strong>{contact.name}</strong>
          {contact.office ? `, ${contact.office}` : ''}
          <div>
            {[contact.phone, contact.email].filter(Boolean).join(' · ') || 'No phone or email on file'}
          </div>
          {(contact.officeHours || contact.preferredContact) && (
            <div className="date-change-hint">
              {[contact.officeHours, contact.preferredContact && `Prefers: ${contact.preferredContact}`]
                .filter(Boolean)
                .join(' · ')}
            </div>
          )}
        </div>
      ) : step.status !== 'WAITING' ? (
        <div className="date-change-hint">
          No contact is set for this step. Ask a lead to add one in the authority directory.
        </div>
      ) : null}

      {step.note && (
        <div className="approval-note">
          “{step.note}”{step.decidedByName ? ` — ${step.decidedByName}` : ''}
        </div>
      )}

      {ACTIONS[step.status].length > 0 && (
        <div className="approval-actions">
          <input
            type="text"
            value={note}
            maxLength={500}
            placeholder="Note (needed if denied)"
            onChange={(e) => setNote(e.target.value)}
            aria-label={`Note for ${step.title}`}
          />
          {ACTIONS[step.status].map((a) => (
            <button
              key={a.status}
              type="button"
              className={`btn ${a.primary ? 'btn-primary' : 'btn-secondary'}`}
              disabled={working}
              onClick={() => act(a.status)}
            >
              {a.label}
            </button>
          ))}
        </div>
      )}
      {error && <p className="field-error">{error}</p>}
    </li>
  );
}

/** Upcoming approved tours that still need a venue or lunch permission, soonest first. */
export function ApprovalsPanel() {
  const [steps, setSteps] = useState<ApprovalStep[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    fetchApprovalQueue()
      .then(setSteps)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  // One card per tour, keeping the order the server sent (soonest visit first).
  const tours = useMemo(() => {
    const byTour = new Map<number, ApprovalStep[]>();
    for (const s of steps) {
      byTour.set(s.programmeId, [...(byTour.get(s.programmeId) ?? []), s]);
    }
    return [...byTour.values()];
  }, [steps]);

  return (
    <div className="dashboard-table-wrap">
      {loading && <p>Loading…</p>}
      {error && <p className="field-error">{error}</p>}

      {!loading && !error && tours.length === 0 && (
        <div className="empty-state">
          Nothing is waiting for approval. Venue and lunch permissions for upcoming tours will appear here.
        </div>
      )}

      {tours.map((group) => (
        <section key={group[0].programmeId} className="approval-tour">
          <h3 className="my-tours-heading">
            {group[0].schoolName}
            <span className="approval-tour-date">
              {group[0].visitDate ? formatDateDMY(group[0].visitDate) : ''}
            </span>
          </h3>
          <ul className="approval-steps">
            {group.map((s) => (
              <StepCard key={s.id} step={s} onChanged={load} />
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
