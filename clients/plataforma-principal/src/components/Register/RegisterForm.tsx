import { useState } from "react";
import { FiUser, FiMail, FiLock, FiEye, FiEyeOff } from "react-icons/fi";
// Reutilizamos los estilos del login para que se vea idéntico
import "../Login/LoginForm.scss";

type RegisterRequest = { username: string; email: string; password: string };

type Props = {
  onGoLogin?: (e: React.MouseEvent<HTMLAnchorElement>) => void; // opcional: para animar vuelta a Login
};

export default function RegisterForm({ onGoLogin }: Props) {
  const API = import.meta.env.VITE_API_BASE_URL;

  const [form, setForm] = useState<RegisterRequest>({
    username: "",
    email: "",
    password: "",
  });
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
    <form className="login-card" onSubmit={handleSubmit} noValidate>
      {/* usamos las mismas clases que el LoginForm */}
      <fieldset className="fieldset" disabled={loading}>
        {/* Username */}
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
          />
        </div>

        {/* Email */}
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
          />
        </div>

        {/* Password */}
        <div className="pill-input">
          <span className="left-icon" aria-hidden>
            <FiLock />
          </span>
          <input
            id="password"
            name="password"
            type={showPwd ? "text" : "password"}
            autoComplete="new-password"
            placeholder="Password"
            required
            minLength={8}
            value={form.password}
            onChange={handleChange}
          />
          <button
            type="button"
            className="right-icon"
            onClick={() => setShowPwd((v) => !v)}
            aria-label={showPwd ? "Ocultar contraseña" : "Mostrar contraseña"}
          >
            {showPwd ? <FiEyeOff /> : <FiEye />}
          </button>
        </div>

        {/* Hint */}
        <small className="signup-text" style={{ marginTop: 0 }}>
          Mínimo 8 caracteres (mayúsculas, minúsculas y números).
        </small>

        {/* Error */}
        {errorMsg && (
          <p role="alert" className="error">
            {errorMsg}
          </p>
        )}

        {/* Submit */}
        <button type="submit" className="primary-button">
          {loading ? "Creando cuenta…" : "Create account"}
        </button>

        {/* Link para volver a Login (opcional, para tu animación inversa) */}
        <p className="signup-text">
          Already have an account?{" "}
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
