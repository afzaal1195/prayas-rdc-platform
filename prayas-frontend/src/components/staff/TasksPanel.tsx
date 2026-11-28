import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchTaskBoard, setRequirementStatus } from '../../api/staffApi';
import { extractErrorMessage } from '../../api/errors';
import type { RequirementStatus, TaskBoard, TaskCounts, TaskRow } from '../../api/types';
import { formatDateDMY } from '../DateInput';

const STATUS_LABEL: Record<RequirementStatus, string> = {
  PENDING: 'Not started',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
  BLOCKED: 'Blocked',
  CANCELLED: 'Cancelled',
};

const STATUS_CLASS: Record<RequirementStatus, string> = {
  PENDING: 'pending',
  IN_PROGRESS: 'pending',
  DONE: 'approved',
  BLOCKED: 'rejected',
  CANCELLED: 'neutral',
};

const SETTABLE: RequirementStatus[] = ['PENDING', 'IN_PROGRESS', 'DONE', 'BLOCKED'];

function CountsStrip({ counts }: { counts: TaskCounts }) {
  return (
    <div className="task-counts">
      <span className="task-count">
        <strong>{counts.total}</strong> tasks
      </span>
      <span className="task-count">
        <strong>{counts.done}</strong> done
      </span>
      <span className="task-count">
        <strong>{counts.inProgress}</strong> in progress
      </span>
      <span className="task-count">
        <strong>{counts.pending}</strong> not started
      </span>
      {counts.blocked > 0 && (
        <span className="task-count task-count-bad">
          <strong>{counts.blocked}</strong> blocked
        </span>
      )}
      {counts.needsAttention > 0 && (
        <span className="task-count task-count-bad">
          <strong>{counts.needsAttention}</strong> need a re-check
        </span>
      )}
    </div>
  );
}

function TaskLine({ task, onChanged }: { task: TaskRow & { domainName: string }; onChanged: () => void }) {
  const [error, setError] = useState<string | null>(null);
  const [working, setWorking] = useState(false);

  const change = async (status: RequirementStatus) => {
    setWorking(true);
    setError(null);
    try {
      await setRequirementStatus(task.requirementId, status);
      onChanged();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setWorking(false);
    }
  };

  const fromApprovals = task.kind !== 'VOLUNTEERS';
  return (
    <li className="approval-step">
      <div className="approval-step-head">
        <span className="approval-step-title">
          {task.title}
          <span className="approval-tour-date">{task.domainName}</span>
        </span>
        {task.canSetStatus ? (
          <select
            value={task.status}
            disabled={working}
            aria-label={`Status of ${task.title} for ${task.schoolName}`}
            onChange={(e) => change(e.target.value as RequirementStatus)}
          >
            {SETTABLE.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>
        ) : (
          <span className={`status-badge ${STATUS_CLASS[task.status]}`}>{STATUS_LABEL[task.status]}</span>
        )}
      </div>
      {task.needsAttention && (
        <div className="date-change-hint">Something changed on this tour, so this needs a re-check.</div>
      )}
      {task.progress && <div className="date-change-hint">{task.progress}</div>}
      {fromApprovals && (
        <div className="date-change-hint">
          Worked out from its approvals. <Link to="/staff/approvals">Open Approvals</Link>
        </div>
      )}
      <div className="task-team">
        <span>
          <strong>Coordinators:</strong> {task.coordinators.join(', ') || 'none yet'}
        </span>
        <span>
          <strong>Volunteers:</strong> {task.volunteers.join(', ') || 'none yet'}
        </span>
      </div>
      {task.members.length > 0 && (
        <div className="task-members">
          <strong>
            {task.members.filter((m) => m.done).length} of {task.members.length} finished their part
          </strong>
          <ul>
            {task.members.map((m, i) => (
              <li key={`${m.name}-${i}`}>
                {m.done ? '✓' : '○'} {m.name} ({m.role === 'COORDINATOR' ? 'coordinator' : 'volunteer'})
                {m.done && m.note && <> — “{m.note}”</>}
                {m.done && m.proofUrl && (
                  <>
                    {' '}
                    <a href={m.proofUrl} target="_blank" rel="noopener noreferrer">
                      proof
                    </a>
                  </>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
      {error && <p className="field-error">{error}</p>}
    </li>
  );
}

interface SchoolGroup {
  programmeId: number;
  schoolName: string;
  visitDate: string;
  tasks: (TaskRow & { domainName: string })[];
}

function SchoolSection({ group, onChanged }: { group: SchoolGroup; onChanged: () => void }) {
  return (
    <section className="school-card">
      <header className="school-card-head">
        <h3 className="school-card-title">{group.schoolName}</h3>
        <span className="school-card-date">{formatDateDMY(group.visitDate)}</span>
        <CountsStrip counts={countTasks(group.tasks)} />
      </header>
      <ul className="school-card-tasks">
        {group.tasks.map((t) => (
          <TaskLine key={t.requirementId} task={t} onChanged={onChanged} />
        ))}
      </ul>
    </section>
  );
}

function countTasks(tasks: TaskRow[]): TaskCounts {
  const count = (s: RequirementStatus) => tasks.filter((t) => t.status === s).length;
  return {
    total: tasks.length,
    pending: count('PENDING'),
    inProgress: count('IN_PROGRESS'),
    done: count('DONE'),
    blocked: count('BLOCKED'),
    needsAttention: tasks.filter((t) => t.needsAttention).length,
  };
}

/** Task status for upcoming approved tours: one block per school, with its tasks listed under it. */
export function TasksPanel() {
  const [board, setBoard] = useState<TaskBoard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    fetchTaskBoard()
      .then(setBoard)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  // One block per school, soonest visit first; each task remembers which domain owns it.
  const schools = useMemo(() => {
    const byTour = new Map<number, SchoolGroup>();
    for (const d of board?.domains ?? []) {
      for (const t of d.tasks) {
        const group = byTour.get(t.programmeId) ?? {
          programmeId: t.programmeId,
          schoolName: t.schoolName,
          visitDate: t.visitDate,
          tasks: [],
        };
        group.tasks.push({ ...t, domainName: d.domainName });
        byTour.set(t.programmeId, group);
      }
    }
    return [...byTour.values()].sort((a, b) => a.visitDate.localeCompare(b.visitDate));
  }, [board]);

  return (
    <div className="dashboard-table-wrap">
      {loading && <p>Loading…</p>}
      {error && <p className="field-error">{error}</p>}

      {board && board.domains.length === 0 && (
        <div className="empty-state">
          No tasks yet. Tasks for each domain appear here once a tour is approved.
        </div>
      )}

      {board && board.domains.length > 0 && (
        <>
          {board.overview && (
            <>
              <h3 className="my-tours-heading">All domains</h3>
              <CountsStrip counts={board.totals} />
              <table className="request-table">
                <thead>
                  <tr>
                    <th>Domain</th>
                    <th>Tasks</th>
                    <th>Done</th>
                    <th>In progress</th>
                    <th>Not started</th>
                    <th>Blocked</th>
                    <th>Re-check</th>
                  </tr>
                </thead>
                <tbody>
                  {board.domains.map((d) => (
                    <tr key={d.domainId}>
                      <td>{d.domainName}</td>
                      <td>{d.counts.total}</td>
                      <td>{d.counts.done}</td>
                      <td>{d.counts.inProgress}</td>
                      <td>{d.counts.pending}</td>
                      <td>{d.counts.blocked}</td>
                      <td>{d.counts.needsAttention}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
          {schools.map((g) => (
            <SchoolSection key={g.programmeId} group={g} onChanged={load} />
          ))}
        </>
      )}
    </div>
  );
}
