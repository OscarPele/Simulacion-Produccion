import { Outlet, Navigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';

type JwtPayload = {
  exp: number;
  uid?: number;
  email?: string;
  sub?: string;
};

export default function PrivateLayout() {
  const token = localStorage.getItem('accessToken');

  if (!token) return <Navigate to="/" replace />;

  try {
    const { exp } = jwtDecode<JwtPayload>(token);
    if (!exp || Date.now() >= exp * 1000) {
      localStorage.removeItem('accessToken');
      return <Navigate to="/" replace />;
    }
  } catch {
    localStorage.removeItem('accessToken');
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
