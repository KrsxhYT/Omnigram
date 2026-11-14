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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.OmnigramPrefs;

/** Shared look for the Omni Preference screens (header, section titles, rounded cards, rows). */
public abstract class OmnigramSettingsBase extends BaseFragment {

    protected LinearLayout list;

    protected View buildRoot(Context context, String title) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(title);
        actionBar.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
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
        list.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(28));
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    protected void open(String url) {
        try {
            Browser.openUrl(getParentActivity(), url);
        } catch (Throwable ignore) {}
    }

    protected void section(Context context, String title) {
        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        t.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(18), 0, AndroidUtilities.dp(8));
        list.addView(t);
    }

    protected LinearLayout card(Context context) {
        LinearLayout c = new LinearLayout(context);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(18), getThemedColor(Theme.key_windowBackgroundWhite)));
        list.addView(c, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return c;
    }

    private void divider(Context context, LinearLayout parent) {
        View d = new View(context);
        d.setBackgroundColor(getThemedColor(Theme.key_divider));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = AndroidUtilities.dp(60);
        parent.addView(d, lp);
    }

    /** Icon + title row with optional value on the right (blue), like the reference design. */
    protected void row(Context context, LinearLayout parent, int iconRes, String title, String value, View.OnClickListener l) {
        if (parent.getChildCount() > 0) divider(context, parent);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        if (iconRes != 0) {
            ImageView icon = new ImageView(context);
            icon.setImageResource(iconRes);
            icon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN));
            row.addView(icon, new LinearLayout.LayoutParams(AndroidUtilities.dp(26), AndroidUtilities.dp(26)));
        }
        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = AndroidUtilities.dp(iconRes != 0 ? 20 : 0);
        row.addView(t, tp);
        if (value != null) {
            TextView v = new TextView(context);
            v.setText(value);
            v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            v.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteValueText));
            row.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        row.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 2));
        row.setOnClickListener(l);
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    protected interface OnToggle {
        void on(boolean value);
    }

    protected void toggle(Context context, LinearLayout parent, String title, String sub, String key, boolean def) {
        toggle(context, parent, title, sub, key, def, null);
    }

    protected void toggle(Context context, LinearLayout parent, String title, String sub, String key, boolean def, OnToggle cb) {
        if (parent.getChildCount() > 0) divider(context, parent);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(13), AndroidUtilities.dp(16), AndroidUtilities.dp(13));

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
        row.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 2));
        row.setOnClickListener(v -> {
            boolean now = !sw.isChecked();
            sw.setChecked(now);
            OmnigramPrefs.set(key, now);
            if (cb != null) cb.on(now);
        });
        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
