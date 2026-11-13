package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;

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

    private static final Set<String> OWN = new HashSet<>();

    private static String key(long dialogId, int msgId) {
        return dialogId + ":" + msgId;
    }

    /** Message ids are account-wide for users/small groups (stored under dialog 0) but per-channel for channels. */
    private static long scope(long dialogId) {
        try {
            if (dialogId < 0 && ChatObject.isChannel(MessagesController.getInstance(UserConfig.selectedAccount).getChat(-dialogId))) {
                return dialogId;
            }
        } catch (Throwable ignore) {}
        return 0;
    }

    /** Call when the USER deletes messages, so the echo from the server is not treated as someone else's delete. */
    public static void rememberOwn(long dialogId, java.util.ArrayList<Integer> ids) {
        if (ids == null) return;
        synchronized (OWN) {
            for (int i = 0; i < ids.size(); i++) OWN.add(key(scope(dialogId), ids.get(i)));
        }
    }

    /**
     * Incoming delete from someone else: keep the messages (just mark them) and return only the ids that
     * the user deleted on purpose, which still get removed as usual. {@code storageKey} is 0 or -channelId.
     */
    public static java.util.ArrayList<Integer> keepIncoming(long storageKey, java.util.ArrayList<Integer> ids) {
        java.util.ArrayList<Integer> toDelete = new java.util.ArrayList<>();
        if (ids == null) return toDelete;
        for (int i = 0; i < ids.size(); i++) {
            Integer id = ids.get(i);
            boolean own;
            synchronized (OWN) {
                own = OWN.remove(key(storageKey, id));
            }
            if (own) {
                toDelete.add(id);
            } else {
                mark(storageKey, id);
            }
        }
        return toDelete;
    }

    public static void mark(long dialogId, int msgId) {
        if (msgId == 0) return;
        String k = key(scope(dialogId), msgId);
        MEM.add(k);
        try {
            SharedPreferences sp = p();
            if (sp != null) sp.edit().putString(k, "1").apply();
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
        String k = key(scope(dialogId), msgId);
        if (MEM.contains(k)) return true;
        try {
            SharedPreferences sp = p();
            if (sp != null && sp.contains(k)) {
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
                if (!k.startsWith("snap:") && (k.startsWith(prefix) || k.startsWith("0:"))) MEM.add(k);
            }
        } catch (Throwable ignore) {}
    }
}
