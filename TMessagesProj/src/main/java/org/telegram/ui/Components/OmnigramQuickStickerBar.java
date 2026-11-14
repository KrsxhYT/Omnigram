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
    }
}
