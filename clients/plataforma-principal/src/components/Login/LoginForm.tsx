import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FiUser, FiLock, FiEye, FiEyeOff } from 'react-icons/fi';
import './LoginForm.scss';
import { useNavigate } from 'react-router-dom';

type LoginRequest = { usernameOrEmail: string; password: string };
type LoginResponse = { token: string; refreshToken?: string };

type Props = {
  onSuccess?: (data: LoginResponse) => void;
  onGoRegister?: (e: React.MouseEvent<HTMLAnchorElement>) => void;
};

export default function LoginForm({ onSuccess, onGoRegister }: Props) {
  const { t } = useTranslation('LoginForm');

  const API = (import.meta.env.VITE_API_BASE_URL as string) ?? '';
  const endpoint = `${API.replace(/\/$/, '').trim()}/auth/login`;
  const navigate = useNavigate();

  const [form, setForm] = useState<LoginRequest>({ usernameOrEmail: '', password: '' });
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const readBodySafely = async (res: Response): Promise<any> => {
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
      try { return await res.json(); } catch { return null; }
    }
    try { return await res.text(); } catch { return null; }
  };

  const pickFirstFieldError = (obj: any): string | null => {
    if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return null;
    const values = Object.values(obj);
    if (values.length === 0) return null;
    const first = values[0];
    return typeof first === 'string' ? first : null;
  };

  // Solo depende del campo que valida
  const clientError = useMemo(() => {
    if (!form.usernameOrEmail.trim()) {
      return t('usernameOrEmailRequired') || 'Username or email is required';
    }
    return null;
  }, [form.usernameOrEmail, t]);

  const canSubmit = useMemo(() => !clientError && !loading, [clientError, loading]);

  function saveTokens(data: LoginResponse) {
    localStorage.setItem('accessToken', data.token);
    if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
  }

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

      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const body = await readBodySafely(res);

      if (!res.ok) {
        const code = (body && body.code) || '';
        const message = (body && body.message) || '';
        const errorName = (body && body.error) || '';

        const isInvalid =
          res.status === 401 ||
          code === 'INVALID_CREDENTIALS' ||
          message === 'INVALID_CREDENTIALS' ||
          errorName === 'InvalidCredentialsException';

        if (isInvalid) {
          setErrorMsg(t('invalidCredentials') || 'Invalid credentials');
          return;
        }

        if (res.status === 400) {
          const firstFieldError = pickFirstFieldError(body);
          if (firstFieldError) {
            setErrorMsg(firstFieldError);
            return;
          }
        }

        if (res.status >= 500) {
          setErrorMsg(t('serverError') || `Server error (${res.status}) - please try again later`);
        } else {
          setErrorMsg(t('errorWithCode', { code: res.status }));
        }
        return;
      }

      const data: LoginResponse = body;
      saveTokens(data);

      onSuccess?.(data);
      setForm({ usernameOrEmail: '', password: '' });
      setErrorMsg(null); // limpiar posibles mensajes antes de navegar
      navigate('/main');
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
            <FiUser />
          </span>
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
          <p role="alert" aria-live="polite" className="error">
            {errorMsg}
          </p>
        )}

        <button type="submit" className="primary-button" disabled={!canSubmit}>
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
