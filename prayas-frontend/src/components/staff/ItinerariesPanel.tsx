import { useEffect, useState } from 'react';
import { addItineraryStop, fetchItineraries, removeItineraryStop } from '../../api/staffApi';
import { extractErrorMessage } from '../../api/errors';
import type { ItineraryBoard, TourItinerary, VenueOption } from '../../api/types';
import { formatDateDMY } from '../DateInput';
import { TimeInput12h, formatTime12h } from '../TimeInput12h';

/** The time of day a backend "HH:mm:ss" value shows as, e.g. "10:30 AM". */
function show(time: string): string {
  return formatTime12h(time.slice(0, 5));
}

function AddStopForm({ tour, venues, onAdded }: { tour: TourItinerary; venues: VenueOption[]; onAdded: () => void }) {
  const [venueId, setVenueId] = useState('');
  const [title, setTitle] = useState('');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const id = (name: string) => `${name}-${tour.programmeId}`;
  const canSubmit = (venueId || title.trim()) && start && end && !busy;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await addItineraryStop(tour.programmeId, {
        venueId: venueId ? Number(venueId) : undefined,
        title: title.trim() || undefined,
        startTime: start,
        endTime: end,
        notes: notes.trim() || undefined,
      });
      setVenueId('');
      setTitle('');
      setStart('');
      setEnd('');
      setNotes('');
      onAdded();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="itinerary-form" onSubmit={submit}>
      <div className="field">
        <label htmlFor={id('venue')}>Venue</label>
        <select id={id('venue')} value={venueId} onChange={(e) => setVenueId(e.target.value)}>
          <option value="">Not a venue (e.g. welcome talk, lunch)</option>
          {venues.map((v) => (
            <option key={v.id} value={v.id}>
              {v.name}
            </option>
          ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor={id('title')}>Title {venueId ? '(optional)' : ''}</label>
        <input
          id={id('title')}
          type="text"
          maxLength={150}
          value={title}
          placeholder={venueId ? 'Defaults to the venue name' : 'What happens here'}
          onChange={(e) => setTitle(e.target.value)}
        />
      </div>
      <div className="itinerary-times">
        <div className="field">
          <label htmlFor={id('start')}>From</label>
          <TimeInput12h id={id('start')} value={start} onChange={setStart} />
        </div>
        <div className="field">
          <label htmlFor={id('end')}>To</label>
          <TimeInput12h id={id('end')} value={end} onChange={setEnd} />
        </div>
      </div>
      <div className="field">
        <label htmlFor={id('notes')}>Notes (optional)</label>
        <input
          id={id('notes')}
          type="text"
          maxLength={500}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
        />
      </div>
      {error && <p className="field-error">{error}</p>}
      <button type="submit" className="btn btn-primary" disabled={!canSubmit}>
        {busy ? 'Adding…' : 'Add stop'}
      </button>
    </form>
  );
}

function TourCard({ tour, venues, onChanged }: { tour: TourItinerary; venues: VenueOption[]; onChanged: () => void }) {
  const [error, setError] = useState<string | null>(null);
  const [removingId, setRemovingId] = useState<number | null>(null);

  const remove = async (stopId: number) => {
    setRemovingId(stopId);
    setError(null);
    try {
      await removeItineraryStop(stopId);
      onChanged();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <section className="school-card">
      <header className="school-card-head">
        <h3 className="school-card-title">{tour.schoolName}</h3>
        <span className="school-card-date">{formatDateDMY(tour.visitDate)}</span>
        <span className="school-card-date">
          Visit runs {show(tour.windowStart)} to {show(tour.windowEnd)}
        </span>
      </header>
      <ul className="school-card-tasks">
        {tour.stops.length === 0 && (
          <li className="approval-step">
            <span className="date-change-hint">No stops yet. Add the first one below.</span>
          </li>
        )}
        {tour.stops.map((s) => (
          <li key={s.id} className="approval-step">
            <div className="approval-step-head">
              <span className="approval-step-title">
                {show(s.startTime)} – {show(s.endTime)}
                <span className="approval-tour-date">{s.title}</span>
              </span>
              <button
                type="button"
                className="btn-text"
                disabled={removingId === s.id}
                onClick={() => remove(s.id)}
              >
                Remove
              </button>
            </div>
            {s.venueName && s.venueName !== s.title && <div className="date-change-hint">At {s.venueName}</div>}
            {s.notes && <div className="date-change-hint">{s.notes}</div>}
            {s.problems.map((p) => (
              <div key={p} className="field-error">
                {p}
              </div>
            ))}
          </li>
        ))}
      </ul>
      {error && <p className="field-error itinerary-error">{error}</p>}
      <div className="itinerary-add">
        <AddStopForm tour={tour} venues={venues} onAdded={onChanged} />
      </div>
    </section>
  );
}

/** Upcoming approved tours, each with its stops in time order and a form to add another. */
export function ItinerariesPanel() {
  const [board, setBoard] = useState<ItineraryBoard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    fetchItineraries()
      .then(setBoard)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  return (
    <div className="dashboard-table-wrap">
      {loading && <p>Loading…</p>}
      {error && <p className="field-error">{error}</p>}

      {board && board.tours.length === 0 && (
        <div className="empty-state">No upcoming approved tours yet. Itineraries are planned once a tour is approved.</div>
      )}

      {board?.tours.map((t) => (
        <TourCard key={t.programmeId} tour={t} venues={board.venues} onChanged={load} />
      ))}
    </div>
  );
}
