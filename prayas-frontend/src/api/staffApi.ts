import { apiClient } from './client';
import type { Me, ProgrammeSummary, DecisionRequest, ProgrammeStatus, ProgrammeDetail } from './types';

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

export async function fetchProgrammeDetail(id: number): Promise<ProgrammeDetail> {
  const { data } = await apiClient.get<ProgrammeDetail>(`/api/v1/tour-requests/${id}/detail`);
  return data;
}

export async function regenerateTrackingLink(id: number): Promise<string> {
  const { data } = await apiClient.post<{ trackingToken: string }>(`/api/v1/tour-requests/${id}/tracking-link`);
  return data.trackingToken;
}

export function loginUrl(): string {
  // Relative, so it goes through Vite's dev proxy (same-origin) rather than
  // a hardcoded absolute backend URL.
  return '/oauth2/authorization/google';
}