package com.safevoice.app;

import android.content.Context;
import android.content.SharedPreferences;

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
}