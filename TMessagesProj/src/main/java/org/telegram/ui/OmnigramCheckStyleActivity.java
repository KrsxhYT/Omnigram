package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.OmnigramCheckStyle;
import org.telegram.ui.Components.OmnigramPrefs;

/** Omni Preference > Chat > Check Style: choose the sent / read tick icons. */
public class OmnigramCheckStyleActivity extends OmnigramSettingsBase {

    private final View[] cards = new View[OmnigramCheckStyle.NAMES.length];

    private class Preview extends View {
        private final int style;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable a;
        private final Drawable b;

        Preview(Context context, int style) {
            super(context);
            this.style = style;
            text.setTextSize(AndroidUtilities.dp(16));
            small.setTextSize(AndroidUtilities.dp(11));
            a = context.getResources().getDrawable(OmnigramCheckStyle.previewRes(style, false)).mutate();
            b = context.getResources().getDrawable(OmnigramCheckStyle.previewRes(style, true)).mutate();
            int c = Theme.getColor(Theme.key_chat_outSentCheckRead);
            a.setColorFilter(new PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN));
            b.setColorFilter(new PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN));
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), AndroidUtilities.dp(64));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            String name = OmnigramCheckStyle.NAMES[style];
            float pad = AndroidUtilities.dp(14);
            float w = text.measureText(name) + AndroidUtilities.dp(110);
            RectF r = new RectF(getMeasuredWidth() - pad - w, AndroidUtilities.dp(10), getMeasuredWidth() - pad, AndroidUtilities.dp(54));
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setColor(getThemedColor(Theme.key_chat_outBubble));
            canvas.drawRoundRect(r, AndroidUtilities.dp(14), AndroidUtilities.dp(14), fill);
            text.setColor(getThemedColor(Theme.key_chat_messageTextOut));
            canvas.drawText(name, r.left + AndroidUtilities.dp(12), r.centerY() + AndroidUtilities.dp(6), text);
            small.setColor(getThemedColor(Theme.key_chat_outTimeText));
            canvas.drawText("3:09 PM", r.right - AndroidUtilities.dp(84), r.bottom - AndroidUtilities.dp(14), small);
            int cx = (int) (r.right - AndroidUtilities.dp(26));
            int cy = (int) (r.bottom - AndroidUtilities.dp(24));
            a.setBounds(cx, cy, cx + AndroidUtilities.dp(12), cy + AndroidUtilities.dp(12));
            a.draw(canvas);
            b.setBounds(cx + AndroidUtilities.dp(4), cy, cx + AndroidUtilities.dp(16), cy + AndroidUtilities.dp(12));
            b.draw(canvas);
        }
    }

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Check Style");
        for (int i = 0; i < OmnigramCheckStyle.NAMES.length; i++) {
            final int style = i;
            LinearLayout card = card(context);
            card.addView(new Preview(context, i), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            lp.bottomMargin = AndroidUtilities.dp(10);
            card.setOnClickListener(v -> {
                OmnigramPrefs.setInt("check_style", style);
                refresh();
            });
            cards[i] = card;
        }
        footnote(context, "Replaces the sent and read tick marks on your messages. Restart the app to see it in chats.");
        refresh();
        return root;
    }

    private void refresh() {
        int sel = OmnigramPrefs.checkStyle();
        for (int i = 0; i < cards.length; i++) {
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setCornerRadius(AndroidUtilities.dp(18));
            g.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
            if (i == sel) g.setStroke(AndroidUtilities.dp(2), getThemedColor(Theme.key_featuredStickers_addButton));
            cards[i].setBackground(g);
        }
    }
}
