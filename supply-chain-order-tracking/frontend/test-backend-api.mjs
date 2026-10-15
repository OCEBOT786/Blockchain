import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createServer } from 'vite';

test('React API client sends Supabase token and handles backend errors', async () => {
  process.env.VITE_SUPABASE_URL ||= 'https://example.supabase.co';
  process.env.VITE_SUPABASE_PUBLISHABLE_KEY ||= 'public-test-key';
  const vite = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
  try {
    const { backendRequest } = await vite.ssrLoadModule('/src/lib/backend.js');
    const loadSession = async () => ({ data: { session: { access_token: 'test-access-token' } }, error: null });
    const calls = [];
    const client = async (url, options) => {
      calls.push({ url, options });
      return { ok: true, json: async () => ({ blockchain_order_id: 1 }) };
    };
    const id = '12345678-1234-4234-8234-123456789abc';
    const result = await backendRequest(`/${id}/link`, { method: 'POST' }, client, loadSession);
    assert.equal(result.blockchain_order_id, 1);
    assert.equal(calls.length, 1);
    assert.equal(calls[0].url, `http://localhost:8080/api/orders/${id}/link`);
    assert.equal(calls[0].options.headers.Authorization, 'Bearer test-access-token');
    await assert.rejects(backendRequest(`/${id}/link`, {}, async () => ({
      ok: false, status: 403, json: async () => ({ error: 'Not assigned' }),
    }), loadSession), /Not assigned/);
  } finally {
    await vite.close();
  }
});
