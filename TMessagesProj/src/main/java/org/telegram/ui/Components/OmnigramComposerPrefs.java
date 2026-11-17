package org.telegram.ui.Components;

import org.json.JSONArray;
import org.json.JSONObject;

/** On/off switches and saved data for the composer tools (Omni Preference > Chat > Advanced Settings). */
public final class OmnigramComposerPrefs {
    private OmnigramComposerPrefs() {}

    public static final String CONTEXT_MENU = "cmp_context_menu";
    public static final String FORMAT_TOOLBAR = "cmp_format_toolbar";
    public static final String CONVERTER = "cmp_converter";
    public static final String LIVE_PREVIEW = "cmp_live_preview";
    public static final String AUTO_FIX = "cmp_auto_fix";
    public static final String COUNTER = "cmp_counter";
    public static final String THREAD_SPLITTER = "cmp_thread_splitter";
    public static final String LIST_TABLE = "cmp_list_table";
    public static final String UNDO_REDO = "cmp_undo_redo";
    public static final String GRAMMAR_AI = "cmp_grammar_ai";
    public static final String TRANSLATE_AI = "cmp_translate_ai";
    public static final String EMOJI_SUGGEST = "cmp_emoji_suggest";
    public static final String DENSITY_SLIDER = "cmp_density_slider";
    public static final String WELCOME = "cmp_welcome";

    /** Every tool is on by default; the user can switch each one off. */
    public static boolean on(String key) {
        return OmnigramPrefs.get(key, true);
    }

    public static int density() {
        try {
            return org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences("omnigram_prefs", android.content.Context.MODE_PRIVATE).getInt("cmp_density", 2);
        } catch (Throwable t) {
            return 2;
        }
    }

    public static void setDensity(int d) {
        try {
            org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences("omnigram_prefs", android.content.Context.MODE_PRIVATE).edit().putInt("cmp_density", d).apply();
        } catch (Throwable ignore) {}
    }

    // ---- welcome / auto-reply templates
    public static JSONArray templates() {
        try {
            String raw = org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences("omnigram_prefs", android.content.Context.MODE_PRIVATE).getString("cmp_templates", "[]");
            return new JSONArray(raw);
        } catch (Throwable t) {
            return new JSONArray();
        }
    }

    public static void saveTemplates(JSONArray arr) {
        try {
            org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences("omnigram_prefs", android.content.Context.MODE_PRIVATE).edit().putString("cmp_templates", arr.toString()).apply();
        } catch (Throwable ignore) {}
    }

    public static JSONObject template(String name, String text, boolean enabled) {
        try {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("text", text);
            o.put("enabled", enabled);
            return o;
        } catch (Throwable t) {
            return new JSONObject();
        }
    }
}
