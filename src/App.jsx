import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import blocklist from './data/scamNumbers.json';
import './App.css';
import { registerPlugin } from '@capacitor/core';
import Onboarding from './Onboarding';

const ScamReport = registerPlugin('ScamReport');
const SafeVoiceSettings = registerPlugin('SafeVoiceSettings');
const SafeVoiceTts = registerPlugin('SafeVoiceTts');

const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'hi', label: 'हिन्दी' },
  { code: 'ta', label: 'தமிழ்' },
  { code: 'te', label: 'తెలుగు' },
  { code: 'ml', label: 'മലയാളം' },
  { code: 'kn', label: 'ಕನ್ನಡ' }
];

function App() {
  const { t, i18n } = useTranslation();

  const [showOnboarding, setShowOnboarding] = useState(
    () => localStorage.getItem('safevoice_onboarding_complete') !== 'true'
  );

  const [theme, setTheme] = useState(
    () => localStorage.getItem('safevoice_theme') || 'dark'
  );

  const [callState, setCallState] = useState({
    active: false,
    number: null,
    verdict: 'idle'
  });

  const [permissions, setPermissions] = useState({
    phoneState: 'unknown',
    overlay: 'unknown',
    notifications: 'unknown',
    microphone: 'unknown',
    sms: 'unknown'
  });
  const [recentAlerts, setRecentAlerts] = useState([]);
  const [lastNumber, setLastNumber] = useState(null);
  const [reportStatus, setReportStatus] = useState('idle');
  const [reading, setReading] = useState(false);

  const [familyNumber, setFamilyNumber] = useState('');
  const [familyNumberInput, setFamilyNumberInput] = useState('');
  const [familyNumberSaveStatus, setFamilyNumberSaveStatus] = useState('idle');
  const [editingFamilyNumber, setEditingFamilyNumber] = useState(false);

  const [protectionPaused, setProtectionPaused] = useState(null);

  const totalBlocked = blocklist.exactNumbers.length + blocklist.prefixPatterns.length;

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('safevoice_theme', theme);
  }, [theme]);

  useEffect(() => {
    SafeVoiceSettings.getLanguage()
      .then(({ code }) => {
        if (code) i18n.changeLanguage(code);
      })
      .catch(() => {});
  }, [i18n]);

  useEffect(() => {
    SafeVoiceSettings.getFamilyNumber()
      .then(({ number }) => {
        setFamilyNumber(number || '');
        setFamilyNumberInput(number || '');
        setEditingFamilyNumber(!number);
      })
      .catch(() => {
        setEditingFamilyNumber(true);
      });
  }, []);

  // Pull the real permission state once on mount. The native side also
  // pushes a "permissionsUpdated" event (below), but that push can fire
  // before this component has mounted and attached its listener — on a
  // cold start the JS bundle can take longer to load than the native
  // emit delay, so the event gets dropped and the dashboard is stuck on
  // "unknown". This pull guarantees correct values regardless of timing.
  useEffect(() => {
    SafeVoiceSettings.getPermissionStatus()
      .then((status) => {
        setPermissions((prev) => ({ ...prev, ...status }));
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    const handleIncomingCall = (event) => {
      const { number, verdict } = event.detail || {};
      setCallState({ active: true, number: number || 'Unknown number', verdict: verdict || 'monitoring' });
      setLastNumber(number || null);
      setReportStatus('idle');
      if (verdict === 'flagged') {
        setRecentAlerts((prev) => [{ number, time: new Date().toLocaleString() }, ...prev].slice(0, 20));
      }
    };

    const handleCallEnded = () => {
      setCallState({ active: false, number: null, verdict: 'idle' });
      setProtectionPaused(null);
    };

    const handlePermissionsUpdated = (event) => {
      setPermissions((prev) => ({ ...prev, ...(event.detail || {}) }));
    };

    const handleProtectionPaused = (event) => {
      const { reason } = event.detail || {};
      setProtectionPaused({ reason: reason || 'unknown' });
    };

    window.addEventListener('incomingCall', handleIncomingCall);
    window.addEventListener('callEnded', handleCallEnded);
    window.addEventListener('permissionsUpdated', handlePermissionsUpdated);
    window.addEventListener('protectionPaused', handleProtectionPaused);

    return () => {
      window.removeEventListener('incomingCall', handleIncomingCall);
      window.removeEventListener('callEnded', handleCallEnded);
      window.removeEventListener('permissionsUpdated', handlePermissionsUpdated);
      window.removeEventListener('protectionPaused', handleProtectionPaused);
    };
  }, []);

  const handleReport = async () => {
    if (!lastNumber) return;
    setReportStatus('reporting');
    try {
      await ScamReport.reportNumber({ number: lastNumber });
      setReportStatus('reported');
    } catch (e) {
      setReportStatus('idle');
    }
  };

  const handleLanguageChange = async (event) => {
    const code = event.target.value;
    i18n.changeLanguage(code);
    try {
      await SafeVoiceSettings.setLanguage({ code });
    } catch (e) {}
  };

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const handleSaveFamilyNumber = async () => {
    setFamilyNumberSaveStatus('saving');
    try {
      await SafeVoiceSettings.setFamilyNumber({ number: familyNumberInput.trim() });
      setFamilyNumber(familyNumberInput.trim());
      setFamilyNumberSaveStatus('saved');
      setEditingFamilyNumber(false);
      setTimeout(() => setFamilyNumberSaveStatus('idle'), 2000);
    } catch (e) {
      setFamilyNumberSaveStatus('idle');
    }
  };

  const allPermissionsGranted =
    permissions.phoneState === 'granted' &&
    permissions.overlay === 'granted' &&
    permissions.notifications === 'granted' &&
    permissions.microphone === 'granted' &&
    permissions.sms === 'granted';

  const handleReadAloud = async () => {
    const parts = [t('appName'), t('subtitle')];

    if (callState.active) {
      parts.push(callState.verdict === 'flagged' ? t('reportedScamNumber') : t('callInProgress'));
      parts.push(callState.verdict === 'flagged' ? t('flaggedText') : t('monitoringText'));
    } else {
      parts.push(t('systemActive'));
      parts.push(t('standingBy'));
      parts.push(`${totalBlocked} ${t('blockedCountSuffix')}`);
    }

    if (!allPermissionsGranted) {
      parts.push(t('setupNeeded'));
      parts.push(t('permissionsHint'));
    }

    const text = parts.join('. ');
    setReading(true);
    try {
      await SafeVoiceTts.speak({ text, language: i18n.language });
    } catch (e) {
    } finally {
      setReading(false);
    }
  };

  if (showOnboarding) {
    return <Onboarding onComplete={() => setShowOnboarding(false)} />;
  }

  return (
    <div className="app-container">
      <header className="header">
        <div className="header-row">
          <h1>{t('appName')} 🛡️</h1>
          <div className="header-controls">
            <select
              className="language-select"
              value={i18n.language}
              onChange={handleLanguageChange}
              aria-label={t('languageLabel')}
            >
              {LANGUAGES.map((lang) => (
                <option key={lang.code} value={lang.code}>{lang.label}</option>
              ))}
            </select>
            <button
              className="theme-toggle-button"
              onClick={toggleTheme}
              aria-label="Toggle light or dark mode"
            >
              {theme === 'dark' ? '☀️' : '🌙'}
            </button>
          </div>
        </div>
        <p className="subtitle">{t('subtitle')}</p>
        <button
          className="read-aloud-button"
          onClick={handleReadAloud}
          disabled={reading}
        >
          🔊 {t('readScreenAloud')}
        </button>
      </header>

      <main className="status-card">
        {callState.active ? (
          <div className={`active-call ${callState.verdict}`}>
            <span className={`badge ${callState.verdict === 'flagged' ? 'warning' : 'info'}`}>
              {callState.verdict === 'flagged' ? t('reportedScamNumber') : t('callInProgress')}
            </span>
            <h2>{callState.number}</h2>
            <p className="alert-text">
              {callState.verdict === 'flagged' ? t('flaggedText') : t('monitoringText')}
            </p>
          </div>
        ) : (
          <div className="idle-state">
            <span className="badge success">{t('systemActive')}</span>
            <h2>{t('standingBy')}</h2>
            <p>{totalBlocked} {t('blockedCountSuffix')}</p>
          </div>
        )}

        {protectionPaused && (
          <div className="protection-paused-banner">
            <span className="badge warning">{t('protectionPausedTitle')}</span>
            <p>
              {protectionPaused.reason === 'speaker_off'
                ? t('protectionPausedSpeakerOff')
                : t('protectionPausedUnavailable')}
            </p>
          </div>
        )}

        {lastNumber && (
          <div className="report-row">
            <button
              className="report-button"
              disabled={reportStatus !== 'idle'}
              onClick={handleReport}
            >
              {reportStatus === 'reported' ? t('reportedThanks') : t('reportButton', { number: lastNumber })}
            </button>
          </div>
        )}
      </main>

      {!allPermissionsGranted && (
        <section className="permissions-card">
          <div className="card-header">
            <span className="card-icon">⚙️</span>
            <h3>{t('setupNeeded')}</h3>
          </div>
          <ul>
            <li>{t('phoneStateLabel')}: {permissions.phoneState}</li>
            <li>{t('overlayLabel')}: {permissions.overlay}</li>
            <li>{t('notificationsLabel')}: {permissions.notifications}</li>
            <li>{t('microphoneLabel')}: {permissions.microphone}</li>
            <li>{t('smsLabel')}: {permissions.sms}</li>
          </ul>
          <p>{t('permissionsHint')}</p>
        </section>
      )}

      <section className="family-alert-card">
        <div className="card-header">
          <span className="card-icon">👪</span>
          <h3>{t('familyAlertTitle')}</h3>
        </div>
        <p>{t('familyAlertDescription')}</p>

        {editingFamilyNumber ? (
          <>
            <label htmlFor="family-number-input" className="family-number-label">
              {t('familyNumberLabel')}
            </label>
            <input
              id="family-number-input"
              type="tel"
              className="family-number-input"
              value={familyNumberInput}
              onChange={(e) => setFamilyNumberInput(e.target.value)}
              placeholder={t('familyNumberPlaceholder')}
            />
            <div className="family-number-actions">
              <button
                className="save-family-number-button"
                onClick={handleSaveFamilyNumber}
                disabled={familyNumberSaveStatus === 'saving' || familyNumberInput.trim() === ''}
              >
                {familyNumberSaveStatus === 'saved' ? t('familyNumberSaved') : t('saveFamilyNumber')}
              </button>
              {familyNumber && (
                <button
                  className="cancel-edit-button"
                  onClick={() => {
                    setFamilyNumberInput(familyNumber);
                    setEditingFamilyNumber(false);
                  }}
                >
                  {t('cancel')}
                </button>
              )}
            </div>
          </>
        ) : (
          <div className="family-number-display">
            <div className="family-number-display-value">
              <span className="family-number-display-label">{t('familyNumberLabel')}</span>
              <span className="family-number-display-number">{familyNumber}</span>
            </div>
            <button className="edit-family-number-button" onClick={() => setEditingFamilyNumber(true)}>
              {t('editFamilyNumber')}
            </button>
          </div>
        )}
      </section>

      <section className="alerts-card">
        <div className="card-header">
          <span className="card-icon">🕑</span>
          <h3>{t('recentFlaggedCalls')}</h3>
        </div>
        {recentAlerts.length === 0 ? (
          <p className="muted">{t('noFlaggedCalls')}</p>
        ) : (
          <ul>
            {recentAlerts.map((a, i) => (
              <li key={i}>{a.number} — {a.time}</li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

export default App;