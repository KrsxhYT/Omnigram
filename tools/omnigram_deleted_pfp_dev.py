#!/usr/bin/env python3
"""Deleted msgs dustbin (SS style), PFP profile<->settings sync, Developers UI."""
from pathlib import Path
import re

ROOT = Path(".")


def fix_timestring_deleted():
    """timeString: deleted => dustbin + time (any chat); edited => pencil."""
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java"
    t = p.read_text(encoding="utf-8", errors="replace")

    # Replace broken edited-branch injection with clean helper at start of timeString body
    # First undo bad inject in edited branch
    t = t.replace(
        '''                String omniT = LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);
            try { if (message != null && org.telegram.ui.Components.OmnigramDeleted.isDeleted(message.getDialogId(), message.getId())) return "\\uD83D\\uDDD1 " + omniT; } catch (Throwable ignore) {}
            return "\\u270E " + omniT;''',
        '''                return "\\u270E " + LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);''',
    )

    # Add deleted check right after early returns, before edited
    marker = "        } else if (edited) {"
    if "OmnigramDeleted.isDeleted" not in t.split("public static String timeString")[1][:2500] if "public static String timeString" in t else "":
        pass

    # Inject at beginning after notime checks - always prefix dustbin when deleted
    inject_at = "    public static String timeString(MessageObject current, MessageObject message, boolean edited, int editDate) {"
    helper = '''    public static String timeString(MessageObject current, MessageObject message, boolean edited, int editDate) {
        try {
            if (message != null && org.telegram.ui.Components.OmnigramDeleted.isDeleted(message.getDialogId(), message.getId())) {
                String tm = LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);
                return "\\uD83D\\uDDD1 " + tm;
            }
        } catch (Throwable ignore) {}
'''
    if inject_at in t and "OmnigramDeleted.isDeleted(message.getDialogId()" not in t[t.find(inject_at):t.find(inject_at)+400]:
        # replace method start - careful only once
        t = t.replace(inject_at, helper, 1)
        # remove duplicate body opening brace issue - helper already has { and try, original continues with if
        # Original after { is "if (current.notime" - our helper ends with try block then original if needs to stay
        # Currently: helper { try...} then original "if (current.notime" without opening - we ate the {
        # Fix: original was "{ \n if" - we need "{ try... if"
        print("timeString: injected deleted check")
    else:
        # ensure final return also checks deleted
        print("timeString: inject skip or already")

    # onDraw alpha 0.7 already may exist
    if "OmnigramDeleted.isDeleted(currentMessageObject" not in t:
        t = t.replace(
            "protected void onDraw(Canvas canvas) {",
            "protected void onDraw(Canvas canvas) {\n"
            "        try { if (currentMessageObject != null && org.telegram.ui.Components.OmnigramDeleted.isDeleted(currentMessageObject.getDialogId(), currentMessageObject.getId())) setAlpha(0.70f); else if (Math.abs(getAlpha()-0.70f)<0.001f) setAlpha(1f); } catch (Throwable ignore) {}\n",
            1,
        )

    p.write_text(t, encoding="utf-8")
    print("CMC timeString/alpha ok")


