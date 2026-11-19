import { useEffect, useState } from 'react';
import { assignTeamMember, fetchTeams, removeTeamMember } from '../../api/staffApi';
import { extractErrorMessage } from '../../api/errors';
import type { DomainTeam, TeamMember, TeamRole, TourTeams, UserOption } from '../../api/types';
import { formatDateDMY } from '../DateInput';

interface RowProps {
  label: string;
  members: TeamMember[];
  canChange: boolean;
  options: UserOption[];
  busy: boolean;
  onAdd: (userId: number) => void;
  onRemove: (assignmentId: number) => void;
}

/** One line of a domain's team: the people on it, plus a picker to add someone if allowed. */
function TeamRow({ label, members, canChange, options, busy, onAdd, onRemove }: RowProps) {
  const [chosen, setChosen] = useState('');
  return (
    <div className="team-row">
      <span className="team-row-label">{label}</span>
      <div className="team-row-body">
        {members.length === 0 && <span className="date-change-hint">Nobody yet</span>}
        {members.map((m) => (
          <span key={m.assignmentId} className="team-chip">
            {m.name}
            {canChange && (
              <button
                type="button"
                className="team-chip-remove"
                disabled={busy}
                aria-label={`Remove ${m.name}`}
                onClick={() => onRemove(m.assignmentId)}
              >
                ×
              </button>
            )}
          </span>
        ))}
        {canChange && (
          <span className="team-add">
            <select
              value={chosen}
              onChange={(e) => setChosen(e.target.value)}
              aria-label={`Add to ${label}`}
              disabled={busy || options.length === 0}
            >
              <option value="">{options.length === 0 ? 'No one to add' : 'Add…'}</option>
              {options.map((o) => (
                <option key={o.userId} value={o.userId}>
                  {o.name}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={busy || !chosen}
              onClick={() => {
                onAdd(Number(chosen));
                setChosen('');
              }}
            >
              Add
            </button>
          </span>
        )}
      </div>
    </div>
  );
}

function DomainCard({ tour, team, onChanged }: { tour: TourTeams; team: DomainTeam; onChanged: () => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
      onChanged();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const add = (role: TeamRole) => (userId: number) =>
    run(() => assignTeamMember(tour.programmeId, team.domainId, userId, role));
  const remove = (assignmentId: number) => run(() => removeTeamMember(assignmentId));

  return (
    <li className="approval-step">
      <div className="approval-step-head">
        <span className="approval-step-title">{team.domainName}</span>
        <span className="date-change-hint">{team.jobs.join(' · ')}</span>
      </div>
      <TeamRow
        label="Coordinators"
        members={team.coordinators}
        canChange={team.canAssignCoordinator}
        options={team.coordinatorOptions}
        busy={busy}
        onAdd={add('COORDINATOR')}
        onRemove={remove}
      />
      <TeamRow
        label="Volunteers"
        members={team.volunteers}
        canChange={team.canAssignVolunteer}
        options={team.volunteerOptions}
        busy={busy}
        onAdd={add('VOLUNTEER')}
        onRemove={remove}
      />
      {error && <p className="field-error">{error}</p>}
    </li>
  );
}

/** Upcoming approved tours, each with the domains that have a job on it and who is looking after them. */
export function TeamsPanel() {
  const [tours, setTours] = useState<TourTeams[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    fetchTeams()
      .then(setTours)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  return (
    <div className="dashboard-table-wrap">
      {loading && <p>Loading…</p>}
      {error && <p className="field-error">{error}</p>}

      {!loading && !error && tours.length === 0 && (
        <div className="empty-state">No upcoming approved tours yet. Teams are set up once a tour is approved.</div>
      )}

      {tours.map((tour) => (
        <section key={tour.programmeId} className="approval-tour">
          <h3 className="my-tours-heading">
            {tour.schoolName}
            <span className="approval-tour-date">{formatDateDMY(tour.visitDate)}</span>
          </h3>
          <ul className="approval-steps">
            {tour.domains.map((d) => (
              <DomainCard key={d.domainId} tour={tour} team={d} onChanged={load} />
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
