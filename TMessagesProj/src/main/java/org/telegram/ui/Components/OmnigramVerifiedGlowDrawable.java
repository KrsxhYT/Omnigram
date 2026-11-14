package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.provider.Settings;

import org.telegram.messenger.ApplicationLoader;

/**
 * Soft pulsing ring behind the genuine verified badge. It only wraps the existing badge drawable, never creates
 * one, and is off when disabled in Omni Preference or when the system animation scale is 0 (Reduce Motion).
 */
public class OmnigramVerifiedGlowDrawable extends Drawable {
    private final Drawable base;
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = 0xFF3897F0;

    public OmnigramVerifiedGlowDrawable(Drawable base) {
        this.base = base;
        ring.setStyle(Paint.Style.STROKE);
    }

    private static boolean reduceMotion() {
        try {
            return Settings.Global.getFloat(ApplicationLoader.applicationContext.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (OmnigramPrefs.verifiedAnim() && !reduceMotion() && b.width() > 0) {
            float phase = (SystemClock.uptimeMillis() % 2200L) / 2200f;
            float pulse = (float) (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI));
            float cx = b.exactCenterX();
            float cy = b.exactCenterY();
            float r = b.width() / 2f + b.width() * (0.12f + 0.16f * pulse);
            ring.setStrokeWidth(Math.max(1f, b.width() * 0.08f));
            ring.setColor(color);
            ring.setAlpha((int) (110 * (1f - pulse * 0.6f)));
            canvas.drawCircle(cx, cy, r, ring);
        }
        base.setBounds(b);
        base.draw(canvas);
    }

    @Override public void setBounds(int l, int t, int r, int b) { super.setBounds(l, t, r, b); base.setBounds(l, t, r, b); }
    @Override public void setAlpha(int alpha) { base.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter cf) { base.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return base.getIntrinsicWidth(); }
    @Override public int getIntrinsicHeight() { return base.getIntrinsicHeight(); }
}
