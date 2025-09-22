import { Routes, Route, Navigate } from 'react-router-dom';
import Home from './pages/Home/Home';
import MainPage from './pages/Main/MainPage';
import PrivateLayout from './routes/PrivateLayout';
import ResetPasswordPage from './pages/ResetPassword/ResetPasswordPage';
import VerifiedPage from './pages/Verify/VerifiedPage';
import VerifyErrorPage from './pages/Verify/VerifyErrorPage';

// Páginas del área Main
import HR from './pages/Main/MainPages/HR/HR';

//TODO: Cambiar importaciones a sus archivos correspondientes:
import Production from './pages/Main/MainPages/HR/HR';
import RawMaterials from './pages/Main/MainPages/HR/HR';
import Sales from './pages/Main/MainPages/HR/HR';

export default function App() {
  return (
    <Routes>
      {/* Públicas */}
      <Route path="/" element={<Home />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/verified" element={<VerifiedPage />} />
      <Route path="/verify-error" element={<VerifyErrorPage />} />

      {/* Privadas */}
      <Route element={<PrivateLayout />}>
        {/* Layout Main */}
        <Route path="/main" element={<MainPage />}>
          {/* Redirect por defecto a HR */}
          <Route index element={<Navigate to="human-resources" replace />} />

          {/* Secciones */}
          <Route path="human-resources" element={<HR />} />
          <Route path="production" element={<Production />} />
          <Route path="raw-materials" element={<RawMaterials />} />
          <Route path="sales" element={<Sales />} />

          {/* 404 dentro de main -> redirige a HR */}
          <Route path="*" element={<Navigate to="human-resources" replace />} />
        </Route>
      </Route>

      {/* 404 global -> home */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
