package org.telegram.ui.Components;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
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

    /** Result of {@link #process}: drop the request, or send it with this (possibly wrapped) callback. */
    public static final class Out {
        public final boolean drop;
        public final RequestDelegate complete;

        Out(boolean drop, RequestDelegate complete) {
            this.drop = drop;
            this.complete = complete;
        }
    }

    private static Out sendAsIs(RequestDelegate c) {
        return new Out(false, c);
    }

    private static Out dropped() {
        return new Out(true, null);
    }

    /** Read receipts are not sent, but the app is told they worked so nothing waits for an answer. */
    private static Out droppedWithFakeAnswer(RequestDelegate onComplete) {
        if (onComplete != null) {
            TLRPC.TL_messages_affectedMessages fake = new TLRPC.TL_messages_affectedMessages();
            fake.pts = -1;
            fake.pts_count = 0;
            onComplete.run(fake, null);
        }
        return dropped();
    }

    private static long dialogIdOf(int account, TLRPC.InputPeer peer) {
        if (peer instanceof TLRPC.TL_inputPeerUser) return peer.user_id;
        if (peer instanceof TLRPC.TL_inputPeerChat) return -peer.chat_id;
        if (peer instanceof TLRPC.TL_inputPeerChannel) return -peer.channel_id;
        if (peer instanceof TLRPC.TL_inputPeerSelf) return UserConfig.getInstance(account).getClientUserId();
        return 0;
    }

    /**
     * Ghost Mode, called for every request just before it is sent. Typing, read receipts and story views are not sent,
     * the online status is always sent as offline, and (Read on Interact) a real read receipt goes out after you send a
     * message or tap a reaction.
     */
    public static Out process(TLObject object, RequestDelegate onComplete, final int account) {
        try {
            if (object == null) return dropped();
            if (ALLOWED.remove(object)) return sendAsIs(onComplete);

            if (object instanceof TLRPC.TL_messages_setTyping || object instanceof TLRPC.TL_messages_setEncryptedTyping) {
                return on(NO_TYPING) ? dropped() : sendAsIs(onComplete);
            }
            if (object instanceof TL_account.updateStatus) {
                if (on(NO_ONLINE) || on(AUTO_OFFLINE)) {
                    ((TL_account.updateStatus) object).offline = true;
                }
                return sendAsIs(onComplete);
            }
            if (object instanceof TLRPC.TL_messages_readHistory
                    || object instanceof TLRPC.TL_channels_readHistory
                    || object instanceof TLRPC.TL_messages_readDiscussion
                    || object instanceof TLRPC.TL_messages_readEncryptedHistory
                    || object instanceof TLRPC.TL_messages_readMessageContents
                    || object instanceof TLRPC.TL_channels_readMessageContents) {
                return on(NO_READ) ? droppedWithFakeAnswer(onComplete) : sendAsIs(onComplete);
            }
            if (object instanceof TL_stories.TL_stories_readStories || object instanceof TL_stories.TL_stories_incrementStoryViews) {
                return on(NO_STORIES) ? dropped() : sendAsIs(onComplete);
            }
            if (on(NO_READ) && readOnInteract()) {
                if (object instanceof TLRPC.TL_messages_sendReaction) {
                    TLRPC.TL_messages_sendReaction r = (TLRPC.TL_messages_sendReaction) object;
                    markRead(account, r.peer, r.msg_id);
                    return sendAsIs(onComplete);
                }
                TLRPC.InputPeer sendPeer = null;
                if (object instanceof TLRPC.TL_messages_sendMessage) sendPeer = ((TLRPC.TL_messages_sendMessage) object).peer;
                else if (object instanceof TLRPC.TL_messages_sendMedia) sendPeer = ((TLRPC.TL_messages_sendMedia) object).peer;
                else if (object instanceof TLRPC.TL_messages_sendMultiMedia) sendPeer = ((TLRPC.TL_messages_sendMultiMedia) object).peer;
                if (sendPeer != null) {
                    final TLRPC.InputPeer peer = sendPeer;
                    final RequestDelegate original = onComplete;
                    return sendAsIs((response, error) -> {
                        if (original != null) original.run(response, error);
                        if (error == null) {
                            AndroidUtilities.runOnUIThread(() -> markReadAfterSend(account, peer), 500);
                        }
                    });
                }
            }
        } catch (Throwable ignore) {}
        return sendAsIs(onComplete);
    }

    /** After a message was sent: read everything up to the newest message of that chat. */
    private static void markReadAfterSend(int account, TLRPC.InputPeer peer) {
        try {
            long did = dialogIdOf(account, peer);
            if (did == 0) return;
            TLRPC.Dialog d = MessagesController.getInstance(account).dialogs_dict.get(did);
            int maxId = d != null ? d.top_message : 0;
            if (maxId > 0) markRead(account, peer, maxId);
        } catch (Throwable ignore) {}
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
