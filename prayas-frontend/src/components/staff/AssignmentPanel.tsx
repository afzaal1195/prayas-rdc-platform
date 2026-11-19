import { useCallback, useEffect, useState } from 'react';
import {
  assignPerson,
  fetchAssignable,
  fetchAssignments,
  handoverShift,
  releaseAssignment,
} from '../../api/staffApi';
import { extractErrorMessage } from '../../api/errors';
import type { AssigneeOption, AssignmentRole, AssignmentView } from '../../api/types';
import { campusClock } from '../../lib/campusTime';
import { formatTime12h } from '../TimeInput12h';

const ROLE_LABEL: Record<AssignmentRole, string> = {
  VOLUNTEER: 'Volunteer',
  COORDINATOR: 'Coordinator',
};

interface Props {
  programmeId: number;
  arrivalTime: string | null;
  departureTime: string | null;
}

/** Who is doing what on one approved tour: add people, hand a volunteer shift over, or remove someone. */
export function AssignmentPanel({ programmeId, arrivalTime, departureTime }: Props) {
  const [shifts, setShifts] = useState<AssignmentView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [working, setWorking] = useState(false);

  // "Add someone" form. Leaving from/to empty means the whole visit.
  const [role, setRole] = useState<AssignmentRole>('VOLUNTEER');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [options, setOptions] = useState<AssigneeOption[]>([]);
  const [personId, setPersonId] = useState('');

  // "Hand over" form for one shift at a time.
  const [handoverId, setHandoverId] = useState<number | null>(null);
  const [handoverAt, setHandoverAt] = useState('');
  const [handoverOptions, setHandoverOptions] = useState<AssigneeOption[]>([]);
  const [handoverPersonId, setHandoverPersonId] = useState('');

  const loadShifts = useCallback(() => {
    return fetchAssignments(programmeId)
      .then(setShifts)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [programmeId]);

  const loadOptions = useCallback(() => {
    fetchAssignable(programmeId, role, from || undefined, to || undefined)
      .then((list) => {
        setOptions(list);
        setPersonId('');
      })
      .catch((err) => {
        setOptions([]);
        setError(extractErrorMessage(err));
      });
  }, [programmeId, role, from, to]);

  useEffect(() => {
    loadShifts();
  }, [loadShifts]);

  useEffect(() => {
    loadOptions();
  }, [loadOptions]);

  const handoverShiftRow = shifts.find((s) => s.id === handoverId);
  const handoverEnd = handoverShiftRow ? campusClock(handoverShiftRow.endsAt) : '';
  const handoverRole = handoverShiftRow?.role;

  // Who could take over from the chosen time until the end of that shift.
  useEffect(() => {
    if (handoverId === null || !handoverAt || !handoverEnd || !handoverRole) {
      setHandoverOptions([]);
      return;
    }
    fetchAssignable(programmeId, handoverRole, handoverAt, handoverEnd)
      .then((list) => {
        setHandoverOptions(list);
        setHandoverPersonId('');
      })
      .catch((err) => {
        setHandoverOptions([]);
        setError(extractErrorMessage(err));
      });
  }, [programmeId, handoverId, handoverAt, handoverEnd, handoverRole]);

  const refresh = async () => {
    await loadShifts();
    loadOptions();
  };

  const run = async (action: () => Promise<void>) => {
    setWorking(true);
    setError(null);
    try {
      await action();
      await refresh();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setWorking(false);
    }
  };

  const add = () =>
    run(async () => {
      await assignPerson(programmeId, {
        userId: Number(personId),
        role,
        from: from || undefined,
        to: to || undefined,
      });
      setFrom('');
      setTo('');
    });

  const confirmHandover = () =>
    run(async () => {
      await handoverShift(handoverId as number, Number(handoverPersonId), handoverAt);
      closeHandover();
    });

  const remove = (shift: AssignmentView) => run(() => releaseAssignment(shift.id));

  const openHandover = (shift: AssignmentView) => {
    setHandoverId(shift.id);
    setHandoverAt('');
    setHandoverOptions([]);
    setHandoverPersonId('');
  };

  const closeHandover = () => {
    setHandoverId(null);
    setHandoverAt('');
    setHandoverOptions([]);
    setHandoverPersonId('');
  };

  return (
    <div className="assign-panel" onClick={(e) => e.stopPropagation()}>
      <h4>Assigned team</h4>
      <p className="assign-note">
        Visit: {formatTime12h(arrivalTime) || '—'} to {formatTime12h(departureTime) || '—'}
      </p>

      {loading && <p>Loading…</p>}
      {error && <p className="field-error">{error}</p>}

      {!loading && shifts.length === 0 && <p className="assign-note">No one is assigned yet.</p>}

      {shifts.length > 0 && (
        <ul className="shift-list">
          {shifts.map((s) => (
            <li key={s.id} className="shift-item">
              <div className="shift-main">
                <span className="shift-name">{s.userName}</span>
                <span className="shift-role">{ROLE_LABEL[s.role]}</span>
                <span className="shift-time">
                  {formatTime12h(campusClock(s.startsAt))} – {formatTime12h(campusClock(s.endsAt))}
                </span>
                {handoverId !== s.id && (
                  <button className="btn-text" disabled={working} onClick={() => openHandover(s)}>
                    Hand over
                  </button>
                )}
                <button className="btn-text" disabled={working} onClick={() => remove(s)}>
                  Remove
                </button>
              </div>

              {handoverId === s.id && (
                <div className="handover-form">
                  <label>
                    Leaves at
                    <input type="time" value={handoverAt} onChange={(e) => setHandoverAt(e.target.value)} />
                  </label>
                  <label>
                    Replaced by
                    <select
                      value={handoverPersonId}
                      onChange={(e) => setHandoverPersonId(e.target.value)}
                      disabled={!handoverAt}
                    >
                      <option value="">
                        {!handoverAt
                          ? 'Pick a time first'
                          : handoverOptions.length === 0
                            ? 'No one is free'
                            : `Choose a ${ROLE_LABEL[s.role].toLowerCase()}…`}
                      </option>
                      {handoverOptions.map((o) => (
                        <option key={o.userId} value={o.userId}>
                          {o.fullName}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    className="btn btn-primary"
                    disabled={working || !handoverAt || !handoverPersonId}
                    onClick={confirmHandover}
                  >
                    Confirm hand-over
                  </button>
                  <button className="btn btn-secondary" disabled={working} onClick={closeHandover}>
                    Cancel
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      <div className="assign-form">
        <label>
          Role
          <select value={role} onChange={(e) => setRole(e.target.value as AssignmentRole)}>
            <option value="VOLUNTEER">Volunteer</option>
            <option value="COORDINATOR">Coordinator</option>
          </select>
        </label>
        <label>
          From (optional)
          <input type="time" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label>
          To (optional)
          <input type="time" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
        <label>
          Person
          <select value={personId} onChange={(e) => setPersonId(e.target.value)}>
            <option value="">{options.length === 0 ? 'No one is free' : 'Choose…'}</option>
            {options.map((o) => (
              <option key={o.userId} value={o.userId}>
                {o.fullName}
              </option>
            ))}
          </select>
        </label>
        <button className="btn btn-primary" disabled={working || !personId} onClick={add}>
          Assign
        </button>
      </div>
      <p className="assign-note">Leave From and To empty to cover the whole visit. Only people who are free are listed.</p>
    </div>
  );
}
