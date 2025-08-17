import { authApiUrl } from '../config';

export async function forgotPassword(email: string): Promise<boolean> {
  const res = await fetch(authApiUrl('/auth/forgot-password'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });
  return res.status === 204;
}

export async function resetPassword(token: string, newPassword: string): Promise<boolean> {
  const res = await fetch(authApiUrl('/auth/reset-password'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword }),
  });
  return res.status === 204;
}
