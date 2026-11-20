package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import org.telegram.messenger.AndroidUtilities;

/** Avatar corner setting: 100 = circle (normal), 0 = square. Only round avatar-sized images are changed. */
public final class OmnigramAvatar {
    private OmnigramAvatar() {}

    private static final RectF RECT = new RectF();

    public static int[] map(int[] radii) {
        int pct = OmnigramPrefs.avatarCorner();
        if (pct >= 100 || radii == null) return radii;
        int[] out = new int[radii.length];
        int min = AndroidUtilities.dp(18);
        for (int i = 0; i < radii.length; i++) {
            out[i] = radii[i] >= min ? Math.round(radii[i] * pct / 100f) : radii[i];
        }
        return out;
    }

    public static void drawShape(Canvas canvas, float size, Paint paint) {
        int pct = OmnigramPrefs.avatarCorner();
        if (pct >= 100) {
            canvas.drawCircle(size / 2.0f, size / 2.0f, size / 2.0f, paint);
            return;
        }
        float r = size / 2.0f * pct / 100f;
        RECT.set(0, 0, size, size);
        canvas.drawRoundRect(RECT, r, r, paint);
    }
}
