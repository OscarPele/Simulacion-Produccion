// src/pages/MainPage/components/ComponentePrueba/ComponentePrueba.tsx
import { useEffect, useMemo, useState } from 'react';
import './ComponentePrueba.scss';
import { tokenStore } from '../../api/token/tokenStore';
import { hrApiUrl, productionApiUrl, API_HR_BASE, API_PRODUCTION_BASE } from '../../api/config';

// Logs de diagnóstico (.env normalizado desde config)
console.log('[ComponentePrueba] BASES:', {
  API_HR_BASE,
  API_PRODUCTION_BASE,
});

// Hook para leer y mantenerse sincronizado con tokenStore
function useAccessToken() {
  const [token, setToken] = useState<string>('');

  useEffect(() => {
    try { tokenStore.load?.(); } catch {}
    const push = () => setToken(tokenStore.access || '');
    push(); // primera lectura
    const unsub = tokenStore.subscribe(push);
    return () => { try { unsub?.(); } catch {} };
  }, []);

  return token;
}

type FetchResult = { ok: boolean; status: number; data: unknown; error?: string };

async function getJSON(url: string, bearer?: string): Promise<FetchResult> {
  try {
    const auth = bearer
      ? (bearer.startsWith('Bearer ') ? bearer : `Bearer ${bearer}`)
      : undefined;

    const res = await fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
        ...(auth ? { Authorization: auth } : {}),
      },
      // sin credentials para comportarnos como tus helpers por defecto (same-origin)
    });
    const ct = res.headers.get('content-type') || '';
    const data = ct.includes('application/json') ? await res.json() : await res.text();
    return { ok: res.ok, status: res.status, data };
  } catch (e: any) {
    return { ok: false, status: 0, data: null, error: e?.message || 'network error' };
  }
}

export default function ComponentePrueba() {
  const token = useAccessToken();
  const [busy, setBusy] = useState(false);

  // Usa los helpers, igual que en el cliente real
  const hrPublicUrl   = useMemo(() => hrApiUrl('/api/hr/public/ping'), []);
  const hrSecureUrl   = useMemo(() => hrApiUrl('/api/hr/secure/me'), []);
  const prodPublicUrl = useMemo(() => productionApiUrl('/api/production/public/ping'), []);
  const prodSecureUrl = useMemo(() => productionApiUrl('/api/production/secure/me'), []);

  // Si las bases están vacías, deshabilitamos llamadas (evitamos pegarle al origin)
  const canHR   = !!API_HR_BASE;
  const canProd = !!API_PRODUCTION_BASE;

  console.log('[ComponentePrueba] Endpoints:', { hrPublicUrl, hrSecureUrl, prodPublicUrl, prodSecureUrl });
  console.log('[ComponentePrueba] Token length:', token ? token.length : 0);

  const [hrPub, setHrPub]     = useState<FetchResult | null>(null);
  const [hrSec, setHrSec]     = useState<FetchResult | null>(null);
  const [prodPub, setProdPub] = useState<FetchResult | null>(null);
  const [prodSec, setProdSec] = useState<FetchResult | null>(null);

  const callHrPublic = async () => {
    if (!canHR) return setHrPub({ ok: false, status: 0, data: null, error: 'VITE_API_HR_URL no definida' });
    setHrPub(await getJSON(hrPublicUrl));
  };
  const callHrSecure = async () => {
    if (!canHR) return setHrSec({ ok: false, status: 0, data: null, error: 'VITE_API_HR_URL no definida' });
    setHrSec(await getJSON(hrSecureUrl, token));
  };
  const callProdPublic = async () => {
    if (!canProd) return setProdPub({ ok: false, status: 0, data: null, error: 'VITE_API_PRODUCTION_URL no definida' });
    setProdPub(await getJSON(prodPublicUrl));
  };
  const callProdSecure = async () => {
    if (!canProd) return setProdSec({ ok: false, status: 0, data: null, error: 'VITE_API_PRODUCTION_URL no definida' });
    setProdSec(await getJSON(prodSecureUrl, token));
  };

  const runAll = async () => {
    setBusy(true);
    try {
      const a = canHR   ? await getJSON(hrPublicUrl)            : { ok: false, status: 0, data: null, error: 'VITE_API_HR_URL no definida' };
      const b = canHR   ? await getJSON(hrSecureUrl, token)     : { ok: false, status: 0, data: null, error: 'VITE_API_HR_URL no definida' };
      const c = canProd ? await getJSON(prodPublicUrl)          : { ok: false, status: 0, data: null, error: 'VITE_API_PRODUCTION_URL no definida' };
      const d = canProd ? await getJSON(prodSecureUrl, token)   : { ok: false, status: 0, data: null, error: 'VITE_API_PRODUCTION_URL no definida' };
      setHrPub(a); setHrSec(b); setProdPub(c); setProdSec(d);
    } finally {
      setBusy(false);
    }
  };

  const tokenInfo = token ? `••• (${token.length} chars)` : '(vacío)';

  return (
    <div className="componente-prueba card">
      <h2 className="cp-title">Comprobación — ms-hr / ms-production</h2>
      <p className="cp-subtitle">
        Usa <code>VITE_API_HR_URL</code> y <code>VITE_API_PRODUCTION_URL</code> de tu <code>.env</code>, y el token del cliente.
        Revisa la consola para ver los valores cargados.
      </p>

      <div className="cp-grid">
        <div className="field">
          <label className="label">API_HR_BASE</label>
          <input className="input" value={API_HR_BASE} readOnly />
        </div>

        <div className="field">
          <label className="label">API_PRODUCTION_BASE</label>
          <input className="input" value={API_PRODUCTION_BASE} readOnly />
        </div>

        <div className="field">
          <label className="label">Token (solo info)</label>
          <input className="input" value={tokenInfo} readOnly />
        </div>

        <div className="actions">
          <button className="button" onClick={callHrPublic} disabled={!canHR}>HR público</button>
          <button className="button blue" onClick={callHrSecure} disabled={!canHR || !token}>HR protegido</button>
          <button className="button" onClick={callProdPublic} disabled={!canProd}>Production público</button>
          <button className="button blue" onClick={callProdSecure} disabled={!canProd || !token}>Production protegido</button>
          <button className="primary-button" onClick={runAll} disabled={busy || !canHR || !canProd || !token}>
            {busy ? 'Probando…' : 'Probar todo'}
          </button>
        </div>
      </div>

      <Resultado titulo="HR /public/ping" url={hrPublicUrl} result={hrPub} />
      <Resultado titulo="HR /secure/me" url={hrSecureUrl} result={hrSec} />
      <Resultado titulo="Production /public/ping" url={prodPublicUrl} result={prodPub} />
      <Resultado titulo="Production /secure/me" url={prodSecureUrl} result={prodSec} />
    </div>
  );
}

function Resultado({ titulo, url, result }: { titulo: string; url: string; result: FetchResult | null }) {
  if (!result) return null;
  return (
    <div className="cp-result">
      <div className="cp-result-title">{titulo}</div>
      <code className="cp-code">{url}</code>
      <pre className="cp-pre">
{JSON.stringify({
  ok: result.ok,
  status: result.status,
  ...(result.error ? { error: result.error } : {}),
  data: result.data,
}, null, 2)}
      </pre>
    </div>
  );
}
