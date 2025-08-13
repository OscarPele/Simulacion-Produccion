// src/api/token/tests/authClient.test.ts
import { http, HttpResponse } from 'msw';
import { server, sleep } from '../../../test/setupTests';
import { AuthClient } from '../authClient';
import { tokenStore } from '../tokenStore';
import { beforeEach, describe, expect, test } from 'vitest';

const baseURL = 'http://api.local';

const tokens1 = {
  tokenType: 'Bearer' as const,
  accessToken: 'access-1',
  expiresIn: 1, // 1s → para otros tests, en "adjunta Authorization" lo sobrescribimos
  refreshToken: 'refresh-1',
  refreshExpiresIn: 3600,
};

const tokens2 = {
  tokenType: 'Bearer' as const,
  accessToken: 'access-2',
  expiresIn: 300,
  refreshToken: 'refresh-2',
  refreshExpiresIn: 3600,
};

describe('authClient', () => {
  beforeEach(() => {
    tokenStore.clear();
    server.resetHandlers();
  });

  test('login guarda tokens y devuelve data', async () => {
    server.use(
      http.post(`${baseURL}/auth/login`, async () => HttpResponse.json(tokens1, { status: 200 })),
    );

    const c = new AuthClient({ baseURL });
    const data = await c.login({ usernameOrEmail: 'alice', password: 'x' });

    expect(data.accessToken).toBe('access-1');
    expect(tokenStore.access).toBe('access-1');
    expect(tokenStore.refresh).toBe('refresh-1');
  });

  test('adjunta Authorization en peticiones', async () => {
    // ⬇️ Usamos un expiresIn alto para que NO intente /auth/refresh y evitemos el warning de MSW
    server.use(
      http.post(`${baseURL}/auth/login`, async () =>
        HttpResponse.json({ ...tokens1, expiresIn: 300 }),
      ),
      http.get(`${baseURL}/protected`, async ({ request }) => {
        const auth = request.headers.get('authorization');
        return HttpResponse.json({ authHeader: auth });
      }),
    );

    const c = new AuthClient({ baseURL });
    await c.login({ usernameOrEmail: 'alice', password: 'x' });

    const resp = await c.fetch('/protected');
    const body = await resp.json();
    expect(body.authHeader).toBe('Bearer access-1');
  });

  test('401 → refresh → reintento OK', async () => {
    server.use(
      http.post(`${baseURL}/auth/login`, async () => HttpResponse.json(tokens1)),
      // Primer intento: 401, pero si ya hay access-2 (tras refresh) devolvemos 200
      http.get(`${baseURL}/protected`, async ({ request }) => {
        const auth = request.headers.get('authorization');
        if (auth === 'Bearer access-2') return HttpResponse.json({ ok: true });
        return new HttpResponse(null, { status: 401 });
      }),
      http.post(`${baseURL}/auth/refresh`, async ({ request }) => {
        const body = (await request.json()) as any;
        if (body?.refreshToken === 'refresh-1') return HttpResponse.json(tokens2);
        return new HttpResponse(null, { status: 401 });
      }),
    );

    const c = new AuthClient({ baseURL });
    await c.login({ usernameOrEmail: 'alice', password: 'x' });

    await sleep(1100); // expira access-1

    const r = await c.fetch('/protected');
    expect(r.status).toBe(200);
    expect(tokenStore.access).toBe('access-2');
  });

  test('refresh 401 → logout y se mantiene 401', async () => {
    server.use(
      http.post(`${baseURL}/auth/login`, async () => HttpResponse.json(tokens1)),
      http.get(`${baseURL}/protected`, async () => new HttpResponse(null, { status: 401 })),
      http.post(`${baseURL}/auth/refresh`, async () => new HttpResponse(null, { status: 401 })),
    );

    const c = new AuthClient({ baseURL });
    await c.login({ usernameOrEmail: 'alice', password: 'x' });

    await sleep(1100);

    const resp = await c.fetch('/protected');
    expect(resp.status).toBe(401);
    expect(tokenStore.access).toBeUndefined();
  });

  test('desduplica refresh concurrentes (solo 1 POST /auth/refresh)', async () => {
    let refreshCalls = 0;

    server.use(
      http.post(`${baseURL}/auth/login`, async () => HttpResponse.json(tokens1)),
      // ⬇️ Igual que en el test de reintento OK: 200 tras tener access-2
      http.get(`${baseURL}/protected`, async ({ request }) => {
        const auth = request.headers.get('authorization');
        if (auth === 'Bearer access-2') return HttpResponse.json({ ok: true });
        return new HttpResponse(null, { status: 401 });
      }),
      http.post(`${baseURL}/auth/refresh`, async () => {
        refreshCalls++;
        await new Promise((r) => setTimeout(r, 50)); // simula latencia
        return HttpResponse.json(tokens2);
      }),
    );

    const c = new AuthClient({ baseURL });
    await c.login({ usernameOrEmail: 'alice', password: 'x' });
    await sleep(1100); // forzar refresh

    const [r1, r2] = await Promise.all([c.fetch('/protected'), c.fetch('/protected')]);

    expect(r1.status).toBe(200);
    expect(r2.status).toBe(200);
    expect(refreshCalls).toBe(1);
  });

  test('no añade Authorization a /auth/login ni /auth/register', async () => {
    let authHeaderAtLogin: string | null = null;

    server.use(
      http.post(`${baseURL}/auth/login`, async ({ request }) => {
        authHeaderAtLogin = request.headers.get('authorization');
        return HttpResponse.json(tokens1);
      }),
    );

    const c = new AuthClient({ baseURL });
    await c.login({ usernameOrEmail: 'alice', password: 'x' });

    expect(authHeaderAtLogin).toBeNull();
  });


  
  test('ensureValidAccess no dispara /auth/refresh si el access es válido', async () => {
    let refreshCalls = 0;

    server.use(
      // /auth/refresh nunca debería llamarse aquí
      http.post(`${baseURL}/auth/refresh`, async () => {
        refreshCalls++;
        return HttpResponse.json({}); // por si se llamase
      }),
      // endpoint protegido: responde 200 si trae el access correcto
      http.get(`${baseURL}/protected`, async ({ request }) => {
        const auth = request.headers.get('authorization');
        if (auth === 'Bearer still-valid') {
          return HttpResponse.json({ ok: true });
        }
        return new HttpResponse(null, { status: 401 });
      }),
    );

    const c = new AuthClient({ baseURL });

    // Sembramos tokens "largos" directamente en el store del cliente
    tokenStore.save({
      accessToken: 'still-valid',
      expiresIn: 300,          // >> 5s → válido
      refreshToken: 'r-ok',
      refreshExpiresIn: 3600,
    });

    const r = await c.fetch('/protected');
    expect(r.status).toBe(200);
    expect(refreshCalls).toBe(0);
  });


  test('sin tokens: no añade Authorization y el 401 se propaga', async () => {
    tokenStore.clear();

    let sawAuth: string | null = 'sentinel';
    server.use(
      http.get(`${baseURL}/protected`, async ({ request }) => {
        sawAuth = request.headers.get('authorization');
        return new HttpResponse(null, { status: 401 });
      }),
    );

    const c = new AuthClient({ baseURL });
    const r = await c.fetch('/protected');

    expect(sawAuth).toBeNull(); // no hay cabecera Authorization
    expect(r.status).toBe(401); // se propaga tal cual
  });



  test('401 inicial + /auth/refresh 5xx → limpia sesión y se mantiene 401', async () => {
    server.use(
      http.get(`${baseURL}/protected`, async () => new HttpResponse(null, { status: 401 })),
      http.post(`${baseURL}/auth/refresh`, async () => new HttpResponse(null, { status: 500 })),
    );

    const c = new AuthClient({ baseURL });
    tokenStore.save({
      accessToken: 'short',
      expiresIn: 1,
      refreshToken: 'r-x',
      refreshExpiresIn: 3600,
    });

    const resp = await c.fetch('/protected');
    expect(resp.status).toBe(401);
    expect(tokenStore.access).toBeUndefined();
    expect(tokenStore.refresh).toBeUndefined();
  });

});

