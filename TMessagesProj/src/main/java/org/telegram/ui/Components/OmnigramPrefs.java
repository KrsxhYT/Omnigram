package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** Omnigram-only switches (Settings > Omni Preference). */
public final class OmnigramPrefs {
    public static final String TIME_SECONDS = "time_seconds";
    public static final String NO_ADS = "no_ads";
    public static final String HIDE_PHONE = "hide_phone";

    private OmnigramPrefs() {}

    private static SharedPreferences sp() {
        return ApplicationLoader.applicationContext.getSharedPreferences("omnigram_prefs", Context.MODE_PRIVATE);
    }

    public static boolean get(String key, boolean def) {
        try {
            return sp().getBoolean(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public static void set(String key, boolean value) {
        try {
            sp().edit().putBoolean(key, value).apply();
        } catch (Throwable ignore) {}
    }

    public static boolean timeSeconds() { return get(TIME_SECONDS, false); }
    public static boolean noAds() { return get(NO_ADS, false); }
    public static boolean hidePhone() { return get(HIDE_PHONE, false); }
}
