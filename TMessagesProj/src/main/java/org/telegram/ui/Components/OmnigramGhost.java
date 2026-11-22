package org.telegram.ui.Components;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Ghost Mode. Every request goes through {@link #filter} just before it is sent to Telegram, so the app simply does not
 * tell Telegram when you read, type, view stories or are online. Nothing else is changed.
 */
public final class OmnigramGhost {
    private OmnigramGhost() {}

    public static final String NO_READ = "ghost_no_read";
    public static final String NO_STORIES = "ghost_no_stories";
    public static final String NO_ONLINE = "ghost_no_online";
    public static final String NO_TYPING = "ghost_no_typing";
    public static final String AUTO_OFFLINE = "ghost_auto_offline";
    public static final String READ_ON_INTERACT = "ghost_read_interact";

    public static final String[] OPTION_KEYS = {NO_READ, NO_STORIES, NO_ONLINE, NO_TYPING, AUTO_OFFLINE};
    public static final String[] OPTION_NAMES = {"Don't Read Messages", "Don't Read Stories", "Don't Send Online", "Don't Send Typing", "Go Offline Automatically"};

    private static final Set<Object> ALLOWED = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

    public static boolean on(String key) {
        return OmnigramPrefs.get(key, false);
    }

    public static int activeCount() {
        int n = 0;
        for (String k : OPTION_KEYS) if (on(k)) n++;
        return n;
    }

    public static void setAll(boolean value) {
        for (String k : OPTION_KEYS) OmnigramPrefs.set(k, value);
    }

    public static boolean readOnInteract() {
        return OmnigramPrefs.get(READ_ON_INTERACT, true);
    }

    /** Returns the request to send (maybe changed) or null when it must not be sent. */
    public static TLObject filter(TLObject object, int account) {
        try {
            if (object == null) return null;
            if (ALLOWED.remove(object)) return object;

            if (object instanceof TLRPC.TL_messages_setTyping) {
                return on(NO_TYPING) ? null : object;
            }
            if (object instanceof TL_account.updateStatus) {
                if (on(NO_ONLINE) || on(AUTO_OFFLINE)) {
                    ((TL_account.updateStatus) object).offline = true;
                }
                return object;
            }
            if (object instanceof TLRPC.TL_messages_readHistory || object instanceof TLRPC.TL_channels_readHistory || object instanceof TLRPC.TL_messages_readDiscussion) {
                return on(NO_READ) ? null : object;
            }
            if (object instanceof TL_stories.TL_stories_readStories || object instanceof TL_stories.TL_stories_incrementStoryViews) {
                return on(NO_STORIES) ? null : object;
            }
            if (on(NO_READ) && readOnInteract()) {
                if (object instanceof TLRPC.TL_messages_sendReaction) {
                    TLRPC.TL_messages_sendReaction r = (TLRPC.TL_messages_sendReaction) object;
                    markRead(account, r.peer, r.msg_id);
                } else if (object instanceof TLRPC.TL_messages_sendMessage) {
                    markRead(account, ((TLRPC.TL_messages_sendMessage) object).peer, 0);
                }
            }
        } catch (Throwable ignore) {}
        return object;
    }

    /** Real read receipt, sent when the user interacts (reacts or writes). */
    private static void markRead(int account, TLRPC.InputPeer peer, int maxId) {
        if (peer == null) return;
        TLObject req;
        if (peer instanceof TLRPC.TL_inputPeerChannel) {
            TLRPC.TL_channels_readHistory r = new TLRPC.TL_channels_readHistory();
            TLRPC.TL_inputChannel ch = new TLRPC.TL_inputChannel();
            ch.channel_id = peer.channel_id;
            ch.access_hash = peer.access_hash;
            r.channel = ch;
            r.max_id = maxId;
            req = r;
        } else {
            TLRPC.TL_messages_readHistory r = new TLRPC.TL_messages_readHistory();
            r.peer = peer;
            r.max_id = maxId;
            req = r;
        }
        ALLOWED.add(req);
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {});
    }
}
