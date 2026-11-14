package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.TLRPC;

/**
 * Telegram does not send the real last-seen time of users who hide it. This keeps an APPROXIMATE time seen
 * by this app only (their last message or status update that reached this device).
 */
public final class OmnigramLastSeen {
    private OmnigramLastSeen() {}

    private static SharedPreferences sp() {
        return ApplicationLoader.applicationContext.getSharedPreferences("omnigram_lastseen", Context.MODE_PRIVATE);
    }

    public static void record(long userId, long timeMs) {
        if (userId <= 0 || timeMs <= 0) return;
        try {
            SharedPreferences p = sp();
            if (timeMs > p.getLong("u" + userId, 0)) p.edit().putLong("u" + userId, timeMs).apply();
        } catch (Throwable ignore) {}
    }

    public static void recordMessage(TLRPC.Message m) {
        try {
            if (m == null || m.out || m.from_id == null || m.date <= 0) return;
            if (m.from_id instanceof TLRPC.TL_peerUser) record(m.from_id.user_id, (long) m.date * 1000L);
        } catch (Throwable ignore) {}
    }

    public static long get(long userId) {
        try { return sp().getLong("u" + userId, 0); } catch (Throwable t) { return 0; }
    }
}
