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
    /** Set while drawing so the host row knows it must redraw again to continue the pulse. */
    public static volatile boolean animatedDrawn = false;

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

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (OmnigramPrefs.verifiedAnim() && !reduceMotion() && b.width() > 0) {
            long now = SystemClock.uptimeMillis();
            float phase = (now % 2000L) / 2000f;
            float pulse = (float) (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI));
            float cx = b.exactCenterX();
            float cy = b.exactCenterY();
            float w = b.width();

            // soft halo
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(color);
            fill.setAlpha((int) (46 + 40 * pulse));
            canvas.drawCircle(cx, cy, w * (0.62f + 0.10f * pulse), fill);

            // expanding ring
            ring.setStrokeWidth(Math.max(1f, w * 0.07f));
            ring.setColor(color);
            ring.setAlpha((int) (210 * (1f - phase)));
            canvas.drawCircle(cx, cy, w * (0.5f + 0.5f * phase), ring);

            // three small sparkles orbiting the badge
            fill.setColor(0xFFFFFFFF);
            for (int i = 0; i < 3; i++) {
                double ang = (now % 3000L) / 3000.0 * 2 * Math.PI + i * (2 * Math.PI / 3);
                float tw = (float) (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI * 2 + i * 2.1));
                float r = w * 0.66f;
                fill.setAlpha((int) (60 + 180 * tw));
                canvas.drawCircle(cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r, Math.max(0.8f, w * (0.045f + 0.035f * tw)), fill);
            }
            animatedDrawn = true;
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
