import { Outlet } from 'react-router-dom';
import './MainPage.scss';
import UserMenu from './components/Nav/UserMenu/UserMenu';
import myLogo from '../../assets/verticalLogo.png';
import SideBar from './components/SideBar/SideBar';

export default function MainPage() {
  return (
    <div className="main-page">
      <nav className="main-nav" aria-label="Navegación principal">
        <div className="nav-left">
          <img src={myLogo} alt="Logotipo" className="nav-logo" />
        </div>
        <div className="nav-center" />
        <div className="nav-right"><UserMenu /></div>
      </nav>

      <div className="layout">
        <SideBar />
        <main id="main-content" className="main-content" role="main">
          <Outlet /> {/* Aquí se inyecta HR/Production/etc según la URL */}
        </main>
      </div>
    </div>
  );
}
