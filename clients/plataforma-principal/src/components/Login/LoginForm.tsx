import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FiMail, FiLock, FiEye, FiEyeOff } from 'react-icons/fi';
import './LoginForm.scss';

type LoginRequest = { email: string; password: string };
type LoginResponse = { token: string; refreshToken?: string };

type Props = {
  onSuccess?: (data: LoginResponse) => void;
  onGoRegister?: (e: React.MouseEvent<HTMLAnchorElement>) => void;
};

export default function LoginForm({ onSuccess, onGoRegister }: Props) {
  const { t } = useTranslation('LoginForm');

  const API = (import.meta.env.VITE_API_BASE_URL as string) ?? '';
  const endpoint = `${API.replace(/\/$/, '')}/auth/login`;

  const [form, setForm] = useState<LoginRequest>({ email: '', password: '' });
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;
    setErrorMsg(null);
    setLoading(true);
    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      });
      if (!res.ok) {
        setErrorMsg(`Error ${res.status}`);
        return;
      }
      const data: LoginResponse = await res.json();
      localStorage.setItem('access_token', data.token);
      if (data.refreshToken) localStorage.setItem('refresh_token', data.refreshToken);
      onSuccess?.(data);
      setForm({ email: '', password: '' });
    } catch {
      setErrorMsg(t('errorConnecting'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="login-card" onSubmit={handleSubmit} noValidate>
      <fieldset className="fieldset" disabled={loading}>
        <div className="pill-input">
          <span className="left-icon" aria-hidden>
            <FiMail />
          </span>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            placeholder={t('emailPlaceholder')}
            value={form.email}
            onChange={handleChange}
            required
            aria-invalid={!!errorMsg}
          />
        </div>

        <div className="pill-input">
          <span className="left-icon" aria-hidden>
            <FiLock />
          </span>
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
            aria-invalid={!!errorMsg}
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
          <p role="alert" aria-live="polite" className="error">
            {errorMsg}
          </p>
        )}

        <button type="submit" className="primary-button" disabled={loading}>
          {loading ? t('loggingIn') : t('login')}
        </button>

        <p className="signup-text">
          {t('dontHaveAccount')}{' '}
          <a
            href="#register"
            className="signup-link"
            onClick={(e) => {
              e.preventDefault();
              onGoRegister?.(e);
            }}
          >
            {t('signUp')}
          </a>
        </p>
      </fieldset>
    </form>
  );
}
