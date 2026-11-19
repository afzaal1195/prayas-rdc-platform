// Tour times are campus-clock times, so shifts are shown in that clock
// whatever time zone the browser happens to be in.
const CAMPUS_TIME_ZONE = 'Asia/Kolkata';

/** "HH:mm" on the campus clock for an ISO instant coming from the backend. */
export function campusClock(iso: string): string {
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: CAMPUS_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date(iso));
}
