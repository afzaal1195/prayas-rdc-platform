import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { fetchTourStatus, reviseHeadcount, cancelTourRequest, requestDateChange } from '../api/tourApi';
import { extractErrorMessage } from '../api/errors';
import { TimeInput12h, formatTime12h } from '../components/TimeInput12h';
import { DateInput, formatDateDMY } from '../components/DateInput';
import type { TourStatusView } from '../api/types';
import type { GradeRow, CourseRow } from './formState';
import '../styles/form.css';
import '../styles/dashboard.css';

const EDITABLE_STATUSES: TourStatusView['status'][] = ['SUBMITTED', 'UNDER_REVIEW'];
const ALL_GRADES = Array.from({ length: 12 }, (_, i) => String(i + 1));

function statusClass(status: TourStatusView['status']): string {
  if (status === 'APPROVED' || status === 'COMPLETED') return 'approved';
  if (status === 'REJECTED' || status === 'CANCELLED') return 'rejected';
  return 'pending';
}

function statusMessage(status: TourStatusView['status']): string {
  switch (status) {
    case 'SUBMITTED':
    case 'UNDER_REVIEW':
      return 'Your request is under review. We\u2019ll be in touch on the phone number you provided once a decision is made.';
    case 'RESCHEDULE_PROPOSED':
      return 'We\u2019ve proposed a different date for this visit -- see below. Please contact us to confirm.';
    case 'APPROVED':
      return 'Your campus tour is approved. See you then!';
    case 'REJECTED':
      return 'Unfortunately this request was not approved. Contact Prayas directly if you have questions.';
    case 'CANCELLED':
      return 'This request has been cancelled.';
    case 'COMPLETED':
      return 'This tour has already taken place. Thanks for visiting!';
    default:
      return '';
  }
}

function computeTotal(isCollege: boolean, grades: GradeRow[], courses: CourseRow[]): number {
  const rows = isCollege ? courses : grades;
  return rows.reduce((sum, r) => sum + (Number(r.count) || 0), 0);
}

