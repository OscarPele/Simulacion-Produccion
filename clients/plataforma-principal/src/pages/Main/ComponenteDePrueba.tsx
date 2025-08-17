import { useEffect, useState } from 'react';
import { FiCheckCircle, FiAlertTriangle } from 'react-icons/fi';
import { authApiUrl } from '../../api/config';
import { authFetch } from '../../api/token/authClient';

export default function ComponenteDePrueba() {
  const [status, setStatus] = useState<'idle' | 'loading' | 'ok' | 'error'>('idle');
  const [payload, setPayload] = useState<any>(null);
  const [error, setError] = useState<string>('');

  async function probe() {
    try {
      setStatus('loading');
      setError('');
      setPayload(null);

      const res = await authFetch(authApiUrl('/test/protected'), { method: 'GET' });

      if (!res.ok) {
        const body = await res.text().catch(() => '');
        setError(`HTTP ${res.status} ${res.statusText}${body ? ` — ${body}` : ''}`);
        setStatus('error');
        return;
      }

      const json = await res.json().catch(() => ({}));
      setPayload(json);
      setStatus('ok');
    } catch (e: any) {
      setError(String(e?.message || e));
      setStatus('error');
    }
  }

  useEffect(() => { probe(); }, []);

  const ok = status === 'ok';
  const loading = status === 'loading';
  const variant = loading ? 'warning' : ok ? 'success' : 'warning';

  return (
    <div className={`auth-flag ${variant}`} style={{ marginTop: 16 }}>
      <div className="auth-flag__icon">
        {ok ? <FiCheckCircle /> : <FiAlertTriangle />}
      </div>
      <div className="auth-flag__body">
        <h2>Probar endpoint protegido</h2>
        {loading && <p>Comprobando…</p>}
        {!loading && ok && (
          <p>
            Respuesta:&nbsp;
            <code>{JSON.stringify(payload)}</code>
          </p>
        )}
        {!loading && !ok && <p>Error: {error || 'Token ausente o expirado.'}</p>}
        <button className="btn" onClick={probe} style={{ marginTop: 8 }}>
          Reintentar
        </button>
      </div>
    </div>
  );
}
