import { useState } from "react";
import './RegisterForm.scss';

type RegisterRequest = { username: string; email: string; password: string };

export default function RegisterForm() {
  const API = import.meta.env.VITE_API_BASE_URL;

  const [form, setForm] = useState<RegisterRequest>({ username: "", email: "", password: "" });
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setLoading(true);
    try {
      const res = await fetch(`${API}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });

      if (!res.ok) {
        setErrorMsg(`Error ${res.status}`);
        return;
      }
      setForm({ username: "", email: "", password: "" });
    } catch {
      setErrorMsg("No se pudo conectar con el servidor");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="register-card" onSubmit={handleSubmit} noValidate>
      <fieldset className="fieldset" disabled={loading}>
        <div className="field">
          <label htmlFor="username" className="label">Usuario</label>
          <input
            id="username"
            name="username"
            className="input"
            type="text"
            required
            minLength={3}
            autoComplete="username"
            value={form.username}
            onChange={handleChange}
            placeholder="tu_usuario"
          />
        </div>

        <div className="field">
          <label htmlFor="email" className="label">Correo</label>
          <input
            id="email"
            name="email"
            className="input"
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={handleChange}
            placeholder="tu@email.com"
          />
        </div>

        <div className="field">
          <label htmlFor="password" className="label">Contraseña</label>
          <div className="password-row">
            <input
              id="password"
              name="password"
              className="input"
              type={showPwd ? "text" : "password"}
              required
              minLength={8}
              autoComplete="new-password"
              value={form.password}
              onChange={handleChange}
              placeholder="••••••••"
            />
            <button
              type="button"
              className="ghost-button"
              onClick={() => setShowPwd((v) => !v)}
              aria-label={showPwd ? "Ocultar contraseña" : "Mostrar contraseña"}
            >
              {showPwd ? "Ocultar" : "Mostrar"}
            </button>
          </div>
          <small className="hint">Mínimo 8 caracteres (mayúsculas, minúsculas y números).</small>
        </div>

        {errorMsg && (
          <p role="alert" className="error">
            {errorMsg}
          </p>
        )}

        <div className="actions">
          <button type="submit" className="primary-button">
            {loading ? "Creando cuenta…" : "Crear cuenta"}
          </button>
        </div>
      </fieldset>
    </form>
  );
}
