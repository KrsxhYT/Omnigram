package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.messenger.R;

/** Omni Preference > Chat */
public class OmnigramChatActivity extends OmnigramSettingsBase {

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Chat");
        section(context, "Composer");
        LinearLayout c = card(context);
        row(context, c, R.drawable.solar_msg_settings, "Advanced Settings", null, x -> presentFragment(new OmnigramChatAdvancedActivity()));
        row(context, c, R.drawable.solar_msg_discuss, "Auto Answer When Offline", null, x -> presentFragment(new OmnigramAutoAnswerActivity()));

        row(context, c, R.drawable.solar_msg_sticker, "Stickers", null, x -> presentFragment(new OmnigramStickersActivity()));
        row(context, c, R.drawable.solar_msg_theme, "Chat Bubble", org.telegram.ui.Components.OmnigramBubble.NAMES[org.telegram.ui.Components.OmnigramPrefs.bubbleStyle()], x -> presentFragment(new OmnigramBubbleStyleActivity()));
        row(context, c, R.drawable.solar_msg_edit, "Check Style", org.telegram.ui.Components.OmnigramCheckStyle.NAMES[org.telegram.ui.Components.OmnigramPrefs.checkStyle()], x -> presentFragment(new OmnigramCheckStyleActivity()));
        return root;
    }
}
