package com.safevoice.app;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Large, high-contrast full-screen warning designed to be readable by senior
 * citizens at a glance, and shown even over the lock screen or another app.
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
        TextView numberView = findViewById(R.id.warning_number);
        numberView.setText(number != null ? number : getString(R.string.unknown_number));

        Button dismissButton = findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(v -> finish());
    }
}
