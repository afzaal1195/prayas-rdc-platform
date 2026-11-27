import { apiClient } from './client';
import type {
  Me,
  ProgrammeSummary,
  DecisionRequest,
  ProgrammeStatus,
  ProgrammeDetail,
  AssignmentView,
  AssigneeOption,
  AssignmentRole,
  MyAssignment,
  ApprovalStep,
  DomainJob,
  ItineraryBoard,
  RequirementStatus,
  TaskBoard,
  TeamRole,
  TourTeams,
  ApprovalStatus,
} from './types';

/** Returns null if not logged in (401), rather than throwing -- that's an
 * expected, normal state for this call, not an error condition. */
export async function fetchMe(): Promise<Me | null> {
  try {
    const { data } = await apiClient.get<Me>('/api/v1/me');
    return data;
  } catch (err: any) {
    if (err.response && err.response.status === 401) {
      return null;
    }
    throw err;
  }
}

export async function fetchTourRequests(): Promise<ProgrammeSummary[]> {
  const { data } = await apiClient.get<ProgrammeSummary[]>('/api/v1/tour-requests');
  return data;
}

export async function submitDecision(
  id: number,
  decision: DecisionRequest,
): Promise<{ id: number; status: ProgrammeStatus }> {
  const { data } = await apiClient.post(`/api/v1/tour-requests/${id}/decision`, decision);
  return data;
}

/**
 * Takes a rejection back so the request can be decided again (lead / faculty only; a reason is required).
 * With a proposedDate the request doesn't reopen yet: the school is offered that date and the request
 * reopens only when they accept it.
 */
export async function reopenRequest(id: number, reason: string, proposedDate?: string): Promise<void> {
  await apiClient.post(`/api/v1/tour-requests/${id}/reopen`, { reason, proposedDate });
}

/** Staff answering a school's request for a different date. Only APPROVE changes the real date. */
export async function resolveDateChange(id: number, decision: 'APPROVE' | 'DECLINE', note?: string): Promise<void> {
  await apiClient.post(`/api/v1/tour-requests/${id}/date-change`, { decision, note });
}

export async function fetchProgrammeDetail(id: number): Promise<ProgrammeDetail> {
  const { data } = await apiClient.get<ProgrammeDetail>(`/api/v1/tour-requests/${id}/detail`);
  return data;
}

export async function regenerateTrackingLink(id: number): Promise<string> {
  const { data } = await apiClient.post<{ trackingToken: string }>(`/api/v1/tour-requests/${id}/tracking-link`);
  return data.trackingToken;
}

// ---------- Assigning coordinators and volunteers ----------

/** The signed-in person's own shifts (any staff member may call this). */
export async function fetchMyAssignments(): Promise<MyAssignment[]> {
  const { data } = await apiClient.get<MyAssignment[]>('/api/v1/me/assignments');
  return data;
}

export async function fetchAssignments(programmeId: number): Promise<AssignmentView[]> {
  const { data } = await apiClient.get<AssignmentView[]>(`/api/v1/tour-requests/${programmeId}/assignments`);
  return data;
}

/** People free for the whole visit, or for the part between from and to ("HH:mm"). */
export async function fetchAssignable(
  programmeId: number,
  role: AssignmentRole,
  from?: string,
  to?: string,
): Promise<AssigneeOption[]> {
  const { data } = await apiClient.get<AssigneeOption[]>(`/api/v1/tour-requests/${programmeId}/assignable`, {
    params: { role, from, to },
  });
  return data;
}

export async function assignPerson(
  programmeId: number,
  input: { userId: number; role: AssignmentRole; from?: string; to?: string },
): Promise<void> {
  await apiClient.post(`/api/v1/tour-requests/${programmeId}/assignments`, input);
}

/** The volunteer on this shift leaves at `at` ("HH:mm") and newUserId takes over. */
export async function handoverShift(assignmentId: number, newUserId: number, at: string): Promise<void> {
  await apiClient.post(`/api/v1/assignments/${assignmentId}/handover`, { newUserId, at });
}

export async function releaseAssignment(assignmentId: number): Promise<void> {
  await apiClient.delete(`/api/v1/assignments/${assignmentId}`);
}

/** Upcoming approved tours that still have a venue or lunch permission outstanding. */
export async function fetchApprovalQueue(): Promise<ApprovalStep[]> {
  const { data } = await apiClient.get<ApprovalStep[]>('/api/v1/approvals');
  return data;
}

export async function updateApproval(stepId: number, status: ApprovalStatus, note?: string): Promise<ApprovalStep> {
  const { data } = await apiClient.patch<ApprovalStep>(`/api/v1/approvals/${stepId}`, { status, note });
  return data;
}

export function loginUrl(): string {
  // Relative, so it goes through Vite's dev proxy (same-origin) rather than
  // a hardcoded absolute backend URL.
  return '/oauth2/authorization/google';
}
/** Upcoming approved tours with each domain's coordinators and volunteers. */
export async function fetchTeams(): Promise<TourTeams[]> {
  const { data } = await apiClient.get<TourTeams[]>('/api/v1/teams');
  return data;
}

export async function assignTeamMember(
  programmeId: number,
  domainId: number,
  userId: number,
  role: TeamRole,
): Promise<void> {
  await apiClient.post(`/api/v1/tour-requests/${programmeId}/team`, { domainId, userId, role });
}

export async function removeTeamMember(assignmentId: number): Promise<void> {
  await apiClient.delete(`/api/v1/team/${assignmentId}`);
}

/** The signed-in person's own domain jobs on upcoming tours. */
export async function fetchMyDomainJobs(): Promise<DomainJob[]> {
  const { data } = await apiClient.get<DomainJob[]>('/api/v1/me/domain-jobs');
  return data;
}

/** Every domain's tasks for leads; the caller's own domains for everyone else. */
export async function fetchTaskBoard(): Promise<TaskBoard> {
  const { data } = await apiClient.get<TaskBoard>('/api/v1/task-status');
  return data;
}

export async function setRequirementStatus(requirementId: number, status: RequirementStatus): Promise<void> {
  await apiClient.patch(`/api/v1/requirements/${requirementId}`, { status });
}

/** Every active venue plus each upcoming approved tour's stops (with any problems found). */
export async function fetchItineraries(): Promise<ItineraryBoard> {
  const { data } = await apiClient.get<ItineraryBoard>('/api/v1/itineraries');
  return data;
}

export async function addItineraryStop(
  programmeId: number,
  stop: { venueId?: number; title?: string; startTime: string; endTime: string; notes?: string },
): Promise<void> {
  await apiClient.post(`/api/v1/tour-requests/${programmeId}/itinerary`, stop);
}

export async function removeItineraryStop(stopId: number): Promise<void> {
  await apiClient.delete(`/api/v1/itinerary/${stopId}`);
}
