import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { registerPlugin } from '@capacitor/core';
import './Onboarding.css';

const SafeVoiceTts = registerPlugin('SafeVoiceTts');

const STEPS = [
  { titleKey: 'onboardingTitle1', bodyKey: 'onboardingBody1' },
  { titleKey: 'onboardingTitle2', bodyKey: 'onboardingBody2' },
  { titleKey: 'onboardingTitle3', bodyKey: 'onboardingBody3' }
];

function Onboarding({ onComplete }) {
  const { t, i18n } = useTranslation();
  const [step, setStep] = useState(0);
  const [speaking, setSpeaking] = useState(false);

  const isLastStep = step === STEPS.length - 1;
  const current = STEPS[step];

  const stopSpeaking = () => {
    SafeVoiceTts.stop().catch(() => {
      // Native side unavailable (e.g. running in a browser during dev) — ignore.
    });
  };

  const handleListen = async () => {
    const text = `${t(current.titleKey)}. ${t(current.bodyKey)}`;
    setSpeaking(true);
    try {
      await SafeVoiceTts.speak({ text, language: i18n.language });
    } catch (e) {
      // TTS unavailable — the text is still readable on screen either way.
    } finally {
      setSpeaking(false);
    }
  };

  const finish = () => {
    localStorage.setItem('safevoice_onboarding_complete', 'true');
    onComplete();
  };

  const handleNext = () => {
    stopSpeaking();
    if (isLastStep) {
      finish();
    } else {
      setStep((s) => s + 1);
    }
  };

  const handleSkip = () => {
    stopSpeaking();
    finish();
  };

  return (
    <div className="onboarding-container">
      <div className="onboarding-progress">
        {STEPS.map((_, i) => (
          <span key={i} className={`onboarding-dot ${i === step ? 'active' : ''}`} />
        ))}
      </div>

      <div className="onboarding-card">
        <h2>{t(current.titleKey)}</h2>
        <p>{t(current.bodyKey)}</p>

        <button
          className="onboarding-listen-button"
          onClick={handleListen}
          disabled={speaking}
        >
          🔊 {t('listenButton')}
        </button>
      </div>

      <div className="onboarding-actions">
        {!isLastStep && (
          <button className="onboarding-skip-button" onClick={handleSkip}>
            {t('skip')}
          </button>
        )}
        <button className="onboarding-next-button" onClick={handleNext}>
          {isLastStep ? t('getStarted') : t('next')}
        </button>
      </div>
    </div>
  );
}

export default Onboarding;