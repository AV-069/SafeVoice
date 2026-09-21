package com.safevoice.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.getcapacitor.JSObject;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Foreground service that runs while a call with an unknown (non-blocklisted)
 * number is active. Auto-enables speakerphone, runs the built-in Android
 * SpeechRecognizer in a restart loop, and checks each transcript chunk
 * against ScamKeywordEngine. On a match, stops listening, launches
 * ScamWarningActivity with liveDetection info, and (if a family number is
 * configured) sends a short SMS alert.
 */
public class ScamListenerService extends Service {

    private static final String TAG = "SafeVoice_Listener";

    public static final String EXTRA_PHONE_NUMBER = "phoneNumber";

    private static final String CHANNEL_ID = "safevoice_listener_channel";
    private static final int NOTIFICATION_ID = 4301;
    private static final String PREFS_NAME = "safevoice_prefs";
    private static final String PREFS_LANGUAGE_KEY = "language";
    private static final String PREFS_FAMILY_NUMBER_KEY = "family_number";
    private static final String DEFAULT_LANGUAGE = "en";

    private SpeechRecognizer speechRecognizer;
    private ScamKeywordEngine keywordEngine;
    private AudioManager audioManager;
    private Handler mainHandler;
    private String currentNumber;
    private boolean listening = false;
    private boolean stoppedForMatch = false;

    @Override
    public void onCreate() {
        super.onCreate();
        keywordEngine = new ScamKeywordEngine(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        currentNumber = intent != null ? intent.getStringExtra(EXTRA_PHONE_NUMBER) : null;
        startForeground(NOTIFICATION_ID, buildNotification());
        enableSpeakerphone();
        startListeningLoop();
        return START_NOT_STICKY;
    }

    private void enableSpeakerphone() {
        // Locked decision: auto-speaker on for unknown numbers, no dialer-app
        // privileges needed. If the user or caller later switches it off, we
        // do NOT treat that as suspicious (false-positive risk) — we just
        // show an informational "protection paused" state via the JS event.
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(true);
    }

    private boolean isSpeakerphoneOn() {
        return audioManager.isSpeakerphoneOn();
    }

    private void startListeningLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            notifyProtectionPaused("unavailable");
            return;
        }

        mainHandler.post(() -> {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onResults(Bundle results) {
                    handleTranscript(results);
                    restartListeningIfActive();
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    handleTranscript(partialResults);
                }

                @Override
                public void onError(int error) {
                    // Common during normal use (e.g. brief silence timeouts).
                    // Just restart the loop rather than surfacing every error.
                    restartListeningIfActive();
                }

                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
            beginSingleListenCycle();
        });
    }

    private void beginSingleListenCycle() {
        if (stoppedForMatch || speechRecognizer == null) return;

        if (!isSpeakerphoneOn()) {
            notifyProtectionPaused("speaker_off");
            // Keep retrying — the user or a legitimate reason may turn it
            // back on mid-call; we don't want to permanently give up.
            mainHandler.postDelayed(this::beginSingleListenCycle, 3000);
            return;
        }

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognizerLocaleTag());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        try {
            speechRecognizer.startListening(recognizerIntent);
            listening = true;
        } catch (Exception e) {
            restartListeningIfActive();
        }
    }

    private String recognizerLocaleTag() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String code = prefs.getString(PREFS_LANGUAGE_KEY, DEFAULT_LANGUAGE);
        switch (code == null ? DEFAULT_LANGUAGE : code) {
            case "hi": return "hi-IN";
            case "ta": return "ta-IN";
            case "te": return "te-IN";
            case "ml": return "ml-IN";
            case "kn": return "kn-IN";
            default: return "en-IN";
        }
    }

    private void handleTranscript(Bundle results) {
        if (results == null) return;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) return;

        for (String transcript : matches) {
            ScamKeywordEngine.ScamCheckResult result = keywordEngine.checkText(transcript);
            if (result.flagged) {
                onScamDetected(result.matchedKeyword, result.category);
                return;
            }
        }
    }

    private void restartListeningIfActive() {
        listening = false;
        if (!stoppedForMatch) {
            mainHandler.postDelayed(this::beginSingleListenCycle, 400);
        }
    }

    private void onScamDetected(String matchedKeyword, String category) {
        stoppedForMatch = true;
        stopListeningInternal();

        Intent warningIntent = new Intent(this, ScamWarningActivity.class);
        warningIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        warningIntent.putExtra("number", currentNumber);
        warningIntent.putExtra("liveDetection", true);
        warningIntent.putExtra("matchedKeyword", matchedKeyword);
        warningIntent.putExtra("category", category != null ? category : "general");
        startActivity(warningIntent);

        JSObject data = new JSObject();
        data.put("number", currentNumber);
        data.put("verdict", "flagged");
        data.put("liveDetection", true);
        data.put("category", category != null ? category : "general");
        MainActivity.notifyCallState(currentNumber, "flagged");

        sendFamilyAlertSms(category);
    }

    /**
     * Sends a short SMS to the configured family number, if any. Wrapped so
     * that no failure here (missing permission, no SIM, no signal, blank
     * number) can ever crash or interrupt the detection/warning flow —
     * the warning screen has already been shown regardless of this outcome.
     */
    private void sendFamilyAlertSms(String category) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String familyNumber = prefs.getString(PREFS_FAMILY_NUMBER_KEY, "");
            if (familyNumber == null || familyNumber.trim().isEmpty()) {
                return; // Feature not configured — nothing to do.
            }

            String numberText = (currentNumber == null || currentNumber.isEmpty())
                    ? "an unknown number" : currentNumber;
            String message = "SafeVoice Alert: A possible scam call was detected from "
                    + numberText + ". Please check on your family member.";

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(familyNumber.trim(), null, message, null, null);
            Log.d(TAG, "Family alert SMS sent for category: " + category);
        } catch (Exception e) {
            // Deliberately swallowed — see method comment. Log only, never
            // let an SMS failure affect the scam-detection flow itself.
            Log.e(TAG, "Failed to send family alert SMS", e);
        }
    }

    private void notifyProtectionPaused(String reason) {
        // Informational only — locked decision explicitly rejects treating
        // "not on speaker" as a suspicion signal. JS layer can show a
        // "Protection paused" message using this event.
        if (MainActivity.instance == null || MainActivity.instance.getBridge() == null) return;
        JSObject data = new JSObject();
        data.put("number", currentNumber);
        data.put("reason", reason);
        MainActivity.instance.getBridge().triggerJSEvent("protectionPaused", "window", data.toString());
    }

    private void stopListeningInternal() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }
            speechRecognizer = null;
        }
        listening = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SafeVoice Call Protection",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows while SafeVoice is checking an active call for scam warning signs.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeVoice is protecting this call")
                .setContentText("Listening for common scam warning signs.")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        stoppedForMatch = true;
        stopListeningInternal();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}