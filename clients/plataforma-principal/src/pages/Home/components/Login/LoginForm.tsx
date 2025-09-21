import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FiUser, FiLock, FiEye, FiEyeOff } from 'react-icons/fi';
import './LoginForm.scss';
import { useNavigate } from 'react-router-dom';
import { login, type TokenResponse } from '../../../../api/token/authClient';
import { forgotPassword } from '../../../../api/users/passwordResetClient';
import { authApiUrl } from '../../../../api/config';

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

  // Forgot password modal/state
  const [forgotOpen, setForgotOpen] = useState(false);
  const [forgotLoading, setForgotLoading] = useState(false);
  const [maskedEmail, setMaskedEmail] = useState('');

  // Email no verificado (para mostrar CTA de reenviar verificación)
  const [notVerifiedEmail, setNotVerifiedEmail] = useState<string | null>(null);
  const [resendLoading, setResendLoading] = useState(false);
  const [resendInfo, setResendInfo] = useState<string | null>(null);

  const resendEndpoint = authApiUrl('/auth/verify-email/request');

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

  const isValidEmail = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

  const maskEmail = (email: string) => {
    const [u, d] = email.split('@');
    if (!d) return email;
    const mUser =
      u.length <= 2 ? '*'.repeat(u.length) : `${u.at(0)}${'*'.repeat(Math.max(u.length - 2, 1))}${u.at(-1)}`;
    const dParts = d.split('.');
    const dn = dParts[0] || '';
    const rest = dParts.slice(1).join('.');
    const mDom =
      dn.length <= 2 ? '*'.repeat(dn.length) : `${dn.at(0)}${'*'.repeat(Math.max(dn.length - 2, 1))}${dn.at(-1)}`;
    return `${mUser}@${mDom}${rest ? '.' + rest : ''}`;
  };

  const handleForgot = async (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    if (loading || forgotLoading) return;

    const email = form.usernameOrEmail.trim();
    if (!isValidEmail(email)) {
      setErrorMsg(t('emailRequiredForReset') || 'Please enter a valid email to recover your password');
      return;
    }

    setErrorMsg(null);
    setForgotLoading(true);
    try {
      await forgotPassword(email); // 204 siempre por privacidad
    } catch {
      // mostramos el mismo modal aunque falle (privacy UX)
    } finally {
      setMaskedEmail(maskEmail(email));
      setForgotOpen(true);
      setForgotLoading(false);
    }
  };

  const handleResendVerification = async () => {
    if (!notVerifiedEmail || resendLoading) return;
    setResendLoading(true);
    setResendInfo(null);
    try {
      await fetch(resendEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email: notVerifiedEmail })
      });
      setResendInfo(
        t('verificationResent') ||
          'If the email exists and is not verified, we have resent the verification email.'
      );
    } catch {
      setResendInfo(t('errorConnecting') || 'Could not connect to the server');
    } finally {
      setResendLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return;
    if (clientError) {
      setErrorMsg(clientError);
      return;
    }

    setErrorMsg(null);
    setNotVerifiedEmail(null);
    setResendInfo(null);
    setLoading(true);

    try {
      const payload: LoginRequest = {
        usernameOrEmail: form.usernameOrEmail.trim(),
        password: form.password,
      };

      const data = await login(payload);
      onSuccess?.(data);
      setForm({ usernameOrEmail: '', password: '' });
      navigate('/main');
    } catch (err: any) {
      const msg = String(err?.message ?? '');

      // Detecta explícitamente el caso de email no verificado:
      // el backend responde 403 con body {"code":"EMAIL_NOT_VERIFIED"}.
      // Tu cliente `login()` probablemente incorpore el status/código en el mensaje.
      if (msg.includes('EMAIL_NOT_VERIFIED') || /_403$/.test(msg)) {
        // Solo mostramos CTA de reenvío si lo introducido parece un email
        const emailCandidate = form.usernameOrEmail.trim();
        if (isValidEmail(emailCandidate)) setNotVerifiedEmail(emailCandidate);
        setErrorMsg(t('emailNotVerified') || 'Please verify your email address to continue');
        setLoading(false);
        return;
      }

      if (msg.startsWith('LOGIN_FAILED_')) {
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
        setErrorMsg(t('errorConnecting') || 'Could not connect to the server');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
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

          {/* Forgot password */}
          <div className="forgot-container">
            <p className="forgot-text">
              <a
                href="#forgot"
                className="forgot-link"
                onClick={handleForgot}
                aria-disabled={loading || forgotLoading}
              >
                {forgotLoading ? t('sendingReset') : t('forgotPassword')}
              </a>
            </p>

            {errorMsg && (
              <p role="alert" aria-live="polite" className="error">{errorMsg}</p>
            )}
          </div>

          {/* Bloque especial cuando el email no está verificado */}
          {notVerifiedEmail && (
            <div className="success-block" role="status" aria-live="polite">
              <p className="success-title">
                {t('emailNotVerified') || 'Please verify your email address to continue'}
              </p>
              <p className="success-hint">
                {t('verifyEmailHint', { email: notVerifiedEmail }) ||
                  `Sent to: ${notVerifiedEmail}. If you don't see it, check SPAM.`}
              </p>
              <div className="inline-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={handleResendVerification}
                  disabled={resendLoading}
                >
                  {resendLoading ? (t('resending') || 'Resending…') : (t('resendVerification') || 'Resend verification')}
                </button>
              </div>
              {resendInfo && <p className="info">{resendInfo}</p>}
            </div>
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

      {/* Modal de confirmación reset password */}
      {forgotOpen && (
        <div className="modal-backdrop" onClick={() => setForgotOpen(false)}>
          <div
            className="modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="reset-title"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 id="reset-title">{t('resetEmailModal.title')}</h3>
            <p>{t('resetEmailModal.body', { email: maskedEmail })}</p>
            <div className="modal-actions">
              <button type="button" onClick={() => setForgotOpen(false)}>{t('ok')}</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
