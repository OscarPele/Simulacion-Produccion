import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FiUser, FiMail, FiLock, FiEye, FiEyeOff } from 'react-icons/fi';
import '../Login/LoginForm.scss';
import { authApiUrl } from '../../../../api/config';

type RegisterRequest = { username: string; email: string; password: string };

type Props = {
  onGoLogin?: (e?: React.MouseEvent<HTMLAnchorElement>) => void;
};

export default function RegisterForm({ onGoLogin }: Props) {
  const { t } = useTranslation('RegisterForm');

  const registerEndpoint = authApiUrl('/auth/register');
  const resendEndpoint   = authApiUrl('/auth/verify-email/request');

  const [form, setForm] = useState<RegisterRequest>({ username: '', email: '', password: '' });
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [lastRegisteredEmail, setLastRegisteredEmail] = useState<string | null>(null);
  const [resendInfo, setResendInfo] = useState<string | null>(null);
  const [resendLoading, setResendLoading] = useState(false);

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

  const clientError = useMemo(() => {
    if (!form.username.trim()) return t('usernameRequired') || 'El nombre de usuario es obligatorio';
    if (!form.email.trim()) return t('emailRequired') || 'El email es obligatorio';
    if (form.password.trim().length < 8) return t('passwordTooShort') || 'La contraseña debe tener al menos 8 caracteres';
    return null;
  }, [form.username, form.email, form.password, t]);

  const canSubmit = useMemo(() => !clientError && !loading, [clientError, loading]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;

    if (clientError) {
      setErrorMsg(clientError);
      return;
    }

    setLoading(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    setResendInfo(null);

    try {
      const payload: RegisterRequest = {
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
      };

      const res = await fetch(registerEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(payload),
      });

      const body = await readBodySafely(res);

      if (!res.ok) {
        const code = body?.code ?? '';

        if (res.status === 400) {
          if (code === 'USERNAME_EXISTS') { setErrorMsg(t('usernameTaken') || 'El nombre de usuario ya existe'); return; }
          if (code === 'EMAIL_EXISTS')    { setErrorMsg(t('emailTaken') || 'El email ya está registrado'); return; }
          if (code === 'VALIDATION_ERROR'){ setErrorMsg(t('invalidForm') || 'Formulario inválido'); return; }
        }

        if (res.status >= 500) {
          setErrorMsg(t('serverError') || `Server error (${res.status}) — please try again later`);
          return;
        }

        setErrorMsg(t('errorWithCode', { code: res.status }) || `Error ${res.status}`);
        return;
      }

      // Guardamos email para "Reenviar verificación"
      setLastRegisteredEmail(payload.email);
      // Limpiamos form
      setForm({ username: '', email: '', password: '' });

      // Mensaje claro de verificación
      setSuccessMsg(
        t('verifyEmailMessage') ||
          '¡Registro completado! Te hemos enviado un correo para verificar tu cuenta. Revisa tu bandeja y SPAM.'
      );
    } catch {
      setErrorMsg(t('errorConnecting') || 'Error de conexión');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!lastRegisteredEmail || resendLoading) return;
    setResendLoading(true);
    setResendInfo(null);
    try {
      await fetch(resendEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email: lastRegisteredEmail }),
      });
      setResendInfo(
        t('verificationResent') ||
          'Si el correo existe y no está verificado, hemos reenviado el email de verificación.'
      );
    } catch {
      setResendInfo(t('errorConnecting') || 'Error de conexión');
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <form className="login-card" onSubmit={handleSubmit} noValidate>
      <fieldset className="fieldset" disabled={loading}>
        <div className="pill-input">
          <span className="left-icon" aria-hidden><FiUser /></span>
          <input
            id="username"
            name="username"
            type="text"
            autoComplete="username"
            placeholder={t('usernamePlaceholder')}
            required
            minLength={3}
            value={form.username}
            onChange={handleChange}
            aria-invalid={!!(errorMsg && !form.username.trim())}
          />
        </div>

        <div className="pill-input">
          <span className="left-icon" aria-hidden><FiMail /></span>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            placeholder={t('emailPlaceholder')}
            required
            value={form.email}
            onChange={handleChange}
            aria-invalid={!!(errorMsg && !form.email.trim())}
          />
        </div>

        <div className="pill-input">
          <span className="left-icon" aria-hidden><FiLock /></span>
          <input
            id="password"
            name="password"
            type={showPwd ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder={t('passwordPlaceholder')}
            required
            minLength={8}
            value={form.password}
            onChange={handleChange}
            aria-invalid={!!(errorMsg && form.password.trim().length < 8)}
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

        <small className="signup-text">{t('passwordHint')}</small>

        {errorMsg && <p role="alert" aria-live="polite" className="error">{errorMsg}</p>}

        {successMsg && (
          <div className="success-block" role="status" aria-live="polite">
            <p className="success-title">{successMsg}</p>
            {lastRegisteredEmail && (
              <>
                <p className="success-hint">
                  {t('verifyEmailHint', { email: lastRegisteredEmail }) || `Enviado a: ${lastRegisteredEmail}. Si no lo ves, revisa SPAM.`}
                </p>
                <div className="inline-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={handleResend}
                    disabled={resendLoading}
                  >
                    {resendLoading ? (t('resending') || 'Reenviando…') : (t('resendVerification') || 'Reenviar verificación')}
                  </button>
                </div>
                {resendInfo && <p className="info">{resendInfo}</p>}
              </>
            )}
          </div>
        )}

        <button type="submit" className="primary-button" disabled={!canSubmit}>
          {loading ? (t('creatingAccount') || 'Creando cuenta…') : (t('createAccount') || 'Crear cuenta')}
        </button>

        <p className="signup-text">
          {t('alreadyHaveAccount')}{' '}
          <a
            href="#login"
            className="signup-link"
            onClick={(e) => { e.preventDefault(); onGoLogin?.(e); }}
          >
            {t('logIn')}
          </a>
        </p>
      </fieldset>
    </form>
  );
}
