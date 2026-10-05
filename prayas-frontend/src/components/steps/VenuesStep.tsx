import type { FormState } from '../../pages/formState';
import type { Venue } from '../../api/types';

interface Props {
  form: FormState;
  errors: Record<string, string>;
  venues: Venue[];
  venuesLoading: boolean;
  venuesError: boolean;
  onChange: (patch: Partial<FormState>) => void;
}

export function VenuesStep({ form, errors, venues, venuesLoading, venuesError, onChange }: Props) {
  const toggleVenue = (id: number) => {
    const venueIds = form.venueIds.includes(id)
      ? form.venueIds.filter((v) => v !== id)
      : [...form.venueIds, id];
    onChange({ venueIds });
  };

  return (
    <>
      <h2>Places to visit</h2>
      <p className="step-intro">Pick the venues your group would like to see on campus.</p>

      {venuesLoading && <p>Loading venues…</p>}
      {venuesError && (
        <p className="field-error">
          Couldn't load the venue list. Check your connection and reload the page.
        </p>
      )}

      {!venuesLoading && !venuesError && (
        <div className="venue-grid">
          {venues.map((venue) => (
            <button
              type="button"
              key={venue.id}
              className={`venue-card ${form.venueIds.includes(venue.id) ? 'selected' : ''}`}
              onClick={() => toggleVenue(venue.id)}
              aria-pressed={form.venueIds.includes(venue.id)}
            >
              <h3>{venue.name}</h3>
              {venue.department && <p>{venue.department}</p>}
            </button>
          ))}
        </div>
      )}
      {errors.venueIds && <span className="field-error">{errors.venueIds}</span>}

      <div className="field full" style={{ marginTop: '1rem' }}>
        <label htmlFor="interestNotes">Anything specific your group is interested in?</label>
        <textarea
          id="interestNotes"
          placeholder="e.g. Interested in computer science labs and robotics"
          value={form.interestNotes}
          onChange={(e) => onChange({ interestNotes: e.target.value })}
        />
      </div>
    </>
  );
}
