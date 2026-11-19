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

/** Takes a rejection back so the request can be decided again (lead / faculty only; a reason is required). */
export async function reopenRequest(id: number, reason: string): Promise<void> {
  await apiClient.post(`/api/v1/tour-requests/${id}/reopen`, { reason });
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

export function loginUrl(): string {
  // Relative, so it goes through Vite's dev proxy (same-origin) rather than
  // a hardcoded absolute backend URL.
  return '/oauth2/authorization/google';
}