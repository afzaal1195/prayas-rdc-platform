import type { InstitutionType, TourRequestCreate } from '../api/types';

const NAME_PATTERN = /^[A-Za-z .'-]+$/;
const GRADE_PATTERN = /^([1-9]|1[0-2])$/;
const EARLIEST_TIME = '09:30';
const LATEST_TIME = '18:00';
const MIN_VISIT_HOURS = 3;
const MIN_ADVANCE_DAYS = 3;

export interface GradeRow {
  grade: string;
  count: string;
}

export interface CourseRow {
  courseOrBranch: string;
  yearOfStudy: string;
  count: string;
}

export interface FormState {
  institutionType: InstitutionType;

  schoolName: string;
  address: string;
  villageOrTown: string;
  district: string;
  state: string;

  contactName: string;
  contactPhone: string;
  contactEmail: string;

  visitDate: string;
  arrivalTime: string;
  departureTime: string;

  grades: GradeRow[]; // SCHOOL -- total student count is derived from these
  courses: CourseRow[]; // COLLEGE -- total student count is derived from these

  teachers: { name: string; phone: string }[];

  venueIds: number[];
  interestNotes: string;

  lunchRequired: boolean;
  mealCount: string;

  vehicleNumber: string;

  website: string; // honeypot, must stay empty
}

export const emptyFormState: FormState = {
  institutionType: 'SCHOOL',
  schoolName: '',
  address: '',
  villageOrTown: '',
  district: '',
  state: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  visitDate: '',
  arrivalTime: '',
  departureTime: '',
  grades: [{ grade: '', count: '' }],
  courses: [{ courseOrBranch: '', yearOfStudy: '', count: '' }],
  teachers: [{ name: '', phone: '' }],
  venueIds: [],
  interestNotes: '',
  lunchRequired: false,
  mealCount: '',
  vehicleNumber: '',
  website: '',
};

export function totalStudents(form: FormState): number {
  const rows = form.institutionType === 'SCHOOL' ? form.grades : form.courses;
  return rows.reduce((sum, r) => sum + (Number(r.count) || 0), 0);
}

export function toPayload(form: FormState, captchaToken: string): TourRequestCreate {
  return {
    institutionType: form.institutionType,
    school: {
      name: form.schoolName.trim(),
      address: form.address.trim(),
      villageOrTown: form.villageOrTown.trim(),
      district: form.district.trim(),
      state: form.state.trim(),
    },
    contact: {
      name: form.contactName.trim(),
      phone: form.contactPhone.trim(),
      email: form.contactEmail.trim(),
    },
    visitDate: form.visitDate,
    arrivalTime: form.arrivalTime || undefined,
    departureTime: form.departureTime || undefined,
    grades:
      form.institutionType === 'SCHOOL'
        ? form.grades
            .filter((r) => r.grade.trim() && r.count)
            .map((r) => ({ grade: r.grade.trim(), count: Number(r.count) }))
        : undefined,
    courses:
      form.institutionType === 'COLLEGE'
        ? form.courses
            .filter((r) => r.courseOrBranch.trim() && r.count)
            .map((r) => ({
              courseOrBranch: r.courseOrBranch.trim(),
              yearOfStudy: r.yearOfStudy.trim() || undefined,
              count: Number(r.count),
            }))
        : undefined,
    teachers: form.teachers
      .filter((t) => t.name.trim().length > 0)
      .map((t) => ({ name: t.name.trim(), phone: t.phone.trim() })),
    venueIds: form.venueIds,
    interestNotes: form.interestNotes.trim() || undefined,
    lunch: {
      required: form.lunchRequired,
      count: form.lunchRequired && form.mealCount ? Number(form.mealCount) : undefined,
    },
    vehicleNumber: form.vehicleNumber.trim() || undefined,
    captchaToken,
    website: form.website,
  };
}

function todayPlusDays(days: number): Date {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + days);
  return d;
}

