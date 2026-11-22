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
    public static final String HIDE_ARCHIVED = "hide_archived";
    public static final String GLARE = "glare";
    public static final String NO_ROUNDING = "no_rounding";
    public static final String VIBRATION = "in_app_vibration";
    public static final String OPEN_ARCHIVE_PULLDOWN = "open_archive_pulldown";
    public static final String DISABLE_UNARCHIVE_SWIPE = "disable_unarchive_swipe";
    public static final String PREMIUM_EMOJI = "premium_emoji";
    public static final String FORCE_SNOW = "force_snow";
    public static final String SYSTEM_FONT = "system_font";
    public static final int ID_TELEGRAM = 0, ID_BOT_API = 1, ID_HIDE = 2;

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
    public static boolean hideArchived() { return get(HIDE_ARCHIVED, false); }
    public static boolean glare() { return get(GLARE, false); }
    public static boolean noRounding() { return get(NO_ROUNDING, false); }
    public static boolean vibration() { return get(VIBRATION, false); }
    public static boolean openArchivePulldown() { return get(OPEN_ARCHIVE_PULLDOWN, true); }
    public static boolean disableUnarchiveSwipe() { return get(DISABLE_UNARCHIVE_SWIPE, true); }
    public static boolean premiumEmoji() { return get(PREMIUM_EMOJI, false); }
    public static boolean forceSnow() { return get(FORCE_SNOW, false); }
    public static boolean systemFont() { return get(SYSTEM_FONT, false); }

    public static int getInt(String key, int def) {
        try { return sp().getInt(key, def); } catch (Throwable t) { return def; }
    }

    public static void setInt(String key, int value) {
        try { sp().edit().putInt(key, value).apply(); } catch (Throwable ignore) {}
    }

    public static int bubbleStyle() { return getInt("bubble_style", 0); }
    public static int checkStyle() { return getInt("check_style", 0); }
    public static int deletedIcon() { return getInt("deleted_icon", 0); }
    public static int deletedColor() { return getInt("deleted_color", 0); }

    /** 0 = square ... 100 = circle (default). */
    public static int avatarCorner() {
        try { return sp().getInt("avatar_corner", 100); } catch (Throwable t) { return 100; }
    }

    public static void setAvatarCorner(int v) {
        try { sp().edit().putInt("avatar_corner", Math.max(0, Math.min(100, v))).apply(); } catch (Throwable ignore) {}
    }

    /** Chat list title: 0 name, 1 username, 2 "Omnigram", 3 "Chats". */
    public static int titleMode() {
        try { return sp().getInt("title_mode", 0); } catch (Throwable t) { return 0; }
    }

    public static void setTitleMode(int m) {
        try { sp().edit().putInt("title_mode", m).apply(); } catch (Throwable ignore) {}
    }

    public static int idMode() {
        try { return sp().getInt("id_mode", ID_TELEGRAM); } catch (Throwable t) { return ID_TELEGRAM; }
    }

    public static void setIdMode(int mode) {
        try { sp().edit().putInt("id_mode", mode).apply(); } catch (Throwable ignore) {}
    }

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
