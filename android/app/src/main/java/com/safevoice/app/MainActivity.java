package com.safevoice.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends BridgeActivity {

    // Exposed so CallReceiver can push events into the web layer while the
    // app is alive, without holding a second Activity reference around.
    public static MainActivity instance = null;

    private static final int PERMISSION_REQUEST_CODE = 4201;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ScamReportPlugin.class);
        registerPlugin(SafeVoiceSettingsPlugin.class);
        registerPlugin(SafeVoiceTtsPlugin.class);
        super.onCreate(savedInstanceState);
        instance = this;
        requestCorePermissions();
        new Handler(Looper.getMainLooper()).postDelayed(this::emitPermissionStatus, 800);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null; // Prevent static memory leaks on exit
    }

    @Override
    public void onResume() {
        super.onResume();
        // Overlay permission is granted via a separate Settings screen, so
        // re-check and report status every time the user comes back to the app.
        emitPermissionStatus();
    }

    private void requestCorePermissions() {
        java.util.List<String> toRequest = new java.util.ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(permission);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            emitPermissionStatus();
        }

        if (!Settings.canDrawOverlays(this)) {
            Uri uri = Uri.parse("package:" + getPackageName());
            startActivity(new android.content.Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        emitPermissionStatus();
    }

    private void emitPermissionStatus() {
        boolean phoneStateGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
        boolean notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean microphoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        boolean smsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;

        JSObject data = new JSObject();
        data.put("phoneState", phoneStateGranted ? "granted" : "denied");
        data.put("notifications", notificationsGranted ? "granted" : "denied");
        data.put("overlay", overlayGranted ? "granted" : "denied");
        data.put("microphone", microphoneGranted ? "granted" : "denied");
        data.put("sms", smsGranted ? "granted" : "denied");
        if (getBridge() != null) {
            getBridge().triggerJSEvent("permissionsUpdated", "window", data.toString());
        }
    }

    /** Called by CallReceiver to update the dashboard while the app is in the foreground. */
    static void notifyCallState(String number, String verdict) {
        if (instance == null || instance.getBridge() == null) return;
        JSObject data = new JSObject();
        data.put("number", number);
        data.put("verdict", verdict);
        instance.getBridge().triggerJSEvent("incomingCall", "window", data.toString());
    }

    static void notifyCallEnded() {
        if (instance == null || instance.getBridge() == null) return;
        instance.getBridge().triggerJSEvent("callEnded", "window", "{}");
    }
}