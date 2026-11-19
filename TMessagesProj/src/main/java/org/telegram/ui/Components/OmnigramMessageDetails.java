package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Message > Details. Only fields that really exist for the message are shown, grouped by message type. */
public final class OmnigramMessageDetails {
    private OmnigramMessageDetails() {}

    private static void add(ArrayList<String[]> rows, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        rows.add(new String[]{label, value});
    }

    private static String date(long seconds) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(new Date(seconds * 1000L));
    }

    private static String size(long bytes) {
        if (bytes <= 0) return null;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String duration(double seconds) {
        int s = (int) Math.round(seconds);
        if (s <= 0) return null;
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    public static ArrayList<String[]> build(int account, MessageObject m, long dialogId) {
        ArrayList<String[]> rows = new ArrayList<>();
        TLRPC.Message msg = m.messageOwner;
        TLRPC.Document doc = m.getDocument();
        TLRPC.Photo photo = msg.media != null ? msg.media.photo : null;
        boolean sticker = m.isSticker() || m.isAnimatedSticker();
        boolean image = !sticker && m.isPhoto();
        boolean video = !sticker && doc != null && (m.isVideo() || m.isRoundVideo() || m.isGif());
        boolean file = !sticker && !image && !video && doc != null;

        // channel counters (only when Telegram gave them)
        if (msg.post || (msg.views > 0)) {
            if (msg.views > 0) add(rows, "Views", String.valueOf(msg.views));
            if (msg.forwards > 0) add(rows, "Shares", String.valueOf(msg.forwards));
        }
        add(rows, "ID", String.valueOf(m.getId()));
        if (msg.date > 0) add(rows, "Date", date(msg.date));
        if (msg.fwd_from != null && msg.fwd_from.date > 0) add(rows, "Forward Date", date(msg.fwd_from.date));

        if (doc != null) {
            add(rows, "File Size", size(doc.size));
            add(rows, "MIME Type", doc.mime_type);
            String fileName = null;
            String resolution = null;
            double dur = 0;
            for (TLRPC.DocumentAttribute a : doc.attributes) {
                if (a instanceof TLRPC.TL_documentAttributeFilename) fileName = a.file_name;
                if (a instanceof TLRPC.TL_documentAttributeVideo) {
                    if (a.w > 0 && a.h > 0) resolution = a.w + " x " + a.h;
                    dur = a.duration;
                }
                if (a instanceof TLRPC.TL_documentAttributeImageSize && resolution == null && a.w > 0 && a.h > 0) resolution = a.w + " x " + a.h;
                if (a instanceof TLRPC.TL_documentAttributeAudio && dur == 0) dur = a.duration;
            }
            if (resolution == null && doc.thumbs != null) {
                TLRPC.PhotoSize ps = FileLoader.getClosestPhotoSizeWithSize(doc.thumbs, Integer.MAX_VALUE);
                if (ps != null && ps.w > 0 && ps.h > 0 && (sticker || video)) resolution = ps.w + " x " + ps.h;
            }
            if (sticker || video) add(rows, "Resolution", resolution);
            if (video || m.isVoice() || m.isMusic()) add(rows, "Duration", duration(dur));
            if (video && dur > 0 && doc.size > 0) {
                add(rows, "Bitrate", String.format(Locale.US, "%d kbps", Math.round(doc.size * 8.0 / dur / 1000.0)));
            }
            add(rows, "File Name", fileName);
            try {
                File f = FileLoader.getInstance(account).getPathToMessage(msg);
                if (f != null && f.exists()) add(rows, "File Path", f.getAbsolutePath());
            } catch (Throwable ignore) {}
            if (doc.dc_id > 0) add(rows, "Data Center", "DC " + doc.dc_id);
        } else if (photo != null) {
            TLRPC.PhotoSize ps = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, Integer.MAX_VALUE);
            add(rows, "MIME Type", "image/jpeg");
            if (ps != null && ps.w > 0 && ps.h > 0) add(rows, "Resolution", ps.w + " x " + ps.h);
            try {
                File f = FileLoader.getInstance(account).getPathToMessage(msg);
                if (f != null && f.exists()) {
                    add(rows, "File Path", f.getAbsolutePath());
                    add(rows, "File Name", f.getName());
                }
            } catch (Throwable ignore) {}
            if (photo.dc_id > 0) add(rows, "Data Center", "DC " + photo.dc_id);
        }

        // sender
        try {
            long fromId = m.getFromChatId();
            String name = null;
            if (fromId > 0) {
                TLRPC.User u = MessagesController.getInstance(account).getUser(fromId);
                if (u != null) name = UserObject.getUserName(u);
            } else if (fromId < 0) {
                TLRPC.Chat c = MessagesController.getInstance(account).getChat(-fromId);
                if (c != null) name = c.title;
            }
            if (fromId != 0 && !(msg.post && fromId == dialogId)) {
                add(rows, "From", (name != null ? name + "  \u00B7  " : "") + "ID " + fromId);
            } else if (msg.post && fromId != 0) {
                add(rows, "From", (name != null ? name + "  \u00B7  " : "") + "ID " + fromId);
            }
        } catch (Throwable ignore) {}
        return rows;
    }

    public static void show(Context context, int account, MessageObject m, long dialogId) {
        if (context == null || m == null) return;
        ArrayList<String[]> rows = build(account, m, dialogId);
        String type = m.isSticker() || m.isAnimatedSticker() ? "Sticker" : m.isPhoto() ? "Image" : (m.getDocument() != null && (m.isVideo() || m.isRoundVideo() || m.isGif())) ? "Video" : m.getDocument() != null ? "File" : "Message";

        ScrollView scroll = new ScrollView(context);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), AndroidUtilities.dp(20), AndroidUtilities.dp(4));
        scroll.addView(box);
        for (final String[] r : rows) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
            TextView label = new TextView(context);
            label.setText(r[0]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            row.addView(label);
            TextView value = new TextView(context);
            value.setText(r[1]);
            value.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            value.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            if (r[0].equals("File Path")) value.setTypeface(Typeface.MONOSPACE);
            row.addView(value);
            row.setOnClickListener(v -> {
                AndroidUtilities.addToClipboard(r[1]);
                Toast.makeText(context, r[0] + " copied", Toast.LENGTH_SHORT).show();
            });
            box.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(type + " details");
        b.setView(scroll);
        b.setPositiveButton("Close", null);
        b.show();
    }
}
