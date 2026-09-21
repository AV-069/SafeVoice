package com.safevoice.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads scam_keywords.json from assets and checks transcribed speech
 * against the "universal" list plus the list for the user's currently
 * selected language (read from SafeVoiceSettingsPlugin's SharedPreferences
 * on every check, so a language change mid-call takes effect immediately).
 */
public class ScamKeywordEngine {

    private static final String ASSET_FILE = "scam_keywords.json";
    private static final String PREFS_NAME = "safevoice_prefs";
    private static final String PREFS_LANGUAGE_KEY = "language";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String UNIVERSAL_KEY = "universal";

    private final Context context;
    private final Map<String, List<String>> keywordsByLanguage = new HashMap<>();
    private List<String> universalKeywords = new ArrayList<>();
    private boolean loaded = false;

    public ScamKeywordEngine(Context context) {
        this.context = context.getApplicationContext();
        loadKeywords();
    }

    private void loadKeywords() {
        try (InputStream is = context.getAssets().open(ASSET_FILE)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONObject root = new JSONObject(builder.toString());

            universalKeywords = jsonArrayToList(root.optJSONArray(UNIVERSAL_KEY));

            java.util.Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (UNIVERSAL_KEY.equals(key) || "_readme".equals(key)) continue;
                keywordsByLanguage.put(key, jsonArrayToList(root.optJSONArray(key)));
            }
            loaded = true;
        } catch (IOException | JSONException e) {
            // Asset missing or malformed — engine will simply never flag anything
            // rather than crashing the call-monitoring service.
            loaded = false;
        }
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, null);
            if (value != null) list.add(value.toLowerCase(Locale.ROOT));
        }
        return list;
    }

    private String currentLanguage() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String code = prefs.getString(PREFS_LANGUAGE_KEY, DEFAULT_LANGUAGE);
        return (code == null || code.isEmpty()) ? DEFAULT_LANGUAGE : code;
    }

    /** Result of checking one transcript chunk. */
    public static class ScamCheckResult {
        public final boolean flagged;
        public final String matchedKeyword;
        public final String category;

        ScamCheckResult(boolean flagged, String matchedKeyword, String category) {
            this.flagged = flagged;
            this.matchedKeyword = matchedKeyword;
            this.category = category;
        }
    }

    /**
     * Checks a transcript chunk against the universal list plus the
     * currently selected language's list. Case-insensitive.
     */
    public ScamCheckResult checkText(String transcript) {
        if (!loaded || transcript == null || transcript.isEmpty()) {
            return new ScamCheckResult(false, null, null);
        }
        String lower = transcript.toLowerCase(Locale.ROOT);

        for (String keyword : universalKeywords) {
            if (lower.contains(keyword)) {
                return new ScamCheckResult(true, keyword, categoryFor(keyword));
            }
        }

        String language = currentLanguage();
        List<String> languageList = keywordsByLanguage.get(language);
        if (languageList == null) {
            languageList = keywordsByLanguage.get(DEFAULT_LANGUAGE);
        }
        if (languageList != null) {
            for (String keyword : languageList) {
                if (lower.contains(keyword)) {
                    return new ScamCheckResult(true, keyword, categoryFor(keyword));
                }
            }
        }

        return new ScamCheckResult(false, null, null);
    }

    /**
     * Lightweight grouping so v4 can show a tip tailored to what kind of
     * scam language was heard, without needing categories baked into the
     * JSON file itself. Falls back to "general" for anything unmatched.
     */
    private String categoryFor(String matchedKeyword) {
        String k = matchedKeyword.toLowerCase(Locale.ROOT);
        if (k.contains("otp") || k.contains("verification") || k.contains("one time password")) {
            return "otp";
        }
        if (k.contains("arrest") || k.contains("cbi") || k.contains("police") || k.contains("warrant")
                || k.contains("case") || k.contains("parcel") || k.contains("customs")
                || k.contains("income tax")) {
            return "digital_arrest";
        }
        if (k.contains("kyc") || k.contains("aadhar")) {
            return "kyc";
        }
        if (k.contains("refund") || k.contains("lottery") || k.contains("processing fee")) {
            return "refund_lottery";
        }
        if (k.contains("anydesk") || k.contains("screen share") || k.contains("screen")) {
            return "remote_access";
        }
        if (k.contains("electricity")) {
            return "utility";
        }
        if (k.contains("secret") || k.contains("confidential") || k.contains("do not tell")
                || k.contains("do not disconnect")) {
            return "secrecy";
        }
        return "general";
    }
}