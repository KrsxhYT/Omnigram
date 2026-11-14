package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaDataController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.HashSet;

/** Floating strip with recently used + favorite stickers. One tap sends, same as the sticker panel. */
public class OmnigramQuickStickerBar extends HorizontalScrollView {

    public interface Listener {
        void onSticker(TLRPC.Document document);
    }

    private final LinearLayout row;
    private final int account;
    private final Listener listener;
    private boolean loaded;
    private View anchor;
    private View container;
    private android.view.ViewTreeObserver.OnPreDrawListener preDraw;

    /** The bar sits just above {@code anchor} (the message box) inside {@code container} (full-screen layout). */
    public void setAnchor(View anchor, View container) {
        this.anchor = anchor;
        this.container = container;
    }

    private void follow() {
        try {
            if (anchor == null || container == null) return;
            int[] a = new int[2];
            int[] b = new int[2];
            anchor.getLocationInWindow(a);
            container.getLocationInWindow(b);
            float top = a[1] - b[1];
            setTranslationY(top - container.getHeight() - AndroidUtilities.dp(8));
            setVisibility(anchor.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        } catch (Throwable ignore) {}
    }

    public OmnigramQuickStickerBar(Context context, int account, Listener listener) {
        super(context);
        this.account = account;
        this.listener = listener;
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), Theme.getColor(Theme.key_chat_messagePanelBackground)));
        setElevation(AndroidUtilities.dp(2));
        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6), 0);
        addView(row, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }

    public void reload() {
        try {
            MediaDataController mdc = MediaDataController.getInstance(account);
            ArrayList<TLRPC.Document> docs = new ArrayList<>();
            HashSet<Long> seen = new HashSet<>();
            ArrayList<TLRPC.Document> fav = mdc.getRecentStickers(MediaDataController.TYPE_FAVE);
            ArrayList<TLRPC.Document> recent = mdc.getRecentStickers(MediaDataController.TYPE_IMAGE);
            if (fav != null) for (TLRPC.Document d : fav) if (d != null && seen.add(d.id)) docs.add(d);
            if (recent != null) for (TLRPC.Document d : recent) if (d != null && seen.add(d.id)) docs.add(d);
            if (docs.isEmpty()) return;
            row.removeAllViews();
            int count = Math.min(docs.size(), 20);
            for (int i = 0; i < count; i++) {
                final TLRPC.Document doc = docs.get(i);
                BackupImageView iv = new BackupImageView(getContext());
                iv.setImage(ImageLocation.getForDocument(doc), "80_80", (Drawable) null, doc);
                iv.setOnClickListener((View v) -> {
                    if (listener != null) listener.onSticker(doc);
                });
                row.addView(iv, new LinearLayout.LayoutParams(AndroidUtilities.dp(44), AndroidUtilities.dp(44)));
            }
            loaded = true;
        } catch (Throwable ignore) {}
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!loaded) reload();
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

    @Override
    public boolean onTouchEvent(android.view.MotionEvent ev) {
        // keep horizontal scrolling inside the strip, not stolen by the chat
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        return super.onTouchEvent(ev);
    }
}
