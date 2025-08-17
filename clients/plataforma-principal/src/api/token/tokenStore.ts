
export type Tokens = {
  accessToken: string;
  accessTokenExp: number; // epoch ms
  refreshToken: string;
  refreshTokenExp?: number;
};

type SaveInput = {
  accessToken: string;
  expiresIn: number;           
  refreshToken: string;
  refreshExpiresIn?: number;   
};

const LS = {
  access: 'accessToken',
  accessExp: 'accessTokenExp',
  refresh: 'refreshToken',
  refreshExp: 'refreshTokenExp',
};

let _accessToken: string | undefined;
let _accessTokenExp = 0;
let _refreshToken: string | undefined;
let _refreshTokenExp = 0;

let refreshTimer: number | null = null;
const listeners = new Set<() => void>();

function notify() {
  listeners.forEach((fn) => {
    try { fn(); } catch {}
  });
}

function scheduleProactiveRefresh() {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer);
    refreshTimer = null;
  }
  if (!_accessToken || !_accessTokenExp) return;
  const now = Date.now();
  const skew = 15_000; // 15s antes de expirar
  const delay = Math.max(_accessTokenExp - now - skew, 0);
  // Evitar valores absurdos (si ya está expirado, no programamos)
  if (delay <= 0) return;
  refreshTimer = window.setTimeout(() => {
    // Solo notificamos; el cliente decidirá llamar a ensureValidAccess/refresh
    notify();
  }, delay);
}

function loadFromLS() {
  _accessToken = localStorage.getItem(LS.access) || undefined;
  _refreshToken = localStorage.getItem(LS.refresh) || undefined;
  _accessTokenExp = parseInt(localStorage.getItem(LS.accessExp) || '0', 10) || 0;
  _refreshTokenExp = parseInt(localStorage.getItem(LS.refreshExp) || '0', 10) || 0;
  scheduleProactiveRefresh();
}

function persist() {
  if (_accessToken) localStorage.setItem(LS.access, _accessToken);
  else localStorage.removeItem(LS.access);

  if (_refreshToken) localStorage.setItem(LS.refresh, _refreshToken);
  else localStorage.removeItem(LS.refresh);

  if (_accessTokenExp) localStorage.setItem(LS.accessExp, String(_accessTokenExp));
  else localStorage.removeItem(LS.accessExp);

  if (_refreshTokenExp) localStorage.setItem(LS.refreshExp, String(_refreshTokenExp));
  else localStorage.removeItem(LS.refreshExp);
}

export const tokenStore = {
  load(): void {
    loadFromLS();
  },

  save(input: SaveInput): void {
    const now = Date.now();
    _accessToken = input.accessToken;
    _accessTokenExp = now + input.expiresIn * 1000 - 5000; // 5s margen
    _refreshToken = input.refreshToken;
    if (input.refreshExpiresIn && input.refreshExpiresIn > 0) {
      _refreshTokenExp = now + input.refreshExpiresIn * 1000 - 5000;
    }
    persist();
    scheduleProactiveRefresh();
    notify();
  },

  clear(): void {
    _accessToken = undefined;
    _accessTokenExp = 0;
    _refreshToken = undefined;
    _refreshTokenExp = 0;
    persist();
    if (refreshTimer) {
      window.clearTimeout(refreshTimer);
      refreshTimer = null;
    }
    notify();
  },

  get access(): string | undefined { return _accessToken; },
  get exp(): number { return _accessTokenExp; },
  get refresh(): string | undefined { return _refreshToken; },
  get refreshExp(): number { return _refreshTokenExp; },

  subscribe(fn: () => void): () => void {
    listeners.add(fn);
    return () => listeners.delete(fn);
  },
};

// Sincronización multi-pestaña
window.addEventListener('storage', (e) => {
  if (!e.key || !Object.values(LS).includes(e.key)) return;
  loadFromLS();
  notify();
});
