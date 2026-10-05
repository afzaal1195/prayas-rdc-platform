import { apiClient } from './client';
import type {
  AdminUser,
  AdminUserInput,
  AdminVenue,
  AdminVenueInput,
  AuthorityContact,
  AuthorityContactInput,
  DevUser,
  DomainRef,
} from './types';

// ---------- Venues ----------

export async function fetchAdminVenues(): Promise<AdminVenue[]> {
  const { data } = await apiClient.get<AdminVenue[]>('/api/v1/admin/venues');
  return data;
}

export async function createAdminVenue(input: AdminVenueInput): Promise<AdminVenue> {
  const { data } = await apiClient.post<AdminVenue>('/api/v1/admin/venues', input);
  return data;
}

export async function deleteAdminVenue(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/admin/venues/${id}`);
}

export async function updateAdminVenue(id: number, input: AdminVenueInput): Promise<AdminVenue> {
  const { data } = await apiClient.put<AdminVenue>(`/api/v1/admin/venues/${id}`, input);
  return data;
}

// ---------- Authority contacts ----------

export async function fetchAuthorities(): Promise<AuthorityContact[]> {
  const { data } = await apiClient.get<AuthorityContact[]>('/api/v1/admin/authority-contacts');
  return data;
}

export async function createAuthority(input: AuthorityContactInput): Promise<AuthorityContact> {
  const { data } = await apiClient.post<AuthorityContact>('/api/v1/admin/authority-contacts', input);
  return data;
}

export async function deleteAuthority(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/admin/authority-contacts/${id}`);
}

export async function updateAuthority(id: number, input: AuthorityContactInput): Promise<AuthorityContact> {
  const { data } = await apiClient.put<AuthorityContact>(`/api/v1/admin/authority-contacts/${id}`, input);
  return data;
}

// ---------- Staff ----------

export async function fetchAdminUsers(): Promise<AdminUser[]> {
  const { data } = await apiClient.get<AdminUser[]>('/api/v1/admin/users');
  return data;
}

export async function fetchDomains(): Promise<DomainRef[]> {
  const { data } = await apiClient.get<DomainRef[]>('/api/v1/admin/domains');
  return data;
}

export async function createAdminUser(input: AdminUserInput): Promise<AdminUser> {
  const { data } = await apiClient.post<AdminUser>('/api/v1/admin/users', input);
  return data;
}

export async function deleteAdminUser(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/admin/users/${id}`);
}

export async function updateAdminUser(id: number, input: AdminUserInput): Promise<AdminUser> {
  const { data } = await apiClient.put<AdminUser>(`/api/v1/admin/users/${id}`, input);
  return data;
}

// ---------- Local "test as" switch (the backend only has these under the local profile) ----------

export async function fetchDevUsers(): Promise<DevUser[]> {
  const { data } = await apiClient.get<DevUser[]>('/api/v1/dev/users');
  return data;
}

export async function actAs(email: string): Promise<void> {
  await apiClient.post('/api/v1/dev/act-as', { email });
}

export async function stopActing(): Promise<void> {
  await apiClient.delete('/api/v1/dev/act-as');
}
