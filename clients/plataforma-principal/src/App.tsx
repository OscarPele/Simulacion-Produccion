import { Routes, Route } from 'react-router-dom';
import Home from './pages/Home/Home';
import MainPage from './pages/Main/MainPage';
import ProfilePage from './pages/Profile/ProfilePage';
import PrivateLayout from './components/PrivateLayout';

export default function App() {
  return (
    <Routes>
      {/* Rutas públicas */}
      <Route path="/" element={<Home />} />

      {/* Grupo de rutas privadas */}
      <Route element={<PrivateLayout />}>
        <Route path="/main" element={<MainPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Route>
    </Routes>
  );
}

