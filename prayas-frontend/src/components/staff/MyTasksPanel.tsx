import { useEffect, useState } from 'react';
import { fetchMyDomainJobs, markJobDone, undoJobDone } from '../../api/staffApi';
import { extractErrorMessage } from '../../api/errors';
import type { DomainJob } from '../../api/types';
import { formatDateDMY } from '../DateInput';

const ROLE_LABEL = { VOLUNTEER: 'Volunteer', COORDINATOR: 'Coordinator' } as const;

function JobCard({ job, onChanged }: { job: DomainJob; onChanged: () => void }) {
  const [note, setNote] = useState('');
  const [proofUrl, setProofUrl] = useState('');
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = async (action: () => Promise<void>) => {
    setWorking(true);
    setError(null);
    try {
      await action();
      onChanged();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setWorking(false);
    }
  };

  return (
    <section className="school-card">
      <header className="school-card-head">
        <h3 className="school-card-title">{job.schoolName}</h3>
        <span className="school-card-date">{formatDateDMY(job.visitDate)}</span>
        <span className={`status-badge ${job.done ? 'approved' : 'pending'}`}>
          {job.done ? 'My part is done' : 'Not done yet'}
        </span>
      </header>
      <div className="my-task-body">
        <div>
          <strong>{job.domainName}</strong> · you are a {ROLE_LABEL[job.role].toLowerCase()}
        </div>
        <div className="date-change-hint">
          Jobs: {job.jobs.join(', ') || '—'}
          {job.role === 'VOLUNTEER' && ` · Coordinator: ${job.coordinatorNames.join(', ') || 'none yet'}`}
        </div>

        {job.done ? (
          <div className="my-task-done">
            {job.note && <div>“{job.note}”</div>}
            {job.proofUrl && (
              <div>
                <a href={job.proofUrl} target="_blank" rel="noopener noreferrer">
                  Open proof
                </a>
              </div>
            )}
            <button
              type="button"
              className="btn btn-secondary"
              disabled={working}
              onClick={() => run(() => undoJobDone(job.assignmentId))}
            >
              Not done after all
            </button>
          </div>
        ) : (
          <div className="my-task-form">
            <input
              type="text"
              value={note}
              maxLength={500}
              placeholder="Note (optional)"
              aria-label={`Note for ${job.schoolName}`}
              onChange={(e) => setNote(e.target.value)}
            />
            <input
              type="url"
              value={proofUrl}
              maxLength={500}
              placeholder="Link to a photo or document (optional)"
              aria-label={`Proof link for ${job.schoolName}`}
              onChange={(e) => setProofUrl(e.target.value)}
            />
            <button
              type="button"
              className="btn btn-primary"
              disabled={working}
              onClick={() => run(() => markJobDone(job.assignmentId, note, proofUrl))}
            >
              Mark my part done
            </button>
          </div>
        )}
        {error && <p className="field-error">{error}</p>}
      </div>
    </section>
  );
}

/** The signed-in person's own jobs on upcoming tours, soonest first, each with a "my part is done" tick. */
export function MyTasksPanel() {
  const [jobs, setJobs] = useState<DomainJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    fetchMyDomainJobs()
      .then(setJobs)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  return (
    <div className="dashboard-table-wrap">
      {loading && <p>Loading…</p>}
      {error && <p className="field-error">{error}</p>}
      {!loading && !error && jobs.length === 0 && (
        <div className="empty-state">
          You have no tasks yet. A coordinator or domain head adds you to a tour&apos;s team, and the task appears here.
        </div>
      )}
      {jobs.map((j) => (
        <JobCard key={j.assignmentId} job={j} onChanged={load} />
      ))}
    </div>
  );
}
