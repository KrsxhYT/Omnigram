package org.telegram.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.OmnigramPrefs;

/** Omni Preference > Appearance: avatar corners, chat list title, snow, fonts and emoji. */
public class OmnigramAppearanceActivity extends OmnigramSettingsBase {

    private TextView cornerChip;
    private View previewAvatar;
    private TextView titlePreview;
    private TextView titleValue;
    private android.widget.ImageView deletedIconView;
    private TextView deletedIconValue;
    private final View[] colorDots = new View[org.telegram.ui.Components.OmnigramIconSpan.COLORS.length];

    private static String cornerLabel(int pct) {
        if (pct >= 100) return "CIRCLE";
        if (pct <= 0) return "SQUARE";
        return "ROUNDED " + pct + "%";
    }

    private static String titleLabel(int mode) {
        switch (mode) {
            case 1: return "Username";
            case 2: return "Omnigram";
            case 3: return "Chats";
            default: return "Name";
        }
    }

    private String titleText(int mode) {
        try {
            org.telegram.tgnet.TLRPC.User me = getUserConfig().getCurrentUser();
            switch (mode) {
                case 1: {
                    String un = me != null ? org.telegram.messenger.UserObject.getPublicUsername(me) : null;
                    return un != null && un.length() > 0 ? "@" + un : (me != null ? org.telegram.messenger.UserObject.getUserName(me) : "Username");
                }
                case 2: return "Omnigram";
                case 3: return "Chats";
                default: return me != null ? org.telegram.messenger.UserObject.getUserName(me) : "Name";
            }
        } catch (Throwable t) {
            return "Omnigram";
        }
    }

