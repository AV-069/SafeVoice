package com.safevoice.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "SafeVoice_CallReceiver";

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

        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            MainActivity.notifyCallEnded();
        }
    }
}
