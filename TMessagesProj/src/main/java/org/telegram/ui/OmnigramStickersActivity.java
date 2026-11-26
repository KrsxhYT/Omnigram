package org.telegram.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.OmnigramPrefs;

/** Omni Preference > Chat > Stickers: sticker size, shape, and sticker/emoji options. */
public class OmnigramStickersActivity extends OmnigramSettingsBase {

    private StickerPreview preview;
    private TextView sizeChip;
    private final View[] shapeCards = new View[3];
    private final TextView[] shapeLabels = new TextView[3];

    private static final String[] SHAPES = {"Default", "Rounded", "Message"};

    private Path shapePath(RectF r, int shape, boolean out) {
        Path p = new Path();
        float[] radii;
        float big = AndroidUtilities.dp(18);
        float small = AndroidUtilities.dp(4);
        if (shape == 1) {
            float rr = AndroidUtilities.dp(16);
            radii = new float[]{rr, rr, rr, rr, rr, rr, rr, rr};
        } else if (shape == 2) {
            radii = out ? new float[]{big, big, big, big, small, small, big, big} : new float[]{big, big, big, big, big, big, small, small};
        } else {
            radii = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
        }
        p.addRoundRect(r, radii, Path.Direction.CW);
        return p;
    }

    private class StickerPreview extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Bitmap bitmap;

