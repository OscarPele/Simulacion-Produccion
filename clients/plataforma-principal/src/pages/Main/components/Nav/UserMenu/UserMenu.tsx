import { useMemo, useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { useTranslation } from 'react-i18next';
import { FiChevronDown } from 'react-icons/fi';
import './UserMenu.scss';
import LangSwitcher from '../../../../../components/common/LangSwitcher/LangSwitcher';

type JwtPayload = { sub?: string; email?: string; uid?: number };

export default function UserMenu() {
  const { t, i18n } = useTranslation('UserMenu');
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');

  const [open, setOpen] = useState(false);
  const [showPwd, setShowPwd] = useState(false);
  const [currentPassword, setCurrent] = useState('');
  const [newPassword, setNewPwd] = useState('');
  const [confirmPassword, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setErr] = useState<string | null>(null);

  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onPointerDown = (e: MouseEvent | TouchEvent) => {
      if (!open) return;
      const el = rootRef.current;
      if (el && !el.contains(e.target as Node)) setOpen(false);
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onPointerDown);
    document.addEventListener('touchstart', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onPointerDown);
      document.removeEventListener('touchstart', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  const { username, uid } = useMemo(() => {
    if (!token) return { username: '', uid: undefined as number | undefined };
    try {
      const p = jwtDecode<JwtPayload>(token);
      return { username: p.sub || p.email || '', uid: p.uid };
    } catch {
      return { username: '', uid: undefined };
    }
  }, [token]);

  const capitalizeFirst = (s: string, locale?: string) =>
    s ? s.trim().charAt(0).toLocaleUpperCase(locale) + s.trim().slice(1) : s;

  const displayName = useMemo(
    () => (username ? capitalizeFirst(username, i18n.language) : t('labels.user')),
    [username, i18n.language, t]
  );

  const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    navigate('/');
  };

  const API = (import.meta.env.VITE_API_BASE_URL as string) ?? '';
  const changePwdEndpoint = useMemo(() => {
    if (!uid) return null;
    return `${API.replace(/\/$/, '').trim()}/users/${uid}/password`;
  }, [API, uid]);

  const canSubmit =
    !!uid &&
    currentPassword.length >= 8 &&
    newPassword.length >= 8 &&
    newPassword === confirmPassword &&
    !loading;

  const submitChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit || !changePwdEndpoint) return;

    setErr(null);
    setLoading(true);
    try {
      const res = await fetch(changePwdEndpoint, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ currentPassword, newPassword }),
      });

      const ct = res.headers.get('content-type') || '';
      const body = ct.includes('application/json') ? await res.json() : await res.text();
      const code = (body && (body.code as string)) || '';

      if (res.status === 204) {
        setShowPwd(false);
        setCurrent(''); setNewPwd(''); setConfirm('');
        logout();
        return;
      }

      if (res.status === 400) {
        if (code === 'CURRENT_PASSWORD_INCORRECT') {
          setErr(t('errors.currentPasswordIncorrect'));
          return;
        }
        setErr(t('errors.invalidData'));
        return;
      }

      if (res.status === 401 || res.status === 403) {
        setErr(t('errors.sessionExpired'));
        logout();
        return;
      }

      if (res.status === 404) {
        setErr(t('errors.userNotFound'));
        return;
      }

      setErr(t('errors.genericWithCode', { code: res.status }));
    } catch {
      setErr(t('errors.network'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="user-menu" ref={rootRef}>
      <button
        className="user-trigger"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={t('aria.openMenu', { username: displayName })}
      >
        <span className={`chevron ${open ? 'open' : ''}`} aria-hidden>
          <FiChevronDown size={18} />
        </span>
        <span className="user-trigger__name">{displayName}</span>
      </button>

      {open && (
        <div className="user-popover" role="menu">
          <LangSwitcher />
          <button onClick={() => { setShowPwd(true); setOpen(false); }}>
            {t('actions.changePassword')}
          </button>
          <button onClick={logout}>{t('actions.logout')}</button>
        </div>
      )}

      {showPwd && (
        <div className="modal-backdrop" onClick={() => setShowPwd(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3 className="title-changePassword">{t('titles.changePassword')}</h3>
            <form onSubmit={submitChange}>
              <label>
                {t('fields.currentPassword')}
                <input
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrent(e.target.value)}
                  required
                  minLength={8}
                  autoComplete="current-password"
                />
              </label>
              <label>
                {t('fields.newPassword')}
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPwd(e.target.value)}
                  required
                  minLength={8}
                  autoComplete="new-password"
                />
              </label>
              <label>
                {t('fields.repeatNewPassword')}
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirm(e.target.value)}
                  required
                  minLength={8}
                  autoComplete="new-password"
                />
              </label>

              {error && <p className="error">{error}</p>}

              <div className="modal-actions">
                <button type="button" onClick={() => setShowPwd(false)} disabled={loading}>
                  {t('actions.cancel')}
                </button>
                <button type="submit" disabled={!canSubmit}>
                  {loading ? t('states.saving') : t('actions.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
