import { Fragment, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import {
  fetchTourRequests,
  submitDecision,
  regenerateTrackingLink,
  fetchProgrammeDetail,
  reopenRequest,
  fetchReadiness,
  resolveDateChange,
} from '../api/staffApi';
import { extractErrorMessage } from '../api/errors';
import type { Me, ProgrammeSummary, ProgrammeStatus, ProgrammeDetail, ReadinessSummary } from '../api/types';
import { formatDateDMY, DateInput } from '../components/DateInput';
import { AssignmentPanel } from '../components/staff/AssignmentPanel';
import { ReadinessChip } from '../components/staff/ReadinessChip';
import { StaffLayout } from '../components/staff/StaffLayout';
import { StaffLogin } from '../components/staff/StaffLogin';
import { formatTime12h } from '../components/TimeInput12h';
import { useStaffSession } from '../hooks/useStaffSession';
import '../styles/form.css';
import '../styles/dashboard.css';

const DECIDABLE_STATUSES: ProgrammeStatus[] = ['SUBMITTED', 'UNDER_REVIEW', 'RESCHEDULE_PROPOSED'];

const ALL_STATUSES: ProgrammeStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'RESCHEDULE_PROPOSED',
  'APPROVED',
  'REJECTED',
  'CANCELLED',
  'COMPLETED',
];

const REJECT_REASON_PRESETS = ['Slots full for this date', 'Please choose a different date'];

function statusClass(status: ProgrammeStatus): string {
  if (status === 'APPROVED' || status === 'COMPLETED') return 'approved';
  if (status === 'REJECTED' || status === 'CANCELLED') return 'rejected';
  if (DECIDABLE_STATUSES.includes(status)) return 'pending';
  return 'neutral';
}

export function StaffDashboardPage() {
  const session = useStaffSession();

  if (session.checking) {
    return null; // avoid a login-page flash while we check the session
  }
  if (!session.me) {
    return <StaffLogin authError={session.authError} />;
  }
  const me = session.me;

  // People who don't review requests (most volunteers) start on their own shifts instead.
  if (!me.canViewTours) {
    return <Navigate to="/staff/my-tours" replace />;
  }

  return (
    <StaffLayout me={me} active="requests" title="DASHBOARD">
      <RequestsPanel me={me} />
    </StaffLayout>
  );
}

