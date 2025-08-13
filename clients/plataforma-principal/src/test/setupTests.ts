import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { setupServer } from 'msw/node';

// Exporta un server MSW reutilizable en tests
export const server = setupServer();
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

// Pequeña ayuda para tiempo
export const sleep = (ms: number) => new Promise(res => setTimeout(res, ms));

// Stub del ENV si hiciera falta (authClient permite inyectar baseURL, así que normalmente no hará falta)
(globalThis as any).importMeta = { env: { VITE_API_BASE_URL: 'http://localhost' } };

// Polyfill fetch en Node (jsdom no lo trae por defecto)
import 'whatwg-fetch';

// Espía fecha si necesitas controlar “ahora”
vi.useRealTimers();
