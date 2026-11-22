package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Telegram only keeps the latest text of an edited message. While Omnigram runs it remembers the text of messages it
 * loads, and when an edit arrives it saves the text from before the edit here, so it can be shown later.
 * Edits that happened while Omnigram had not yet seen the earlier text cannot be recovered.
 */
public final class OmnigramEditHistory {
    private OmnigramEditHistory() {}

    private static final Map<String, String> LAST_TEXT = new LinkedHashMap<String, String>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 4000;
        }
    };

    private static SharedPreferences sp() {
        return ApplicationLoader.applicationContext.getSharedPreferences("omnigram_edit_history", Context.MODE_PRIVATE);
    }

    private static String key(long dialogId, int id) {
        return dialogId + ":" + id;
    }

    /** Called for every loaded message: remember its current text. */
    public static void remember(TLRPC.Message m) {
        try {
            if (m == null || m.message == null || m.message.isEmpty()) return;
            long did = MessageObject.getDialogId(m);
            synchronized (LAST_TEXT) {
                LAST_TEXT.put(key(did, m.id), m.message);
            }
        } catch (Throwable ignore) {}
    }

    /** Called when an edit update arrives, before the new text replaces the old one. */
    public static void onEdited(TLRPC.Message edited) {
        try {
            if (edited == null) return;
            long did = MessageObject.getDialogId(edited);
            String k = key(did, edited.id);
            String old;
            synchronized (LAST_TEXT) {
                old = LAST_TEXT.get(k);
            }
            if (old == null || old.equals(edited.message)) return;
            SharedPreferences p = sp();
            JSONArray arr = new JSONArray(p.getString(k, "[]"));
            JSONObject o = new JSONObject();
            o.put("t", edited.edit_date > 0 ? edited.edit_date : (int) (System.currentTimeMillis() / 1000));
            o.put("o", old);
            arr.put(o);
            while (arr.length() > 20) arr.remove(0);
            if (p.getAll().size() > 500) {
                SharedPreferences.Editor ed = p.edit();
                int drop = 100;
                for (Map.Entry<String, ?> en : p.getAll().entrySet()) {
                    if (drop-- <= 0) break;
                    ed.remove(en.getKey());
                }
                ed.apply();
            }
            p.edit().putString(k, arr.toString()).apply();
        } catch (Throwable ignore) {}
    }

    public static JSONArray get(long dialogId, int id) {
        try {
            return new JSONArray(sp().getString(key(dialogId, id), "[]"));
        } catch (Throwable t) {
            return new JSONArray();
        }
    }
}