export function TourStatusPage() {
  const { token } = useParams<{ token: string }>();

  const [status, setStatus] = useState<TourStatusView | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [grades, setGrades] = useState<GradeRow[]>([{ grade: '', count: '' }]);
  const [courses, setCourses] = useState<CourseRow[]>([{ courseOrBranch: '', yearOfStudy: '', count: '' }]);
  const [teacherCount, setTeacherCount] = useState('');
  const [lunchRequired, setLunchRequired] = useState(false);
  const [mealCount, setMealCount] = useState('');
  const [revising, setRevising] = useState(false);
  const [reviseError, setReviseError] = useState<string | null>(null);
  const [reviseSuccess, setReviseSuccess] = useState(false);

  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const [confirmingCancel, setConfirmingCancel] = useState(false);
  const [cancelReason, setCancelReason] = useState('');

  const [newDate, setNewDate] = useState('');
  const [newArrivalTime, setNewArrivalTime] = useState('');
  const [newDepartureTime, setNewDepartureTime] = useState('');
  const [rescheduling, setRescheduling] = useState(false);
  const [rescheduleError, setRescheduleError] = useState<string | null>(null);
  const [rescheduleSuccess, setRescheduleSuccess] = useState(false);

  const load = () => {
    if (!token) return;
    setLoading(true);
    setLoadError(null);
    fetchTourStatus(token)
      .then((s) => {
        setStatus(s);
        setTeacherCount('');
        setLunchRequired(s.lunchRequired);
        setMealCount(s.mealCount ? String(s.mealCount) : '');
        // Pre-fill the breakdown from what's currently on file, so the
        // school is editing their existing rows rather than starting over.
        setGrades(
          s.grades && s.grades.length > 0
            ? s.grades.map((g) => ({ grade: g.grade, count: String(g.count) }))
            : [{ grade: '', count: '' }],
        );
        setCourses(
          s.courses && s.courses.length > 0
            ? s.courses.map((c) => ({
                courseOrBranch: c.courseOrBranch,
                yearOfStudy: c.yearOfStudy ?? '',
                count: String(c.count),
              }))
            : [{ courseOrBranch: '', yearOfStudy: '', count: '' }],
        );
        setNewArrivalTime(s.arrivalTime ? s.arrivalTime.slice(0, 5) : '');
        setNewDepartureTime(s.departureTime ? s.departureTime.slice(0, 5) : '');
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, [token]);

  const isCollege = status?.institutionType === 'COLLEGE';

  const updateGrade = (index: number, patch: Partial<GradeRow>) => {
    setGrades((prev) => prev.map((g, i) => (i === index ? { ...g, ...patch } : g)));
  };
  const addGrade = () => {
    if (grades.length >= 12) return;
    setGrades((prev) => [...prev, { grade: '', count: '' }]);
  };
  const removeGrade = (index: number) => setGrades((prev) => prev.filter((_, i) => i !== index));

  const updateCourse = (index: number, patch: Partial<CourseRow>) => {
    setCourses((prev) => prev.map((c, i) => (i === index ? { ...c, ...patch } : c)));
  };
  const addCourse = () => {
    if (courses.length >= 20) return;
    setCourses((prev) => [...prev, { courseOrBranch: '', yearOfStudy: '', count: '' }]);
  };
  const removeCourse = (index: number) => setCourses((prev) => prev.filter((_, i) => i !== index));

  const handleRevise = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    const total = computeTotal(isCollege, grades, courses);
    if (total < 1) {
      setReviseError('Add at least one grade/course with a student count.');
      return;
    }
    if (lunchRequired && (!mealCount || Number(mealCount) < 1)) {
      setReviseError('Meal count is required when lunch is needed.');
      return;
    }
    setRevising(true);
    setReviseError(null);
    setReviseSuccess(false);
    try {
      const gradePayload = !isCollege
        ? grades.filter((g) => g.grade && g.count).map((g) => ({ grade: g.grade, count: Number(g.count) }))
        : [];
      const coursePayload = isCollege
        ? courses
            .filter((c) => c.courseOrBranch && c.count)
            .map((c) => ({ courseOrBranch: c.courseOrBranch, yearOfStudy: c.yearOfStudy, count: Number(c.count) }))
        : [];
      await reviseHeadcount(
        token,
        gradePayload,
        coursePayload,
        Number(teacherCount) || 0,
        lunchRequired,
        lunchRequired ? Number(mealCount) : undefined,
      );
      setReviseSuccess(true);
      load();
    } catch (err) {
      setReviseError(extractErrorMessage(err));
    } finally {
      setRevising(false);
    }
  };

  const handleCancel = async () => {
    if (!token) return;
    setCancelling(true);
    setCancelError(null);
    try {
      await cancelTourRequest(token, cancelReason.trim() || undefined);
      setConfirmingCancel(false);
      load();
    } catch (err) {
      setCancelError(extractErrorMessage(err));
    } finally {
      setCancelling(false);
    }
  };

  const handleReschedule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token || !newDate || !status) return;
    if (newDate <= status.visitDate) {
      setRescheduleError(`The new date must be later than ${formatDateDMY(status.visitDate)}.`);
      return;
    }
    setRescheduling(true);
    setRescheduleError(null);
    setRescheduleSuccess(false);
    try {
      await requestDateChange(token, newDate, newArrivalTime || undefined, newDepartureTime || undefined);
      setRescheduleSuccess(true);
      setNewDate('');
      load();
    } catch (err) {
      setRescheduleError(extractErrorMessage(err));
    } finally {
      setRescheduling(false);
    }
  };

  const canEdit = status != null && EDITABLE_STATUSES.includes(status.status);
  const canCancel = status != null && !['REJECTED', 'CANCELLED', 'COMPLETED'].includes(status.status);

  return (
    <div className="page">
      <header className="page-header">
        <div className="page-header-logos">
          <img src="/prayas-logo-header.png" alt="PRAYAS" className="logo-prayas" />
          <span className="logo-divider" aria-hidden="true" />
          <img src="/rdc-logo.png" alt="Rural Development Center" className="logo-rdc" />
          <span className="logo-divider" aria-hidden="true" />
          <img src="/iith-logo.png" alt="Indian Institute of Technology Hyderabad" className="logo-iith" />
        </div>
        <h1>Your tour request</h1>
      </header>

      <div className="dashboard-table-wrap" style={{ maxWidth: 560, paddingTop: '2.5rem' }}>
        {loading && <p>Loading…</p>}
        {loadError && <p className="field-error">{loadError}</p>}

        {!loading && !loadError && status && (
          <div className="form-panel">
            <div className="form-panel-content">
              <span className={`status-badge ${statusClass(status.status)}`}>{status.status}</span>
              <p style={{ marginTop: '1rem' }}>{statusMessage(status.status)}</p>

              {status.decisionNote && (
                <p className="field-error" style={{ marginTop: '0.5rem' }}>
                  Reason: {status.decisionNote}
                </p>
              )}

              <div className="review-section">
                <div className="review-row">
                  <span className="k">Visit date</span>
                  <span className="v">{formatDateDMY(status.visitDate)}</span>
                </div>
                {status.proposedDate && (
                  <div className="review-row">
                    <span className="k">Proposed date</span>
                    <span className="v">{formatDateDMY(status.proposedDate)}</span>
                  </div>
                )}
                {status.arrivalTime && (
                  <div className="review-row">
                    <span className="k">Arrival</span>
                    <span className="v">{formatTime12h(status.arrivalTime)}</span>
                  </div>
                )}
                {status.departureTime && (
                  <div className="review-row">
                    <span className="k">Departure</span>
                    <span className="v">{formatTime12h(status.departureTime)}</span>
                  </div>
                )}
                <div className="review-row">
                  <span className="k">Students</span>
                  <span className="v">{status.currentStudentCount}</span>
                </div>
                <div className="review-row">
                  <span className="k">Lunch</span>
                  <span className="v">
                    {status.lunchRequired ? `Required (${status.mealCount ?? 0} people)` : 'Not required'}
                  </span>
                </div>
              </div>

              {canEdit && (
                <>
                  <h2 style={{ marginTop: '1.5rem' }}>Update headcount</h2>
                  <p className="step-intro">
                    Edit your {isCollege ? 'course' : 'grade'} breakdown below -- you can adjust this up
                    until 2 days before the visit.
                  </p>
                  <form onSubmit={handleRevise}>
                    {!isCollege &&
                      grades.map((row, i) => {
                        const usedByOthers = grades.filter((_, j) => j !== i).map((g) => g.grade);
                        const availableGrades = ALL_GRADES.filter(
                          (g) => g === row.grade || !usedByOthers.includes(g),
                        );
                        return (
                          <div className="teacher-row" key={i}>
                            <div className="field" style={{ marginBottom: 0 }}>
                              <label htmlFor={`sg-grade-${i}`}>Grade</label>
                              <select
                                id={`sg-grade-${i}`}
                                value={row.grade}
                                onChange={(e) => updateGrade(i, { grade: e.target.value })}
                              >
                                <option value="">Choose a grade</option>
                                {availableGrades.map((g) => (
                                  <option key={g} value={g}>
                                    Grade {g}
                                  </option>
                                ))}
                              </select>
                            </div>
                            <div className="field" style={{ marginBottom: 0 }}>
                              <label htmlFor={`sg-count-${i}`}>Students</label>
                              <input
                                id={`sg-count-${i}`}
                                type="number"
                                min={1}
                                value={row.count}
                                onChange={(e) => updateGrade(i, { count: e.target.value })}
                              />
                            </div>
                            {grades.length > 1 && (
                              <button type="button" className="btn-text" onClick={() => removeGrade(i)}>
                                Remove
                              </button>
                            )}
                          </div>
                        );
                      })}
                    {!isCollege && grades.length < 12 && (
                      <button type="button" className="btn-text" onClick={addGrade}>
                        + Add more grade
                      </button>
                    )}

                    {isCollege &&
                      courses.map((row, i) => (
                        <div className="teacher-row" key={i}>
                          <div className="field" style={{ marginBottom: 0 }}>
                            <label htmlFor={`sc-course-${i}`}>Course / branch</label>
                            <input
                              id={`sc-course-${i}`}
                              placeholder="e.g. B.Tech CSE"
                              value={row.courseOrBranch}
                              onChange={(e) => updateCourse(i, { courseOrBranch: e.target.value })}
                            />
                          </div>
                          <div className="field" style={{ marginBottom: 0 }}>
                            <label htmlFor={`sc-year-${i}`}>Year of study</label>
                            <input
                              id={`sc-year-${i}`}
                              placeholder="e.g. 2nd Year"
                              value={row.yearOfStudy}
                              onChange={(e) => updateCourse(i, { yearOfStudy: e.target.value })}
                            />
                          </div>
                          <div className="field" style={{ marginBottom: 0 }}>
                            <label htmlFor={`sc-count-${i}`}>Students</label>
                            <input
                              id={`sc-count-${i}`}
                              type="number"
                              min={1}
                              value={row.count}
                              onChange={(e) => updateCourse(i, { count: e.target.value })}
                            />
                          </div>
                          {courses.length > 1 && (
                            <button type="button" className="btn-text" onClick={() => removeCourse(i)}>
                              Remove
                            </button>
                          )}
                        </div>
                      ))}
                    {isCollege && courses.length < 20 && (
                      <button type="button" className="btn-text" onClick={addCourse}>
                        + Add more course
                      </button>
                    )}

                    <p className="hint" style={{ marginTop: '0.75rem' }}>
                      New total students: <strong>{computeTotal(isCollege, grades, courses)}</strong>
                    </p>

                    <div className="field full" style={{ marginTop: '1rem' }}>
                      <label>
                        <input
                          type="checkbox"
                          checked={lunchRequired}
                          onChange={(e) => {
                            setLunchRequired(e.target.checked);
                            if (!e.target.checked) setMealCount('');
                          }}
                          style={{ marginRight: '0.5rem' }}
                        />
                        Need lunch arrangement (on payment basis)
                      </label>
                    </div>

                    {lunchRequired && (
                      <div className="field-grid">
                        <div className="field">
                          <label htmlFor="mealCount">
                            Meal Head count:{' '}
                            <span className="hint" style={{ fontWeight: 400 }}>
                              (Including teachers and other staff)
                            </span>
                          </label>
                          <input
                            id="mealCount"
                            type="number"
                            min={1}
                            value={mealCount}
                            onChange={(e) => setMealCount(e.target.value)}
                          />
                        </div>
                      </div>
                    )}

                    {reviseError && <div className="submit-error">{reviseError}</div>}
                    {reviseSuccess && <p style={{ color: 'var(--success)' }}>Updated successfully.</p>}
                    <button type="submit" className="btn btn-primary" disabled={revising}>
                      {revising ? 'Saving…' : 'Update headcount'}
                    </button>
                  </form>
                </>
              )}

              {canEdit && (
                <>
                  <h2 style={{ marginTop: '1.5rem' }}>Request a different date</h2>
                  <p className="step-intro">
                    Only available before a decision has been made. The new date must be later than{' '}
                    {formatDateDMY(status.visitDate)}.
                  </p>
                  <form onSubmit={handleReschedule}>
                    <div className="field-grid">
                      <div className="field">
                        <label htmlFor="newDate">New visit date</label>
                        <DateInput id="newDate" value={newDate} onChange={setNewDate} />
                      </div>
                    </div>
                    <div className="field-grid">
                      <div className="field">
                        <label htmlFor="newArrivalTime">Expected arrival time</label>
                        <TimeInput12h id="newArrivalTime" value={newArrivalTime} onChange={setNewArrivalTime} />
                      </div>
                      <div className="field">
                        <label htmlFor="newDepartureTime">Expected departure time</label>
                        <TimeInput12h
                          id="newDepartureTime"
                          value={newDepartureTime}
                          onChange={setNewDepartureTime}
                        />
                      </div>
                    </div>
                    {rescheduleError && <div className="submit-error">{rescheduleError}</div>}
                    {rescheduleSuccess && <p style={{ color: 'var(--success)' }}>Date updated successfully.</p>}
                    <button type="submit" className="btn btn-primary" disabled={rescheduling || !newDate}>
                      {rescheduling ? 'Saving…' : 'Request this date'}
                    </button>
                  </form>
                </>
              )}

              {canCancel && (
                <>
                  <h2 style={{ marginTop: '1.5rem' }}>Cancel your visit</h2>
                  {!confirmingCancel ? (
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setConfirmingCancel(true)}
                    >
                      Cancel your visit
                    </button>
                  ) : (
                    <div>
                      <p className="step-intro">
                        Are you sure? This can't be undone, and you'll need to submit a new request if you
                        change your mind.
                      </p>
                      <div className="field">
                        <label htmlFor="cancelReason">Reason (optional)</label>
                        <textarea
                          id="cancelReason"
                          rows={2}
                          value={cancelReason}
                          onChange={(e) => setCancelReason(e.target.value)}
                          placeholder="Let us know why, if you'd like"
                        />
                      </div>
                      {cancelError && <div className="submit-error">{cancelError}</div>}
                      <div className="row-actions">
                        <button
                          type="button"
                          className="btn btn-primary"
                          disabled={cancelling}
                          onClick={handleCancel}
                        >
                          {cancelling ? 'Cancelling…' : 'Yes, cancel it'}
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => setConfirmingCancel(false)}
                        >
                          Never mind
                        </button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}