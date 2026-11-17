package org.telegram.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.OmnigramAutoAnswer;

/** Omni Preference > Chat > Auto Answer When Offline */
public class OmnigramAutoAnswerActivity extends OmnigramSettingsBase {

    private android.widget.Switch sw;
    private EditText input;

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Auto Answer When Offline");

        LinearLayout c = card(context);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        TextView t = new TextView(context);
        t.setText("Auto answer when offline");
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sw = new android.widget.Switch(context);
        sw.setChecked(OmnigramAutoAnswer.enabled());
        sw.setClickable(false);
        row.addView(sw);
        row.setOnClickListener(v -> {
            boolean now = !sw.isChecked();
            sw.setChecked(now);
            OmnigramAutoAnswer.setEnabled(now);
        });
        c.addView(row);

        section(context, "Message to send");
        input = new EditText(context);
        input.setHint("I am offline right now, I will reply as soon as I can.");
        input.setText(OmnigramAutoAnswer.message());
        input.setMinLines(4);
        input.setGravity(android.view.Gravity.TOP);
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        input.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_windowBackgroundWhite)));
        input.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        list.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView save = new TextView(context);
        save.setText("Save message");
        save.setGravity(android.view.Gravity.CENTER);
        save.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        save.setTypeface(AndroidUtilities.bold());
        save.setTextColor(0xFFFFFFFF);
        save.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        save.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_featuredStickers_addButton)));
        save.setOnClickListener(v -> {
            OmnigramAutoAnswer.setMessage(input.getText().toString());
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = AndroidUtilities.dp(12);
        list.addView(save, sp);

        TextView note = new TextView(context);
        note.setText("Sent once (every 6 hours per person) when someone writes to you in a private chat while Omnigram is not open on your screen. It never answers bots, groups or channels. It can only reply while the app is running in the background, so it may be delayed if your phone stops the app.");
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        note.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        note.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4), 0);
        list.addView(note);
        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (input != null) OmnigramAutoAnswer.setMessage(input.getText().toString());
        } catch (Throwable ignore) {}
    }
}
