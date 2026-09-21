package com.safevoice.app;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Large, high-contrast full-screen warning designed to be readable by senior
 * citizens at a glance, and shown even over the lock screen or another app.
 *
 * Two distinct sources trigger this screen:
 * 1. A blocklist match (v1) — CallReceiver launches this with just "number".
 * 2. A live-detection match (v2/v4) — ScamListenerService launches this with
 *    "number", "liveDetection"=true, "matchedKeyword", and "category". In
 *    this case we show static, rule-based tips (locked decision: NOT
 *    dynamic/AI-generated) instead of the "reported number" message, since
 *    nothing was actually reported — it was heard on this call.
 */
public class ScamWarningActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_scam_warning);

        String number = getIntent().getStringExtra("number");
        boolean liveDetection = getIntent().getBooleanExtra("liveDetection", false);
        String category = getIntent().getStringExtra("category");

        TextView headingView = findViewById(R.id.warning_heading);
        TextView numberView = findViewById(R.id.warning_number);
        TextView blocklistMessageView = findViewById(R.id.blocklist_message);
        TextView primaryTipView = findViewById(R.id.primary_tip);
        TextView categoryTipView = findViewById(R.id.category_tip);
        Button dismissButton = findViewById(R.id.dismiss_button);

        numberView.setText(number != null ? number : getString(R.string.unknown_number));

        if (liveDetection) {
            headingView.setText("⚠ POSSIBLE SCAM DETECTED");
            blocklistMessageView.setVisibility(View.GONE);

            primaryTipView.setText(ScamTipProvider.primaryTip());
            primaryTipView.setVisibility(View.VISIBLE);

            categoryTipView.setText(ScamTipProvider.categoryTip(this, category));
            categoryTipView.setVisibility(View.VISIBLE);
        } else {
            headingView.setText("⚠ SCAM NUMBER DETECTED");
            blocklistMessageView.setVisibility(View.VISIBLE);

            primaryTipView.setVisibility(View.GONE);
            categoryTipView.setVisibility(View.GONE);
        }

        dismissButton.setOnClickListener(v -> finish());
    }
}