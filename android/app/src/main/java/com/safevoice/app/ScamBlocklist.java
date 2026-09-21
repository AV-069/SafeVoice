package com.safevoice.app;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads scam_numbers.json bundled in assets/ and checks incoming numbers
 * against it. Fully offline — no network call is made.
 *
 * assets/scam_numbers.json must stay in sync with src/data/scamNumbers.json;
 * run `npm run android:sync` after editing the web copy, or update both by hand.
 */
public class ScamBlocklist {

    private static final String TAG = "SafeVoice_Blocklist";
    private static final String ASSET_FILE = "scam_numbers.json";

    private final Set<String> exactNumbers = new HashSet<>();
    private final List<String> prefixPatterns = new ArrayList<>();
    private static ScamBlocklist instance;
    private android.content.SharedPreferences prefs;
    private static final String PREFS_NAME = "safevoice_reports";
    private static final String REPORTED_KEY = "user_reported_numbers";
    private ScamBlocklist(Context context) {
        load(context);
    }

    public static synchronized ScamBlocklist getInstance(Context context) {
        if (instance == null) {
            instance = new ScamBlocklist(context.getApplicationContext());
        }
        return instance;
    }

    private void load(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try (InputStream is = context.getAssets().open(ASSET_FILE)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject root = new JSONObject(sb.toString());

            JSONArray exact = root.optJSONArray("exactNumbers");
            if (exact != null) {
                for (int i = 0; i < exact.length(); i++) {
                    exactNumbers.add(normalize(exact.getString(i)));
                }
            }

            JSONArray prefixes = root.optJSONArray("prefixPatterns");
            if (prefixes != null) {
                for (int i = 0; i < prefixes.length(); i++) {
                    prefixPatterns.add(normalize(prefixes.getString(i)));
                }
            }

            Log.d(TAG, "Loaded " + exactNumbers.size() + " exact numbers, "
                    + prefixPatterns.size() + " prefix patterns");
        } catch (IOException | org.json.JSONException e) {
            Log.e(TAG, "Failed to load " + ASSET_FILE + " — treating blocklist as empty", e);
        }
    }

    /** Strips spaces, dashes, and a leading +91/0 so formatting differences don't cause misses. */
    private String normalize(String number) {
        if (number == null) return "";
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.startsWith("91") && digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        } else if (digits.startsWith("0") && digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits;
    }

    public boolean isFlagged(String rawNumber) {
        if (rawNumber == null) return false;
        String normalized = normalize(rawNumber);
        if (normalized.isEmpty()) return false;

        if (exactNumbers.contains(normalized)) return true;
        if (getUserReportedNumbers().contains(normalized)) return true;

        for (String prefix : prefixPatterns) {
            if (!prefix.isEmpty() && normalized.startsWith(prefix)) return true;
        }
        return false;
    }

    public void addUserReportedNumber(String rawNumber) {
        String normalized = normalize(rawNumber);
        if (normalized.isEmpty()) return;

        java.util.Set<String> current = new HashSet<>(prefs.getStringSet(REPORTED_KEY, new HashSet<>()));
        current.add(normalized);
        prefs.edit().putStringSet(REPORTED_KEY, current).apply();

        exactNumbers.add(normalized); // reflect immediately without a reload
    }

    private java.util.Set<String> getUserReportedNumbers() {
        return prefs.getStringSet(REPORTED_KEY, new HashSet<>());
    }
}