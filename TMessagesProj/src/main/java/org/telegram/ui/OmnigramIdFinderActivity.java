package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

/** ID Finder: look a user/chat up by @username or numeric ID, open it, add it to a group/channel or make it admin. */
public class OmnigramIdFinderActivity extends OmnigramSettingsBase {

    private static final int RED = 0xFFE53935;
    private static final int GREEN = 0xFF2E9E4F;

    private EditText input;
    private TextView status;
    private TextView info;
    private TextView btnAdd;
    private TextView btnAdmin;
    private TextView btnOpen;
    private LinearLayout buttons;

    private TLRPC.User foundUser;
    private TLRPC.Chat foundChat;
    private int token;
    private Runnable pending;

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "ID Finder");

        // Buttons appear above the field once something is typed
        buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setVisibility(View.GONE);
        btnAdd = actionButton(context, "Add to a group or channel");
        btnAdmin = actionButton(context, "Add as admin in a group or channel");
        buttons.addView(btnAdd, buttonParams());
        buttons.addView(btnAdmin, buttonParams());
        list.addView(buttons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        section(context, "Username or Chat ID");
        input = new EditText(context);
        input.setSingleLine(true);
        input.setHint("@username or chat id");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        input.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_windowBackgroundWhite)));
        input.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        list.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(context);
        status.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        status.setTypeface(AndroidUtilities.bold());
        status.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(12), 0, 0);
        list.addView(status);

        info = new TextView(context);
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        info.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        info.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(4), 0, 0);
        list.addView(info);

        btnOpen = actionButton(context, "Open profile");
        btnOpen.setVisibility(View.GONE);
        LinearLayout.LayoutParams op = buttonParams();
        op.topMargin = AndroidUtilities.dp(14);
        list.addView(btnOpen, op);

        btnOpen.setOnClickListener(v -> openFound());
        btnAdd.setOnClickListener(v -> addToChat(false));
        btnAdmin.setOnClickListener(v -> addToChat(true));

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                boolean has = s != null && s.toString().trim().length() > 0;
                buttons.setVisibility(has ? View.VISIBLE : View.GONE);
                foundUser = null;
                foundChat = null;
                btnOpen.setVisibility(View.GONE);
                setButtonsEnabled(false);
                info.setText("");
                if (pending != null) AndroidUtilities.cancelRunOnUIThread(pending);
                if (!has) {
                    status.setText("");
                    token++;
                    return;
                }
                pending = () -> search(s.toString().trim());
                AndroidUtilities.runOnUIThread(pending, 500);
            }
        });
        setButtonsEnabled(false);
        return root;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = AndroidUtilities.dp(8);
        return lp;
    }

    private TextView actionButton(Context context, String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(0xFFFFFFFF);
        t.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        t.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_featuredStickers_addButton)));
        return t;
    }

    private void setButtonsEnabled(boolean enabled) {
        btnAdd.setEnabled(enabled);
        btnAdmin.setEnabled(enabled);
        btnAdd.setAlpha(enabled ? 1f : 0.45f);
        btnAdmin.setAlpha(enabled ? 1f : 0.45f);
    }

    private void show(String text, int color) {
        status.setText(text);
        status.setTextColor(color);
    }

    private void search(String raw) {
        final int my = ++token;
        String q = raw.startsWith("@") ? raw.substring(1) : raw;
        MessagesController mc = getMessagesController();
        if (q.matches("-?[0-9]+")) {
            try {
                long id = Math.abs(Long.parseLong(q));
                TLRPC.User u = mc.getUser(id);
                TLRPC.Chat c = u == null ? mc.getChat(id) : null;
                finish(my, u, c);
            } catch (Throwable t) {
                show("Username or Chat ID is not valid", RED);
            }
        } else if (q.matches("[A-Za-z][A-Za-z0-9_]{3,31}")) {
            show("Searching...", getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
            mc.getUserNameResolver().resolve(q, peerId -> AndroidUtilities.runOnUIThread(() -> {
                if (my != token) return;
                if (peerId == null || peerId == 0) {
                    finish(my, null, null);
                    return;
                }
                TLRPC.User u = peerId > 0 ? mc.getUser(peerId) : null;
                TLRPC.Chat c = peerId < 0 ? mc.getChat(-peerId) : null;
                finish(my, u, c);
            }));
        } else {
            show("Username or Chat ID is not valid", RED);
        }
    }

    private void finish(int my, TLRPC.User u, TLRPC.Chat c) {
        if (my != token) return;
        foundUser = u;
        foundChat = c;
        if (u == null && c == null) {
            show("Not found", RED);
            info.setText("");
            btnOpen.setVisibility(View.GONE);
            setButtonsEnabled(false);
            return;
        }
        show("User Founded", GREEN);
        if (u != null) {
            info.setText(UserObject.getUserName(u) + "\nID: " + u.id);
        } else {
            info.setText(c.title + "\nID: " + c.id);
        }
        btnOpen.setVisibility(View.VISIBLE);
        setButtonsEnabled(u != null);
    }

    private void openFound() {
        Bundle args = new Bundle();
        if (foundUser != null) {
            args.putLong("user_id", foundUser.id);
        } else if (foundChat != null) {
            args.putLong("chat_id", foundChat.id);
        } else {
            return;
        }
        presentFragment(new ProfileActivity(args));
    }

    private void addToChat(boolean asAdmin) {
        final TLRPC.User user = foundUser;
        if (user == null) return;
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_ADD_USERS_TO);
        args.putBoolean("resetDelegate", false);
        args.putBoolean("closeFragment", false);
        DialogsActivity picker = new DialogsActivity(args);
        picker.setDelegate((fragment1, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            long did = dids.get(0).dialogId;
            TLRPC.Chat chat = getMessagesController().getChat(-did);
            if (chat == null) return true;
            if (asAdmin) {
                if (chat.creator || (chat.admin_rights != null && chat.admin_rights.add_admins)) {
                    getMessagesController().checkIsInChat(false, chat, user, (isInChatAlready, rightsAdmin, currentRank) -> AndroidUtilities.runOnUIThread(() -> {
                        ChatRightsEditActivity edit = new ChatRightsEditActivity(user.id, -did, rightsAdmin, null, null, currentRank, ChatRightsEditActivity.TYPE_ADMIN, true, !isInChatAlready, null);
                        presentFragment(edit);
                    }));
                } else {
                    AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                    b.setTitle("Add as admin");
                    b.setMessage("You need the \"Add admins\" right in " + chat.title + " to do this.");
                    b.setPositiveButton("OK", null);
                    showDialog(b.create());
                }
            } else {
                AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
                b.setTitle("Add to " + chat.title);
                b.setMessage("Add " + UserObject.getUserName(user) + " to " + chat.title + "?");
                b.setNegativeButton("Cancel", null);
                b.setPositiveButton("Add", (di, i) -> {
                    Bundle chatArgs = new Bundle();
                    chatArgs.putBoolean("scrollToTopOnResume", true);
                    chatArgs.putLong("chat_id", -did);
                    ChatActivity chatActivity = new ChatActivity(chatArgs);
                    getMessagesController().addUserToChat(-did, user, 0, null, chatActivity, true, null, null);
                    presentFragment(chatActivity, true);
                });
                showDialog(b.create());
            }
            return true;
        });
        presentFragment(picker);
    }
}
