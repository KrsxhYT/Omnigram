package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.R;

/**
 * Draws a small tinted vector icon where the time text has the pencil (edited) or bin (deleted) placeholder,
 * so the icons look the same on every device instead of depending on the system emoji font.
 */
public class OmnigramIconSpan extends ReplacementSpan {
    private final Drawable drawable;
    private final int fixedColor;

    private OmnigramIconSpan(Drawable drawable, int fixedColor) {
        this.drawable = drawable;
        this.fixedColor = fixedColor;
    }

    public static final String[] ICON_NAMES = {"Dustbin", "Cross", "Eye crossed", "Block", "Warning", "Cancel"};
    public static final int[] COLORS = {0, 0xFF2196F3, 0xFF9C27B0, 0xFF29B6F6, 0xFFE53935, 0xFFEC407A};
    public static final String[] COLOR_NAMES = {"Default", "Blue", "Purple", "Sky blue", "Red", "Pink"};

    public static int iconRes(int index) {
        switch (index) {
            case 1: return R.drawable.omni_ic_cross;
            case 2: return R.drawable.omni_ic_eye_off;
            case 3: return R.drawable.omni_ic_block;
            case 4: return R.drawable.omni_ic_warning;
            case 5: return R.drawable.omni_ic_cancel;
            default: return R.drawable.omni_ic_bin;
        }
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) Math.ceil(paint.getTextSize() * 0.95f);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        if (drawable == null) return;
        int size = Math.round(paint.getTextSize() * 0.9f);
        int bottomY = Math.round(y - paint.getTextSize() * 0.04f);
        drawable.setBounds(0, 0, size, size);
        drawable.setColorFilter(new PorterDuffColorFilter(fixedColor != 0 ? fixedColor : paint.getColor(), PorterDuff.Mode.SRC_IN));
        drawable.setAlpha(paint.getAlpha());
        canvas.save();
        canvas.translate(x, bottomY - size);
        drawable.draw(canvas);
        canvas.restore();
    }

    private static final String PENCIL_A = "\u270E";
    private static final String PENCIL_B = "\u270F";
    private static final String BIN = "\uD83D\uDDD1";

    /** Replaces the first pencil / bin character of the time string with an icon span. */
    public static CharSequence apply(Context context, CharSequence text) {
        if (context == null || text == null || text.length() == 0) return text;
        try {
            String s = text.toString();
            int idx;
            int len;
            int res;
            int color = 0;
            if ((idx = s.indexOf(BIN)) >= 0) {
                len = BIN.length();
                res = iconRes(OmnigramPrefs.deletedIcon());
                color = COLORS[Math.max(0, Math.min(COLORS.length - 1, OmnigramPrefs.deletedColor()))];
            } else if ((idx = s.indexOf(PENCIL_A)) >= 0 || (idx = s.indexOf(PENCIL_B)) >= 0) {
                len = 1;
                res = R.drawable.omni_ic_pencil;
            } else {
                return text;
            }
            Drawable d = ContextCompat.getDrawable(context, res);
            if (d == null) return text;
            d = d.getConstantState() != null ? d.getConstantState().newDrawable().mutate() : d.mutate();
            SpannableStringBuilder sb = new SpannableStringBuilder(text);
            sb.setSpan(new OmnigramIconSpan(d, color), idx, idx + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sb;
        } catch (Throwable t) {
            return text;
        }
    }
}
