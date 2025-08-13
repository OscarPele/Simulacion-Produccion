// src/api/users/userClient.ts
import { apiUrl } from '../config';
import { authFetch } from '../token/authClient';

export type ChangePasswordResult =
  | { ok: true }
  | { ok: false; status: number; code?: string };

export async function changePassword(
  userId: number,
  currentPassword: string,
  newPassword: string
): Promise<ChangePasswordResult> {
  try {
    const res = await authFetch(apiUrl(`/users/${userId}/password`), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ currentPassword, newPassword }),
    });

    if (res.status === 204) return { ok: true };

    // Intentamos leer el cuerpo (json o texto) una sola vez
    let code: string | undefined;
    let parsedBody: any = null;
    let textBody: string | undefined;
    try {
      const ct = res.headers.get('content-type') || '';
      if (ct.includes('application/json')) {
        parsedBody = await res.json();
        code = parsedBody?.code;
      } else {
        textBody = await res.text();
      }
    } catch (parseErr) {
      // Si el cuerpo no se puede parsear, lo anotamos en debug
      console.warn('[userClient.changePassword] No se pudo parsear el cuerpo de la respuesta:', parseErr);
    }

    // Logs específicos para 5xx
    if (res.status >= 500) {
      const requestId =
        res.headers.get('x-request-id') ||
        res.headers.get('x-correlation-id') ||
        undefined;

      console.error('[userClient.changePassword] Respuesta 5xx del servidor', {
        status: res.status,
        requestId,
        code,                // si el backend envía { code: '...' }
        body: parsedBody ?? textBody, // cuerpo útil para diagnóstico
      });
    }

    return { ok: false, status: res.status, code };
  } catch (err) {
    // Error de red / fetch abortado / CORS, etc.
    console.error('[userClient.changePassword] Error de red ejecutando la petición:', err);
    throw err;
  }
}
