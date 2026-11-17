package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

/**
 * Auto answer when offline. Replies once (per person, every few hours) to private messages from real people
 * while Omnigram is not open on screen. Never answers bots, groups or channels.
 */
public final class OmnigramAutoAnswer {
    private OmnigramAutoAnswer() {}

    public static final String ENABLED = "auto_answer_on";
    private static final long COOLDOWN_MS = 6L * 60 * 60 * 1000;

    private static SharedPreferences sp() {
        return ApplicationLoader.applicationContext.getSharedPreferences("omnigram_autoanswer", Context.MODE_PRIVATE);
    }

    public static boolean enabled() {
        try { return sp().getBoolean(ENABLED, false); } catch (Throwable t) { return false; }
    }

    public static void setEnabled(boolean v) {
        try { sp().edit().putBoolean(ENABLED, v).apply(); } catch (Throwable ignore) {}
    }

    public static String message() {
        try { return sp().getString("message", ""); } catch (Throwable t) { return ""; }
    }

    public static void setMessage(String m) {
        try { sp().edit().putString("message", m == null ? "" : m).apply(); } catch (Throwable ignore) {}
    }

    public static void onIncoming(int account, TLRPC.Message m) {
        try {
            if (!enabled() || m == null || m.out || m.action != null) return;
            // "offline" = Omnigram is not in the foreground
            if (!ApplicationLoader.mainInterfacePaused) return;
            if (!(m.peer_id instanceof TLRPC.TL_peerUser)) return;
            long userId = m.peer_id.user_id;
            if (userId <= 0 || userId == 777000 || userId == UserConfig.getInstance(account).getClientUserId()) return;
            if (m.from_id != null && m.from_id.user_id != 0 && m.from_id.user_id != userId) return;
            String text = message();
            if (text.trim().isEmpty()) return;
            TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
            if (user == null || user.bot || user.self || user.deleted || user.support) return;
            long now = System.currentTimeMillis();
            String key = "last_" + account + "_" + userId;
            if (now - sp().getLong(key, 0) < COOLDOWN_MS) return;
            sp().edit().putLong(key, now).apply();
            final String send = text;
            final long did = userId;
            org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> {
                try {
                    SendMessagesHelper.getInstance(account).sendMessage(SendMessagesHelper.SendMessageParams.of(send, did, null, null, null, true, null, null, null, true, 0, 0, null, false));
                } catch (Throwable ignore) {}
            });
        } catch (Throwable ignore) {}
    }
}
