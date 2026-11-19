package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Map;

/**
 * Keeps premium (custom) emojis alive on this device. When local Premium Emojis is on, the custom emoji entities of a
 * message you send are saved here. Telegram's server drops them for accounts without Premium, so when the message is
 * loaded again (refresh, reopen chat) they are put back from this store. Other people still see the normal emoji.
 */
public final class OmnigramCustomEmoji {
    private OmnigramCustomEmoji() {}

    private static SharedPreferences sp() {
        return ApplicationLoader.applicationContext.getSharedPreferences("omnigram_custom_emoji", Context.MODE_PRIVATE);
    }

    private static String key(long dialogId, String text) {
        return dialogId + ":" + text.length() + ":" + Integer.toHexString(text.hashCode());
    }

    public static void record(long peer, String text, ArrayList<TLRPC.MessageEntity> entities) {
        try {
            if (!OmnigramPrefs.premiumEmoji() || text == null || text.isEmpty() || entities == null) return;
            StringBuilder sb = new StringBuilder();
            for (TLRPC.MessageEntity e : entities) {
                if (e instanceof TLRPC.TL_messageEntityCustomEmoji) {
                    TLRPC.TL_messageEntityCustomEmoji c = (TLRPC.TL_messageEntityCustomEmoji) e;
                    sb.append(c.offset).append(',').append(c.length).append(',').append(c.document_id).append(';');
                }
            }
            if (sb.length() == 0) return;
            SharedPreferences p = sp();
            if (p.getAll().size() > 600) {
                // keep it small: forget the oldest part
                SharedPreferences.Editor ed = p.edit();
                int drop = 150;
                for (Map.Entry<String, ?> en : p.getAll().entrySet()) {
                    if (drop-- <= 0) break;
                    ed.remove(en.getKey());
                }
                ed.apply();
            }
            p.edit().putString(key(peer, text), sb.toString()).apply();
        } catch (Throwable ignore) {}
    }

    /** Called for every loaded message: puts back custom emoji entities of messages this account sent. */
    public static void apply(TLRPC.Message m) {
        try {
            if (m == null || !m.out || m.message == null || m.message.isEmpty() || !OmnigramPrefs.premiumEmoji()) return;
            if (m.entities != null) {
                for (TLRPC.MessageEntity e : m.entities) {
                    if (e instanceof TLRPC.TL_messageEntityCustomEmoji) return;
                }
            }
            String saved = sp().getString(key(MessageObject.getDialogId(m), m.message), null);
            if (saved == null) return;
            if (m.entities == null) m.entities = new ArrayList<>();
            for (String part : saved.split(";")) {
                if (part.isEmpty()) continue;
                String[] f = part.split(",");
                if (f.length < 3) continue;
                int offset = Integer.parseInt(f[0]);
                int length = Integer.parseInt(f[1]);
                long doc = Long.parseLong(f[2]);
                if (offset < 0 || length <= 0 || offset + length > m.message.length()) continue;
                TLRPC.TL_messageEntityCustomEmoji c = new TLRPC.TL_messageEntityCustomEmoji();
                c.offset = offset;
                c.length = length;
                c.document_id = doc;
                m.entities.add(c);
            }
            m.flags |= 128;
        } catch (Throwable ignore) {}
    }
}
