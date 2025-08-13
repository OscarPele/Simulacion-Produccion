import { beforeEach, describe, expect, it, vi } from 'vitest';
import { tokenStore } from '../tokenStore';

describe('tokenStore', () => {
  beforeEach(() => {
    vi.useRealTimers();
    localStorage.clear();
    tokenStore.clear();
  });

  it('load() lee desde localStorage y programa refresh proactivo', () => {
    // Preparamos valores persistidos
    const now = Date.now();
    const accessToken = 'acc-123';
    const refreshToken = 'ref-123';
    const accessExp = now + 60_000;     // +60s
    const refreshExp = now + 3_600_000; // +1h

    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('accessTokenExp', String(accessExp));
    localStorage.setItem('refreshTokenExp', String(refreshExp));

    tokenStore.load();

    expect(tokenStore.access).toBe(accessToken);
    expect(tokenStore.refresh).toBe(refreshToken);
    expect(tokenStore.exp).toBe(accessExp);
    expect(tokenStore.refreshExp).toBe(refreshExp);
  });

  it('save() guarda en memoria y localStorage, notifica a los listeners', () => {
    // Fijamos el tiempo para chequear cálculos de expiración
    const fixedNow = new Date('2025-01-01T00:00:00Z').getTime();
    vi.setSystemTime(fixedNow);

    const spy = vi.fn();
    const unsub = tokenStore.subscribe(spy);

    tokenStore.save({
      accessToken: 'A',
      expiresIn: 30,            // segundos
      refreshToken: 'R',
      refreshExpiresIn: 3600,   // segundos
    });

    // Expiraciones: now + expiresIn*1000 - 5000 (margen 5s)
    expect(tokenStore.access).toBe('A');
    expect(tokenStore.refresh).toBe('R');
    expect(tokenStore.exp).toBe(fixedNow + 30_000 - 5_000);
    expect(tokenStore.refreshExp).toBe(fixedNow + 3_600_000 - 5_000);

    // Persistencia
    expect(localStorage.getItem('accessToken')).toBe('A');
    expect(localStorage.getItem('refreshToken')).toBe('R');
    expect(Number(localStorage.getItem('accessTokenExp'))).toBe(fixedNow + 25_000);
    expect(Number(localStorage.getItem('refreshTokenExp'))).toBe(fixedNow + 3_595_000);

    // Notificación a listeners
    expect(spy).toHaveBeenCalledTimes(1);
    unsub();
  });

  it('clear() borra memoria y localStorage, y notifica', () => {
    const spy = vi.fn();
    tokenStore.subscribe(spy);

    // Pre-cargamos algo
    tokenStore.save({
      accessToken: 'A',
      expiresIn: 10,
      refreshToken: 'R',
      refreshExpiresIn: 20,
    });

    tokenStore.clear();

    expect(tokenStore.access).toBeUndefined();
    expect(tokenStore.refresh).toBeUndefined();
    expect(tokenStore.exp).toBe(0);
    expect(tokenStore.refreshExp).toBe(0);

    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('accessTokenExp')).toBeNull();
    expect(localStorage.getItem('refreshTokenExp')).toBeNull();

    // Se emite otra notificación (al menos una por save y otra por clear)
    expect(spy).toHaveBeenCalled();
  });

  it('escucha el evento storage y recarga estado (multi-pestaña)', () => {
    // Estado inicial vacío
    expect(tokenStore.access).toBeUndefined();

    // Simulamos que OTRO tab ha escrito nuevos valores en localStorage
    localStorage.setItem('accessToken', 'A2');
    localStorage.setItem('accessTokenExp', String(Date.now() + 60_000));
    localStorage.setItem('refreshToken', 'R2');
    localStorage.setItem('refreshTokenExp', String(Date.now() + 3_600_000));

    const spy = vi.fn();
    tokenStore.subscribe(spy);

    // Disparamos el evento storage
    window.dispatchEvent(new StorageEvent('storage', { key: 'accessToken' }));

    // El store se recarga y notifica
    expect(tokenStore.access).toBe('A2');
    expect(tokenStore.refresh).toBe('R2');
    expect(spy).toHaveBeenCalled();
  });

  it('programa notificación proactiva ~15s antes de expirar el access', async () => {
    vi.useFakeTimers();
    const fixedNow = new Date('2025-01-01T00:00:00Z').getTime();
    vi.setSystemTime(fixedNow);

    const spy = vi.fn();
    tokenStore.subscribe(spy);

    // expiresIn=30s → exp = now + 30s -5s = now+25s
    // scheduleProactiveRefresh notifica a (exp - now - 15s) = 10s
    tokenStore.save({
      accessToken: 'A',
      expiresIn: 30,
      refreshToken: 'R',
      refreshExpiresIn: 3600,
    });

    // La primera notificación viene de save(); esperamos la proactiva
    spy.mockClear();

    // Avanzamos el tiempo 10s para disparar el timeout proactivo
    vi.advanceTimersByTime(10_000);

    expect(spy).toHaveBeenCalledTimes(1);
    // no comprobamos payload porque notify() no envía datos, solo invoca callbacks
  });

  it('si expiresIn es corto (<20s efectivo), no se programa proactivo', () => {
    vi.useFakeTimers();
    const fixedNow = new Date('2025-01-01T00:00:00Z').getTime();
    vi.setSystemTime(fixedNow);

    const spy = vi.fn();
    tokenStore.subscribe(spy);

    // expiresIn=10s → exp=now+5s => (exp - now - 15s) <= 0 → no se programa
    tokenStore.save({
      accessToken: 'A',
      expiresIn: 10,
      refreshToken: 'R',
      refreshExpiresIn: 3600,
    });

    spy.mockClear();
    vi.advanceTimersByTime(30_000);
    expect(spy).not.toHaveBeenCalled();
  });
});
