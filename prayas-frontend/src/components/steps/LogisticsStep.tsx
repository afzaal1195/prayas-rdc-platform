import type { FormState } from '../../pages/formState';

interface Props {
  form: FormState;
  errors: Record<string, string>;
  onChange: (patch: Partial<FormState>) => void;
}

export function LogisticsStep({ form, errors, onChange }: Props) {
  return (
    <>
      <h2>Lunch &amp; logistics</h2>
      <p className="step-intro">A few practical details to help us plan the day.</p>

      <div className="field full">
        <label>
          <input
            type="checkbox"
            checked={form.lunchRequired}
            onChange={(e) => onChange({ lunchRequired: e.target.checked, mealCount: e.target.checked ? form.mealCount : '' })}
            style={{ marginRight: '0.5rem' }}
          />
           Need lunch arrangement (on payment basis)
        </label>
      </div>

      {form.lunchRequired && (
        <div className="field-grid">
          <div className="field">
            <label htmlFor="mealCount">
              Meal Head Count: <span className="hint" style={{ fontWeight: 400 }}>(Including teachers and other staff)</span> *
            </label>
            <input
              id="mealCount"
              type="number"
              min={1}
              value={form.mealCount}
              onChange={(e) => onChange({ mealCount: e.target.value })}
            />
            {errors.mealCount && <span className="field-error">{errors.mealCount}</span>}
          </div>
        </div>
      )}

      <div className="field-grid" style={{ marginTop: form.lunchRequired ? 0 : '0.5rem' }}>
        <div className="field">
          <label htmlFor="vehicleNumber">Vehicle number</label>
          <input
            id="vehicleNumber"
            maxLength={30}
            value={form.vehicleNumber}
            onChange={(e) => onChange({ vehicleNumber: e.target.value })}
          />
        </div>
      </div>
    </>
  );
}