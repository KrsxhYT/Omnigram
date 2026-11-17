package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.security.MessageDigest;

/** Per-chat 4-digit password. Only a salted hash is stored on this device. */
public final class OmnigramChatLock {
    private OmnigramChatLock() {}

    public interface Result {
        void done(boolean ok);
    }

    private static SharedPreferences sp() {
        return ApplicationLoader.applicationContext.getSharedPreferences("omnigram_chatlock", Context.MODE_PRIVATE);
    }

    private static String hash(long dialogId, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("omnigram:" + dialogId + ":" + pin).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Throwable t) {
            return pin;
        }
    }

    public static boolean isLocked(long dialogId) {
        try { return sp().contains("pin_" + dialogId); } catch (Throwable t) { return false; }
    }

    public static void setPin(long dialogId, String pin) {
        try { sp().edit().putString("pin_" + dialogId, hash(dialogId, pin)).apply(); } catch (Throwable ignore) {}
    }

    public static void remove(long dialogId) {
        try { sp().edit().remove("pin_" + dialogId).apply(); } catch (Throwable ignore) {}
    }

    public static boolean verify(long dialogId, String pin) {
        try { return hash(dialogId, pin).equals(sp().getString("pin_" + dialogId, "")); } catch (Throwable t) { return false; }
    }

    private static EditText pinField(Context context, String hint) {
        EditText e = new EditText(context);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        e.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        e.setTextSize(22);
        e.setLetterSpacing(0.4f);
        return e;
    }

    private static LinearLayout box(Context context) {
        LinearLayout b = new LinearLayout(context);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(4));
        return b;
    }

    /** Ask for the current password until it is right or the user cancels. */
    public static void askPin(final Context context, final long dialogId, final String title, final Result result) {
        if (context == null) {
            result.done(false);
            return;
        }
        final EditText field = pinField(context, "4-digit password");
        LinearLayout b = box(context);
        b.addView(field);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage("Enter the 4-digit password for this chat");
        builder.setView(b);
        builder.setPositiveButton("Unlock", (d, w) -> {
            if (verify(dialogId, field.getText().toString())) {
                result.done(true);
            } else {
                Toast.makeText(context, "Wrong password", Toast.LENGTH_SHORT).show();
                askPin(context, dialogId, title, result);
            }
        });
        builder.setNegativeButton("Cancel", (d, w) -> result.done(false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /** Set a new 4-digit password (typed twice). */
    public static void askNewPin(final Context context, final long dialogId, final Runnable onSaved) {
        if (context == null) return;
        final EditText one = pinField(context, "New 4-digit password");
        final EditText two = pinField(context, "Repeat password");
        LinearLayout b = box(context);
        b.addView(one);
        b.addView(two);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Lock chat");
        builder.setMessage("Choose a password with exactly 4 digits. It is saved only on this device.");
        builder.setView(b);
        builder.setPositiveButton("Lock", (d, w) -> {
            String p1 = one.getText().toString();
            String p2 = two.getText().toString();
            if (!p1.matches("[0-9]{4}")) {
                Toast.makeText(context, "The password must be exactly 4 digits", Toast.LENGTH_SHORT).show();
                askNewPin(context, dialogId, onSaved);
            } else if (!p1.equals(p2)) {
                Toast.makeText(context, "The two passwords do not match", Toast.LENGTH_SHORT).show();
                askNewPin(context, dialogId, onSaved);
            } else {
                setPin(dialogId, p1);
                Toast.makeText(context, "Chat locked", Toast.LENGTH_SHORT).show();
                if (onSaved != null) onSaved.run();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
