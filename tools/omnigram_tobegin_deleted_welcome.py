#!/usr/bin/env python3
"""To Beginning in 3-dot menu, durable local deleted msgs, welcome branding."""
from pathlib import Path
import re

ROOT = Path(".")


def durable_deleted_storage():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/OmnigramDeleted.java"
    p.write_text(r'''package org.telegram.ui.Components;

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
''', encoding="utf-8")
    print("OmnigramDeleted durable ok")


def to_beginning_menu():
    ca = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java"
    t = ca.read_text(encoding="utf-8", errors="replace")

    # Add menu item near clear history
    if "lazilyAddSubItem(OPTION_OMNI_TO_BEGINNING" not in t and "addSubItem(OPTION_OMNI_TO_BEGINNING" not in t:
        needle = "clearHistoryItem = headerItem.lazilyAddSubItem(clear_history,"
        if needle in t:
            t = t.replace(
                needle,
                "headerItem.lazilyAddSubItem(OPTION_OMNI_TO_BEGINNING, R.drawable.msg_go_up, \"To Beginning\");\n"
                "            " + needle,
                1,
            )
            print("menu: To Beginning added")
        else:
            # fallback icon name
            needle2 = "headerItem.lazilyAddSubItem(clear_history,"
            if needle2 in t:
                t = t.replace(
                    needle2,
                    "headerItem.lazilyAddSubItem(OPTION_OMNI_TO_BEGINNING, R.drawable.msg_up, \"To Beginning\");\n"
                    "            " + needle2,
                    1,
                )
                print("menu: To Beginning added (fallback)")

    # Handle header item click for OPTION_OMNI_TO_BEGINNING
    if "id == OPTION_OMNI_TO_BEGINNING" not in t:
        # inject in onItemClick near clear_history
        if "id == clear_history || id == delete_chat" in t:
            t = t.replace(
                "id == clear_history || id == delete_chat",
                "id == OPTION_OMNI_TO_BEGINNING || id == clear_history || id == delete_chat",
                1,
            )
        # more explicit handler before clear_history block
        if "} else if (id == clear_history || id == delete_chat" in t:
            t = t.replace(
                "} else if (id == OPTION_OMNI_TO_BEGINNING || id == clear_history || id == delete_chat",
                "} else if (id == OPTION_OMNI_TO_BEGINNING) {\n"
                "                try { omnigramGoToBeginning(); } catch (Throwable ignore) {}\n"
                "            } else if (id == clear_history || id == delete_chat",
                1,
            )
            print("onItemClick: To Beginning")

    # Improve processSelectedOption handler
    old_h = '''        if (option == OPTION_OMNI_TO_BEGINNING) {
            try {
                if (chatListView != null && messages != null && !messages.isEmpty()) {
                    chatListView.scrollToPosition(messages.size() - 1);
                }
            } catch (Throwable ignore) {}
            return;
        }'''
    new_h = '''        if (option == OPTION_OMNI_TO_BEGINNING) {
            try { omnigramGoToBeginning(); } catch (Throwable ignore) {}
            return;
        }'''
    if old_h in t:
        t = t.replace(old_h, new_h, 1)

    if "void omnigramGoToBeginning" not in t:
        # add method before processSelectedOption
        t = t.replace(
            "    private void processSelectedOption(int option) {",
            '''    private void omnigramGoToBeginning() {
        try {
            org.telegram.ui.Components.OmnigramDeleted.warm(dialog_id);
            // Load history from the oldest side, then scroll to first loaded
            getMessagesController().loadMessages(
                dialog_id, mergeDialogId, false, 30, 1, 0, true, 0, classGuid,
                3, 0, chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++, isTopic);
            if (chatListView != null && messages != null && !messages.isEmpty()) {
                int target = messages.size() - 1;
                chatListView.stopScroll();
                chatLayoutManager.scrollToPositionWithOffset(target, 0);
            }
            if (getParentActivity() != null) {
                org.telegram.ui.Components.BulletinFactory.of(this)
                    .createSimpleBulletin(org.telegram.messenger.R.raw.contact_check, "Jumping to beginning...")
                    .show();
            }
        } catch (Throwable e) {
            try {
                if (chatListView != null && messages != null && !messages.isEmpty()) {
                    chatListView.scrollToPosition(messages.size() - 1);
                }
            } catch (Throwable ignore) {}
        }
    }

    private void processSelectedOption(int option) {''',
            1,
        )
        print("omnigramGoToBeginning method")

    # markAndSave on delete
    if "markAndSave(selectedObject)" not in t:
        t = t.replace(
            "org.telegram.ui.Components.OmnigramDeleted.mark(dialog_id, selectedObject.getId());",
            "org.telegram.ui.Components.OmnigramDeleted.markAndSave(selectedObject);",
        )

    # drawable may be msg_up vs msg_go_up - use msg_up if go_up missing is compile risk; use msg_up
    t = t.replace("R.drawable.msg_go_up", "R.drawable.msg_up")

    ca.write_text(t, encoding="utf-8")
    print("ChatActivity To Beginning ok")


def welcome_branding():
    # strings.xml AppName / Page titles
    for rel in (
        "TMessagesProj/src/main/res/values/strings.xml",
        "TMessagesProj/src/main/res/values-en/strings.xml",
    ):
        p = ROOT / rel
        if not p.exists():
            continue
        t = p.read_text(encoding="utf-8", errors="replace")
        orig = t
        # Only replace Telegram app name style strings carefully
        t = re.sub(
            r'<string name="AppName">Telegram</string>',
            '<string name="AppName">Omnigram</string>',
            t,
        )
        t = re.sub(
            r'<string name="Page1Title">[^<]*</string>',
            '<string name="Page1Title">Welcome to **Omnigram**</string>',
            t,
            count=1,
        )
        t = re.sub(
            r'<string name="StartMessaging">[^<]*</string>',
            '<string name="StartMessaging">Start Messaging</string>',
            t,
            count=1,
        )
        if t != orig:
            p.write_text(t, encoding="utf-8")
            print("strings branded", rel)

    # IntroActivity hard-coded Telegram word if any
    intro = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java"
    if intro.exists():
        t = intro.read_text(encoding="utf-8", errors="replace")
        if "Telegram for Android" in t:
            t = t.replace("Telegram for Android", "Omnigram for Android")
            intro.write_text(t, encoding="utf-8")
            print("IntroActivity branded")


def ensure_cmc_deleted_ui():
    cmc = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java"
    t = cmc.read_text(encoding="utf-8", errors="replace")
    # Ensure timeString starts with deleted check
    if "public static String timeString(MessageObject current, MessageObject message, boolean edited, int editDate)" in t:
        if "OmnigramDeleted.isDeleted(message.getDialogId()" not in t[t.find("public static String timeString"):t.find("public static String timeString")+500]:
            t = t.replace(
                "    public static String timeString(MessageObject current, MessageObject message, boolean edited, int editDate) {",
                "    public static String timeString(MessageObject current, MessageObject message, boolean edited, int editDate) {\n"
                "        try {\n"
                "            if (message != null && org.telegram.ui.Components.OmnigramDeleted.isDeleted(message.getDialogId(), message.getId())) {\n"
                "                return \"\\uD83D\\uDDD1 \" + LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);\n"
                "            }\n"
                "        } catch (Throwable ignore) {}\n",
                1,
            )
            cmc.write_text(t, encoding="utf-8")
            print("CMC timeString deleted")


def main():
    durable_deleted_storage()
    to_beginning_menu()
    welcome_branding()
    ensure_cmc_deleted_ui()
    print("ALL DONE")


if __name__ == "__main__":
    main()
