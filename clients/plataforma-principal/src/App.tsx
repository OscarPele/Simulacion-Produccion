import { Routes, Route } from 'react-router-dom';
import Home from './pages/Home/Home';
import MainPage from './pages/Main/MainPage';
import PrivateLayout from './routes/PrivateLayout';
import ResetPasswordPage from './pages/ResetPassword/ResetPasswordPage';

export default function App() {
  return (
    <Routes>
      {/* Rutas públicas */}
      <Route path="/" element={<Home />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      {/* Grupo de rutas privadas */}
      <Route element={<PrivateLayout />}>
        <Route path="/main" element={<MainPage />} />
      </Route>
    </Routes>
  );
}

