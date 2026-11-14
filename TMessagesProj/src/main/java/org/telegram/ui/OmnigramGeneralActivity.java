package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.OmnigramDeleted;
import org.telegram.ui.Components.OmnigramPrefs;

/** Omni Preference > General. Every switch here can be turned off (or on) by the user. */
public class OmnigramGeneralActivity extends OmnigramSettingsBase {

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "General");

        section(context, "Features");
        LinearLayout c = card(context);
        toggle(context, c, "Fast download speed", "More parallel parts per file. Real speed still depends on your connection and Telegram's server limits",
                OmnigramPrefs.FAST_DOWNLOAD, true);
        toggle(context, c, "Verified badge animation", "Soft pulsing ring on genuine verified badges. Off automatically when system animations are off",
                OmnigramPrefs.VERIFIED_ANIM, true);
        toggle(context, c, "Sticker undo", "Shows an Undo bar for a few seconds after sending a sticker",
                OmnigramPrefs.STICKER_UNDO, true);
        toggle(context, c, "Quick sticker bar", "Strip with favorite and recent stickers above the message box. Tap once to send. Reopen the chat after changing",
                OmnigramPrefs.QUICK_STICKER_BAR, false);
        toggle(context, c, "Format time with seconds", "Shows 12:34:56 instead of 12:34 on messages",
                OmnigramPrefs.TIME_SECONDS, false);
        toggle(context, c, "Show all users last seen", "Telegram hides the real time for some users. This shows an approximate local time (last message or status this app received)",
                OmnigramPrefs.LAST_SEEN_ALL, false);
        toggle(context, c, "No ads", "Hides sponsored messages",
                OmnigramPrefs.NO_ADS, false);
        toggle(context, c, "Hide phone number", "The number will be hidden in the UI only. To hide it from others, please use Privacy settings.",
                OmnigramPrefs.HIDE_PHONE, false);

        section(context, "Deleted messages");
        LinearLayout d = card(context);
        row(context, d, R.drawable.msg_delete, "Delete deleted messages history", null, x -> confirmClearHistory());
        return root;
    }

    private void confirmClearHistory() {
        if (getParentActivity() == null) return;
        new AlertDialog.Builder(getParentActivity())
                .setTitle("Deleted messages history")
                .setMessage("You Want to Permanent Delete the Deleted messages history?")
                .setPositiveButton("Yes", (dlg, w) -> OmnigramDeleted.clearAll())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
