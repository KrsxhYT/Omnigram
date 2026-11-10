package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;

public class DevelopersActivity extends BaseFragment {

    private static final Object[][] DEVELOPERS = {
            {8077714480L, "Krsxh", "Krsxh", "Main Owner"},
            {5938491424L, "Zestyji", "zestyji", "Owner Brother"}
    };

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Developers");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        ScrollView scroll = new ScrollView(context);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (Object[] dev : DEVELOPERS) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = AndroidUtilities.dp(12);
            list.addView(createCard(context, (Long) dev[0], (String) dev[1], (String) dev[2], (String) dev[3]), lp);
        }
        return fragmentView;
    }

    private View createCard(Context context, long id, String name, String username, String role) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_windowBackgroundWhite)));

        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(32));
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setInfo(id, name, "");
        TLRPC.User user = getMessagesController().getUser(id);
        if (user != null) {
            avatarDrawable.setInfo(currentAccount, user);
            avatar.setForUserOrChat(user, avatarDrawable);
        } else {
            avatar.setImageDrawable(avatarDrawable);
        }
        card.addView(avatar, new LinearLayout.LayoutParams(AndroidUtilities.dp(64), AndroidUtilities.dp(64)));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.leftMargin = AndroidUtilities.dp(14);
        card.addView(texts, textsLp);

        TextView nameView = new TextView(context);
        nameView.setText(name);
        nameView.setTextSize(17);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        nameView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        texts.addView(nameView);

        TextView userView = new TextView(context);
        userView.setText("@" + username);
        userView.setTextSize(14);
        userView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        texts.addView(userView);

        TextView roleView = new TextView(context);
        roleView.setText(role);
        roleView.setTextSize(13);
        roleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        texts.addView(roleView);

        card.setOnClickListener(v -> Browser.openUrl(context, "https://t.me/" + username));
        return card;
    }
}
