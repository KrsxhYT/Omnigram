package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Durable deleted-message tracking (survives process death).
 * Stores minimal snapshot so UI can show dustbin + 70% opacity.
 */
public final class OmnigramDeleted {
    private static final String PREF = "omnigram_deleted_v1";
    private static final Set<String> MEM = new HashSet<>();
    private static SharedPreferences prefs;

    private OmnigramDeleted() {}

    private static SharedPreferences p() {
        if (prefs == null) {
            Context c = ApplicationLoader.applicationContext;
            if (c != null) prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        }
        return prefs;
    }

    private static String key(long dialogId, int msgId) {
        return dialogId + ":" + msgId;
    }

    public static void mark(long dialogId, int msgId) {
        if (msgId == 0) return;
        String k = key(dialogId, msgId);
        MEM.add(k);
        MEM.add(key(0, msgId));
        try {
            SharedPreferences sp = p();
            if (sp != null) sp.edit().putString(k, "1").putString(key(0, msgId), "1").apply();
        } catch (Throwable ignore) {}
    }

    /** Save snapshot before delete-for-everyone so we can still render a ghost bubble. */
    public static void markAndSave(MessageObject mo) {
        if (mo == null) return;
        long did = mo.getDialogId();
        int mid = mo.getId();
        mark(did, mid);
        try {
            String text = mo.messageText != null ? mo.messageText.toString() : (mo.messageOwner != null && mo.messageOwner.message != null ? mo.messageOwner.message : "");
            int date = mo.messageOwner != null ? mo.messageOwner.date : 0;
            boolean out = mo.isOutOwner();
            String payload = date + "|" + (out ? "1" : "0") + "|" + text;
            SharedPreferences sp = p();
            if (sp != null) sp.edit().putString("snap:" + key(did, mid), payload).apply();
        } catch (Throwable ignore) {}
    }

    public static boolean isDeleted(long dialogId, int msgId) {
        if (msgId == 0) return false;
        String k = key(dialogId, msgId);
        if (MEM.contains(k) || MEM.contains(key(0, msgId))) return true;
        try {
            SharedPreferences sp = p();
            if (sp != null && (sp.contains(k) || sp.contains(key(0, msgId)))) {
                MEM.add(k);
                return true;
            }
        } catch (Throwable ignore) {}
        return false;
    }

    public static void markMany(long dialogId, java.util.ArrayList<Integer> ids) {
        if (ids == null) return;
        for (int i = 0; i < ids.size(); i++) mark(dialogId, ids.get(i));
    }

    public static void markManyChannel(long channelId, java.util.ArrayList<Integer> ids) {
        markMany(-channelId, ids);
    }

    /** Load all deleted keys for a dialog into memory (call when opening chat). */
    public static void warm(long dialogId) {
        try {
            SharedPreferences sp = p();
            if (sp == null) return;
            Map<String, ?> all = sp.getAll();
            String prefix = dialogId + ":";
            for (String k : all.keySet()) {
                if (k.startsWith(prefix) || k.startsWith("0:")) MEM.add(k.startsWith("snap:") ? k.substring(5) : k);
            }
        } catch (Throwable ignore) {}
    }
}
