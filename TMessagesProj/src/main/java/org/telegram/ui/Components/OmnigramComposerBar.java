package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

/**
 * Floating strip above the message box: live character counter, the "Composer tools" button and the optional
 * live preview card. Shown while the user is typing.
 */
public class OmnigramComposerBar extends LinearLayout {

    /** Local undo / redo history for composer changes (independent of Telegram's message history). */
    public static class History {
        private final ArrayList<CharSequence> undo = new ArrayList<>();
        private final ArrayList<CharSequence> redo = new ArrayList<>();
        public CharSequence original;

        private static CharSequence copy(CharSequence cs) {
            return new SpannableStringBuilder(cs == null ? "" : cs);
        }

        public void push(CharSequence current) {
            undo.add(copy(current));
            if (undo.size() > 60) undo.remove(0);
            redo.clear();
        }

        public boolean canUndo() { return !undo.isEmpty(); }
        public boolean canRedo() { return !redo.isEmpty(); }

        public CharSequence undo(CharSequence current) {
            if (undo.isEmpty()) return null;
            redo.add(copy(current));
            return undo.remove(undo.size() - 1);
        }

        public CharSequence redo(CharSequence current) {
            if (redo.isEmpty()) return null;
            undo.add(copy(current));
            return redo.remove(redo.size() - 1);
        }
    }

    public final History history = new History();

    private final int account;
    private final ChatActivityEnterView enterView;
    private final View container;
    private final TextView counter;
    private final TextView toolsButton;
    private final LinearLayout previewCard;
    private final TextView previewText;
    private boolean previewOn;
    private ViewTreeObserver.OnPreDrawListener preDraw;
    private TextWatcher watcher;

