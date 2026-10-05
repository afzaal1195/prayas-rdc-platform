import type { FormState, GradeRow, CourseRow } from '../../pages/formState';
import { totalStudents } from '../../pages/formState';
import { TimeInput12h } from '../TimeInput12h';
import { DateInput } from '../DateInput';

const ALL_GRADES = Array.from({ length: 12 }, (_, i) => String(i + 1));

interface Props {
  form: FormState;
  errors: Record<string, string>;
  onChange: (patch: Partial<FormState>) => void;
}

export function VisitDetailsStep({ form, errors, onChange }: Props) {
  const isCollege = form.institutionType === 'COLLEGE';

  const updateTeacher = (index: number, patch: Partial<{ name: string; phone: string }>) => {
    const teachers = form.teachers.map((t, i) => (i === index ? { ...t, ...patch } : t));
    onChange({ teachers });
  };
  const addTeacher = () => {
    if (form.teachers.length >= 20) return;
    onChange({ teachers: [...form.teachers, { name: '', phone: '' }] });
  };
  const removeTeacher = (index: number) => {
    onChange({ teachers: form.teachers.filter((_, i) => i !== index) });
  };

  const updateGrade = (index: number, patch: Partial<GradeRow>) => {
    const grades = form.grades.map((g, i) => (i === index ? { ...g, ...patch } : g));
    onChange({ grades });
  };
  const addGrade = () => {
    if (form.grades.length >= 12) return;
    onChange({ grades: [...form.grades, { grade: '', count: '' }] });
  };
  const removeGrade = (index: number) => {
    onChange({ grades: form.grades.filter((_, i) => i !== index) });
  };

  const updateCourse = (index: number, patch: Partial<CourseRow>) => {
    const courses = form.courses.map((c, i) => (i === index ? { ...c, ...patch } : c));
    onChange({ courses });
  };
  const addCourse = () => {
    if (form.courses.length >= 20) return;
    onChange({ courses: [...form.courses, { courseOrBranch: '', yearOfStudy: '', count: '' }] });
  };
  const removeCourse = (index: number) => {
    onChange({ courses: form.courses.filter((_, i) => i !== index) });
  };

  return (
    <>
      <h2>Visit details</h2>
      <p className="step-intro">
        When are you planning to come? Visits need to be booked at least 3 days ahead, between
        9:30 AM and 6:00 PM, with at least a 3-hour visit window.
      </p>

      <div className="field-grid">
        <div className="field full">
          <label htmlFor="visitDate">Visit date *</label>
          <DateInput id="visitDate" value={form.visitDate} onChange={(v) => onChange({ visitDate: v })} />
          {errors.visitDate && <span className="field-error">{errors.visitDate}</span>}
        </div>
        <div className="field">
          <label htmlFor="arrivalTime">Expected arrival time</label>
          <TimeInput12h
            id="arrivalTime"
            value={form.arrivalTime}
            onChange={(v) => onChange({ arrivalTime: v })}
          />
          {errors.arrivalTime && <span className="field-error">{errors.arrivalTime}</span>}
        </div>
        <div className="field">
          <label htmlFor="departureTime">Expected departure time</label>
          <TimeInput12h
            id="departureTime"
            value={form.departureTime}
            onChange={(v) => onChange({ departureTime: v })}
          />
          {errors.departureTime && <span className="field-error">{errors.departureTime}</span>}
        </div>
      </div>

      <div className="field full" style={{ marginTop: '0.5rem' }}>
        <label>{isCollege ? 'Courses / branches' : 'Grades'} *</label>
        <span className="hint">Add each {isCollege ? 'course' : 'grade'} with how many students from it are coming.</span>
      </div>

      {!isCollege &&
        form.grades.map((row, i) => {
          const usedByOthers = form.grades.filter((_, j) => j !== i).map((g) => g.grade);
          const availableGrades = ALL_GRADES.filter((g) => g === row.grade || !usedByOthers.includes(g));
          return (
          <div className="teacher-row" key={i}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor={`grade-${i}`}>Grade{i === 0 ? ' *' : ''}</label>
              <select
                id={`grade-${i}`}
                value={row.grade}
                onChange={(e) => updateGrade(i, { grade: e.target.value })}
              >
                <option value="">Choose a grade</option>
                {availableGrades.map((g) => (
                  <option key={g} value={g}>
                    Grade {g}
                  </option>
                ))}
              </select>
              {i === 0 && errors.gradeRow0 && <span className="field-error">{errors.gradeRow0}</span>}
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor={`grade-count-${i}`}>Students{i === 0 ? ' *' : ''}</label>
              <input
                id={`grade-count-${i}`}
                type="number"
                min={1}
                value={row.count}
                onChange={(e) => updateGrade(i, { count: e.target.value })}
              />
              {i === 0 && errors.gradeCount0 && <span className="field-error">{errors.gradeCount0}</span>}
            </div>
            {form.grades.length > 1 && (
              <button type="button" className="btn-text" onClick={() => removeGrade(i)}>
                Remove
              </button>
            )}
          </div>
          );
        })}
      {!isCollege && form.grades.length < 12 && (
        <button type="button" className="btn-text" onClick={addGrade}>
          + Add more grade
        </button>
      )}

      {isCollege &&
        form.courses.map((row, i) => (
          <div className="teacher-row" key={i}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor={`course-${i}`}>Course / branch{i === 0 ? ' *' : ''}</label>
              <input
                id={`course-${i}`}
                placeholder="e.g. B.Tech CSE"
                value={row.courseOrBranch}
                onChange={(e) => updateCourse(i, { courseOrBranch: e.target.value })}
              />
              {i === 0 && errors.courseRow0 && <span className="field-error">{errors.courseRow0}</span>}
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor={`course-year-${i}`}>Year of study</label>
              <input
                id={`course-year-${i}`}
                placeholder="e.g. 2nd Year"
                value={row.yearOfStudy}
                onChange={(e) => updateCourse(i, { yearOfStudy: e.target.value })}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor={`course-count-${i}`}>Students{i === 0 ? ' *' : ''}</label>
              <input
                id={`course-count-${i}`}
                type="number"
                min={1}
                value={row.count}
                onChange={(e) => updateCourse(i, { count: e.target.value })}
              />
              {i === 0 && errors.courseCount0 && <span className="field-error">{errors.courseCount0}</span>}
            </div>
            {form.courses.length > 1 && (
              <button type="button" className="btn-text" onClick={() => removeCourse(i)}>
                Remove
              </button>
            )}
          </div>
        ))}
      {isCollege && form.courses.length < 20 && (
        <button type="button" className="btn-text" onClick={addCourse}>
          + Add more course
        </button>
      )}
      {errors.courseDuplicate && <div className="field-error">{errors.courseDuplicate}</div>}

      <p className="hint" style={{ marginTop: '0.75rem' }}>
        Total students: <strong>{totalStudents(form)}</strong>
      </p>
      {errors.totalStudents && <span className="field-error">{errors.totalStudents}</span>}

      <div className="field full" style={{ marginTop: '1.5rem' }}>
        <label>{isCollege ? 'Faculty coordinators' : 'Accompanying teachers'}</label>
        <span className="hint">At least one name and phone number is required.</span>
      </div>

      {form.teachers.map((teacher, i) => (
        <div className="teacher-row" key={i}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor={`teacher-name-${i}`}>Name{i === 0 ? ' *' : ''}</label>
            <input
              id={`teacher-name-${i}`}
              maxLength={150}
              value={teacher.name}
              onChange={(e) => updateTeacher(i, { name: e.target.value })}
            />
            {i === 0 && errors.teacherName0 && <span className="field-error">{errors.teacherName0}</span>}
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor={`teacher-phone-${i}`}>Phone{i === 0 ? ' *' : ''}</label>
            <input
              id={`teacher-phone-${i}`}
              maxLength={20}
              value={teacher.phone}
              onChange={(e) => updateTeacher(i, { phone: e.target.value })}
            />
            {i === 0 && errors.teacherPhone0 && <span className="field-error">{errors.teacherPhone0}</span>}
          </div>
          {form.teachers.length > 1 && (
            <button type="button" className="btn-text" onClick={() => removeTeacher(i)}>
              Remove
            </button>
          )}
        </div>
      ))}

      {form.teachers.length < 20 && (
        <button type="button" className="btn-text" onClick={addTeacher}>
          + Add another {isCollege ? 'coordinator' : 'teacher'}
        </button>
      )}
    </>
  );
}