import { useState } from 'react';
import { FiUser, FiMail, FiLock, FiEye, FiEyeOff } from 'react-icons/fi';
import '../Login/LoginForm.scss';

type RegisterRequest = { username: string; email: string; password: string };

type Props = {
  onGoLogin?: (e: React.MouseEvent<HTMLAnchorElement>) => void;
};

export default function RegisterForm({ onGoLogin }: Props) {
  const API = (import.meta.env.VITE_API_BASE_URL as string) ?? '';
  const endpoint = `${API.replace(/\/$/, '')}/auth/register`;

  const [form, setForm] = useState<RegisterRequest>({ username: '', email: '', password: '' });
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
      setForm({ username: '', email: '', password: '' });
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
            <FiUser />
          </span>
          <input
            id="username"
            name="username"
            type="text"
            autoComplete="username"
            placeholder="Username"
            required
            minLength={3}
            value={form.username}
            onChange={handleChange}
            aria-invalid={!!errorMsg}
          />
        </div>

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
            required
            value={form.email}
            onChange={handleChange}
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
            autoComplete='new-password'
            placeholder="Password"
            required
            minLength={8}
            value={form.password}
            onChange={handleChange}
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

        <small className="signup-text" style={{ marginTop: 0 }}>
          Mínimo 8 caracteres (mayúsculas, minúsculas y números).
        </small>

        {errorMsg && (
          <p role="alert" aria-live="polite" className="error">
            {errorMsg}
          </p>
        )}

        <button type="submit" className="primary-button" disabled={loading}>
          {loading ? 'Creando cuenta…' : 'Create account'}
        </button>

        <p className="signup-text">
          Already have an account?{' '}
          <a
            href="#login"
            className="signup-link"
            onClick={(e) => {
              e.preventDefault();
              onGoLogin?.(e);
            }}
          >
            Log in
          </a>
        </p>
      </fieldset>
    </form>
  );
}
