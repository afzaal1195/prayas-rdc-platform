import type { FormState } from '../../pages/formState';

interface Props {
  form: FormState;
  errors: Record<string, string>;
  onChange: (patch: Partial<FormState>) => void;
}

export function SchoolStep({ form, errors, onChange }: Props) {
  const isCollege = form.institutionType === 'COLLEGE';

  return (
    <>
      <h2>Are you a school or a college?</h2>
      <p className="step-intro">This changes a couple of the questions we ask next.</p>

      <div className="field full" role="radiogroup" aria-label="Institution type">
        <div style={{ display: 'flex', gap: '1rem' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 400 }}>
            <input
              type="radio"
              name="institutionType"
              checked={form.institutionType === 'SCHOOL'}
              onChange={() => onChange({ institutionType: 'SCHOOL' })}
            />
            School
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 400 }}>
            <input
              type="radio"
              name="institutionType"
              checked={isCollege}
              onChange={() => onChange({ institutionType: 'COLLEGE' })}
            />
            College
          </label>
        </div>
      </div>

      <h2 style={{ marginTop: '1.5rem' }}>{isCollege ? 'Institution' : 'School'} &amp; contact details</h2>
      <p className="step-intro">Tell us who's visiting and who we should reach for the arrangements.</p>

      <div className="field field-grid">
        <div className="field full">
          <label htmlFor="schoolName">{isCollege ? 'Institution' : 'School'} name *</label>
          <input
            id="schoolName"
            maxLength={200}
            value={form.schoolName}
            onChange={(e) => onChange({ schoolName: e.target.value })}
          />
          {errors.schoolName && <span className="field-error">{errors.schoolName}</span>}
        </div>
      </div>

      <div className="field-grid">
        <div className="field">
          <label htmlFor="villageOrTown">Village / town *</label>
          <input
            id="villageOrTown"
            maxLength={120}
            value={form.villageOrTown}
            onChange={(e) => onChange({ villageOrTown: e.target.value })}
          />
          {errors.villageOrTown && <span className="field-error">{errors.villageOrTown}</span>}
        </div>
        <div className="field">
          <label htmlFor="district">District *</label>
          <input
            id="district"
            maxLength={120}
            value={form.district}
            onChange={(e) => onChange({ district: e.target.value })}
          />
          {errors.district && <span className="field-error">{errors.district}</span>}
        </div>
        <div className="field">
          <label htmlFor="state">State *</label>
          <input
            id="state"
            maxLength={80}
            value={form.state}
            onChange={(e) => onChange({ state: e.target.value })}
          />
          {errors.state && <span className="field-error">{errors.state}</span>}
        </div>
        <div className="field">
          <label htmlFor="address">Full address *</label>
          <input
            id="address"
            maxLength={500}
            value={form.address}
            onChange={(e) => onChange({ address: e.target.value })}
          />
          {errors.address && <span className="field-error">{errors.address}</span>}
        </div>
      </div>

      <div className="field-grid" style={{ marginTop: '0.5rem' }}>
        <div className="field">
          <label htmlFor="contactName">Contact person's name *</label>
          <input
            id="contactName"
            maxLength={150}
            value={form.contactName}
            onChange={(e) => onChange({ contactName: e.target.value })}
          />
          {errors.contactName && <span className="field-error">{errors.contactName}</span>}
        </div>
        <div className="field">
          <label htmlFor="contactPhone">Contact phone *</label>
          <input
            id="contactPhone"
            maxLength={20}
            value={form.contactPhone}
            onChange={(e) => onChange({ contactPhone: e.target.value })}
          />
          {errors.contactPhone && <span className="field-error">{errors.contactPhone}</span>}
        </div>
        <div className="field">
          <label htmlFor="contactEmail">Contact email *</label>
          <input
            id="contactEmail"
            type="email"
            value={form.contactEmail}
            onChange={(e) => onChange({ contactEmail: e.target.value })}
          />
          {errors.contactEmail && <span className="field-error">{errors.contactEmail}</span>}
        </div>
      </div>
    </>
  );
}