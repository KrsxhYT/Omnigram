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
        row(context, cats, R.drawable.solar_msg_settings, "General", null, x -> presentFragment(new OmnigramGeneralActivity()));
        row(context, cats, R.drawable.solar_ghost, "Ghost Mode", null, x -> presentFragment(new OmnigramGhostActivity()));
        row(context, cats, R.drawable.solar_msg_theme, "Appearance", null, x -> presentFragment(new OmnigramAppearanceActivity()));
        row(context, cats, R.drawable.solar_msg_discuss, "Chat", null, x -> presentFragment(new OmnigramChatActivity()));

        section(context, "Support");
        LinearLayout sup = card(context);
        row(context, sup, R.drawable.solar_msg_gift_premium, "Support Omnigram", null, x -> presentFragment(new OmnigramSupportActivity()));

        section(context, "Links");
        LinearLayout links = card(context);
        row(context, links, R.drawable.solar_msg_channel, "Channel", "@OfficalOmnigram", x -> open("https://t.me/OfficalOmnigram"));
        row(context, links, R.drawable.solar_msg_groups, "Chats", "@OmnigramChat", x -> open("https://t.me/OmnigramChat"));
        return root;
    }
}
