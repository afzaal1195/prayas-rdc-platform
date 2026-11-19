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
  // A date change the school asked for that staff haven't answered yet.
  pendingVisitDate: string | null;
  pendingArrivalTime: string | null;
  pendingDepartureTime: string | null;
  // Why staff declined the school's last date-change request.
  dateChangeNote: string | null;
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
  canAssign: boolean;
  canWorkApprovals: boolean;
  canWorkTeams: boolean;
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
  // Set while the school's request for a different date is waiting for staff.
  requestedVisitDate: string | null;
  requestedArrivalTime: string | null;
  requestedDepartureTime: string | null;
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
// ---------- Assigning people to an approved tour ----------

export type AssignmentRole = 'COORDINATOR' | 'VOLUNTEER';

/** One person covering one stretch of a tour. startsAt / endsAt are ISO instants (UTC). */
export interface AssignmentView {
  id: number;
  userId: number;
  userName: string;
  role: AssignmentRole;
  startsAt: string;
  endsAt: string;
  status: 'ACTIVE' | 'RELEASED';
}

/** One of the signed-in person's own shifts. */
export interface MyAssignment {
  assignmentId: number;
  programmeId: number;
  role: AssignmentRole;
  startsAt: string;
  endsAt: string;
  visitDate: string;
  institutionName: string;
  villageOrTown: string | null;
  district: string | null;
  studentCount: number;
  contactName: string | null;
  contactPhone: string | null;
}

export interface AssigneeOption {
  userId: number;
  fullName: string;
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

export type ApprovalKind = 'VENUE' | 'HOSTEL_OFFICE' | 'MESS';
export type ApprovalStatus = 'WAITING' | 'PENDING' | 'REQUESTED' | 'APPROVED' | 'DENIED';

export interface ApprovalContact {
  id: number;
  name: string;
  office: string | null;
  phone: string | null;
  email: string | null;
  officeHours: string | null;
  preferredContact: string | null;
}

/** One permission a tour needs from outside PRAYAS (a venue's approver, or a step of the lunch chain). */
export interface ApprovalStep {
  id: number;
  programmeId: number;
  visitDate: string | null;
  schoolName: string;
  kind: ApprovalKind;
  title: string;
  detail: string | null;
  status: ApprovalStatus;
  note: string | null;
  decidedByName: string | null;
  decidedAt: string | null;
  contact: ApprovalContact | null;
}

export type TeamRole = 'COORDINATOR' | 'VOLUNTEER';

export interface TeamMember {
  assignmentId: number;
  userId: number;
  name: string;
}

export interface UserOption {
  userId: number;
  name: string;
}

/** One domain's team on one tour, with what the signed-in person may do to it. */
export interface DomainTeam {
  domainId: number;
  domainCode: string;
  domainName: string;
  jobs: string[];
  coordinators: TeamMember[];
  volunteers: TeamMember[];
  canAssignCoordinator: boolean;
  canAssignVolunteer: boolean;
  coordinatorOptions: UserOption[];
  volunteerOptions: UserOption[];
}

export interface TourTeams {
  programmeId: number;
  visitDate: string;
  schoolName: string;
  domains: DomainTeam[];
}

export interface DomainJob {
  assignmentId: number;
  programmeId: number;
  visitDate: string;
  schoolName: string;
  domainName: string;
  role: TeamRole;
  jobs: string[];
  coordinatorNames: string[];
}
