import { useEffect, useState } from 'react';
import { fetchMyAssignments } from '../../api/staffApi';
import { extractErrorMessage } from '../../api/errors';
import type { MyAssignment } from '../../api/types';
import { campusClock } from '../../lib/campusTime';
import { formatDateDMY } from '../DateInput';
import { formatTime12h } from '../TimeInput12h';

const ROLE_LABEL = { VOLUNTEER: 'Volunteer', COORDINATOR: 'Coordinator' } as const;

function Table({ rows, past }: { rows: MyAssignment[]; past?: boolean }) {
  return (
    <table className={`request-table ${past ? 'my-tours-past' : ''}`}>
      <thead>
        <tr>
          <th>Date</th>
          <th>Your shift</th>
          <th>Role</th>
          <th>School</th>
          <th>Students</th>
          <th>Contact at the school</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((a) => (
          <tr key={a.assignmentId}>
            <td>{formatDateDMY(a.visitDate)}</td>
            <td>
              {formatTime12h(campusClock(a.startsAt))} – {formatTime12h(campusClock(a.endsAt))}
            </td>
            <td>{ROLE_LABEL[a.role]}</td>
            <td>
              {a.institutionName}
              {[a.villageOrTown, a.district].filter(Boolean).length > 0 && (
                <div className="date-change-hint" style={{ margin: 0 }}>
                  {[a.villageOrTown, a.district].filter(Boolean).join(', ')}
                </div>
              )}
            </td>
            <td>{a.studentCount || '—'}</td>
            <td>
              {a.contactName || '—'}
              {a.contactPhone ? ` · ${a.contactPhone}` : ''}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** The signed-in person's own shifts: upcoming first, finished ones underneath. */
export function MyToursPanel() {
  const [rows, setRows] = useState<MyAssignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchMyAssignments()
      .then(setRows)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const now = Date.now();
  const upcoming = rows.filter((a) => new Date(a.endsAt).getTime() >= now);
  const finished = rows.filter((a) => new Date(a.endsAt).getTime() < now).reverse();

  return (
    <div className="dashboard-table-wrap">
      {loading && <p>Loading…</p>}
      {error && <p className="field-error">{error}</p>}

      {!loading && !error && rows.length === 0 && (
        <div className="empty-state">You aren&apos;t assigned to any tours yet. They will appear here once you are.</div>
      )}

      {upcoming.length > 0 && (
        <>
          <h3 className="my-tours-heading">Upcoming</h3>
          <Table rows={upcoming} />
        </>
      )}
      {finished.length > 0 && (
        <>
          <h3 className="my-tours-heading">Finished</h3>
          <Table rows={finished} past />
        </>
      )}
    </div>
  );
}
