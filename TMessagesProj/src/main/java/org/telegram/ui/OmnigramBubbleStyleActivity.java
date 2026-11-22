package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.OmnigramBubble;
import org.telegram.ui.Components.OmnigramPrefs;

/** Omni Preference > Chat > Chat Bubble: pick how every message bubble looks. */
public class OmnigramBubbleStyleActivity extends OmnigramSettingsBase {

    private final View[] cards = new View[OmnigramBubble.NAMES.length];

    private class Preview extends View {
        private final int style;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);

        Preview(Context context, int style) {
            super(context);
            this.style = style;
            text.setTextSize(AndroidUtilities.dp(15));
            small.setTextSize(AndroidUtilities.dp(11));
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), AndroidUtilities.dp(108));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            String name = OmnigramBubble.NAMES[style];
            int in = getThemedColor(Theme.key_chat_inBubble);
            int out = getThemedColor(Theme.key_chat_outBubble);
            float pad = AndroidUtilities.dp(12);
            // incoming
            float inW = text.measureText(name) + AndroidUtilities.dp(70);
            RectF a = new RectF(pad, AndroidUtilities.dp(8), pad + inW, AndroidUtilities.dp(46));
            OmnigramBubble.drawPreview(canvas, a, false, style, in);
            text.setColor(getThemedColor(Theme.key_chat_messageTextIn));
            text.setTypeface(Typeface.DEFAULT);
            canvas.drawText(name, a.left + AndroidUtilities.dp(12), a.centerY() + AndroidUtilities.dp(5), text);
            small.setColor(getThemedColor(Theme.key_chat_inTimeText));
            canvas.drawText("2:54 PM", a.right - AndroidUtilities.dp(52), a.bottom - AndroidUtilities.dp(9), small);
            // outgoing
            float outW = text.measureText(name) + AndroidUtilities.dp(92);
            RectF b = new RectF(getMeasuredWidth() - pad - outW, AndroidUtilities.dp(58), getMeasuredWidth() - pad, AndroidUtilities.dp(96));
            OmnigramBubble.drawPreview(canvas, b, true, style, out);
            text.setColor(getThemedColor(Theme.key_chat_messageTextOut));
            canvas.drawText(name, b.left + AndroidUtilities.dp(12), b.centerY() + AndroidUtilities.dp(5), text);
            small.setColor(getThemedColor(Theme.key_chat_outTimeText));
            canvas.drawText("3:09 PM  \u2713\u2713", b.right - AndroidUtilities.dp(76), b.bottom - AndroidUtilities.dp(9), small);
        }
    }

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Chat Bubble");
        for (int i = 0; i < OmnigramBubble.NAMES.length; i++) {
            final int style = i;
            LinearLayout card = card(context);
            card.addView(new Preview(context, i), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            lp.bottomMargin = AndroidUtilities.dp(12);
            card.setOnClickListener(v -> {
                OmnigramPrefs.setInt("bubble_style", style);
                refresh();
            });
            cards[i] = card;
        }
        footnote(context, "Tap a style to use it in every chat. Reopen a chat to see the change. Default keeps Telegram's own bubbles and your message corner setting.");
        refresh();
        return root;
    }

    private void refresh() {
        int sel = OmnigramPrefs.bubbleStyle();
        for (int i = 0; i < cards.length; i++) {
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setCornerRadius(AndroidUtilities.dp(18));
            g.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
            if (i == sel) g.setStroke(AndroidUtilities.dp(2), getThemedColor(Theme.key_featuredStickers_addButton));
            cards[i].setBackground(g);
        }
    }
}