        StickerPreview(Context context) {
            super(context);
            try {
                bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.omni_bg_1);
            } catch (Throwable ignore) {}
            text.setTextSize(AndroidUtilities.dp(16));
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), AndroidUtilities.dp(330));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float width = getMeasuredWidth();
            // incoming quote bubble
            Paint bubble = new Paint(Paint.ANTI_ALIAS_FLAG);
            bubble.setColor(getThemedColor(Theme.key_chat_inBubble));
            RectF a = new RectF(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(150), AndroidUtilities.dp(62));
            canvas.drawRoundRect(a, AndroidUtilities.dp(14), AndroidUtilities.dp(14), bubble);
            text.setColor(getThemedColor(Theme.key_chat_messageTextIn));
            text.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("Omnigram", a.left + AndroidUtilities.dp(12), a.top + AndroidUtilities.dp(22), text);
            text.setTypeface(Typeface.DEFAULT);
            canvas.drawText("wow", a.left + AndroidUtilities.dp(12), a.top + AndroidUtilities.dp(42), text);

            // sticker (right side), size and shape follow the settings
            float side = Math.min(AndroidUtilities.dp(150) * OmnigramPrefs.stickerScale(), width * 0.62f);
            RectF s = new RectF(width - AndroidUtilities.dp(14) - side, AndroidUtilities.dp(12), width - AndroidUtilities.dp(14), AndroidUtilities.dp(12) + side);
            if (bitmap != null) {
                canvas.save();
                canvas.clipPath(shapePath(s, OmnigramPrefs.stickerShape(), true));
                canvas.drawBitmap(bitmap, null, s, paint);
                canvas.restore();
            }

            // incoming message under it
            float top = Math.max(AndroidUtilities.dp(190), s.bottom + AndroidUtilities.dp(14));
            RectF b = new RectF(AndroidUtilities.dp(14), top, width * 0.78f, top + AndroidUtilities.dp(86));
            canvas.drawRoundRect(b, AndroidUtilities.dp(18), AndroidUtilities.dp(18), bubble);
            text.setColor(getThemedColor(Theme.key_chat_messageTextIn));
            text.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("Telegram", b.left + AndroidUtilities.dp(14), b.top + AndroidUtilities.dp(26), text);
            text.setTypeface(Typeface.DEFAULT);
            canvas.drawText("woah, he is so cute!", b.left + AndroidUtilities.dp(14), b.top + AndroidUtilities.dp(60), text);
        }
    }

    private class ShapeBox extends View {
        private final int shape;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

        ShapeBox(Context context, int shape) {
            super(context);
            this.shape = shape;
            fill.setColor(getThemedColor(Theme.key_windowBackgroundGray));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float pad = AndroidUtilities.dp(10);
            RectF r = new RectF(pad, pad, getMeasuredWidth() - pad, getMeasuredHeight() - pad);
            canvas.drawPath(shapePath(r, shape, true), fill);
        }
    }

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Stickers");
        final int accent = getThemedColor(Theme.key_featuredStickers_addButton);

        // ---- size
        LinearLayout size = card(context);
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(2));
        TextView title = new TextView(context);
        title.setText("Stickers Size");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        head.addView(title);
        sizeChip = new TextView(context);
        sizeChip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        sizeChip.setTypeface(AndroidUtilities.bold());
        sizeChip.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteValueText));
        sizeChip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), getThemedColor(Theme.key_windowBackgroundGray)));
        sizeChip.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.leftMargin = AndroidUtilities.dp(10);
        head.addView(sizeChip, cp);
        size.addView(head);
        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.HORIZONTAL);
        labels.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), 0);
        TextView small = new TextView(context);
        small.setText("Small");
        small.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        labels.addView(small, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView big = new TextView(context);
        big.setText("Big");
        big.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        labels.addView(big);
        size.addView(labels);
        SeekBar seek = new SeekBar(context);
        seek.setMax(15);
        seek.setProgress(OmnigramPrefs.stickerSize() - 5);
        seek.setProgressTintList(ColorStateList.valueOf(accent));
        seek.setThumbTintList(ColorStateList.valueOf(accent));
        seek.setPadding(AndroidUtilities.dp(22), AndroidUtilities.dp(10), AndroidUtilities.dp(22), AndroidUtilities.dp(10));
        size.addView(seek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        preview = new StickerPreview(context);
        size.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sizeChip.setText(String.valueOf(OmnigramPrefs.stickerSize()));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                OmnigramPrefs.setInt("sticker_size", p + 5);
                sizeChip.setText(String.valueOf(p + 5));
                preview.invalidate();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // ---- shape
        section(context, "Stickers Shape");
        LinearLayout shapes = new LinearLayout(context);
        shapes.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 3; i++) {
            final int shape = i;
            LinearLayout col = new LinearLayout(context);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            View box = new ShapeBox(context, i);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(96));
            col.addView(box, bp);
            shapeCards[i] = box;
            TextView label = new TextView(context);
            label.setText(SHAPES[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            label.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(4));
            shapeLabels[i] = label;
            col.addView(label);
            col.setOnClickListener(v -> {
                OmnigramPrefs.setInt("sticker_shape", shape);
                refreshShapes();
                preview.invalidate();
            });
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cl.setMargins(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            shapes.addView(col, cl);
        }
        list.addView(shapes, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        refreshShapes();

        // ---- options
        section(context, "Stickers and Emoji");
        LinearLayout opts = card(context);
        toggle(context, opts, "Hide Time on Stickers", null, OmnigramPrefs.HIDE_STICKER_TIME, false);
        toggle(context, opts, "Unlimited Recent Stickers", null, OmnigramPrefs.UNLIMITED_RECENT_STICKERS, false);
        footnote(context, "Sticker size and shape apply to stickers in chats. Reopen a chat to see the change.");
        return root;
    }

    private void refreshShapes() {
        int sel = OmnigramPrefs.stickerShape();
        for (int i = 0; i < 3; i++) {
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setCornerRadius(AndroidUtilities.dp(16));
            g.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
            if (i == sel) g.setStroke(AndroidUtilities.dp(2), getThemedColor(Theme.key_featuredStickers_addButton));
            shapeCards[i].setBackground(g);
            shapeLabels[i].setTextColor(i == sel ? getThemedColor(Theme.key_windowBackgroundWhiteValueText) : getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            shapeLabels[i].setGravity(Gravity.CENTER);
        }
    }
}
