// Mirrors the backend's record types exactly (TourRequestCreate.java,
// TourRequestCreated.java, VenueView.java) so a shape change on either
// side is easy to spot.

export type VenueType = 'LEGACY_ROOM' | 'LIBRARY' | 'SPORTS' | 'LAB' | 'CLASSROOM' | 'OTHER';
export type InstitutionType = 'SCHOOL' | 'COLLEGE';

export interface Venue {
  id: number;
  name: string;
  venueType: VenueType;
  department: string | null;
  description: string | null;
}

export interface SchoolInfo {
  name: string;
  address: string;
  villageOrTown: string;
  district: string;
  state: string;
}

export interface ContactInfo {
  name: string;
  phone: string;
  email: string;
}

export interface TeacherInfo {
  name: string;
  phone: string;
}

export interface LunchInfo {
  required: boolean;
  count?: number;
}

export interface GradeCount {
  grade: string; // "1".."12"
  count: number;
}

export interface CourseCount {
  courseOrBranch: string;
  yearOfStudy?: string;
  count: number;
}

export interface TourRequestCreate {
  institutionType: InstitutionType;
  school: SchoolInfo;
  contact: ContactInfo;
  visitDate: string; // ISO date, e.g. "2026-12-15"
  arrivalTime?: string; // "HH:mm:ss" or "HH:mm"
  departureTime?: string;
  grades?: GradeCount[]; // SCHOOL only -- total student count is derived from these
  courses?: CourseCount[]; // COLLEGE only -- total student count is derived from these
  teachers: TeacherInfo[];
  venueIds: number[];
  interestNotes?: string;
  lunch: LunchInfo;
  vehicleNumber?: string;
  captchaToken: string;
  website: string; // honeypot -- must stay empty
}

export type ProgrammeStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'RESCHEDULE_PROPOSED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'COMPLETED';

export interface TourRequestCreated {
  programmeId: number;
  trackingToken: string;
  status: ProgrammeStatus;
}

export interface TourStatusView {
  status: ProgrammeStatus;
  institutionType: InstitutionType;
  visitDate: string;
  proposedDate: string | null;
  arrivalTime: string | null;
  departureTime: string | null;
  currentStudentCount: number;
  lunchRequired: boolean;
  mealCount: number | null;
  grades: GradeCount[];
  courses: CourseCount[];
  decisionNote: string | null;
}

export type GlobalRole = 'FACULTY_INCHARGE' | 'LEAD' | 'MEMBER';
export type DomainRole = 'HEAD' | 'COORDINATOR' | 'VOLUNTEER';

export interface DomainRoleView {
  domainCode: string;
  domainName: string;
  role: DomainRole;
}

export interface ActingAs {
  realName: string;
  realEmail: string;
}

export interface Me {
  name: string;
  email: string;
  globalRole: GlobalRole;
  domainRoles: DomainRoleView[];
  // What the UI offers. The server still enforces every one of these.
  canViewTours: boolean;
  canDecide: boolean;
  canReopen: boolean;
  canAdmin: boolean;
  // Local profile only: the "Test as" switch.
  devTools: boolean;
  actingAs: ActingAs | null;
}

export interface ProgrammeSummary {
  id: number;
  institutionName: string;
  villageOrTown: string | null;
  district: string | null;
  institutionType: InstitutionType;
  visitDate: string;
  arrivalTime: string | null;
  departureTime: string | null;
  status: ProgrammeStatus;
  studentCount: number;
  contactName: string | null;
  contactPhone: string | null;
}

export type Decision = 'APPROVE' | 'REJECT' | 'PROPOSE_DATE';

export interface DecisionRequest {
  decision: Decision;
  note?: string;
  proposedDate?: string;
}

export interface ProgrammeDetail {
  address: string | null;
  state: string | null;
  contactEmail: string | null;
  gradeRange: string | null;
  teacherCount: number;
  teachers: TeacherInfo[];
  lunchRequired: boolean;
  mealCount: number | null;
  vehicleNumber: string | null;
  interestNotes: string | null;
  decisionNote: string | null;
}
// ---------- Admin ----------

export interface DomainRef {
  code: string;
  name: string;
}

export interface AuthorityContact {
  id: number;
  name: string;
  office: string | null;
  phone: string | null;
  email: string | null;
  officeHours: string | null;
  preferredContact: string | null;
  notes: string | null;
  active: boolean;
}

export interface AuthorityContactInput {
  name: string;
  office: string | null;
  phone: string | null;
  email: string | null;
  officeHours: string | null;
  preferredContact: string | null;
  notes: string | null;
  active: boolean;
}

export interface AdminVenue {
  id: number;
  name: string;
  venueType: VenueType;
  department: string | null;
  capacity: number | null;
  requiresApproval: boolean;
  publicVisible: boolean;
  active: boolean;
  description: string | null;
  authorityContactId: number | null;
  authorityName: string | null;
}

export interface AdminVenueInput {
  name: string;
  venueType: VenueType;
  department: string | null;
  capacity: number | null;
  requiresApproval: boolean;
  publicVisible: boolean;
  active: boolean;
  description: string | null;
  authorityContactId: number | null;
}

export interface AdminMembership {
  domainCode: string;
  domainName: string;
  role: DomainRole;
}

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  globalRole: GlobalRole;
  canFillVolunteerSlots: boolean;
  active: boolean;
  memberships: AdminMembership[];
}

export interface AdminUserInput {
  email: string;
  fullName: string;
  phone: string | null;
  globalRole: GlobalRole;
  canFillVolunteerSlots: boolean;
  active: boolean;
  memberships: { domainCode: string; role: DomainRole }[];
}

// ---------- Local "test as" switch ----------

export interface DevUser {
  email: string;
  fullName: string;
  globalRole: GlobalRole;
  domainRoles: string[];
}
