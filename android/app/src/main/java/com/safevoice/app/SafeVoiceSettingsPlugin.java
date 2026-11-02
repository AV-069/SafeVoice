package com.safevoice.app;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "SafeVoiceSettings")
public class SafeVoiceSettingsPlugin extends Plugin {

    private static final String PREFS_NAME = "safevoice_prefs";
    private static final String LANGUAGE_KEY = "language";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String FAMILY_NUMBER_KEY = "family_number";

    @PluginMethod
    public void setLanguage(PluginCall call) {
        String code = call.getString("code");
        if (code == null || code.isEmpty()) {
            call.reject("Missing language code");
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(LANGUAGE_KEY, code).apply();

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void getLanguage(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String code = prefs.getString(LANGUAGE_KEY, DEFAULT_LANGUAGE);

        JSObject ret = new JSObject();
        ret.put("code", code);
        call.resolve(ret);
    }

    @PluginMethod
    public void setFamilyNumber(PluginCall call) {
        String number = call.getString("number");
        // Allow clearing the field: empty/null is valid and means "no
        // family number set", not an error — SMS sending just no-ops then.
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(FAMILY_NUMBER_KEY, number == null ? "" : number.trim()).apply();

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void getFamilyNumber(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String number = prefs.getString(FAMILY_NUMBER_KEY, "");

        JSObject ret = new JSObject();
        ret.put("number", number);
        call.resolve(ret);
    }

    /**
     * Pull-based permission check, used on React mount so the dashboard
     * doesn't depend on catching the "permissionsUpdated" event that
     * MainActivity fires — that event can be dispatched before the
     * WebView has finished loading and attaching its listener, especially
     * on a cold start, so it's effectively a coin flip whether React ever
     * sees it. This gives the same five values on demand instead.
     */
    @PluginMethod
    public void getPermissionStatus(PluginCall call) {
        Context context = getContext();

        boolean phoneStateGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
        boolean notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        boolean overlayGranted = Settings.canDrawOverlays(context);
        boolean microphoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        boolean smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;

        JSObject ret = new JSObject();
        ret.put("phoneState", phoneStateGranted ? "granted" : "denied");
        ret.put("notifications", notificationsGranted ? "granted" : "denied");
        ret.put("overlay", overlayGranted ? "granted" : "denied");
        ret.put("microphone", microphoneGranted ? "granted" : "denied");
        ret.put("sms", smsGranted ? "granted" : "denied");
        call.resolve(ret);
    }
}