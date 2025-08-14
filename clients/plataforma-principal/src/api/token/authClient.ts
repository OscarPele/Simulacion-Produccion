// src/api/token/authClient.ts
import { tokenStore } from './tokenStore';
import { authApiUrl } from '../config'; // <-- cambiado: helper que construye URLs del ms-auth

export type LoginRequest = { usernameOrEmail: string; password: string };
export type TokenResponse = {
  tokenType: 'Bearer';
  accessToken: string;
  expiresIn: number;
  refreshToken: string;
  refreshExpiresIn: number;
};

function joinUrl(baseURL: string, path: string): string {
  const base = baseURL.replace(/\/$/, '');
  if (/^https?:\/\//i.test(path)) return path;
  const clean = path.startsWith('/') ? path : `/${path}`;
  return `${base}${clean}`;
}

export class AuthClient {
  private baseURL: string;
  private inflight: Promise<void> | null = null;

  constructor(opts: { baseURL: string }) {
    this.baseURL = opts.baseURL.replace(/\/$/, '');
  }

  private isAuthPath(input: string): boolean {
    const absolute = joinUrl(this.baseURL, input);
    const escaped = this.baseURL.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&');
    const path = absolute.replace(new RegExp(`^${escaped}`), '');
    return /^\/?auth\//i.test(path);
  }

  async login(body: LoginRequest): Promise<TokenResponse> {
    const res = await fetch(authApiUrl('/auth/login'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error(`LOGIN_FAILED_${res.status}`);
    const data = (await res.json()) as TokenResponse;
    tokenStore.save({
      accessToken: data.accessToken,
      expiresIn: data.expiresIn,
      refreshToken: data.refreshToken,
      refreshExpiresIn: data.refreshExpiresIn,
    });
    return data;
  }

  private async refresh(): Promise<void> {
    const rt = tokenStore.refresh;
    if (!rt) throw new Error('NO_REFRESH_TOKEN');

    const res = await fetch(authApiUrl('/auth/refresh'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt }),
    });

    if (!res.ok) {
      tokenStore.clear();
      throw new Error(`REFRESH_FAILED_${res.status}`);
    }

    const data = (await res.json()) as TokenResponse;
    tokenStore.save({
      accessToken: data.accessToken,
      expiresIn: data.expiresIn,
      refreshToken: data.refreshToken,
      refreshExpiresIn: data.refreshExpiresIn,
    });
  }

  private async ensureValidAccess(): Promise<void> {
    const now = Date.now();
    if (tokenStore.access && tokenStore.exp && tokenStore.exp - now > 5000) return;
    if (!tokenStore.refresh) {
      tokenStore.clear();
      throw new Error('NO_TOKENS');
    }
    if (!this.inflight) {
      this.inflight = (async () => {
        try {
          await this.refresh();
        } finally {
          this.inflight = null;
        }
      })();
    }
    return this.inflight;
  }

  async fetch(input: string, init?: RequestInit): Promise<Response> {
    const url = joinUrl(this.baseURL, input);
    const skipAuto = this.isAuthPath(input);

    if (!skipAuto) {
      await this.ensureValidAccess().catch(() => {
        // sin sesión válida: dejamos que el 401 fluya si el endpoint la requiere
      });
    }

    const headers = new Headers(init?.headers || {});
    const at = tokenStore.access;
    if (at && !skipAuto) headers.set('Authorization', `Bearer ${at}`);

    let res = await fetch(url, { ...init, headers });

    if (!skipAuto && res.status === 401 && tokenStore.refresh) {
      try {
        await this.ensureValidAccess(); // fuerza refresh
        const h2 = new Headers(init?.headers || {});
        const at2 = tokenStore.access;
        if (at2) h2.set('Authorization', `Bearer ${at2}`);
        res = await fetch(url, { ...init, headers: h2 });
      } catch {
        tokenStore.clear();
      }
    }
    return res;
  }

  async logout(): Promise<void> {
    const rt = tokenStore.refresh;
    tokenStore.clear();
    if (!rt) return;
    try {
      await fetch(authApiUrl('/auth/logout'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: rt }),
      });
    } catch { /* silenciar */ }
  }

  async logoutAll(): Promise<void> {
    const rt = tokenStore.refresh;
    tokenStore.clear();
    if (!rt) return;
    try {
      await fetch(authApiUrl('/auth/logout-all'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: rt }),
      });
    } catch { /* silenciar */ }
  }
}

/* ========= Helpers y cliente por defecto (para usar en componentes) ========= */

// Cliente por defecto para ms-auth
export const defaultAuthClient = new AuthClient({ baseURL: import.meta.env.VITE_API_AUTH_URL });

// fetch con auto-attach de Authorization y auto-refresh
export function authFetch(input: string, init?: RequestInit): Promise<Response> {
  return defaultAuthClient.fetch(input, init);
}

export const login = (body: LoginRequest) => defaultAuthClient.login(body);
export const logout = () => defaultAuthClient.logout();
export const logoutAll = () => defaultAuthClient.logoutAll();