/** Per-step validation. Returns an error message per invalid field, keyed by field name. */
export function validateStep(step: number, form: FormState): Record<string, string> {
  const errors: Record<string, string> = {};

  if (step === 0) {
    if (!form.schoolName.trim()) errors.schoolName = 'This field is required.';
    if (!form.villageOrTown.trim()) errors.villageOrTown = 'This field is required.';
    if (!form.district.trim()) errors.district = 'This field is required.';
    if (!form.state.trim()) errors.state = 'This field is required.';
    if (!form.address.trim()) errors.address = 'This field is required.';

    if (!form.contactName.trim()) {
      errors.contactName = 'Contact person is required.';
    } else if (!NAME_PATTERN.test(form.contactName.trim())) {
      errors.contactName = 'Name may only contain letters, spaces, and . \' -';
    }
    if (!/^[0-9+ -]{7,20}$/.test(form.contactPhone.trim())) {
      errors.contactPhone = 'Enter a valid phone number (7-20 digits).';
    }
    if (!form.contactEmail.trim()) {
      errors.contactEmail = 'Email is required.';
    } else if (!/^\S+@\S+\.\S+$/.test(form.contactEmail.trim())) {
      errors.contactEmail = 'Enter a valid email address.';
    }
  }

  if (step === 1) {
    const earliestDate = todayPlusDays(MIN_ADVANCE_DAYS);
    if (!form.visitDate) {
      errors.visitDate = 'Visit date is required.';
    } else if (new Date(form.visitDate) < earliestDate) {
      errors.visitDate = `Visit date must be at least ${MIN_ADVANCE_DAYS} days from today.`;
    }

    if (form.arrivalTime && (form.arrivalTime < EARLIEST_TIME || form.arrivalTime > LATEST_TIME)) {
      errors.arrivalTime = `Arrival must be between ${EARLIEST_TIME} and ${LATEST_TIME}.`;
    }
    if (form.departureTime && (form.departureTime < EARLIEST_TIME || form.departureTime > LATEST_TIME)) {
      errors.departureTime = `Departure must be between ${EARLIEST_TIME} and ${LATEST_TIME}.`;
    }
    if (form.arrivalTime && form.departureTime && !errors.arrivalTime && !errors.departureTime) {
      const [ah, am] = form.arrivalTime.split(':').map(Number);
      const [dh, dm] = form.departureTime.split(':').map(Number);
      const gapMinutes = dh * 60 + dm - (ah * 60 + am);
      if (gapMinutes < MIN_VISIT_HOURS * 60) {
        errors.departureTime = `There must be at least ${MIN_VISIT_HOURS} hours between arrival and departure.`;
      }
    }

    if (form.institutionType === 'SCHOOL') {
      const first = form.grades[0];
      if (!first || !first.grade.trim()) {
        errors.gradeRow0 = 'At least one grade is required.';
      } else if (!GRADE_PATTERN.test(first.grade.trim())) {
        errors.gradeRow0 = 'Grade must be 1-12.';
      }
      if (!first || !first.count || Number(first.count) < 1) {
        errors.gradeCount0 = 'Enter a student count for this grade.';
      }
        } else {
      const first = form.courses[0];
      if (!first || !first.courseOrBranch.trim()) {
        errors.courseRow0 = 'At least one course/branch is required.';
      }
      if (!first || !first.count || Number(first.count) < 1) {
        errors.courseCount0 = 'Enter a student count for this course.';
      }

      // Grade duplicates are structurally impossible (it's a filtered
      // select), but course/branch is free text, so check it explicitly.
      const seen = new Set<string>();
      for (const row of form.courses) {
        const key = `${row.courseOrBranch.trim().toLowerCase()}|${row.yearOfStudy.trim().toLowerCase()}`;
        if (row.courseOrBranch.trim() && seen.has(key)) {
          errors.courseDuplicate = 'The same course/branch and year is listed more than once.';
          break;
        }
        seen.add(key);
      }
    }

    const total = totalStudents(form);
    if (total > 500) {
      errors.totalStudents = 'Total student count across all rows must be 500 or fewer.';
    }

    const firstTeacher = form.teachers[0];
    if (!firstTeacher || !firstTeacher.name.trim()) {
      errors.teacherName0 = 'At least one teacher\u2019s name is required.';
    } else if (!NAME_PATTERN.test(firstTeacher.name.trim())) {
      errors.teacherName0 = 'Name may only contain letters, spaces, and . \' -';
    }
    if (!firstTeacher || !firstTeacher.phone.trim()) {
      errors.teacherPhone0 = 'At least one teacher\u2019s phone is required.';
    } else if (!/^[0-9+ -]{7,20}$/.test(firstTeacher.phone.trim())) {
      errors.teacherPhone0 = 'Enter a valid phone number.';
    }
  }

  if (step === 2) {
    if (form.venueIds.length === 0) errors.venueIds = 'Select at least one venue.';
  }

  if (step === 3) {
    if (form.lunchRequired) {
      const count = Number(form.mealCount);
      if (!form.mealCount || count < 1) {
        errors.mealCount = 'Meal count is required when lunch is needed.';
      }
    }
  }

  return errors;
}