    private void updateAvatarPreview(int pct) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2) & 0x00FFFFFF | 0x88000000);
        g.setCornerRadius(AndroidUtilities.dp(32) * pct / 100f);
        previewAvatar.setBackground(g);
        cornerChip.setText(cornerLabel(pct));
    }

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Appearance");
        final int accent = getThemedColor(Theme.key_featuredStickers_addButton);

        // ---- avatar corners
        LinearLayout avatar = card(context);
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(4));
        TextView t = new TextView(context);
        t.setText("Avatar Corners");
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        head.addView(t);
        cornerChip = new TextView(context);
        cornerChip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        cornerChip.setTypeface(AndroidUtilities.bold());
        cornerChip.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteValueText));
        cornerChip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), getThemedColor(Theme.key_windowBackgroundGray)));
        cornerChip.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.leftMargin = AndroidUtilities.dp(10);
        head.addView(cornerChip, cp);
        avatar.addView(head);

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.HORIZONTAL);
        labels.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), 0);
        TextView sq = new TextView(context);
        sq.setText("Square");
        sq.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        labels.addView(sq, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView ci = new TextView(context);
        ci.setText("Circle");
        ci.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteValueText));
        labels.addView(ci);
        avatar.addView(labels);

        SeekBar seek = new SeekBar(context);
        seek.setMax(100);
        seek.setProgress(OmnigramPrefs.avatarCorner());
        seek.setProgressTintList(ColorStateList.valueOf(accent));
        seek.setThumbTintList(ColorStateList.valueOf(accent));
        seek.setPadding(AndroidUtilities.dp(22), AndroidUtilities.dp(10), AndroidUtilities.dp(22), AndroidUtilities.dp(10));
        avatar.addView(seek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout preview = new LinearLayout(context);
        preview.setOrientation(LinearLayout.HORIZONTAL);
        preview.setGravity(Gravity.CENTER_VERTICAL);
        preview.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_windowBackgroundGray)));
        preview.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        previewAvatar = new View(context);
        preview.addView(previewAvatar, new LinearLayout.LayoutParams(AndroidUtilities.dp(64), AndroidUtilities.dp(64)));
        LinearLayout lines = new LinearLayout(context);
        lines.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < 3; i++) {
            View ln = new View(context);
            ln.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), getThemedColor(Theme.key_windowBackgroundWhiteGrayText2) & 0x00FFFFFF | 0x55000000));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(AndroidUtilities.dp(i == 0 ? 150 : i == 1 ? 190 : 110), AndroidUtilities.dp(10));
            lp.topMargin = AndroidUtilities.dp(i == 0 ? 0 : 8);
            lines.addView(ln, lp);
        }
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.leftMargin = AndroidUtilities.dp(14);
        preview.addView(lines, lp2);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.setMargins(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        avatar.addView(preview, pp);
        updateAvatarPreview(OmnigramPrefs.avatarCorner());
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) { updateAvatarPreview(p); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { OmnigramPrefs.setAvatarCorner(s.getProgress()); }
        });
        footnote(context, "Changes the shape of chat, profile and contact pictures. Reopen screens (or restart the app) to see it everywhere.");

        // ---- chat list
        section(context, "Chat List");
        LinearLayout chat = card(context);
        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_windowBackgroundGray)));
        bar.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18));
        titlePreview = new TextView(context);
        titlePreview.setText(titleText(OmnigramPrefs.titleMode()));
        titlePreview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titlePreview.setTypeface(AndroidUtilities.bold());
        titlePreview.setSingleLine(true);
        titlePreview.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        bar.addView(titlePreview, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView dots = new TextView(context);
        dots.setText("\u22EE");
        dots.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        dots.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        bar.addView(dots);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
        chat.addView(bar, bp);
        toggle(context, chat, "Snow Animation", "Falling snowflakes in the chat list title bar, side menu and chat backgrounds.", OmnigramPrefs.FORCE_SNOW, false);
        titleValue = row(context, chat, 0, "Title Text", titleLabel(OmnigramPrefs.titleMode()), x -> chooseTitle());
        footnote(context, "Title Text is what the top of the chat list shows: your name, your username, Omnigram or Chats. Reopen the chat list to see it.");

        // ---- deleted message icon
        section(context, "Deleted Message Icon");
        LinearLayout del = card(context);
        LinearLayout prev = new LinearLayout(context);
        prev.setOrientation(LinearLayout.HORIZONTAL);
        prev.setGravity(Gravity.BOTTOM);
        prev.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_windowBackgroundGray)));
        prev.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14));
        View pAvatar = new View(context);
        android.graphics.drawable.GradientDrawable pg = new android.graphics.drawable.GradientDrawable();
        pg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        pg.setColor(0xFF8E6BBF);
        pAvatar.setBackground(pg);
        prev.addView(pAvatar, new LinearLayout.LayoutParams(AndroidUtilities.dp(40), AndroidUtilities.dp(40)));
        LinearLayout bubble = new LinearLayout(context);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setAlpha(0.7f);
        bubble.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_chat_inBubble)));
        bubble.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        TextView pName = new TextView(context);
        pName.setText("Omnigram");
        pName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        pName.setTypeface(AndroidUtilities.bold());
        pName.setTextColor(0xFF4CAF50);
        bubble.addView(pName);
        TextView pText = new TextView(context);
        pText.setText("This message was deleted");
        pText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        pText.setTextColor(getThemedColor(Theme.key_chat_messageTextIn));
        bubble.addView(pText);
        LinearLayout timeRow = new LinearLayout(context);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        deletedIconView = new android.widget.ImageView(context);
        timeRow.addView(deletedIconView, new LinearLayout.LayoutParams(AndroidUtilities.dp(13), AndroidUtilities.dp(13)));
        TextView pTime = new TextView(context);
        pTime.setText("  3:09 PM");
        pTime.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        pTime.setTextColor(getThemedColor(Theme.key_chat_inTimeText));
        timeRow.addView(pTime);
        bubble.addView(timeRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bl.leftMargin = AndroidUtilities.dp(8);
        prev.addView(bubble, bl);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pl.setMargins(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        del.addView(prev, pl);

        deletedIconValue = row(context, del, 0, "Icon", org.telegram.ui.Components.OmnigramIconSpan.ICON_NAMES[OmnigramPrefs.deletedIcon()], x -> chooseDeletedIcon());

        LinearLayout colorsRow = new LinearLayout(context);
        colorsRow.setOrientation(LinearLayout.HORIZONTAL);
        colorsRow.setGravity(Gravity.CENTER_VERTICAL);
        colorsRow.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(16));
        TextView colorLabel = new TextView(context);
        colorLabel.setText("Colour");
        colorLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        colorLabel.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        colorsRow.addView(colorLabel, new LinearLayout.LayoutParams(AndroidUtilities.dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < colorDots.length; i++) {
            final int idx = i;
            View dot = new View(context);
            dot.setOnClickListener(v -> {
                OmnigramPrefs.setInt("deleted_color", idx);
                refreshDeletedPreview();
            });
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(AndroidUtilities.dp(34), AndroidUtilities.dp(34));
            dl.rightMargin = AndroidUtilities.dp(8);
            colorsRow.addView(dot, dl);
            colorDots[i] = dot;
        }
        del.addView(colorsRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        footnote(context, "The icon next to the time of a deleted message. Pick an icon and a colour, the preview above shows how it will look. Reopen a chat to see the change.");
        refreshDeletedPreview();

        // ---- appearance
        section(context, "Appearance");
        LinearLayout look = card(context);
        toggle(context, look, "Use System Font", null, OmnigramPrefs.SYSTEM_FONT, false);
        toggle(context, look, "Use System Emoji", null, "omni_system_emoji", SharedConfig.useSystemEmoji, on -> {
            try {
                SharedConfig.useSystemEmoji = on;
                MessagesController.getGlobalMainSettings().edit().putBoolean("useSystemEmoji", on).apply();
            } catch (Throwable ignore) {}
        });
        footnote(context, "Use System Font uses your phone's font instead of the app's. Use System Emoji shows your phone's emoji instead of the app's emoji set. Restart the app after changing either.");
        return root;
    }

    private void refreshDeletedPreview() {
        try {
            int icon = OmnigramPrefs.deletedIcon();
            int colorIdx = Math.max(0, Math.min(org.telegram.ui.Components.OmnigramIconSpan.COLORS.length - 1, OmnigramPrefs.deletedColor()));
            int color = org.telegram.ui.Components.OmnigramIconSpan.COLORS[colorIdx];
            if (color == 0) color = getThemedColor(Theme.key_chat_inTimeText);
            deletedIconView.setImageResource(org.telegram.ui.Components.OmnigramIconSpan.iconRes(icon));
            deletedIconView.setColorFilter(new android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN));
            if (deletedIconValue != null) deletedIconValue.setText(org.telegram.ui.Components.OmnigramIconSpan.ICON_NAMES[icon]);
            for (int i = 0; i < colorDots.length; i++) {
                int c = org.telegram.ui.Components.OmnigramIconSpan.COLORS[i];
                android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
                g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                g.setColor(c == 0 ? (getThemedColor(Theme.key_windowBackgroundWhiteGrayText2) & 0x00FFFFFF | 0xAA000000) : c);
                if (i == colorIdx) g.setStroke(AndroidUtilities.dp(3), getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
                colorDots[i].setBackground(g);
            }
        } catch (Throwable ignore) {}
    }

    private void chooseDeletedIcon() {
        if (getParentActivity() == null) return;
        new AlertDialog.Builder(getParentActivity())
                .setTitle("Deleted Message Icon")
                .setItems(org.telegram.ui.Components.OmnigramIconSpan.ICON_NAMES, (d, which) -> {
                    OmnigramPrefs.setInt("deleted_icon", which);
                    refreshDeletedPreview();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void chooseTitle() {
        if (getParentActivity() == null) return;
        new AlertDialog.Builder(getParentActivity())
                .setTitle("Title Text")
                .setItems(new CharSequence[]{"Name", "Username", "Omnigram", "Chats"}, (d, which) -> {
                    OmnigramPrefs.setTitleMode(which);
                    if (titleValue != null) titleValue.setText(titleLabel(which));
                    if (titlePreview != null) titlePreview.setText(titleText(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
