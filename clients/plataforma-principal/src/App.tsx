import './App.scss';
import LoginForm from './components/Login/LoginForm';
import RegisterForm from './components/Register/RegisterForm';
import { FaCog } from "react-icons/fa";
import { useRef, useState } from 'react';

type Mode = 'login' | 'register';

function App() {
  const [mode, setMode] = useState<Mode>('login');
  const [anim, setAnim] = useState<'idle' | 'expanding' | 'closing'>('idle');
  const [nextMode, setNextMode] = useState<Mode | null>(null);
  const [circlePos, setCirclePos] = useState({ x: 0, y: 0 });
  const containerRef = useRef<HTMLDivElement>(null);

  const startTransition = (target: Mode, ev: React.MouseEvent) => {
    if (!containerRef.current) return;
    if (anim !== 'idle') return;         // evita solapados
    if (target === mode) return;         // evita relanzar al mismo modo

    const rect = containerRef.current.getBoundingClientRect();
    setCirclePos({ x: ev.clientX - rect.left, y: ev.clientY - rect.top });
    setNextMode(target);
    setAnim('expanding');
  };

  const onCircleTransitionEnd = () => {
    if (anim === 'expanding') {
      // al terminar de expandir, cambiamos el contenido y empezamos a cerrar
      setMode(nextMode ?? mode);
      setNextMode(null);
      setAnim('closing');
    } else if (anim === 'closing') {
      // cierre finalizado
      setAnim('idle');
    }
  };

  const title = mode === 'login' ? 'Log in to your account' : 'Create your account';
  const subtitle =
    mode === 'login'
      ? 'Enter your email address and password to log in'
      : 'Fill in your details to create an account';

  return (
    <div className="main">
      <div className="login-main-content" ref={containerRef}>
        {/* Capa animada */}
        <div
          className={
            `circle-mask ${anim === 'expanding' ? 'is-active' : ''} ${anim === 'closing' ? 'closing' : ''}`
          }
          style={
            {
              ['--cx' as any]: `${circlePos.x}px`,
              ['--cy' as any]: `${circlePos.y}px`,
            } as React.CSSProperties
          }
          onTransitionEnd={onCircleTransitionEnd}
          aria-hidden
        />

        <div className="center-content">
          <div className="company-logo">
            <FaCog className="company-gear" />
            <div className='logo-content'>
              <div className="company-header">
                <h1 className="company-acronym">O.P.S.</h1>
              </div>
              <p className="company-complete-name">Oscar Production Simulator</p>
            </div>
          </div>

          <div>
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>

          <div className="forms">
            {mode === 'login' ? (
              <LoginForm onGoRegister={(e) => startTransition('register', e)} />
            ) : (
              <RegisterForm onGoLogin={(e) => startTransition('login', e)} />
            )}
          </div>
        </div>
      </div>

      <div className="dashboard-preview-container">
      <div className="dashboard-preview">
        {/* Logo */}
        <img
          src="./src/assets/dashboard.png"
          alt="Logo"
          className="dashboard-logo"
        />

        {/* Imagen principal */}
        <img
          src="./src/assets/dashboard.png"
          alt="Dashboard main preview"
          className="dashboard-main-img"
        />

        {/* Mini preview */}
        <img
          src="./src/assets/dashboard.png"
          alt="Mini stat preview"
          className="dashboard-mini-img"
        />
      </div>
    </div>

    </div>
  );
}

export default App;
