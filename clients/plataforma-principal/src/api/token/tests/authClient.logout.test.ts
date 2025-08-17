import { beforeEach, describe, expect, test } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/setupTests';
import { AuthClient } from '../authClient';
import { tokenStore } from '../tokenStore';

const baseURL = 'http://api.local';

describe('logout & logoutAll (AuthClient methods)', () => {
  beforeEach(() => {
    server.resetHandlers();
    tokenStore.clear();
  });

  test('logout limpia tokenStore y hace POST /auth/logout con {refreshToken}', async () => {
    let called = false;
    let body: any = null;

    server.use(
      http.post(`${baseURL}/auth/logout`, async ({ request }) => {
        called = true;
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );

    // semillas
    tokenStore.save({
      accessToken: 'a1',
      expiresIn: 300,
      refreshToken: 'r1',
      refreshExpiresIn: 3600,
    });

    const c = new AuthClient({ baseURL });
    await c.logout();

    expect(called).toBe(true);
    expect(body).toEqual({ refreshToken: 'r1' });
    expect(tokenStore.access).toBeUndefined();
    expect(tokenStore.refresh).toBeUndefined();
  });

  test('logoutAll limpia y hace POST /auth/logout-all con {refreshToken} aunque el server falle', async () => {
    let called = false;
    let body: any = null;

    server.use(
      http.post(`${baseURL}/auth/logout-all`, async ({ request }) => {
        called = true;
        body = await request.json();
        return new HttpResponse(null, { status: 500 });
      }),
    );

    tokenStore.save({
      accessToken: 'a2',
      expiresIn: 300,
      refreshToken: 'r2',
      refreshExpiresIn: 3600,
    });

    const c = new AuthClient({ baseURL });
    await c.logoutAll();

    expect(called).toBe(true);
    expect(body).toEqual({ refreshToken: 'r2' });
    expect(tokenStore.access).toBeUndefined();
    expect(tokenStore.refresh).toBeUndefined();
  });
});
