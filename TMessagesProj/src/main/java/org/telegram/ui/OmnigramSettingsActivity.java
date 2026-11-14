package org.telegram.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;

/** Settings > Omni Preference (landing screen: logo, categories, links). */
public class OmnigramSettingsActivity extends OmnigramSettingsBase {

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "");

        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        head.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(12));

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.mipmap.omni_bg_1);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setClipToOutline(true);
        logo.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(34));
            }
        });
        head.addView(logo, new LinearLayout.LayoutParams(AndroidUtilities.dp(108), AndroidUtilities.dp(108)));

        TextView name = new TextView(context);
        name.setText("Omnigram");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        name.setPadding(0, AndroidUtilities.dp(18), 0, AndroidUtilities.dp(2));
        head.addView(name);

        TextView version = new TextView(context);
        String v = "";
        try {
            v = org.telegram.messenger.BuildVars.BUILD_VERSION_STRING;
        } catch (Throwable ignore) {}
        version.setText(v);
        version.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        version.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        head.addView(version);
        list.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        section(context, "Categories");
        LinearLayout cats = card(context);
        row(context, cats, R.drawable.settings_features, "General", null, x -> presentFragment(new OmnigramGeneralActivity()));

        section(context, "Links");
        LinearLayout links = card(context);
        row(context, links, R.drawable.msg_channel, "Channel", "@OfficalOmnigram", x -> open("https://t.me/OfficalOmnigram"));
        row(context, links, R.drawable.msg_groups, "Chats", "@OmnigramChat", x -> open("https://t.me/OmnigramChat"));
        return root;
    }
}
