// Configuración de las URLs base para las APIs, obtenidas de las variables de entorno
// y normalizadas para evitar errores de concatenación de rutas.

// --- utilidades comunes ---
const normalizeBase = (raw?: string) =>
  raw ? raw.trim().replace(/\/+$/, '') : '';

const isAbsoluteUrl = (p: string) => /^https?:\/\//i.test(p);

const mkApiUrl = (base: string) => (path: string) => {
  if (isAbsoluteUrl(path)) return path;
  const clean = path.startsWith('/') ? path : `/${path}`;
  return `${base}${clean}`;
};


// --- bases desde .env (mismo comportamiento que antes: '' si falta) ---
export const API_AUTH_BASE        = normalizeBase(import.meta.env.VITE_API_AUTH_URL as string);
export const API_HR_BASE          = normalizeBase(import.meta.env.VITE_API_HR_URL as string);
export const API_PRODUCTION_BASE  = normalizeBase(import.meta.env.VITE_API_PRODUCTION_URL as string);

// --- helpers sin duplicación ---
export const authApiUrl        = mkApiUrl(API_AUTH_BASE);
export const hrApiUrl          = mkApiUrl(API_HR_BASE);
export const productionApiUrl  = mkApiUrl(API_PRODUCTION_BASE);