import axios from 'axios';

// Empty baseURL means requests go to the same origin the page was loaded
// from (e.g. POST /api/v1/...), which Vite's dev proxy (see vite.config.ts)
// forwards to the real backend on localhost:8080 -- keeping the browser's
// view of "the origin" consistent so the staff session cookie is same-site.
// VITE_API_BASE_URL can override this for a setup without that proxy.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? '';

export const apiClient = axios.create({
  baseURL,
  withCredentials: true, // sends the session cookie for staff-authenticated routes
});