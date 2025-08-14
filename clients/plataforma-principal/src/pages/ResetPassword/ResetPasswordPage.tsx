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

  const clientError = useMemo(() => {
    if (!token) return t('errors.missingToken');
    if (pwd.length < 8) return t('errors.shortPassword');
    if (pwd !== pwd2) return t('errors.passwordsDontMatch');
    return null;
  }, [token, pwd, pwd2, t]);

  const canSubmit = !!token && !loading && !clientError;

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
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
            required
            aria-invalid={!!(clientError && pwd.length < 8)}
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
            required
            aria-invalid={!!(clientError && pwd !== pwd2)}
          />
        </div>

        {errorMsg || clientError ? (
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
