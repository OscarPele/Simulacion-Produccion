import { useRef, useState } from 'react';
import { FaCog } from 'react-icons/fa';
import { useTranslation } from 'react-i18next';

import './App.scss';
import LoginForm from './components/Login/LoginForm';
import RegisterForm from './components/Register/RegisterForm';
import dashboardImg from './assets/dashboard.png';

type Mode = 'login' | 'register';
type Anim = 'idle' | 'expanding' | 'closing';
type CSSVars = React.CSSProperties & { [key: `--${string}`]: string };

function App() {
  const { t, i18n } = useTranslation('app');


  const [mode, setMode] = useState<Mode>('login');
  const [anim, setAnim] = useState<Anim>('idle');
  const [nextMode, setNextMode] = useState<Mode | null>(null);
  const [circlePos, setCirclePos] = useState({ x: 0, y: 0 });
  const containerRef = useRef<HTMLDivElement>(null);

  const startTransition = (target: Mode, ev: React.MouseEvent) => {
    if (!containerRef.current) return;
    if (anim !== 'idle') return;
    if (target === mode) return;

    const rect = containerRef.current.getBoundingClientRect();
    setCirclePos({ x: ev.clientX - rect.left, y: ev.clientY - rect.top });
    setNextMode(target);
    setAnim('expanding');
  };

  const onCircleTransitionEnd = () => {
    if (anim === 'expanding') {
      setMode(nextMode ?? mode);
      setNextMode(null);
      setAnim('closing');
    } else if (anim === 'closing') {
      setAnim('idle');
    }
  };

  const circleClass = [
    'circle-mask',
    anim === 'expanding' && 'is-active',
    anim === 'closing' && 'closing',
  ]
    .filter(Boolean)
    .join(' ');

  const circleStyle: CSSVars = {
    '--cx': `${circlePos.x}px`,
    '--cy': `${circlePos.y}px`,
  };

  return (
    <div className="main">
      <div className="login-main-content" ref={containerRef}>
        <div
          className={circleClass}
          style={circleStyle}
          onTransitionEnd={onCircleTransitionEnd}
          aria-hidden
        />

        <div className="center-content">
          <div className="company-logo">
            <FaCog className="company-gear" />
            <div className="logo-content">
              <div className="company-header">
                <h1 className="company-acronym">O.P.S.</h1>
              </div>
              <p className="company-complete-name">
                {t('company.fullName')}
              </p>
            </div>
          </div>

          <div>
            <h1>{t(`titles.${mode}.title`)}</h1>
            <p>{t(`titles.${mode}.subtitle`)}</p>
          </div>

          <div className="forms">
            {mode === 'login' ? (
              <LoginForm onGoRegister={(e) => startTransition('register', e)} />
            ) : (
              <RegisterForm onGoLogin={(e) => startTransition('login', e)} />
            )}
          </div>
          

          {/* Selector de idioma */}
          <div className="lang-switcher" role="group" aria-label={t('lang.switch')}>
            <button
              type="button"
              onClick={() => i18n.changeLanguage('es')}
              aria-pressed={i18n.language.startsWith('es')}
              className={i18n.language.startsWith('es') ? 'active' : ''}
            >
              {t('lang.es')}
            </button>
            <button
              type="button"
              onClick={() => i18n.changeLanguage('en')}
              aria-pressed={i18n.language.startsWith('en')}
              className={i18n.language.startsWith('en') ? 'active' : ''}
            >
              {t('lang.en')}
            </button>
          </div>
        </div>

        
      </div>

      <div className="dashboard-preview-container">
        <div className="dashboard-preview">
          <img src={dashboardImg} alt="Logo" className="dashboard-logo" />
          <img
            src={dashboardImg}
            alt={t('images.dashboardMainAlt')}
            className="dashboard-main-img"
          />
          <img
            src={dashboardImg}
            alt={t('images.dashboardMiniAlt')}
            className="dashboard-mini-img"
          />
        </div>
      </div>
    </div>
  );
}

export default App;
