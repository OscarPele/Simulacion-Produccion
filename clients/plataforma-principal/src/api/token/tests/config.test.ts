import { describe, it, expect, vi, beforeEach } from 'vitest';

// helper para importar siempre el módulo fresco tras cambiar env
const importConfig = async () => await import('../../config');

describe('config.ts', () => {
  beforeEach(() => {
    vi.resetModules(); // obliga a reevaluar el módulo y recalcular API_BASE
  });

  it('API_BASE normaliza espacios y quita la barra final', async () => {
    vi.stubEnv('VITE_API_BASE_URL', ' https://api.example.com/ ');
    const { API_BASE } = await importConfig();
    expect(API_BASE).toBe('https://api.example.com');
  });

  it('API_BASE es "" cuando no hay env', async () => {
    vi.stubEnv('VITE_API_BASE_URL', '');
    const { API_BASE } = await importConfig();
    expect(API_BASE).toBe('');
  });

  it('apiUrl no altera URLs absolutas', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com/');
    const { apiUrl } = await importConfig();
    expect(apiUrl('http://other/ok')).toBe('http://other/ok');
    expect(apiUrl('https://other/ok')).toBe('https://other/ok');
  });

  it('apiUrl compone con base y maneja slash inicial', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com/');
    const { apiUrl } = await importConfig();
    expect(apiUrl('/users')).toBe('https://api.example.com/users');
    expect(apiUrl('users')).toBe('https://api.example.com/users');
  });
});
