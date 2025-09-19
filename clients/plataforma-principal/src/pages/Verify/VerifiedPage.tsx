import { Link } from 'react-router-dom';
import './VerifyStyles.scss';

export default function VerifiedPage() {
  return (
    <section className="verify-card is-success" aria-labelledby="verified-title">
      <div className="v-icon" aria-hidden>✅</div>
      <h2 id="verified-title">Correo verificado</h2>
      <p>Tu correo ha sido confirmado correctamente.</p>
      <p className="v-hint">Ya puedes iniciar sesión para continuar.</p>
      <div className="v-actions">
        <Link to="/" className="v-btn v-btn--primary">Ir al login</Link>
        <Link to="/main" className="v-btn v-btn--secondary">Ir al panel</Link>
      </div>
    </section>
  );
}
