import React from "react";
import { NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import "./SideBar.scss";
import { Users, Hammer, Package, Coins } from "lucide-react";

type NavKey = "humanResources" | "production" | "rawMaterials" | "sales";

type NavItem = {
  key: NavKey;
  to: string;
  label: string;
  icon: React.ReactNode;
};

const navItems: NavItem[] = [
  { key: "humanResources", to: "human-resources", label: "Recursos Humanos", icon: <Users /> },
  { key: "production",     to: "production",      label: "Producción",       icon: <Hammer /> },
  { key: "rawMaterials",   to: "raw-materials",   label: "Materias Primas",  icon: <Package /> },
  { key: "sales",          to: "sales",           label: "Ventas",           icon: <Coins /> },
];

const SideBar: React.FC = () => {
  const { t } = useTranslation("SideBar");

  return (
    <aside className="sidebar" aria-label={t("label", { defaultValue: "Sidebar" })}>
      <h4 className="sidebar__title">{t("title", { defaultValue: "Secciones" })}</h4>

      <nav>
        <ul className="sidebar__list">
          {navItems.map((item) => (
            <li key={item.key} className="sidebar__item">
              <NavLink
                to={item.to}
                end
                className={({ isActive }) => `sidebar__link${isActive ? " is-active" : ""}`}
                aria-label={t(item.key, { defaultValue: item.label })}
              >
                <span className="sidebar__icon" aria-hidden="true">{item.icon}</span>
                <span className="sidebar__text">{t(item.key, { defaultValue: item.label })}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  );
};

export default SideBar;
