package org.telegram.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.OmnigramDeleted;
import org.telegram.ui.Components.OmnigramPrefs;

/** Omni Preference > General, organized in sections. Every switch can be turned on or off by the user. */
public class OmnigramGeneralActivity extends OmnigramSettingsBase {

    private TextView idValue;

    private static String idModeLabel(int mode) {
        switch (mode) {
            case OmnigramPrefs.ID_BOT_API: return "Bot API";
            case OmnigramPrefs.ID_HIDE: return "Hide";
            default: return "Telegram API";
        }
    }

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "General");

        // ---- General
        section(context, "General");
        LinearLayout general = card(context);
        toggle(context, general, "Disable Number Rounding", "1.23K \u2192 1,234", OmnigramPrefs.NO_ROUNDING, false);
        toggle(context, general, "Format Time with Seconds", "12:34 \u2192 12:34:56", OmnigramPrefs.TIME_SECONDS, false);
        toggle(context, general, "In-App Vibration", null, OmnigramPrefs.VIBRATION, false);

        // ---- Download speed boost
        section(context, "Download Speed Boost");
        LinearLayout dl = card(context);
        toggle(context, dl, "Fast Download Speed", null, OmnigramPrefs.FAST_DOWNLOAD, true);
        footnote(context, "More parallel parts per file. Real speed still depends on your connection and Telegram's server limits.");

        // ---- Profile
        section(context, "Profile");
        LinearLayout profile = card(context);
        toggle(context, profile, "Show All Users' Last Seen", "last seen at ~8:05 PM", OmnigramPrefs.LAST_SEEN_ALL, false);
        toggle(context, profile, "Hide Phone Number", null, OmnigramPrefs.HIDE_PHONE, false);
        idValue = row(context, profile, 0, "Show ID and DC", idModeLabel(OmnigramPrefs.idMode()), x -> chooseIdMode());
        footnote(context, "Telegram API shows IDs as they are, while the Bot API adds a minus sign for groups and -100 for supergroups and channels. Hide removes IDs from profiles, bots, groups and channels.");

        // ---- Archived chats
        section(context, "Archived Chats");
        LinearLayout archive = card(context);
        toggle(context, archive, "Hide Archived Chats", null, OmnigramPrefs.HIDE_ARCHIVED, false, on -> {
            try {
                if (on != SharedConfig.archiveHidden) SharedConfig.toggleArchiveHidden();
            } catch (Throwable ignore) {}
        });
        toggle(context, archive, "Open Archive When Pulldown", null, OmnigramPrefs.OPEN_ARCHIVE_PULLDOWN, true);
        toggle(context, archive, "Disable \"Unarchive\" Swipe", null, OmnigramPrefs.DISABLE_UNARCHIVE_SWIPE, true);
        footnote(context, "Swiping left will not remove chats from the archive. Hidden archive stays reachable from the 3-dot menu > Archived chats.");

        // ---- Additional features
        section(context, "Additional Features");
        LinearLayout extra = card(context);
        toggle(context, extra, "No Ads", null, OmnigramPrefs.NO_ADS, false);
        toggle(context, extra, "Premium Emojis", null, OmnigramPrefs.PREMIUM_EMOJI, false, on -> {
            if (on) playPremiumAnimation();
        });
        toggle(context, extra, "Glare on Elements", null, OmnigramPrefs.GLARE, false);
        toggle(context, extra, "Sticker Undo", null, OmnigramPrefs.STICKER_UNDO, true);
        toggle(context, extra, "Quick Stickers Bar", null, OmnigramPrefs.QUICK_STICKER_BAR, false);
        footnote(context, "Premium Emojis works on this device only. Other people still see the normal emoji unless your account has Premium. Glare needs an app restart. Reopen a chat after changing Quick Stickers Bar.");

        // ---- Other
        section(context, "Other");
        LinearLayout other = card(context);
        toggle(context, other, "Verified Badge Animation", null, OmnigramPrefs.VERIFIED_ANIM, true);
        row(context, other, R.drawable.msg_delete, "Delete Deleted Messages History", null, x -> confirmClearHistory());
        return root;
    }

    private void chooseIdMode() {
        if (getParentActivity() == null) return;
        new AlertDialog.Builder(getParentActivity())
                .setTitle("Show ID and DC")
                .setItems(new CharSequence[]{"Telegram API", "Bot API", "Hide"}, (d, which) -> {
                    OmnigramPrefs.setIdMode(which);
                    if (idValue != null) idValue.setText(idModeLabel(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Quick springy pop of the whole screen when Premium Emojis is switched on. */
    private void playPremiumAnimation() {
        try {
            if (list == null) return;
            list.setPivotX(list.getWidth() / 2f);
            list.setPivotY(AndroidUtilities.dp(220));
            AnimatorSet set = new AnimatorSet();
            ObjectAnimator sx = ObjectAnimator.ofFloat(list, View.SCALE_X, 1f, 0.94f, 1.02f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(list, View.SCALE_Y, 1f, 0.94f, 1.02f, 1f);
            ObjectAnimator ty = ObjectAnimator.ofFloat(list, View.TRANSLATION_Y, 0f, -AndroidUtilities.dp(12), AndroidUtilities.dp(4), 0f);
            set.playTogether(sx, sy, ty);
            set.setDuration(700);
            set.setInterpolator(new OvershootInterpolator(1.4f));
            set.start();
            AndroidUtilities.vibrate(list);
        } catch (Throwable ignore) {}
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
