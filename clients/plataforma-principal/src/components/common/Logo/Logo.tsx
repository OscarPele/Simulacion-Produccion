import { FaCog } from 'react-icons/fa';
import { useTranslation } from 'react-i18next';
import './Logo.scss';

export default function Logo() {
  const { t } = useTranslation();

  return (
    <div className="company-logo">
      <FaCog className="company-gear" />
      <div className="logo-content">
        <div className="company-header">
          <h1 className="company-acronym">O.P.S.</h1>
        </div>
        <p className="company-complete-name">
          {t('company.fullName')}
        </p>
      </div>
    </div>
  );
}
