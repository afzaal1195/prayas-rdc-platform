import { useEffect, useState } from 'react';

// Native <input type="time"> renders in whatever format the browser/OS
// locale dictates (often 24-hour), which isn't reliably 12-hour/AM-PM
// across users. This builds that exact format ourselves instead, storing
// the underlying value as a 24-hour "HH:mm" string so nothing else in the
// app (validation, payload, display) needs to change.
//
// Hour/minute/period are tracked in local state (not purely derived from
// the `value` prop) so a partial selection -- e.g. hour picked, minute not
// yet -- is remembered and shown while the user keeps picking. The parent
// is only notified via onChange once hour and minute are both filled.

interface Props {
  id: string;
  value: string; // 24-hour "HH:mm", or "" if unset
  onChange: (value24h: string) => void;
}

const HOURS = Array.from({ length: 12 }, (_, i) => i + 1); // 1..12
const MINUTES = ['00', '15', '30', '45'];

function to12h(value24h: string): { hour: string; minute: string; period: 'AM' | 'PM' } | null {
  if (!value24h) return null;
  const [h, m] = value24h.split(':').map(Number);
  const period: 'AM' | 'PM' = h >= 12 ? 'PM' : 'AM';
  let hour12 = h % 12;
  if (hour12 === 0) hour12 = 12;
  return { hour: String(hour12), minute: String(m).padStart(2, '0'), period };
}

function to24h(hour: string, minute: string, period: 'AM' | 'PM'): string {
  let h = Number(hour) % 12;
  if (period === 'PM') h += 12;
  return `${String(h).padStart(2, '0')}:${minute}`;
}

export function TimeInput12h({ id, value, onChange }: Props) {
  const initial = to12h(value);
  const [hour, setHour] = useState(initial?.hour ?? '');
  const [minute, setMinute] = useState(initial?.minute ?? '');
  const [period, setPeriod] = useState<'AM' | 'PM'>(initial?.period ?? 'AM');

  // Stay in sync if the parent resets/changes the value from outside.
  useEffect(() => {
    const p = to12h(value);
    setHour(p?.hour ?? '');
    setMinute(p?.minute ?? '');
    setPeriod(p?.period ?? 'AM');
  }, [value]);

  const commit = (nextHour: string, nextMinute: string, nextPeriod: 'AM' | 'PM') => {
    if (nextHour && nextMinute) {
      onChange(to24h(nextHour, nextMinute, nextPeriod));
    }
  };

  return (
    <div id={id} style={{ display: 'flex', gap: '0.4rem' }}>
      <select
        aria-label="Hour"
        value={hour}
        onChange={(e) => {
          setHour(e.target.value);
          commit(e.target.value, minute, period);
        }}
        style={{ flex: '1 1 0' }}
      >
        <option value="">--</option>
        {HOURS.map((h) => (
          <option key={h} value={h}>
            {h}
          </option>
        ))}
      </select>
      <select
        aria-label="Minute"
        value={minute}
        onChange={(e) => {
          setMinute(e.target.value);
          commit(hour, e.target.value, period);
        }}
        style={{ flex: '1 1 0' }}
      >
        <option value="">--</option>
        {MINUTES.map((m) => (
          <option key={m} value={m}>
            {m}
          </option>
        ))}
      </select>
      <select
        aria-label="AM or PM"
        value={period}
        onChange={(e) => {
          const next = e.target.value as 'AM' | 'PM';
          setPeriod(next);
          commit(hour, minute, next);
        }}
        style={{ flex: '1 1 0' }}
      >
        <option value="AM">AM</option>
        <option value="PM">PM</option>
      </select>
    </div>
  );
}

/** For displaying an already-stored 24-hour "HH:mm" (or "HH:mm:ss") string as e.g. "2:30 PM". */
export function formatTime12h(value24h: string | null | undefined): string {
  if (!value24h) return '';
  const trimmed = value24h.slice(0, 5); // tolerate "HH:mm:ss" from the backend
  const parsed = to12h(trimmed);
  if (!parsed) return '';
  return `${parsed.hour}:${parsed.minute} ${parsed.period}`;
}