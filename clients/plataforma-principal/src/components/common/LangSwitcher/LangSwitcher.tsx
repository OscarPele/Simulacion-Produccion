import { useTranslation } from 'react-i18next';
import './LangSwitcher.scss';

export default function LangSwitcher() {
  const { t, i18n } = useTranslation();
  const isEs = i18n.language?.toLowerCase().startsWith('es');
  const isEn = i18n.language?.toLowerCase().startsWith('en');

  return (
    <div className="lang-switcher" role="group" aria-label={t('lang.switch')}>
      <button
        type="button"
        onClick={() => i18n.changeLanguage('es')}
        aria-pressed={isEs}
        className={isEs ? 'active' : ''}
      >
        {t('lang.es')}
      </button>
      <button
        type="button"
        onClick={() => i18n.changeLanguage('en')}
        aria-pressed={isEn}
        className={isEn ? 'active' : ''}
      >
        {t('lang.en')}
      </button>
    </div>
  );
}
