import './MainPage.scss';
import UserMenu from './components/Nav/UserMenu/UserMenu';
import myLogo from '../../assets/verticalLogo.png';

export default function MainPage() {
  
  return (
    <div className="main-page">
      <nav className="main-nav">
        <div className="nav-left">
          <img src={myLogo} alt="" className='nav-logo' />
        </div>
        <div className="nav-center" />
        <div className="nav-right"><UserMenu /></div>
      </nav>

      <main className="main-content">

      </main>
    </div>
  );
}
