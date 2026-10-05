interface StepTrackerProps {
  steps: string[];
  currentStep: number; // 0-indexed
}

export function StepTracker({ steps, currentStep }: StepTrackerProps) {
  return (
    <nav className="step-tracker" aria-label="Form progress">
      {steps.map((label, i) => {
        const status = i < currentStep ? 'done' : i === currentStep ? 'active' : '';
        return (
          <div key={label} className={`step-tracker-item ${status}`}>
            <div className="step-number" aria-hidden="true">
              {i < currentStep ? '✓' : i + 1}
            </div>
            <div className="step-label">
              <div className="name">{label}</div>
            </div>
          </div>
        );
      })}
    </nav>
  );
}