function RequestsPanel({ me }: { me: Me }) {
  const [requests, setRequests] = useState<ProgrammeSummary[]>([]);
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [actingOnId, setActingOnId] = useState<number | null>(null);

  // Rejecting asks for a reason first, rather than acting immediately.
  const [rejectingId, setRejectingId] = useState<number | null>(null);
  const [rejectPreset, setRejectPreset] = useState('');
  const [rejectNote, setRejectNote] = useState('');

  // Reopening a rejected request also asks for a reason (it goes in the audit log).
  const [reopeningId, setReopeningId] = useState<number | null>(null);
  const [reopenNote, setReopenNote] = useState('');
  // Optional: offer the school a new date; the request then reopens only once they accept it.
  const [offerNewDate, setOfferNewDate] = useState(false);
  const [reopenDate, setReopenDate] = useState('');
  const [reopenError, setReopenError] = useState<string | null>(null);

  // Filters -- client-side, over the already-fetched list.
  const [filterDate, setFilterDate] = useState('');
  const [filterStatus, setFilterStatus] = useState<ProgrammeStatus | ''>('');

  // Tracking-link regeneration, per row.
  const [linkGeneratingId, setLinkGeneratingId] = useState<number | null>(null);
  const [generatedLinks, setGeneratedLinks] = useState<Record<number, string>>({});
  const [linkError, setLinkError] = useState<string | null>(null);
  const [copiedLinkId, setCopiedLinkId] = useState<number | null>(null);

  // Expandable row detail -- fetched lazily and cached per row, since the
  // summary list deliberately doesn't carry every field. Any number of rows
  // can be open at once; each one is closed only by clicking it again.
  const [expandedIds, setExpandedIds] = useState<number[]>([]);
  const [detailCache, setDetailCache] = useState<Record<number, ProgrammeDetail>>({});
  const [detailLoadingIds, setDetailLoadingIds] = useState<number[]>([]);
  const [detailErrors, setDetailErrors] = useState<Record<number, string>>({});

  // A school's request for a different date: staff answer it before anything changes.
  const [dateChangeBusyId, setDateChangeBusyId] = useState<number | null>(null);
  const [dateChangeError, setDateChangeError] = useState<string | null>(null);
  const [decliningChangeId, setDecliningChangeId] = useState<number | null>(null);
  const [declineChangeNote, setDeclineChangeNote] = useState('');

  // How prepared each approved tour is (only for people who plan itineraries).
  const [readiness, setReadiness] = useState<Record<number, ReadinessSummary>>({});

  const loadRequests = () => {
    if (me.canPlanItineraries) {
      fetchReadiness()
        .then((rows) => setReadiness(Object.fromEntries(rows.map((r) => [r.programmeId, r.summary]))))
        .catch(() => setReadiness({}));
    }
    setListLoading(true);
    setListError(null);
    fetchTourRequests()
      .then(setRequests)
      .catch(() => setListError("Couldn't load requests. Try reloading the page."))
      .finally(() => setListLoading(false));
  };

  useEffect(() => {
    loadRequests();
  }, []);

  const act = async (id: number, decision: 'APPROVE' | 'REJECT', note?: string) => {
    setActingOnId(id);
    try {
      await submitDecision(id, { decision, note });
      loadRequests();
    } catch (err) {
      alert(extractErrorMessage(err));
      console.error(err);
    } finally {
      setActingOnId(null);
    }
  };

  const answerDateChange = async (id: number, decision: 'APPROVE' | 'DECLINE', note?: string) => {
    setDateChangeBusyId(id);
    setDateChangeError(null);
    try {
      await resolveDateChange(id, decision, note);
      setDecliningChangeId(null);
      setDeclineChangeNote('');
      loadRequests();
    } catch (err) {
      setDateChangeError(extractErrorMessage(err));
    } finally {
      setDateChangeBusyId(null);
    }
  };

  const startReopen = (id: number) => {
    setReopeningId(id);
    setReopenNote('');
    setReopenError(null);
  };

  const cancelReopen = () => {
    setReopeningId(null);
    setReopenNote('');
    setOfferNewDate(false);
    setReopenDate('');
    setReopenError(null);
  };

  const confirmReopen = async (id: number) => {
    setActingOnId(id);
    setReopenError(null);
    try {
      await reopenRequest(id, reopenNote.trim(), offerNewDate ? reopenDate : undefined);
      cancelReopen();
      loadRequests();
    } catch (err) {
      setReopenError(extractErrorMessage(err));
    } finally {
      setActingOnId(null);
    }
  };

  const startReject = (id: number) => {
    setRejectingId(id);
    setRejectPreset('');
    setRejectNote('');
  };

  const cancelReject = () => {
    setRejectingId(null);
    setRejectPreset('');
    setRejectNote('');
  };

  const choosePreset = (preset: string) => {
    setRejectPreset(preset);
    setRejectNote(preset !== 'other' ? preset : '');
  };

  const confirmReject = async (id: number) => {
    await act(id, 'REJECT', rejectNote.trim());
    cancelReject();
  };

  const getLink = async (id: number) => {
    setLinkGeneratingId(id);
    setLinkError(null);
    try {
      const token = await regenerateTrackingLink(id);
      setGeneratedLinks((prev) => ({ ...prev, [id]: token }));
    } catch (err) {
      setLinkError("Couldn't generate a link. Please try again.");
      console.error(err);
    } finally {
      setLinkGeneratingId(null);
    }
  };

  const copyLink = async (id: number, token: string) => {
    const url = `${window.location.origin}/status/${token}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopiedLinkId(id);
      setTimeout(() => setCopiedLinkId(null), 2000);
    } catch {
      // link text is still visible for manual copy
    }
  };

  const toggleExpand = (id: number) => {
    if (expandedIds.includes(id)) {
      setExpandedIds((prev) => prev.filter((x) => x !== id));
      return;
    }
    setExpandedIds((prev) => [...prev, id]);
    if (!detailCache[id]) {
      setDetailLoadingIds((prev) => [...prev, id]);
      setDetailErrors((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
      fetchProgrammeDetail(id)
        .then((d) => setDetailCache((prev) => ({ ...prev, [id]: d })))
        .catch(() => setDetailErrors((prev) => ({ ...prev, [id]: "Couldn't load details for that row." })))
        .finally(() => setDetailLoadingIds((prev) => prev.filter((x) => x !== id)));
    }
  };

  const pendingDateChanges = requests.filter((r) => r.requestedVisitDate);

  const filteredRequests = useMemo(() => {
    return requests.filter((r) => {
      if (filterStatus && r.status !== filterStatus) return false;
      if (filterDate && r.visitDate !== filterDate) return false;
      return true;
    });
  }, [requests, filterDate, filterStatus]);

  return (
      <div className="dashboard-table-wrap">
      {pendingDateChanges.length > 0 && (
        <div className="date-change-banner" role="alert">
          <strong>
            {pendingDateChanges.length === 1
              ? '1 school has asked for a different visit date'
              : `${pendingDateChanges.length} schools have asked for a different visit date`}
          </strong>
          <p className="date-change-hint">
            The date does not change until you approve it.
          </p>
          {dateChangeError && <p className="field-error">{dateChangeError}</p>}
          <ul>
            {pendingDateChanges.map((r) => (
              <li key={r.id}>
                <div className="date-change-line">
                  <span>
                    #{r.id} {r.institutionName}: {formatDateDMY(r.visitDate)}{' '}
                    <span aria-hidden="true">→</span> <strong>{formatDateDMY(r.requestedVisitDate as string)}</strong>
                    {r.requestedArrivalTime && r.requestedDepartureTime
                      ? ` (${formatTime12h(r.requestedArrivalTime)} to ${formatTime12h(r.requestedDepartureTime)})`
                      : ''}
                  </span>
                  {me.canDecide && decliningChangeId !== r.id && (
                    <span className="row-actions">
                      <button
                        className="btn btn-primary"
                        disabled={dateChangeBusyId === r.id}
                        onClick={() => answerDateChange(r.id, 'APPROVE')}
                      >
                        Approve change
                      </button>
                      <button
                        className="btn btn-secondary"
                        disabled={dateChangeBusyId === r.id}
                        onClick={() => {
                          setDecliningChangeId(r.id);
                          setDeclineChangeNote('');
                        }}
                      >
                        Decline
                      </button>
                    </span>
                  )}
                </div>
                {r.status === 'APPROVED' && (
                  <p className="date-change-warning">
                    This tour is already approved. Approving the change moves it to the new date, frees the old
                    date, and removes the people assigned to it, so they will need assigning again.
                  </p>
                )}
                {me.canDecide && decliningChangeId === r.id && (
                  <div className="date-change-decline">
                    <input
                      type="text"
                      value={declineChangeNote}
                      onChange={(e) => setDeclineChangeNote(e.target.value)}
                      placeholder="Reason shown to the school (optional)"
                      maxLength={500}
                    />
                    <button
                      className="btn btn-primary"
                      disabled={dateChangeBusyId === r.id}
                      onClick={() => answerDateChange(r.id, 'DECLINE', declineChangeNote)}
                    >
                      Confirm decline
                    </button>
                    <button className="btn btn-secondary" onClick={() => setDecliningChangeId(null)}>
                      Cancel
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="dashboard-filters">
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="filterStatus">Status</label>
          <select
            id="filterStatus"
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value as ProgrammeStatus | '')}
          >
            <option value="">All statuses</option>
            {ALL_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="filterDate">Visit date</label>
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <DateInput id="filterDate" value={filterDate} onChange={setFilterDate} />
            {filterDate && (
              <button type="button" className="btn-reset" onClick={() => setFilterDate('')}>
                Reset Date
              </button>
            )}
          </div>
        </div>
        {(filterStatus || filterDate) && (
          <button
            type="button"
            className="btn-reset"
            onClick={() => {
              setFilterStatus('');
              setFilterDate('');
            }}
          >
            Clear filters
          </button>
        )}
      </div>

      {listLoading && <p>Loading requests…</p>}
      {listError && <p className="field-error">{listError}</p>}
      {linkError && <p className="field-error">{linkError}</p>}

      {!listLoading && !listError && filteredRequests.length === 0 && (
        <div className="empty-state">
          {requests.length === 0 ? 'No tour requests yet.' : 'No requests match the current filters.'}
        </div>
      )}

      {!listLoading && !listError && filteredRequests.length > 0 && (
        <table className="request-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Institution</th>
              <th>Location</th>
              <th>Type</th>
              <th>Visit date</th>
              <th>Arrival</th>
              <th>Departure</th>
              <th>Students</th>
              <th>Contact</th>
              <th>Status</th>
              <th>Actions</th>
              <th>Link</th>
            </tr>
          </thead>
          <tbody>
            {filteredRequests.map((r) => (
              <Fragment key={r.id}>
                <tr
                  className="clickable-row"
                  onClick={() => toggleExpand(r.id)}
                  aria-expanded={expandedIds.includes(r.id)}
                >
                <td>{r.id}</td>
                <td>{r.institutionName}</td>
                <td>{[r.villageOrTown, r.district].filter(Boolean).join(', ') || '—'}</td>
                <td>{r.institutionType === 'COLLEGE' ? 'College' : 'School'}</td>
                <td>
                  {formatDateDMY(r.visitDate)}
                  {r.status === 'RESCHEDULE_PROPOSED' && r.proposedDate && (
                    <div className="date-change-flag">
                      → {formatDateDMY(r.proposedDate)} offered, waiting for the school
                    </div>
                  )}
                  {r.requestedVisitDate && (
                    <div className="date-change-flag">→ {formatDateDMY(r.requestedVisitDate)} requested</div>
                  )}
                </td>
                <td>{formatTime12h(r.arrivalTime) || '—'}</td>
                <td>{formatTime12h(r.departureTime) || '—'}</td>
                <td>{r.studentCount}</td>
                <td>
                  {r.contactName}
                  {r.contactPhone ? ` · ${r.contactPhone}` : ''}
                </td>
                <td>
                  <span className={`status-badge ${statusClass(r.status)}`}>{r.status}</span>
                  {r.status === 'APPROVED' && readiness[r.id] && (
                    <div style={{ marginTop: '0.35rem' }}>
                      <ReadinessChip summary={readiness[r.id]} />
                    </div>
                  )}
                </td>
                <td onClick={(e) => e.stopPropagation()}>
                  {rejectingId === r.id ? (
                    <div className="reject-form">
                      <select value={rejectPreset} onChange={(e) => choosePreset(e.target.value)}>
                        <option value="">Choose a reason…</option>
                        {REJECT_REASON_PRESETS.map((p) => (
                          <option key={p} value={p}>
                            {p}
                          </option>
                        ))}
                        <option value="other">Other (specify below)</option>
                      </select>
                      <textarea
                        value={rejectNote}
                        onChange={(e) => setRejectNote(e.target.value)}
                        placeholder="Reason shown to the school"
                        rows={2}
                      />
                      <div className="row-actions">
                        <button
                          className="btn btn-primary"
                          disabled={!rejectNote.trim() || actingOnId === r.id}
                          onClick={() => confirmReject(r.id)}
                        >
                          Confirm reject
                        </button>
                        <button className="btn btn-secondary" onClick={cancelReject}>
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : reopeningId === r.id ? (
                    <div className="reject-form">
                      <textarea
                        value={reopenNote}
                        onChange={(e) => setReopenNote(e.target.value)}
                        placeholder={
                          offerNewDate
                            ? 'Why this new date? (shown to the school)'
                            : 'Why is this being reopened? (kept in the history)'
                        }
                        rows={2}
                      />
                      <label className="check-row">
                        <input
                          type="checkbox"
                          checked={offerNewDate}
                          onChange={(e) => setOfferNewDate(e.target.checked)}
                        />
                        Ask the school to accept a new date first
                      </label>
                      {offerNewDate && (
                        <div className="field" style={{ marginBottom: 0 }}>
                          <label htmlFor={`reopen-date-${r.id}`}>Proposed date</label>
                          <DateInput id={`reopen-date-${r.id}`} value={reopenDate} onChange={setReopenDate} />
                          <span className="hint">
                            The request reopens only when the school accepts this date on their tracking link.
                          </span>
                        </div>
                      )}
                      {reopenError && <span className="field-error">{reopenError}</span>}
                      <div className="row-actions">
                        <button
                          className="btn btn-primary"
                          disabled={!reopenNote.trim() || (offerNewDate && !reopenDate) || actingOnId === r.id}
                          onClick={() => confirmReopen(r.id)}
                        >
                          {offerNewDate ? 'Send date to school' : 'Confirm reopen'}
                        </button>
                        <button className="btn btn-secondary" onClick={cancelReopen}>
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : r.status === 'REJECTED' && me.canReopen ? (
                    <button className="btn btn-secondary" onClick={() => startReopen(r.id)}>
                      Reopen
                    </button>
                  ) : me.canDecide && DECIDABLE_STATUSES.includes(r.status) ? (
                    <div className="row-actions">
                      <button
                        className="btn btn-primary"
                        disabled={actingOnId === r.id || !!r.requestedVisitDate}
                        title={r.requestedVisitDate ? 'Answer the school’s date change request first' : undefined}
                        onClick={() => act(r.id, 'APPROVE')}
                      >
                        Approve
                      </button>
                      <button
                        className="btn btn-secondary"
                        disabled={actingOnId === r.id}
                        onClick={() => startReject(r.id)}
                      >
                        Reject
                      </button>
                    </div>
                  ) : (
                    <span className="hint">—</span>
                  )}
                </td>
                <td onClick={(e) => e.stopPropagation()}>
                  {generatedLinks[r.id] ? (
                    <button className="btn-text" onClick={() => copyLink(r.id, generatedLinks[r.id])}>
                      {copiedLinkId === r.id ? 'Copied!' : 'Copy link'}
                    </button>
                  ) : (
                    <button
                      className="btn-text"
                      disabled={linkGeneratingId === r.id}
                      onClick={() => getLink(r.id)}
                    >
                      {linkGeneratingId === r.id ? 'Generating…' : 'Get link'}
                    </button>
                  )}
                </td>
                </tr>
                <tr className="detail-row-wrap">
                  <td colSpan={12} style={{ padding: 0, border: 'none' }}>
                    <div className={`row-detail ${expandedIds.includes(r.id) ? 'open' : ''}`}>
                      <div className="row-detail-inner">
                        {detailLoadingIds.includes(r.id) && <p>Loading details…</p>}
                        {detailErrors[r.id] && expandedIds.includes(r.id) && (
                          <p className="field-error">{detailErrors[r.id]}</p>
                        )}
                        {detailCache[r.id] && expandedIds.includes(r.id) && (
                          <div className="row-detail-grid">
                            <div className="review-row">
                              <span className="k">Address</span>
                              <span className="v">
                                {[detailCache[r.id].address, detailCache[r.id].state].filter(Boolean).join(', ') ||
                                  '—'}
                              </span>
                            </div>
                            <div className="review-row">
                              <span className="k">Email</span>
                              <span className="v">{detailCache[r.id].contactEmail || '—'}</span>
                            </div>
                            <div className="review-row">
                              <span className="k">Breakdown</span>
                              <span className="v">{detailCache[r.id].gradeRange || '—'}</span>
                            </div>
                            <div className="review-row">
                              <span className="k">Teachers ({detailCache[r.id].teacherCount})</span>
                              <span className="v">
                                {detailCache[r.id].teachers.map((t) => `${t.name} (${t.phone})`).join(', ') ||
                                  '—'}
                              </span>
                            </div>
                            <div className="review-row">
                              <span className="k">Lunch</span>
                              <span className="v">
                                {detailCache[r.id].lunchRequired
                                  ? `Required (${detailCache[r.id].mealCount ?? 0} people)`
                                  : 'Not required'}
                              </span>
                            </div>
                            <div className="review-row">
                              <span className="k">Vehicle</span>
                              <span className="v">{detailCache[r.id].vehicleNumber || '—'}</span>
                            </div>
                            <div className="review-row">
                              <span className="k">Interest notes</span>
                              <span className="v">{detailCache[r.id].interestNotes || '—'}</span>
                            </div>
                            {detailCache[r.id].decisionNote && (
                              <div className="review-row">
                                <span className="k">Decision note</span>
                                <span className="v">{detailCache[r.id].decisionNote}</span>
                              </div>
                            )}
                          </div>
                        )}
                        {expandedIds.includes(r.id) && r.status === 'APPROVED' && me.canAssign && (
                          <AssignmentPanel
                            key={`${r.id}-${r.visitDate}`}
                            programmeId={r.id}
                            arrivalTime={r.arrivalTime}
                            departureTime={r.departureTime}
                          />
                        )}
                      </div>
                    </div>
                  </td>
                </tr>
              </Fragment>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}