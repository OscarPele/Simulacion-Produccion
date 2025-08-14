export const API_AUTH_BASE = (() => {
  const raw = import.meta.env.VITE_API_AUTH_URL as string;
  if (!raw) return '';
  return raw.trim().replace(/\/+$/, '');
})();

export const API_HR_BASE = (() => {
  const raw = import.meta.env.VITE_API_HR_URL as string;
  if (!raw) return '';
  return raw.trim().replace(/\/+$/, '');
})();

// Helpers para construir rutas completas
export function authApiUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) return path;
  const clean = path.startsWith('/') ? path : `/${path}`;
  return `${API_AUTH_BASE}${clean}`;
}

export function hrApiUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) return path;
  const clean = path.startsWith('/') ? path : `/${path}`;
  return `${API_HR_BASE}${clean}`;
}
