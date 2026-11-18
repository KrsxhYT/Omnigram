package org.telegram.ui.Components;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

/**
 * Full screen 4-digit PIN pad used by Chat lock: round dots, number keys, shake on a wrong code.
 * Mode VERIFY checks the saved password of a chat. Mode CREATE asks for a new password twice.
 */
public class OmnigramPinDialog extends Dialog {

    public interface Listener {
        void onResult(boolean ok);
    }

    private final long dialogId;
    private final boolean create;
    private final String title;
    private final Listener listener;
    private final StringBuilder entered = new StringBuilder();
    private String firstEntry;
    private final View[] dots = new View[4];
    private TextView titleView;
    private TextView subtitleView;
    private TextView errorView;
    private LinearLayout dotsRow;
    private boolean finished;

    public OmnigramPinDialog(Context context, long dialogId, boolean create, String title, Listener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.dialogId = dialogId;
        this.create = create;
        this.title = title;
        this.listener = listener;
    }

    private int dp(float v) {
        return AndroidUtilities.dp(v);
    }

    private GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        if (w != null) {
            w.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Theme.getColor(Theme.key_windowBackgroundWhite)));
        }
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        final int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
        final int gray = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);
        final int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

        FrameLayout root = new FrameLayout(getContext());
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        FrameLayout iconBg = new FrameLayout(getContext());
        iconBg.setBackground(circle((accent & 0x00FFFFFF) | 0x26000000));
        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.msg_secret);
        icon.setColorFilter(accent);
        iconBg.addView(icon, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        col.addView(iconBg, new LinearLayout.LayoutParams(dp(76), dp(76)));

        titleView = new TextView(getContext());
        titleView.setText(create ? "Create password" : title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(text);
        titleView.setPadding(0, dp(18), 0, dp(4));
        col.addView(titleView);

        subtitleView = new TextView(getContext());
        subtitleView.setText(create ? "Choose 4 digits. Saved only on this device." : "Enter the 4-digit password");
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleView.setTextColor(gray);
        col.addView(subtitleView);

        dotsRow = new LinearLayout(getContext());
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 4; i++) {
            View d = new View(getContext());
            d.setBackground(circle(gray & 0x00FFFFFF | 0x55000000));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
            lp.setMargins(dp(10), 0, dp(10), 0);
            dotsRow.addView(d, lp);
            dots[i] = d;
        }
        LinearLayout.LayoutParams dr = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dr.topMargin = dp(28);
        col.addView(dotsRow, dr);

        errorView = new TextView(getContext());
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        errorView.setTextColor(0xFFE53935);
        errorView.setMinHeight(dp(24));
        errorView.setGravity(Gravity.CENTER);
        errorView.setPadding(0, dp(10), 0, dp(8));
        col.addView(errorView);

        LinearLayout pad = new LinearLayout(getContext());
        pad.setOrientation(LinearLayout.VERTICAL);
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"Cancel", "0", "\u232B"}};
        for (String[] r : rows) {
            LinearLayout line = new LinearLayout(getContext());
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (String k : r) {
                line.addView(key(k, text, gray));
            }
            pad.addView(line);
        }
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dp(8);
        col.addView(pad, pp);

        setContentView(root);
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(220).start();
    }

    private View key(final String k, int text, int gray) {
        TextView t = new TextView(getContext());
        t.setText(k);
        t.setGravity(Gravity.CENTER);
        boolean digit = k.length() == 1 && Character.isDigit(k.charAt(0));
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, digit ? 28 : (k.equals("Cancel") ? 14 : 24));
        t.setTextColor(digit ? text : gray);
        if (digit) {
            t.setBackground(Theme.createSimpleSelectorCircleDrawable(dp(72), Theme.getColor(Theme.key_windowBackgroundGray), Theme.getColor(Theme.key_listSelector)));
        }
        t.setOnClickListener(v -> press(k));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(72), dp(72));
        lp.setMargins(dp(12), dp(6), dp(12), dp(6));
        t.setLayoutParams(lp);
        return t;
    }

    private void press(String k) {
        if (finished) return;
        if (k.equals("Cancel")) {
            finished = true;
            dismiss();
            listener.onResult(false);
            return;
        }
        errorView.setText("");
        if (k.equals("\u232B")) {
            if (entered.length() > 0) entered.deleteCharAt(entered.length() - 1);
            refreshDots(false);
            return;
        }
        if (entered.length() >= 4) return;
        entered.append(k);
        refreshDots(true);
        if (entered.length() == 4) {
            final String pin = entered.toString();
            dotsRow.postDelayed(() -> complete(pin), 120);
        }
    }

    private void refreshDots(boolean animateLast) {
        final int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        final int off = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2) & 0x00FFFFFF | 0x55000000;
        for (int i = 0; i < 4; i++) {
            boolean on = i < entered.length();
            dots[i].setBackground(circle(on ? accent : off));
            if (on && animateLast && i == entered.length() - 1) {
                dots[i].setScaleX(0.4f);
                dots[i].setScaleY(0.4f);
                dots[i].animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            } else {
                dots[i].setScaleX(1f);
                dots[i].setScaleY(1f);
            }
        }
    }

    private void wrong(String message) {
        errorView.setText(message);
        ObjectAnimator shake = ObjectAnimator.ofFloat(dotsRow, "translationX", 0f, -dp(14), dp(14), -dp(10), dp(10), -dp(5), dp(5), 0f);
        shake.setDuration(380);
        shake.start();
        entered.setLength(0);
        refreshDots(false);
    }

    private void complete(String pin) {
        if (finished) return;
        if (create) {
            if (firstEntry == null) {
                firstEntry = pin;
                entered.setLength(0);
                refreshDots(false);
                titleView.setText("Repeat password");
                subtitleView.setText("Enter the same 4 digits again");
            } else if (firstEntry.equals(pin)) {
                OmnigramChatLock.setPin(dialogId, pin);
                finished = true;
                dismiss();
                listener.onResult(true);
            } else {
                firstEntry = null;
                titleView.setText("Create password");
                subtitleView.setText("Choose 4 digits. Saved only on this device.");
                wrong("The two passwords did not match. Try again");
            }
        } else {
            if (OmnigramChatLock.verify(dialogId, pin)) {
                finished = true;
                dismiss();
                listener.onResult(true);
            } else {
                wrong("Wrong password");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (finished) return;
        finished = true;
        dismiss();
        listener.onResult(false);
    }
}
