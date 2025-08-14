// src/components/Login/LoginForm.tsx
import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FiUser, FiLock, FiEye, FiEyeOff } from 'react-icons/fi';
import './LoginForm.scss';
import { useNavigate } from 'react-router-dom';
import { login, type TokenResponse } from '../../../../api/token/authClient';

type LoginRequest = { usernameOrEmail: string; password: string };

type Props = {
  onSuccess?: (data: TokenResponse) => void;
  onGoRegister?: (e: React.MouseEvent<HTMLAnchorElement>) => void;
};

export default function LoginForm({ onSuccess, onGoRegister }: Props) {
  const { t } = useTranslation('LoginForm');
  const navigate = useNavigate();

  const [form, setForm] = useState<LoginRequest>({ usernameOrEmail: '', password: '' });
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const clientError = useMemo(() => {
    if (!form.usernameOrEmail.trim()) {
      return t('usernameOrEmailRequired') || 'Username or email is required';
    }
    return null;
  }, [form.usernameOrEmail, t]);

  const canSubmit = useMemo(() => !clientError && !loading, [clientError, loading]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;
    if (clientError) {
      setErrorMsg(clientError);
      return;
    }

    setErrorMsg(null);
    setLoading(true);

    try {
      const payload: LoginRequest = {
        usernameOrEmail: form.usernameOrEmail.trim(),
        password: form.password,
      };

      // 👉 delega en authClient: guarda tokens y maneja expiraciones
      const data = await login(payload);

      onSuccess?.(data);
      setForm({ usernameOrEmail: '', password: '' });
      setErrorMsg(null);
      navigate('/main');
    } catch (err: any) {
      const msg = String(err?.message ?? '');
      if (msg.startsWith('LOGIN_FAILED_')) {
        // 401 u otro status de login
        if (msg.endsWith('_401')) {
          setErrorMsg(t('invalidCredentials') || 'Invalid credentials');
        } else if (msg.endsWith('_400')) {
          setErrorMsg(t('invalidForm') || t('errorWithCode', { code: 400 }));
        } else if (/_5\d\d$/.test(msg)) {
          setErrorMsg(t('serverError') || 'Server error — please try again later');
        } else {
          setErrorMsg(t('errorWithCode', { code: msg.split('_').pop() }));
        }
      } else {
        setErrorMsg(t('errorConnecting'));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="login-card" onSubmit={handleSubmit} noValidate>
      <fieldset className="fieldset" disabled={loading}>
        <div className="pill-input">
          <span className="left-icon" aria-hidden><FiUser /></span>
          <input
            id="usernameOrEmail"
            name="usernameOrEmail"
            type="text"
            autoComplete="username"
            placeholder={t('usernameOrEmailPlaceholder')}
            value={form.usernameOrEmail}
            onChange={handleChange}
            required
            aria-invalid={!!(errorMsg && !form.usernameOrEmail.trim())}
          />
        </div>

        <div className="pill-input">
          <span className="left-icon" aria-hidden><FiLock /></span>
          <input
            id="password"
            name="password"
            type={showPwd ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder={t('passwordPlaceholder')}
            value={form.password}
            onChange={handleChange}
            required
            minLength={8}
            aria-invalid={!!(errorMsg && form.password.length < 8)}
          />
          <button
            type="button"
            className="right-icon"
            onClick={() => setShowPwd((v) => !v)}
            aria-label={showPwd ? t('hidePassword') : t('showPassword')}
          >
            {showPwd ? <FiEyeOff /> : <FiEye />}
          </button>
        </div>

        {errorMsg && (
          <p role="alert" aria-live="polite" className="error">{errorMsg}</p>
        )}

        <button type="submit" className="primary-button" disabled={!canSubmit}>
          {loading ? t('loggingIn') : t('login')}
        </button>

        <p className="signup-text">
          {t('dontHaveAccount')}{' '}
          <a
            href="#register"
            className="signup-link"
            onClick={(e) => { e.preventDefault(); onGoRegister?.(e); }}
          >
            {t('signUp')}
          </a>
        </p>
      </fieldset>
    </form>
  );
}
