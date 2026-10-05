import type { FormState } from '../../pages/formState';
import { totalStudents } from '../../pages/formState';
import type { Venue } from '../../api/types';
import { formatTime12h } from '../TimeInput12h';
import { formatDateDMY } from '../DateInput';

interface Props {
  form: FormState;
  venues: Venue[];
}

function Row({ label, value }: { label: string; value: string }) {
  if (!value) return null;
  return (
    <div className="review-row">
      <span className="k">{label}</span>
      <span className="v">{value}</span>
    </div>
  );
}

export function ReviewStep({ form, venues }: Props) {
  const isCollege = form.institutionType === 'COLLEGE';
  const selectedVenues = venues.filter((v) => form.venueIds.includes(v.id)).map((v) => v.name);
  const teacherNames = form.teachers.filter((t) => t.name.trim()).map((t) => t.name);
  const breakdown = isCollege
    ? form.courses
        .filter((c) => c.courseOrBranch.trim() && c.count)
        .map((c) => `${c.courseOrBranch}${c.yearOfStudy ? ` (${c.yearOfStudy})` : ''}: ${c.count}`)
        .join(', ')
    : form.grades
        .filter((g) => g.grade.trim() && g.count)
        .map((g) => `Grade ${g.grade}: ${g.count}`)
        .join(', ');

  return (
    <>
      <h2>Review &amp; submit</h2>
      <p className="step-intro">Check everything looks right before sending your request.</p>

      <div className="review-section">
        <h3>{isCollege ? 'Institution' : 'School'} &amp; contact</h3>
        <Row label={isCollege ? 'Institution' : 'School'} value={form.schoolName} />
        <Row label="Location" value={[form.villageOrTown, form.district, form.state].filter(Boolean).join(', ')} />
        <Row label="Address" value={form.address} />
        <Row label="Contact" value={form.contactName} />
        <Row label="Phone" value={form.contactPhone} />
        <Row label="Email" value={form.contactEmail} />
      </div>

      <div className="review-section">
        <h3>Visit details</h3>
        <Row label="Date" value={formatDateDMY(form.visitDate)} />
        <Row label={isCollege ? 'Courses' : 'Grades'} value={breakdown} />
        <Row label="Total students" value={String(totalStudents(form))} />
        <Row label={isCollege ? 'Faculty coordinators' : 'Teachers'} value={teacherNames.join(', ')} />
        <Row label="Arrival" value={formatTime12h(form.arrivalTime)} />
        <Row label="Departure" value={formatTime12h(form.departureTime)} />
      </div>

      <div className="review-section">
        <h3>Places to visit</h3>
        <Row label="Venues" value={selectedVenues.join(', ')} />
        <Row label="Notes" value={form.interestNotes} />
      </div>

      <div className="review-section">
        <h3>Logistics</h3>
        <Row label="Lunch" value={form.lunchRequired ? 'Required' : 'Not required'} />
        {form.lunchRequired && <Row label="Meal head count" value={`${form.mealCount || 0} people`} />}
        <Row label="Vehicle" value={form.vehicleNumber} />
      </div>
    </>
  );
}