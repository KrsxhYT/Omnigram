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
        row(context, c, R.drawable.msg_customize, "Advanced Settings", null, x -> presentFragment(new OmnigramChatAdvancedActivity()));
        return root;
    }
}
