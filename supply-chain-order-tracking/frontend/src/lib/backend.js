import { supabase } from './supabase';

const baseUrl = (import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080').replace(/\/$/, '');

export async function backendRequest(path, options = {}, client = fetch,
  loadSession = () => supabase.auth.getSession()) {
  const { data: { session }, error } = await loadSession();
  if (error || !session?.access_token) throw new Error('Please sign in again.');
  let response;
  try {
    response = await client(`${baseUrl}/api/orders${path}`, {
      ...options,
      headers: {
        Authorization: `Bearer ${session.access_token}`,
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new Error('Cannot reach the Java backend. Check that it is running.');
  }
  let body;
  try { body = await response.json(); } catch { body = {}; }
  if (!response.ok) throw new Error(body.error || `Backend request failed (${response.status})`);
  return body;
}

export const ordersApi = {
  list: () => backendRequest(''),
  get: (id) => backendRequest(`/${encodeURIComponent(id)}`),
  link: (id) => backendRequest(`/${encodeURIComponent(id)}/link`, { method: 'POST' }),
  verify: (id) => backendRequest(`/${encodeURIComponent(id)}/verify`),
  updateStatus: (id, status) => backendRequest(`/${encodeURIComponent(id)}/status`, {
    method: 'PUT', body: JSON.stringify({ status }),
  }),
};