def strengthen_delete_hooks():
    """Mark deleted with real dialog id; keep showing in chat when possible."""
    store = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/OmnigramDeleted.java"
    store.write_text('''package org.telegram.ui.Components;

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
''', encoding="utf-8")

    # ChatActivity processSelectedOption DELETE - mark before delete
    ca = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java"
    t = ca.read_text(encoding="utf-8", errors="replace")
    if "OmnigramDeleted.mark" not in t:
        # hook common delete path
        for needle in (
            "getMessagesController().deleteMessages(",
            "deleteMessages(ArrayList",
        ):
            if needle in t and "OmnigramDeleted" not in t[max(0,t.find(needle)-200):t.find(needle)]:
                pass
        # After selectedObject when OPTION_DELETE handled
        if "case OPTION_DELETE:" in t and "OmnigramDeleted.mark" not in t:
            t = t.replace(
                "case OPTION_DELETE:",
                "case OPTION_DELETE:\n"
                "                try {\n"
                "                    if (selectedObject != null) {\n"
                "                        org.telegram.ui.Components.OmnigramDeleted.mark(dialog_id, selectedObject.getId());\n"
                "                        if (selectedObjectGroup != null && selectedObjectGroup.messages != null) {\n"
                "                            for (int gi = 0; gi < selectedObjectGroup.messages.size(); gi++) {\n"
                "                                MessageObject mo = selectedObjectGroup.messages.get(gi);\n"
                "                                if (mo != null) org.telegram.ui.Components.OmnigramDeleted.mark(dialog_id, mo.getId());\n"
                "                            }\n"
                "                        }\n"
                "                    }\n"
                "                } catch (Throwable ignore) {}\n",
                1,
            )
            print("ChatActivity: mark on OPTION_DELETE")
        ca.write_text(t, encoding="utf-8")

    # Channel delete updates
    mc = ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java"
    mt = mc.read_text(encoding="utf-8", errors="replace")
    if "OmnigramDeleted.markManyChannel" not in mt:
        for needle in (
            "if (update instanceof TLRPC.TL_updateDeleteChannelMessages)",
            "if (update instanceof TLRPC.TL_updateDeleteMessages)",
        ):
            if needle in mt:
                i = mt.find(needle)
                b = mt.find("{", i)
                if "OmnigramDeleted" not in mt[i:i+300]:
                    extra = (
                        "\n                    try {\n"
                        "                        if (update instanceof TLRPC.TL_updateDeleteChannelMessages) {\n"
                        "                            TLRPC.TL_updateDeleteChannelMessages ud = (TLRPC.TL_updateDeleteChannelMessages) update;\n"
                        "                            org.telegram.ui.Components.OmnigramDeleted.markManyChannel(ud.channel_id, new java.util.ArrayList<>(ud.messages));\n"
                        "                        } else if (update instanceof TLRPC.TL_updateDeleteMessages) {\n"
                        "                            TLRPC.TL_updateDeleteMessages ud = (TLRPC.TL_updateDeleteMessages) update;\n"
                        "                            org.telegram.ui.Components.OmnigramDeleted.markMany(0, new java.util.ArrayList<>(ud.messages));\n"
                        "                        }\n"
                        "                    } catch (Throwable ignore) {}\n"
                    )
                    # only inject once total
                    if "markManyChannel" not in mt:
                        mt = mt[: b + 1] + extra + mt[b + 1 :]
                        print("MessagesController: delete hooks")
                        break
        mc.write_text(mt, encoding="utf-8")


def fix_pfp_sync():
    """When avatar changes, refresh Settings + Profile."""
    # SettingsActivity didReceivedNotification
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java"
    if not p.exists():
        print("SettingsActivity missing")
        return
    t = p.read_text(encoding="utf-8", errors="replace")
    if "omnigramRefreshAvatar" in t:
        print("pfp already")
        return

    # Ensure mainUserInfoChanged and userInfoDidLoad refresh avatarView
    if "didReceivedNotification" in t and "avatarView.setForUserOrChat" in t:
        # Add helper method
        if "void omnigramRefreshAvatar" not in t:
            t = t.replace(
                "public void didReceivedNotification(int id, int account, Object... args) {",
                "private void omnigramRefreshAvatar() {\n"
                "        try {\n"
                "            org.telegram.tgnet.TLRPC.User user = getUserConfig().getCurrentUser();\n"
                "            if (user != null && avatarView != null) {\n"
                "                if (avatarDrawable != null) avatarDrawable.setInfo(currentAccount, user);\n"
                "                avatarView.setForUserOrChat(user, avatarDrawable);\n"
                "                avatarView.invalidate();\n"
                "            }\n"
                "        } catch (Throwable ignore) {}\n"
                "    }\n\n"
                "    public void didReceivedNotification(int id, int account, Object... args) {",
                1,
            )
            # Call at start of didReceivedNotification for user-related events
            t = t.replace(
                "public void didReceivedNotification(int id, int account, Object... args) {",
                "public void didReceivedNotification(int id, int account, Object... args) {\n"
                "        try {\n"
                "            if (id == NotificationCenter.mainUserInfoChanged\n"
                "                    || id == NotificationCenter.updateInterfaces\n"
                "                    || id == NotificationCenter.userInfoDidLoad) {\n"
                "                omnigramRefreshAvatar();\n"
                "            }\n"
                "        } catch (Throwable ignore) {}\n",
                1,
            )
            p.write_text(t, encoding="utf-8")
            print("SettingsActivity PFP sync")
    else:
        print("SettingsActivity structure unexpected")

    # ProfileActivity - already updates usually; force on mainUserInfoChanged
    pa = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java"
    if pa.exists():
        pt = pa.read_text(encoding="utf-8", errors="replace")
        if "omnigramRefreshAvatar" not in pt and "didReceivedNotification" in pt:
            # light touch - post updateInterfaces when photo uploaded is system
            print("ProfileActivity left to system notifications")