    public OmnigramComposerBar(Context context, int account, ChatActivityEnterView enterView, View container) {
        super(context);
        this.account = account;
        this.enterView = enterView;
        this.container = container;
        setOrientation(VERTICAL);
        setGravity(Gravity.END);

        // ---- live preview card (hidden until switched on)
        previewCard = new LinearLayout(context);
        previewCard.setOrientation(VERTICAL);
        previewCard.setVisibility(GONE);
        previewCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_chat_outBubble)));
        previewCard.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
        previewCard.setElevation(AndroidUtilities.dp(3));
        TextView head = new TextView(context);
        head.setText("Live preview (nothing is sent until you press Send)");
        head.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        head.setTextColor(Theme.getColor(Theme.key_chat_messageTextOut));
        head.setAlpha(0.7f);
        previewCard.addView(head);
        previewText = new TextView(context);
        previewText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        previewText.setTextColor(Theme.getColor(Theme.key_chat_messageTextOut));
        previewText.setLinkTextColor(Theme.getColor(Theme.key_chat_messageLinkOut));
        previewText.setMaxLines(12);
        previewText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        previewText.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        previewCard.addView(previewText);
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(HORIZONTAL);
        actions.setGravity(Gravity.END);
        TextView hide = chip(context, "Hide", false);
        TextView send = chip(context, "Send", true);
        hide.setOnClickListener(v -> setPreview(false));
        send.setOnClickListener(v -> {
            try {
                setPreview(false);
                enterView.sendMessage();
            } catch (Throwable ignore) {}
        });
        actions.addView(hide);
        actions.addView(send);
        previewCard.addView(actions);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        pp.bottomMargin = AndroidUtilities.dp(6);
        addView(previewCard, pp);

        // ---- pill row: counter + tools button
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        counter = new TextView(context);
        counter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        counter.setTypeface(Typeface.DEFAULT_BOLD);
        counter.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(5), AndroidUtilities.dp(10), AndroidUtilities.dp(5));
        counter.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), Theme.getColor(Theme.key_chat_messagePanelBackground)));
        counter.setElevation(AndroidUtilities.dp(2));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        cp.rightMargin = AndroidUtilities.dp(8);
        row.addView(counter, cp);
        toolsButton = new TextView(context);
        toolsButton.setText("Aa  Tools");
        toolsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        toolsButton.setTypeface(Typeface.DEFAULT_BOLD);
        toolsButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        toolsButton.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(7), AndroidUtilities.dp(14), AndroidUtilities.dp(7));
        toolsButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_featuredStickers_addButton)));
        toolsButton.setElevation(AndroidUtilities.dp(2));
        toolsButton.setOnClickListener(v -> new OmnigramComposerSheet(getContext(), account, enterView, this).show());
        row.addView(toolsButton);
        addView(row, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        setVisibility(GONE);
    }

    private TextView chip(Context context, String text, boolean filled) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(filled ? Theme.getColor(Theme.key_featuredStickers_buttonText) : Theme.getColor(Theme.key_chat_messageTextOut));
        t.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        if (filled) t.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), Theme.getColor(Theme.key_featuredStickers_addButton)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.leftMargin = AndroidUtilities.dp(8);
        t.setLayoutParams(lp);
        return t;
    }

    public void setPreview(boolean on) {
        previewOn = on;
        if (on) {
            previewCard.setAlpha(0f);
            previewCard.setVisibility(VISIBLE);
            previewCard.animate().alpha(1f).setDuration(180).start();
        } else {
            previewCard.setVisibility(GONE);
        }
        refresh();
    }

    public boolean isPreviewOn() {
        return previewOn;
    }

    public void attachTo(EditTextCaption et) {
        if (et == null || watcher != null) return;
        watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh(); }
        };
        et.addTextChangedListener(watcher);
    }

    private void refresh() {
        try {
            EditTextCaption et = enterView.getEditField();
            CharSequence text = et != null ? et.getText() : "";
            int n = text == null ? 0 : text.length();
            boolean show = OmnigramComposerPrefs.on(OmnigramComposerPrefs.CONTEXT_MENU) && (n > 0 || previewOn);
            animateVisible(show);
            toolsButton.setVisibility(OmnigramComposerPrefs.on(OmnigramComposerPrefs.CONTEXT_MENU) ? VISIBLE : GONE);

            if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.COUNTER)) {
                MessagesController mc = MessagesController.getInstance(account);
                int msgLimit = mc.maxMessageLength > 0 ? mc.maxMessageLength : 4096;
                int capLimit = mc.maxCaptionLength > 0 ? mc.maxCaptionLength : 1024;
                if (UserConfig.getInstance(account).isPremium()) capLimit = capLimit * 2;
                counter.setVisibility(VISIBLE);
                String label = n + " / " + msgLimit;
                int color = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);
                if (n > msgLimit) {
                    color = 0xFFE53935;
                    label += "  over limit";
                } else if (n >= msgLimit * 0.9f) {
                    color = 0xFFF59E0B;
                } else if (n > capLimit) {
                    label += "  (caption max " + capLimit + ")";
                }
                counter.setText(label);
                counter.setTextColor(color);
            } else {
                counter.setVisibility(GONE);
            }

            if (previewOn) {
                SpannableStringBuilder sb = new SpannableStringBuilder(text == null ? "" : text);
                previewText.setText(sb.length() == 0 ? "(empty)" : sb);
            }
        } catch (Throwable ignore) {}
    }

    private boolean lastShow;

    private void animateVisible(boolean show) {
        lastShow = show;
        if (show) {
            animate().cancel();
            if (getVisibility() != VISIBLE) {
                setAlpha(0f);
                setVisibility(VISIBLE);
            }
            if (getAlpha() < 1f) animate().alpha(1f).setDuration(180).start();
        } else if (getVisibility() == VISIBLE) {
            animate().alpha(0f).setDuration(140).withEndAction(() -> {
                if (!lastShow) setVisibility(GONE);
            }).start();
        }
    }

    private void follow() {
        try {
            int[] a = new int[2];
            int[] b = new int[2];
            enterView.getLocationInWindow(a);
            container.getLocationInWindow(b);
            float top = a[1] - b[1];
            // keep clear of the quick sticker strip when that one is on
            float extra = OmnigramPrefs.quickStickerBar() ? AndroidUtilities.dp(60) : 0;
            setTranslationY(top - container.getHeight() - AndroidUtilities.dp(8) - extra);
            if (enterView.getVisibility() != View.VISIBLE && getVisibility() == VISIBLE) setVisibility(GONE);
        } catch (Throwable ignore) {}
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try { attachTo(enterView.getEditField()); } catch (Throwable ignore) {}
        refresh();
        if (preDraw == null) {
            preDraw = () -> {
                follow();
                return true;
            };
        }
        try { getViewTreeObserver().addOnPreDrawListener(preDraw); } catch (Throwable ignore) {}
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try { if (preDraw != null) getViewTreeObserver().removeOnPreDrawListener(preDraw); } catch (Throwable ignore) {}
    }
}
