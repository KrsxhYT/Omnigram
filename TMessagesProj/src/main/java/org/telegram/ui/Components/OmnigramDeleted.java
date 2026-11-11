package org.telegram.ui.Components;

import java.util.HashSet;
import java.util.Set;

/** Tracks deleted message ids so cells can show dustbin + 70% opacity. */
public final class OmnigramDeleted {
    private static final Set<String> KEYS = new HashSet<>();
    private OmnigramDeleted() {}
    private static String key(long dialogId, int msgId) {
        return dialogId + ":" + msgId;
    }
    public static void mark(long dialogId, int msgId) {
        if (msgId == 0) return;
        KEYS.add(key(dialogId, msgId));
        // also mark with 0 dialog for channel updates that omit peer
        KEYS.add(key(0, msgId));
    }
    public static boolean isDeleted(long dialogId, int msgId) {
        if (msgId == 0) return false;
        return KEYS.contains(key(dialogId, msgId)) || KEYS.contains(key(0, msgId));
    }
    public static void markMany(long dialogId, java.util.ArrayList<Integer> ids) {
        if (ids == null) return;
        for (int i = 0; i < ids.size(); i++) mark(dialogId, ids.get(i));
    }
    public static void markManyChannel(long channelId, java.util.ArrayList<Integer> ids) {
        long dialogId = -channelId;
        markMany(dialogId, ids);
    }
}
