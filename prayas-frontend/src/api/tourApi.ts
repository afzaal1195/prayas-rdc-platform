import { apiClient } from './client';
import type { TourRequestCreate, TourRequestCreated, TourStatusView, Venue, GradeCount, CourseCount } from './types';

export async function fetchVenues(): Promise<Venue[]> {
  const { data } = await apiClient.get<Venue[]>('/api/v1/public/venues');
  return data;
}

export async function submitTourRequest(payload: TourRequestCreate): Promise<TourRequestCreated> {
  const { data } = await apiClient.post<TourRequestCreated>('/api/v1/public/tour-requests', payload);
  return data;
}

export async function fetchTourStatus(token: string): Promise<TourStatusView> {
  const { data } = await apiClient.get<TourStatusView>(`/api/v1/public/tour-requests/${token}`);
  return data;
}

export async function reviseHeadcount(
  token: string,
  grades: GradeCount[],
  courses: CourseCount[],
  teacherCount: number,
  lunchRequired: boolean,
  mealCount?: number,
): Promise<void> {
  await apiClient.post(`/api/v1/public/tour-requests/${token}/headcount`, {
    grades,
    courses,
    teacherCount,
    lunchRequired,
    mealCount,
  });
}

export async function cancelTourRequest(token: string, reason?: string): Promise<void> {
  await apiClient.post(`/api/v1/public/tour-requests/${token}/cancel`, { reason });
}

export async function requestDateChange(
  token: string,
  newVisitDate: string,
  newArrivalTime?: string,
  newDepartureTime?: string,
): Promise<void> {
  await apiClient.post(`/api/v1/public/tour-requests/${token}/reschedule`, {
    newVisitDate,
    newArrivalTime,
    newDepartureTime,
  });
}

/** The school accepting the date PRAYAS proposed: only now does the request reopen. */
export async function acceptProposedDate(token: string): Promise<void> {
  await apiClient.post(`/api/v1/public/tour-requests/${token}/proposed-date/accept`);
}

export async function declineProposedDate(token: string, reason?: string): Promise<void> {
  await apiClient.post(`/api/v1/public/tour-requests/${token}/proposed-date/decline`, { reason });
}
