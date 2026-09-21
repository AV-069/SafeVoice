package com.safevoice.app;

import android.speech.tts.TextToSpeech;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CapacitorPlugin(name = "SafeVoiceTts")
public class SafeVoiceTtsPlugin extends Plugin implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean ready = false;

    private static final Map<String, Locale> LOCALES = new HashMap<>();
    static {
        LOCALES.put("en", new Locale("en", "IN"));
        LOCALES.put("hi", new Locale("hi", "IN"));
        LOCALES.put("ta", new Locale("ta", "IN"));
        LOCALES.put("te", new Locale("te", "IN"));
        LOCALES.put("ml", new Locale("ml", "IN"));
        LOCALES.put("kn", new Locale("kn", "IN"));
    }

    @Override
    public void load() {
        tts = new TextToSpeech(getContext(), this);
    }

    @Override
    public void onInit(int status) {
        ready = (status == TextToSpeech.SUCCESS);
    }

    @PluginMethod
    public void speak(PluginCall call) {
        String text = call.getString("text");
        String lang = call.getString("language", "en");

        if (text == null || text.isEmpty()) {
            call.reject("Missing text");
            return;
        }
        if (!ready || tts == null) {
            call.reject("TTS engine not ready");
            return;
        }

        Locale locale = LOCALES.containsKey(lang) ? LOCALES.get(lang) : Locale.ENGLISH;
        int result = tts.setLanguage(locale);
        boolean supported = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;

        if (!supported) {
            // Voice pack for this language isn't installed on the device —
            // fall back to English rather than staying silent.
            tts.setLanguage(Locale.ENGLISH);
        }

        tts.stop();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "safevoice_utterance");

        JSObject ret = new JSObject();
        ret.put("success", true);
        ret.put("usedFallbackLanguage", !supported);
        call.resolve(ret);
    }

    @PluginMethod
    public void stop(PluginCall call) {
        if (tts != null) tts.stop();
        call.resolve();
    }

    @Override
    protected void handleOnDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.handleOnDestroy();
    }
}