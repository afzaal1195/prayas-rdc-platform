import type { ReadinessSummary } from '../../api/types';

/** A small coloured label for how prepared a tour is, e.g. "3 of 5 stops ready". */
export function ReadinessChip({ summary }: { summary: ReadinessSummary }) {
  let text: string;
  let tone: 'approved' | 'pending' | 'rejected' | 'neutral';
  switch (summary.overall) {
    case 'NO_STOPS':
      text = 'No itinerary yet';
      tone = 'neutral';
      break;
    case 'READY':
      text = `All ${summary.total} stops ready`;
      tone = 'approved';
      break;
    case 'BLOCKED':
      text = `${summary.blocked} blocked · ${summary.ready} of ${summary.total} ready`;
      tone = 'rejected';
      break;
    default:
      text = `${summary.ready} of ${summary.total} stops ready`;
      tone = 'pending';
  }
  return <span className={`status-badge ${tone}`}>{text}</span>;
}
