package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;

/**
 * Chat bubble styles. The corner size and the extra outline are changed here, the normal Telegram bubble shape
 * and colors are still used underneath.
 */
public final class OmnigramBubble {
    private OmnigramBubble() {}

    public static final String[] NAMES = {"Default", "iOS", "Telegram X", "Notepad", "LineFineLex", "PictuLineLex"};

    private static final Paint LINE = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF RECT = new RectF();

    /** Corner radius in dp for the current style (the user's own corner setting for Default). */
    public static int radius() {
        switch (OmnigramPrefs.bubbleStyle()) {
            case 1: return 17;
            case 2: return 12;
            case 3: return 2;
            case 4: return 9;
            case 5: return 12;
            default: return SharedConfig.bubbleRadius;
        }
    }

    /** Draws the outline / marks of a style on top of a bubble that was just filled. */
    public static void afterDraw(Canvas canvas, Path path, Rect bounds, boolean out) {
        int style = OmnigramPrefs.bubbleStyle();
        if (style < 3) return;
        LINE.setStyle(Paint.Style.STROKE);
        LINE.setPathEffect(null);
        LINE.clearShadowLayer();
        if (style == 3) {
            // Notepad: dotted edge on the outer side, like a torn page
            LINE.setColor(0x99FFFFFF);
            LINE.setStrokeWidth(AndroidUtilities.dp(1.5f));
            LINE.setPathEffect(new DashPathEffect(new float[]{AndroidUtilities.dp(2), AndroidUtilities.dp(3)}, 0));
            float x = out ? bounds.right - AndroidUtilities.dp(3) : bounds.left + AndroidUtilities.dp(3);
            canvas.drawLine(x, bounds.top + AndroidUtilities.dp(4), x, bounds.bottom - AndroidUtilities.dp(4), LINE);
        } else if (style == 4) {
            LINE.setColor(0xCCFFFFFF);
            LINE.setStrokeWidth(AndroidUtilities.dp(1f));
            canvas.drawPath(path, LINE);
        } else if (style == 5) {
            LINE.setColor(0xFFFFFFFF);
            LINE.setStrokeWidth(AndroidUtilities.dp(1.6f));
            LINE.setShadowLayer(AndroidUtilities.dp(4), 0, 0, 0xAAFFFFFF);
            canvas.drawPath(path, LINE);
            LINE.clearShadowLayer();
        }
    }

    /** Preview of a style for the Chat Bubble screen. */
    public static void drawPreview(Canvas canvas, RectF r, boolean out, int style, int color) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        float rad;
        switch (style) {
            case 1: rad = AndroidUtilities.dp(17); break;
            case 2: rad = AndroidUtilities.dp(12); break;
            case 3: rad = AndroidUtilities.dp(2); break;
            case 4: rad = AndroidUtilities.dp(9); break;
            case 5: rad = AndroidUtilities.dp(12); break;
            default: rad = AndroidUtilities.dp(SharedConfig.bubbleRadius); break;
        }
        canvas.drawRoundRect(r, rad, rad, fill);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        if (style == 3) {
            line.setColor(0x99FFFFFF);
            line.setStrokeWidth(AndroidUtilities.dp(1.5f));
            line.setPathEffect(new DashPathEffect(new float[]{AndroidUtilities.dp(2), AndroidUtilities.dp(3)}, 0));
            float x = out ? r.right - AndroidUtilities.dp(3) : r.left + AndroidUtilities.dp(3);
            canvas.drawLine(x, r.top + AndroidUtilities.dp(4), x, r.bottom - AndroidUtilities.dp(4), line);
        } else if (style == 4) {
            line.setColor(0xCCFFFFFF);
            line.setStrokeWidth(AndroidUtilities.dp(1f));
            canvas.drawRoundRect(r, rad, rad, line);
        } else if (style == 5) {
            line.setColor(0xFFFFFFFF);
            line.setStrokeWidth(AndroidUtilities.dp(1.6f));
            line.setShadowLayer(AndroidUtilities.dp(4), 0, 0, 0xAAFFFFFF);
            canvas.drawRoundRect(r, rad, rad, line);
        }
    }
}
