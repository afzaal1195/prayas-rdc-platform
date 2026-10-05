import { useEffect, useState } from 'react';

// Same rationale as TimeInput12h: native <input type="date"> displays in
// whatever format the browser/OS locale dictates, not reliably dd-mm-yyyy.
// This builds that exact format ourselves, storing the underlying value as
// an ISO "yyyy-mm-dd" string so nothing else in the app (validation,
// payload, min/max comparisons) needs to change.
//
// Day/month/year are tracked in local state (not purely derived from the
// `value` prop) so a partial selection -- e.g. day picked, month and year
// not yet -- is remembered and shown while the user keeps picking. The
// parent is only notified via onChange once all three are filled.

interface Props {
  id: string;
  value: string; // ISO "yyyy-mm-dd", or "" if unset
  onChange: (isoValue: string) => void;
}

const MONTHS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

function parse(iso: string): { day: string; month: string; year: string } | null {
  if (!iso) return null;
  const [y, m, d] = iso.split('-');
  if (!y || !m || !d) return null;
  return { day: String(Number(d)), month: String(Number(m)), year: y };
}

function toIso(day: string, month: string, year: string): string {
  return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
}

function daysInMonth(month: string, year: string): number {
  if (!month || !year) return 31;
  return new Date(Number(year), Number(month), 0).getDate();
}

export function DateInput({ id, value, onChange }: Props) {
  const initial = parse(value);
  const [day, setDay] = useState(initial?.day ?? '');
  const [month, setMonth] = useState(initial?.month ?? '');
  const [year, setYear] = useState(initial?.year ?? '');

  // Stay in sync if the parent resets/changes the value from outside
  // (e.g. clearing a filter, loading a different record).
  useEffect(() => {
    const p = parse(value);
    setDay(p?.day ?? '');
    setMonth(p?.month ?? '');
    setYear(p?.year ?? '');
  }, [value]);

  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 3 }, (_, i) => currentYear + i);

  const dayCount = daysInMonth(month, year);
  const days = Array.from({ length: dayCount }, (_, i) => i + 1);

  const commit = (nextDay: string, nextMonth: string, nextYear: string) => {
    if (nextDay && nextMonth && nextYear) {
      onChange(toIso(nextDay, nextMonth, nextYear));
    }
  };

  return (
    <div id={id} style={{ display: 'flex', gap: '0.4rem' }}>
      <select
        aria-label="Day"
        value={day}
        onChange={(e) => {
          setDay(e.target.value);
          commit(e.target.value, month, year);
        }}
        style={{ flex: '1 1 0' }}
      >
        <option value="">Day</option>
        {days.map((d) => (
          <option key={d} value={d}>
            {d}
          </option>
        ))}
      </select>
      <select
        aria-label="Month"
        value={month}
        onChange={(e) => {
          setMonth(e.target.value);
          commit(day, e.target.value, year);
        }}
        style={{ flex: '1.3 1 0' }}
      >
        <option value="">Month</option>
        {MONTHS.map((m, i) => (
          <option key={m} value={i + 1}>
            {m}
          </option>
        ))}
      </select>
      <select
        aria-label="Year"
        value={year}
        onChange={(e) => {
          setYear(e.target.value);
          commit(day, month, e.target.value);
        }}
        style={{ flex: '1 1 0' }}
      >
        <option value="">Year</option>
        {years.map((y) => (
          <option key={y} value={y}>
            {y}
          </option>
        ))}
      </select>
    </div>
  );
}

/** For displaying an already-stored ISO "yyyy-mm-dd" string as "dd-mm-yyyy". */
export function formatDateDMY(iso: string | null | undefined): string {
  if (!iso) return '';
  const parsed = parse(iso);
  if (!parsed) return '';
  return `${parsed.day.padStart(2, '0')}-${parsed.month.padStart(2, '0')}-${parsed.year}`;
}