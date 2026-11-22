package org.telegram.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.OmnigramGhost;

/** Ghost Mode: choose what Omnigram does not tell Telegram (reading, stories, online, typing). */
public class OmnigramGhostActivity extends OmnigramSettingsBase {

    private Switch master;
    private TextView masterTitle;
    private LinearLayout optionsBox;
    private final CheckBox[] boxes = new CheckBox[OmnigramGhost.OPTION_KEYS.length];
    private boolean expanded = true;
    private boolean updating;

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Ghost Mode");
        section(context, "Ghost essentials");
        LinearLayout card = card(context);

        // ---- master row
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        masterTitle = new TextView(context);
        masterTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        masterTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        masterTitle.setOnClickListener(v -> {
            expanded = !expanded;
            optionsBox.setVisibility(expanded ? View.VISIBLE : View.GONE);
            refresh();
        });
        head.addView(masterTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        master = new Switch(context);
        master.setOnClickListener(v -> {
            if (updating) return;
            boolean now = master.isChecked();
            OmnigramGhost.setAll(now);
            refresh();
            Toast.makeText(context, now ? "Ghost Mode turned on" : "Ghost Mode turned off", Toast.LENGTH_SHORT).show();
        });
        head.addView(master);
        card.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- the five options
        optionsBox = new LinearLayout(context);
        optionsBox.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < OmnigramGhost.OPTION_KEYS.length; i++) {
            final String key = OmnigramGhost.OPTION_KEYS[i];
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(34), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(4));
            CheckBox cb = new CheckBox(context);
            cb.setClickable(false);
            boxes[i] = cb;
            row.addView(cb);
            TextView label = new TextView(context);
            label.setText(OmnigramGhost.OPTION_NAMES[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            label.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = AndroidUtilities.dp(10);
            row.addView(label, lp);
            row.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 2));
            row.setOnClickListener(v -> {
                org.telegram.ui.Components.OmnigramPrefs.set(key, !OmnigramGhost.on(key));
                refresh();
            });
            optionsBox.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(46)));
        }
        card.addView(optionsBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        footnote(context, "Tap an option to switch it on or off. The switch above turns all five on or off. Read receipts, typing, online status and story views are simply not sent to Telegram.");

        LinearLayout extra = card(context);
        toggle(context, extra, "Read on Interact", "Automatically marks a message as read when you send a new one or tap a reaction.", OmnigramGhost.READ_ON_INTERACT, true);
        footnote(context, "Ghost Mode hides your read status from other people. Messages you have not opened may still look unread on your other devices.");
        refresh();
        return root;
    }

    private void refresh() {
        updating = true;
        int n = OmnigramGhost.activeCount();
        int total = OmnigramGhost.OPTION_KEYS.length;
        masterTitle.setText("Ghost Mode  " + n + "/" + total + (expanded ? "  \u25B4" : "  \u25BE"));
        master.setChecked(n == total);
        for (int i = 0; i < boxes.length; i++) {
            boxes[i].setChecked(OmnigramGhost.on(OmnigramGhost.OPTION_KEYS[i]));
        }
        updating = false;
    }
}
