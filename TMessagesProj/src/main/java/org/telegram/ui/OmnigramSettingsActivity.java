package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.util.TypedValue;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.OmnigramDeleted;
import org.telegram.ui.Components.OmnigramPrefs;

/** Settings > Omni Preference */
public class OmnigramSettingsActivity extends BaseFragment {

    private LinearLayout list;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Omni Preference");
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

        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(28));
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- logo + name
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.mipmap.ic_launcher);
        head.addView(logo, new LinearLayout.LayoutParams(AndroidUtilities.dp(84), AndroidUtilities.dp(84)));
        TextView name = new TextView(context);
        name.setText("Omnigram");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        name.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(16));
        head.addView(name);
        list.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- General
        section(context, "General");
        LinearLayout general = card(context);
        toggle(context, general, "Fast download speed", "More parallel parts per file. Real speed still depends on your connection and Telegram's server limits",
                OmnigramPrefs.FAST_DOWNLOAD, true);
        toggle(context, general, "Format time with seconds", "Shows 12:34:56 instead of 12:34 on messages",
                OmnigramPrefs.TIME_SECONDS, false);
        toggle(context, general, "No ads", "Hides sponsored messages",
                OmnigramPrefs.NO_ADS, false);
        toggle(context, general, "Hide phone number", "The number will be hidden in the UI only. To hide it from others, please use Privacy settings.",
                OmnigramPrefs.HIDE_PHONE, false);
        toggle(context, general, "Show all users last seen", "Telegram hides the real time for some users. This shows an approximate local time (last message or status this app received)",
                OmnigramPrefs.LAST_SEEN_ALL, false);
        toggle(context, general, "Verified badge animation", "Soft pulsing ring on genuine verified badges. Off automatically when system animations are off",
                OmnigramPrefs.VERIFIED_ANIM, true);
        toggle(context, general, "Sticker undo", "Shows an Undo bar for a few seconds after sending a sticker",
                OmnigramPrefs.STICKER_UNDO, true);
        button(context, general, "Delete deleted messages history", v -> confirmClearHistory());

        // ---- links
        section(context, "Official channels");
        LinearLayout links = card(context);
        button(context, links, "Omnigram Official · t.me/OfficalOmnigram", v -> open("https://t.me/OfficalOmnigram"));
        button(context, links, "Omnigram Chat · t.me/OmnigramChat", v -> open("https://t.me/OmnigramChat"));

        return fragmentView;
    }

    private void open(String url) {
        try {
            Browser.openUrl(getParentActivity(), url);
        } catch (Throwable ignore) {}
    }

    private void confirmClearHistory() {
        if (getParentActivity() == null) return;
        new AlertDialog.Builder(getParentActivity())
                .setTitle("Deleted messages history")
                .setMessage("You Want to Permanent Delete the Deleted messages history?")
                .setPositiveButton("Yes", (d, w) -> OmnigramDeleted.clearAll())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void section(Context context, String title) {
        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        t.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(14), 0, AndroidUtilities.dp(8));
        list.addView(t);
    }

    private LinearLayout card(Context context) {
        LinearLayout c = new LinearLayout(context);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), getThemedColor(Theme.key_windowBackgroundWhite)));
        list.addView(c, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return c;
    }

    private void toggle(Context context, LinearLayout parent, String title, String sub, String key, boolean def) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        texts.addView(t);
        TextView s = new TextView(context);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        s.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        s.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        texts.addView(s);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(context);
        sw.setChecked(OmnigramPrefs.get(key, def));
        sw.setClickable(false);
        row.addView(sw, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOnClickListener(v -> {
            boolean now = !sw.isChecked();
            sw.setChecked(now);
            OmnigramPrefs.set(key, now);
        });
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void button(Context context, LinearLayout parent, String title, View.OnClickListener l) {
        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        t.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(15), AndroidUtilities.dp(16), AndroidUtilities.dp(15));
        t.setOnClickListener(l);
        parent.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
