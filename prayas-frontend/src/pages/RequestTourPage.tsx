import { useEffect, useState } from 'react';
import { StepTracker } from '../components/StepTracker';
import { SchoolStep } from '../components/steps/SchoolStep';
import { VisitDetailsStep } from '../components/steps/VisitDetailsStep';
import { VenuesStep } from '../components/steps/VenuesStep';
import { LogisticsStep } from '../components/steps/LogisticsStep';
import { ReviewStep } from '../components/steps/ReviewStep';
import { SuccessScreen } from '../components/SuccessScreen';
import { StaffHeader } from '../components/staff/StaffHeader';
import { emptyFormState, toPayload, validateStep, type FormState } from './formState';
import { fetchVenues, submitTourRequest } from '../api/tourApi';
import { extractErrorMessage } from '../api/errors';
import type { TourRequestCreated, Venue } from '../api/types';
import '../styles/form.css';

const STEPS = ['School & Contact', 'Visit Details', 'Venues', 'Logistics', 'Review'];

export function RequestTourPage() {
  const [step, setStep] = useState(0);
  const [form, setForm] = useState<FormState>(emptyFormState);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [venues, setVenues] = useState<Venue[]>([]);
  const [venuesLoading, setVenuesLoading] = useState(true);
  const [venuesError, setVenuesError] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [result, setResult] = useState<TourRequestCreated | null>(null);

  useEffect(() => {
    fetchVenues()
      .then(setVenues)
      .catch(() => setVenuesError(true))
      .finally(() => setVenuesLoading(false));
  }, []);

  const onChange = (patch: Partial<FormState>) => setForm((prev) => ({ ...prev, ...patch }));

  const goNext = () => {
    const stepErrors = validateStep(step, form);
    setErrors(stepErrors);
    if (Object.keys(stepErrors).length === 0) {
      setStep((s) => Math.min(s + 1, STEPS.length - 1));
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const goBack = () => {
    setStep((s) => Math.max(s - 1, 0));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

   const handleSubmit = async () => {
    // TODO: replace with a real hCaptcha widget token before this leaves
    // local dev -- the backend's NoOpCaptchaVerifier (@Profile("local"))
    // accepts any non-blank string, real HCaptchaVerifier will not.
    const captchaToken = 'local-dev-placeholder';

    setSubmitting(true);
    setSubmitError(null);
    try {
      const created = await submitTourRequest(toPayload(form, captchaToken));
      setResult(created);
    } catch (err) {
      setSubmitError(extractErrorMessage(err));
      window.scrollTo({ top: 0, behavior: 'smooth' }); // the message sits at the top of the form
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page page-staff">
      <StaffHeader title="Book a campus tour" />

      {result ? (
        <SuccessScreen result={result} />
      ) : (
        <div className="form-layout">
          <StepTracker steps={STEPS} currentStep={step} />

          <div className="form-panel">
            <div className="form-panel-content">
              {/* Honeypot: real users never see or fill this. */}
              <input
                type="text"
                name="website"
                value={form.website}
                onChange={(e) => onChange({ website: e.target.value })}
                className="honeypot"
                tabIndex={-1}
                autoComplete="off"
                aria-hidden="true"
              />

              {submitError && <div className="submit-error">{submitError}</div>}

              {step === 0 && <SchoolStep form={form} errors={errors} onChange={onChange} />}
              {step === 1 && <VisitDetailsStep form={form} errors={errors} onChange={onChange} />}
              {step === 2 && (
                <VenuesStep
                  form={form}
                  errors={errors}
                  venues={venues}
                  venuesLoading={venuesLoading}
                  venuesError={venuesError}
                  onChange={onChange}
                />
              )}
              {step === 3 && <LogisticsStep form={form} errors={errors} onChange={onChange} />}
              {step === 4 && <ReviewStep form={form} venues={venues} />}

              <div className="button-row">
                {step > 0 ? (
                  <button type="button" className="btn btn-secondary" onClick={goBack} disabled={submitting}>
                    Back
                  </button>
                ) : (
                  <span />
                )}

                {step < STEPS.length - 1 ? (
                  <button type="button" className="btn btn-primary" onClick={goNext}>
                    Continue
                  </button>
                ) : (
                  <button type="button" className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
                    {submitting ? 'Sending…' : 'Send request'}
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}