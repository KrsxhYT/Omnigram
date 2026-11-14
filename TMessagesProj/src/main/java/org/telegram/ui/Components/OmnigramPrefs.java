package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** Omnigram-only switches (Settings > Omni Preference). */
public final class OmnigramPrefs {
    public static final String TIME_SECONDS = "time_seconds";
    public static final String NO_ADS = "no_ads";
    public static final String HIDE_PHONE = "hide_phone";
    public static final String FAST_DOWNLOAD = "fast_download";
    public static final String LAST_SEEN_ALL = "last_seen_all";
    public static final String VERIFIED_ANIM = "verified_anim";
    public static final String STICKER_UNDO = "sticker_undo";
    public static final String QUICK_STICKER_BAR = "quick_sticker_bar";

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
    public static boolean fastDownload() { return get(FAST_DOWNLOAD, true); }
    public static boolean lastSeenAll() { return get(LAST_SEEN_ALL, false); }
    public static boolean verifiedAnim() { return get(VERIFIED_ANIM, true); }
    public static boolean stickerUndo() { return get(STICKER_UNDO, true); }
    public static boolean quickStickerBar() { return get(QUICK_STICKER_BAR, false); }

    /** Local-only nickname used for "@" suggestions inside this app. It never changes the real Telegram profile. */
    public static String getAlias(long dialogId) {
        try { return sp().getString("alias_" + dialogId, ""); } catch (Throwable t) { return ""; }
    }

    public static void setAlias(long dialogId, String alias) {
        try {
            if (alias == null || alias.trim().isEmpty()) sp().edit().remove("alias_" + dialogId).apply();
            else sp().edit().putString("alias_" + dialogId, alias.trim()).apply();
        } catch (Throwable ignore) {}
    }
}
