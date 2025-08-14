import { useMemo } from 'react';
import './MainPage.scss';
import Logo from '../../components/common/Logo/Logo';
import UserMenu from './components/Nav/UserMenu/UserMenu';
import { jwtDecode } from 'jwt-decode';
import { FiCheckCircle, FiAlertTriangle } from 'react-icons/fi';

type JwtPayload = { sub?: string; email?: string; uid?: number; exp?: number };

export default function MainPage() {
  const token = localStorage.getItem('accessToken') || '';
  const { username, exp } = useMemo(() => {
    try {
      const p = token ? jwtDecode<JwtPayload>(token) : {};
      return { username: (p.sub || p.email || '') as string, exp: p.exp as number | undefined };
    } catch {
      return { username: '', exp: undefined };
    }
  }, [token]);

  const isValid = !!token && !!exp && Date.now() < exp * 1000;
  const when = exp ? new Date(exp * 1000).toLocaleString() : '';

  return (
    <div className="main-page">
      <nav className="main-nav">
        <div className="nav-left"><Logo /></div>
        <div className="nav-center" />
        <div className="nav-right"><UserMenu /></div>
      </nav>

      <main className="main-content">

        <div className={`auth-flag ${isValid ? 'success' : 'warning'}`}>
          <div className="auth-flag__icon">
            {isValid ? <FiCheckCircle /> : <FiAlertTriangle />}
          </div>
          <div className="auth-flag__body">
            <h2>{isValid ? 'Autenticación OK' : 'Autenticación inválida'}</h2>
            <p>
              {isValid
                ? <>Sesión iniciada como <strong>{username}</strong>. Expira el <strong>{when}</strong>.</>
                : 'Token ausente o expirado.'}
            </p>
          </div>
        </div>

      </main>
    </div>
  );
}
