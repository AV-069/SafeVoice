package com.safevoice.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "SafeVoice_CallReceiver";

    // Captured on RINGING so it's available when OFFHOOK fires, since the
    // incoming number isn't reliably present on the OFFHOOK broadcast itself.
    private static String pendingNumber = null;
    private static boolean pendingFlagged = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (!intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) return;

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.d(TAG, "Incoming call detected");

            boolean flagged = ScamBlocklist.getInstance(context).isFlagged(incomingNumber);
            String verdict = flagged ? "flagged" : "monitoring";

            pendingNumber = incomingNumber;
            pendingFlagged = flagged;

            // Full-screen native warning fires regardless of whether the app
            // process/web layer is alive — this is what actually protects the
            // user, unlike relying solely on the web dashboard.
            if (flagged) {
                Intent warningIntent = new Intent(context, ScamWarningActivity.class);
                warningIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                warningIntent.putExtra("number", incomingNumber);
                context.startActivity(warningIntent);
            }

            // Also update the in-app dashboard if it happens to be running.
            MainActivity.notifyCallState(incomingNumber, verdict);

        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            // Call connected. Start live audio scam detection ONLY for
            // numbers not already blocklisted (locked decision) — a
            // blocklisted number already got its warning on RINGING above,
            // so there's nothing more for the listener service to check.
            if (!pendingFlagged) {
                Log.d(TAG, "Call connected with unknown number — starting ScamListenerService");
                Intent serviceIntent = new Intent(context, ScamListenerService.class);
                serviceIntent.putExtra(ScamListenerService.EXTRA_PHONE_NUMBER, pendingNumber);
                context.startForegroundService(serviceIntent);
            }

        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            // Call ended — stop the listener service if it was running.
            context.stopService(new Intent(context, ScamListenerService.class));
            pendingNumber = null;
            pendingFlagged = false;
            MainActivity.notifyCallEnded();
        }
    }
}