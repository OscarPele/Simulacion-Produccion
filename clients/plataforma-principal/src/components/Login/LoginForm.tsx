import { useState } from 'react';
import { FiMail, FiLock, FiEye, FiEyeOff } from 'react-icons/fi';
import './LoginForm.scss';

type LoginRequest = { email: string; password: string };
type LoginResponse = { token: string; refreshToken?: string };

type Props = {
  onSuccess?: (data: LoginResponse) => void;
  onGoRegister?: (e: React.MouseEvent<HTMLAnchorElement>) => void;
};

export default function LoginForm({ onSuccess, onGoRegister }: Props) {
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
      setErrorMsg('No se pudo conectar con el servidor');
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
            placeholder="Email Address"
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
            placeholder="Password"
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
            aria-label={showPwd ? 'Ocultar contraseña' : 'Mostrar contraseña'}
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
          {loading ? 'Entrando…' : 'Login'}
        </button>

        <p className="signup-text">
          Don’t you have an account?{' '}
          <a
            href="#register"
            className="signup-link"
            onClick={(e) => {
              e.preventDefault();
              onGoRegister?.(e);
            }}
          >
            Sign Up
          </a>
        </p>
      </fieldset>
    </form>
  );
}
