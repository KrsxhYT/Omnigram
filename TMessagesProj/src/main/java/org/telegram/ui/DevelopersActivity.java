package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class DevelopersActivity extends BaseFragment {

    private static final Object[][] DEVELOPERS = {
            {8077714480L, "Krsxh", "Krsxh", "Main Owner · Omnigram"},
            {5938491424L, "Zestyji", "zestyji", "Owner Brother · Omnigram"}
    };

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Developers");
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

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView header = new TextView(context);
        header.setText("Official Omnigram team");
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        header.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        header.setPadding(AndroidUtilities.dp(4), 0, 0, AndroidUtilities.dp(12));
        list.addView(header);

        for (Object[] dev : DEVELOPERS) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = AndroidUtilities.dp(12);
            list.addView(createCard(context, (Long) dev[0], (String) dev[1], (String) dev[2], (String) dev[3]), lp);
        }

        TextView foot = new TextView(context);
        foot.setText("Tap a card to open profile on Telegram");
        foot.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        foot.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText3));
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        list.addView(foot);

        return fragmentView;
    }

    private View createCard(Context context, long id, String name, String username, String role) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        card.setBackground(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(16),
                getThemedColor(Theme.key_windowBackgroundWhite)));
        card.setElevation(AndroidUtilities.dp(2));

        BackupImageView avatar = new BackupImageView(context);
        avatar.setRoundRadius(AndroidUtilities.dp(30));
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setInfo(id, name, "");
        TLRPC.User user = getMessagesController().getUser(id);
        if (user != null) {
            avatarDrawable.setInfo(currentAccount, user);
            avatar.setForUserOrChat(user, avatarDrawable);
        } else {
            avatar.setImageDrawable(avatarDrawable);
        }
        card.addView(avatar, new LinearLayout.LayoutParams(AndroidUtilities.dp(60), AndroidUtilities.dp(60)));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.leftMargin = AndroidUtilities.dp(14);
        card.addView(texts, textsLp);

        LinearLayout nameRow = new LinearLayout(context);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        texts.addView(nameRow);

        TextView nameView = new TextView(context);
        nameView.setText(name);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        nameView.setTypeface(AndroidUtilities.bold());
        nameView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        nameRow.addView(nameView);

        // verified chip
        TextView badge = new TextView(context);
        badge.setText("  verified");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badge.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        nameRow.addView(badge);

        TextView userView = new TextView(context);
        userView.setText("@" + username);
        userView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        userView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText2));
        texts.addView(userView);

        TextView roleView = new TextView(context);
        roleView.setText(role);
        roleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        roleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        roleView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        texts.addView(roleView);

        card.setOnClickListener(v -> {
            try {
                TLRPC.User u = getMessagesController().getUser(id);
                if (u != null) {
                    presentFragment(ProfileActivity.of(id));
                } else {
                    Browser.openUrl(getParentActivity(), "https://t.me/" + username);
                }
            } catch (Throwable e) {
                Browser.openUrl(getParentActivity(), "https://t.me/" + username);
            }
        });
        return card;
    }
}
