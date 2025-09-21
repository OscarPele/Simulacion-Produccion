import React from 'react';
import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import './SideBar.scss';

const navItems: { key: 'humanResources' | 'production' | 'rawMaterials' | 'sales'; to: string; emoji?: string }[] = [
  { key: 'humanResources', to: 'human-resources', emoji: '👥' },
  { key: 'production',     to: 'production',      emoji: '🏭' },
  { key: 'rawMaterials',   to: 'raw-materials',   emoji: '🧱' },
  { key: 'sales',          to: 'sales',           emoji: '💰' },
];

const SideBar: React.FC = () => {
  const { t } = useTranslation('SideBar');

  return (
    <aside className="sidebar" aria-label={t('label', { defaultValue: 'Sidebar' })}>
      <h4 className="sidebar__title">{t('title', { defaultValue: 'Secciones' })}</h4>

      <nav>
        <ul className="sidebar__list">
          {navItems.map((item) => (
            <li key={item.key} className="sidebar__item">
              <NavLink
                to={item.to}
                end
                className={({ isActive }) => `sidebar__link${isActive ? ' is-active' : ''}`}
              >
                <span className="sidebar__emoji" aria-hidden>{item.emoji}</span>
                <span className="sidebar__text">{t(item.key)}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  );
};

export default SideBar;
