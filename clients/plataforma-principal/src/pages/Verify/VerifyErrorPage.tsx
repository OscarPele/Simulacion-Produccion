import { Link, useLocation } from 'react-router-dom';
import './VerifyStyles.scss';

function reasonLabel(reason?: string | null) {
  switch ((reason || '').toUpperCase()) {
    case 'TOKEN_EXPIRED': return 'El enlace ha caducado. Solicita un nuevo correo de verificación.';
    case 'TOKEN_ALREADY_USED': return 'Este enlace ya fue utilizado.';
    case 'INVALID_TOKEN': return 'El enlace de verificación no es válido.';
    default: return 'No hemos podido verificar tu correo.';
  }
}

export default function VerifyErrorPage() {
  const { search } = useLocation();
  const params = new URLSearchParams(search);
  const reason = params.get('reason');

  return (
    <section className="verify-card is-error" aria-labelledby="verify-error-title">
      <div className="v-icon" aria-hidden>❌</div>
      <h2 id="verify-error-title">Error de verificación</h2>
      <p>{reasonLabel(reason)}</p>
      <p className="v-hint">Revisa tu buzón y carpeta de SPAM, o pide reenviar la verificación desde el login.</p>
      <div className="v-actions">
        <Link to="/" className="v-btn v-btn--primary">Ir al login</Link>
      </div>
    </section>
  );
}
