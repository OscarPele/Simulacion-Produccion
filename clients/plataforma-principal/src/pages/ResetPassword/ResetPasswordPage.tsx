import { useMemo, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { resetPassword } from '../../api/users/passwordResetClient';
import './ResetPasswordPage.scss';

export default function ResetPasswordPage() {
  const { t } = useTranslation('ResetPassword');
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const token = params.get('token')?.trim() || '';
  const [pwd, setPwd] = useState('');
  const [pwd2, setPwd2] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  // NUEVO: control de interacción del usuario
  const [touched, setTouched] = useState({ pwd: false, pwd2: false });
  const [submitted, setSubmitted] = useState(false);

  // Errores por campo (lógica igual, pero separada)
  const pwdError  = pwd.length < 8 ? t('errors.shortPassword') : null;
  const pwd2Error = pwd2 && pwd !== pwd2 ? t('errors.passwordsDontMatch') : null;

  // Error “global” de cliente (el que ya usabas para canSubmit)
  const clientError = useMemo(() => {
    if (!token) return t('errors.missingToken');
    return pwdError ?? pwd2Error ?? null;
  }, [token, pwdError, pwd2Error, t]);

  // Mantiene tu lógica de habilitar/deshabilitar submit
  const canSubmit = !!token && !loading && !clientError;

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);                 // NUEVO: marca intento de envío
    if (!canSubmit) return;

    setLoading(true);
    setError(null);
    try {
      const ok = await resetPassword(token, pwd);
      if (ok) setDone(true);
      else setError(t('errors.generic'));
    } catch {
      setError(t('errors.network'));
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="reset-wrapper">
        <div className="reset-card card">
          <h2 className="reset-title">{t('title')}</h2>
          <p className="error">{t('errors.missingToken')}</p>
          <div className="actions">
            <button onClick={() => navigate('/')} className="primary-button">
              {t('success.goLogin')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (done) {
    return (
      <div className="reset-wrapper">
        <div className="reset-card card">
          <h2 className="reset-title">{t('success.title')}</h2>
        <p className="reset-desc">{t('success.body')}</p>
          <div className="actions">
            <button onClick={() => navigate('/')} className="primary-button">
              {t('success.goLogin')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Mostrar mensajes solo si el usuario interactuó o intentó enviar
  const showErrors = submitted || touched.pwd || touched.pwd2;

  return (
    <div className="reset-wrapper">
      <form className="reset-card card" onSubmit={onSubmit} noValidate>
        <h2 className="reset-title">{t('title')}</h2>
        <p className="reset-desc">{t('subtitle') || ''}</p>

        <div className="field">
          <label htmlFor="newPwd" className="label">{t('fields.newPassword')}</label>
          <input
            id="newPwd"
            className="input"
            type="password"
            autoComplete="new-password"
            minLength={8}
            value={pwd}
            onChange={(e) => setPwd(e.target.value)}
            onBlur={() => setTouched(v => ({ ...v, pwd: true }))}    // NUEVO
            required
            aria-invalid={(submitted || touched.pwd) ? pwd.length < 8 : false}  // NUEVO
          />
        </div>

        <div className="field">
          <label htmlFor="newPwd2" className="label">{t('fields.repeatPassword')}</label>
          <input
            id="newPwd2"
            className="input"
            type="password"
            autoComplete="new-password"
            minLength={8}
            value={pwd2}
            onChange={(e) => setPwd2(e.target.value)}
            onBlur={() => setTouched(v => ({ ...v, pwd2: true }))}   // NUEVO
            required
            aria-invalid={(submitted || touched.pwd2) ? (pwd2.length > 0 && pwd !== pwd2) : false} // NUEVO
          />
        </div>

        {showErrors && (errorMsg || clientError) ? (
          <p role="alert" aria-live="polite" className="error">{errorMsg ?? clientError}</p>
        ) : null}

        <div className="actions">
          <button
            type="button"
            onClick={() => navigate('/')}
            disabled={loading}
            className="button"
          >
            {t('actions.cancel')}
          </button>
          <button type="submit" className="button blue" disabled={!canSubmit}>
            {loading ? t('states.saving') : t('actions.save')}
          </button>
        </div>
      </form>
    </div>
  );
}
