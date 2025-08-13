export const API_BASE = (() => {
  const raw = (import.meta.env.VITE_API_BASE_URL as string) ?? '';
  const trimmed = raw.trim();
  if (!trimmed) return '';
  return trimmed.replace(/\/+$/, '');
})();

export function apiUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) return path;
  const clean = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE}${clean}`;
}
