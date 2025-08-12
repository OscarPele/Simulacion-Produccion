import { Navigate } from 'react-router-dom';
import jwtDecode from 'jwt-decode';

export default function PrivateRoute({ children }) {
  const token = localStorage.getItem('accessToken');

  if (!token) {
    return <Navigate to="/" replace />;
  }

  try {
    const { exp } = jwtDecode(token);
    if (Date.now() >= exp * 1000) {
      localStorage.removeItem('accessToken');
      return <Navigate to="/" replace />;
    }
  } catch (e) {
    localStorage.removeItem('accessToken');
    return <Navigate to="/" replace />;
  }

  return children;
}