def improve_developers_page():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/DevelopersActivity.java"
    p.write_text(r'''package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class DevelopersActivity extends BaseFragment {

    private static final Object[][] DEVELOPERS = {
            {8077714480L, "Krsxh", "Krsxh", "Main Owner · Omnigram"},
            {5938491424L, "Zestyji", "zestyji", "Owner Brother · Omnigram"}
    };

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Developers");
        actionBar.setBackgroundColor(getThemedColor(Theme.key_actionBarDefault));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView header = new TextView(context);
        header.setText("Official Omnigram team");
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        header.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        header.setPadding(AndroidUtilities.dp(4), 0, 0, AndroidUtilities.dp(12));
        list.addView(header);

        for (Object[] dev : DEVELOPERS) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = AndroidUtilities.dp(12);
            list.addView(createCard(context, (Long) dev[0], (String) dev[1], (String) dev[2], (String) dev[3]), lp);
        }

        TextView foot = new TextView(context);
        foot.setText("Tap a card to open profile on Telegram");
        foot.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        foot.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText3));
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        list.addView(foot);

        return fragmentView;
    }

    private View createCard(Context context, long id, String name, String username, String role) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        card.setBackground(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(16),
                getThemedColor(Theme.key_windowBackgroundWhite)));
        card.setElevation(AndroidUtilities.dp(2));

        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(30));
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setInfo(id, name, "");
        TLRPC.User user = getMessagesController().getUser(id);
        if (user != null) {
            avatarDrawable.setInfo(currentAccount, user);
            avatar.setForUserOrChat(user, avatarDrawable);
        } else {
            avatar.setImageDrawable(avatarDrawable);
        }
        card.addView(avatar, new LinearLayout.LayoutParams(AndroidUtilities.dp(60), AndroidUtilities.dp(60)));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.leftMargin = AndroidUtilities.dp(14);
        card.addView(texts, textsLp);

        LinearLayout nameRow = new LinearLayout(context);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        texts.addView(nameRow);

        TextView nameView = new TextView(context);
        nameView.setText(name);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        nameView.setTypeface(AndroidUtilities.bold());
        nameView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        nameRow.addView(nameView);

        // verified chip
        TextView badge = new TextView(context);
        badge.setText("  verified");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badge.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        nameRow.addView(badge);

        TextView userView = new TextView(context);
        userView.setText("@" + username);
        userView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        userView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText2));
        texts.addView(userView);

        TextView roleView = new TextView(context);
        roleView.setText(role);
        roleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        roleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        roleView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        texts.addView(roleView);

        card.setOnClickListener(v -> {
            try {
                TLRPC.User u = getMessagesController().getUser(id);
                if (u != null) {
                    presentFragment(ProfileActivity.of(id));
                } else {
                    Browser.openUrl(getParentActivity(), "https://t.me/" + username);
                }
            } catch (Throwable e) {
                Browser.openUrl(getParentActivity(), "https://t.me/" + username);
            }
        });
        return card;
    }
}
''', encoding="utf-8")
    print("DevelopersActivity improved")


def main():
    strengthen_delete_hooks()
    fix_timestring_deleted()
    fix_pfp_sync()
    improve_developers_page()
    print("PACK DONE")


if __name__ == "__main__":
    main()
