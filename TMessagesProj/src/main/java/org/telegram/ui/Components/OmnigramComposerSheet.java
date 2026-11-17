package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

/** "Composer tools" menu: formatting toolbar, converters, preview, splitter, emoji, AI editor and templates. */
public class OmnigramComposerSheet {

    private final Context context;
    private final int account;
    private final ChatActivityEnterView enter;
    private final OmnigramComposerBar bar;
    private AlertDialog dialog;

    public OmnigramComposerSheet(Context context, int account, ChatActivityEnterView enter, OmnigramComposerBar bar) {
        this.context = context;
        this.account = account;
        this.enter = enter;
        this.bar = bar;
    }

    // ------------------------------------------------------------------ small view helpers

    private int textColor() {
        return Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
    }

    private TextView title(String t) {
        TextView v = new TextView(context);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        v.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(6));
        return v;
    }

    private TextView chip(String text, View.OnClickListener l) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        t.setTextColor(textColor());
        t.setGravity(Gravity.CENTER);
        t.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(9), AndroidUtilities.dp(12), AndroidUtilities.dp(9));
        t.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundGray)));
        t.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = AndroidUtilities.dp(6);
        lp.bottomMargin = AndroidUtilities.dp(6);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView action(String text, View.OnClickListener l) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        t.setTextColor(textColor());
        t.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12));
        t.setOnClickListener(l);
        return t;
    }

    private LinearLayout wrapRow() {
        // simple wrapping: rows of up to 4 chips
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        return col;
    }

    private void addChips(LinearLayout col, ArrayList<TextView> chips) {
        LinearLayout row = null;
        for (int i = 0; i < chips.size(); i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                col.addView(row);
            }
            row.addView(chips.get(i));
        }
    }

    private EditTextCaption field() {
        return enter.getEditField();
    }

    private void toast(String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    private boolean hasSelection(EditTextCaption et) {
        return et != null && et.getSelectionStart() != et.getSelectionEnd();
    }

    private void snapshot() {
        try { bar.history.push(field().getText()); } catch (Throwable ignore) {}
    }

    // ------------------------------------------------------------------ main menu

    public void show() {
        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        scroll.addView(root);

        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.FORMAT_TOOLBAR)) {
            root.addView(title("FORMAT SELECTED TEXT"));
            LinearLayout col = wrapRow();
            ArrayList<TextView> chips = new ArrayList<>();
            chips.add(chip("Bold", v -> format(0)));
            chips.add(chip("Italic", v -> format(1)));
            chips.add(chip("Underline", v -> format(2)));
            chips.add(chip("Strike", v -> format(3)));
            chips.add(chip("Spoiler", v -> format(4)));
            chips.add(chip("Code", v -> format(5)));
            chips.add(chip("Code block", v -> format(6)));
            chips.add(chip("Quote", v -> format(7)));
            chips.add(chip("Link", v -> format(8)));
            chips.add(chip("Mention", v -> format(9)));
            chips.add(chip("Clear", v -> format(10)));
            addChips(col, chips);
            root.addView(col);
        }

        root.addView(title("TOOLS"));
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.CONVERTER)) {
            root.addView(action("Convert Markdown / HTML to Telegram formatting", v -> { dismiss(); convertMarkdownHtml(); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.AUTO_FIX)) {
            root.addView(action("Auto fix formatting", v -> { dismiss(); autoFix(); }));
            root.addView(action("Restore original draft", v -> { dismiss(); restoreOriginal(); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.LIVE_PREVIEW)) {
            root.addView(action(bar.isPreviewOn() ? "Hide live preview" : "Show live preview", v -> { dismiss(); bar.setPreview(!bar.isPreviewOn()); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.THREAD_SPLITTER)) {
            root.addView(action("Split into numbered messages (thread)", v -> { dismiss(); splitThread(); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.LIST_TABLE)) {
            root.addView(action("Convert list / table to Telegram format", v -> { dismiss(); convertListTable(); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.UNDO_REDO)) {
            root.addView(action("Undo formatting change" + (bar.history.canUndo() ? "" : " (nothing to undo)"), v -> { dismiss(); undo(); }));
            root.addView(action("Redo formatting change" + (bar.history.canRedo() ? "" : " (nothing to redo)"), v -> { dismiss(); redo(); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.GRAMMAR_AI)) {
            root.addView(action("Grammar + clarity fix (Telegram AI editor)", v -> { dismiss(); openAiEditor(); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.TRANSLATE_AI)) {
            root.addView(action("Translate + localize (Telegram AI editor)", v -> { dismiss(); openAiEditor(); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.EMOJI_SUGGEST)) {
            root.addView(action("Emoji suggestions", v -> { dismiss(); emojiSuggestions(); }));
        }
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.WELCOME)) {
            root.addView(action("Welcome / auto-reply templates", v -> { dismiss(); templates(); }));
        }

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Composer tools");
        b.setView(scroll);
        b.setNegativeButton("Close", null);
        dialog = b.create();
        dialog.show();
    }

    private void dismiss() {
        try { if (dialog != null) dialog.dismiss(); } catch (Throwable ignore) {}
    }

    // ------------------------------------------------------------------ formatting toolbar

    private void format(int kind) {
        EditTextCaption et = field();
        if (et == null) return;
        if (kind == 9) { // mention
            int s = Math.max(0, et.getSelectionStart());
            et.getText().insert(s, "@");
            et.setSelection(s + 1);
            dismiss();
            return;
        }
        if (!hasSelection(et)) {
            toast("Select some text in the message box first");
            return;
        }
        snapshot();
        try {
            switch (kind) {
                case 0: et.makeSelectedBold(); break;
                case 1: et.makeSelectedItalic(); break;
                case 2: et.makeSelectedUnderline(); break;
                case 3: et.makeSelectedStrike(); break;
                case 4: et.makeSelectedSpoiler(); break;
                case 5: et.makeSelectedMono(); break;
                case 6: {
                    int s = et.getSelectionStart();
                    int e = et.getSelectionEnd();
                    Editable ed = et.getText();
                    if (e < ed.length() && ed.charAt(e) != '\n') {
                        ed.insert(e, "\n");
                    }
                    if (s > 0 && ed.charAt(s - 1) != '\n') {
                        ed.insert(s, "\n");
                        s++;
                        e++;
                    }
                    et.setSelection(s, e);
                    et.makeSelectedMono();
                    break;
                }
                case 7: et.makeSelectedQuote(); break;
                case 8: et.makeSelectedUrl(); break;
                case 10: et.makeSelectedRegular(); break;
                default: break;
            }
        } catch (Throwable t) {
            toast("Could not apply this formatting");
        }
        dismiss();
    }

    // ------------------------------------------------------------------ converters

    private void applyConverted(OmnigramComposerTools.Converted c) {
        EditTextCaption et = field();
        if (et == null) return;
        et.setText(c.text);
        Editable ed = et.getText();
        for (OmnigramComposerTools.Run r : c.runs) {
            int s = Math.max(0, Math.min(r.start, ed.length()));
            int e = Math.max(0, Math.min(r.end, ed.length()));
            if (s >= e) continue;
            try {
                if (r.type == OmnigramComposerTools.LINK) {
                    ed.setSpan(new URLSpanReplacement(r.url), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    continue;
                }
                et.setSelection(s, e);
                switch (r.type) {
                    case OmnigramComposerTools.BOLD: et.makeSelectedBold(); break;
                    case OmnigramComposerTools.ITALIC: et.makeSelectedItalic(); break;
                    case OmnigramComposerTools.UNDERLINE: et.makeSelectedUnderline(); break;
                    case OmnigramComposerTools.STRIKE: et.makeSelectedStrike(); break;
                    case OmnigramComposerTools.SPOILER: et.makeSelectedSpoiler(); break;
                    case OmnigramComposerTools.CODE: et.makeSelectedMono(); break;
                    case OmnigramComposerTools.QUOTE: et.makeSelectedQuote(); break;
                    default: break;
                }
            } catch (Throwable ignore) {}
        }
        et.setSelection(et.getText().length());
    }

    private void convertMarkdownHtml() {
        EditTextCaption et = field();
        if (et == null || et.getText().length() == 0) {
            toast("Type or paste some Markdown / HTML first");
            return;
        }
        if (bar.history.original == null) bar.history.original = new SpannableStringBuilder(et.getText());
        snapshot();
        OmnigramComposerTools.Converted c = OmnigramComposerTools.convert(et.getText().toString());
        applyConverted(c);
        toast("Converted. Use Undo or Restore original if it is not right");
    }

    private void autoFix() {
        EditTextCaption et = field();
        if (et == null || et.getText().length() == 0) {
            toast("Nothing to fix yet");
            return;
        }
        String before = et.getText().toString();
        String after = OmnigramComposerTools.autoFix(before);
        if (after.equals(before)) {
            toast("Nothing to fix");
            return;
        }
        bar.history.original = new SpannableStringBuilder(et.getText());
        snapshot();
        et.setText(after);
        et.setSelection(after.length());
        toast("Fixed. Original draft can be restored from Tools");
    }

    private void restoreOriginal() {
        EditTextCaption et = field();
        if (et == null) return;
        if (bar.history.original == null) {
            toast("No saved original draft");
            return;
        }
        snapshot();
        et.setText(bar.history.original);
        et.setSelection(et.getText().length());
    }

    private void convertListTable() {
        EditTextCaption et = field();
        if (et == null || et.getText().length() == 0) {
            toast("Paste a list or table into the message box first");
            return;
        }
        snapshot();
        OmnigramComposerTools.Converted c = OmnigramComposerTools.convertListOrTable(et.getText().toString());
        applyConverted(c);
    }

    private void undo() {
        EditTextCaption et = field();
        if (et == null) return;
        CharSequence prev = bar.history.undo(et.getText());
        if (prev == null) {
            toast("Nothing to undo");
            return;
        }
        et.setText(prev);
        et.setSelection(et.getText().length());
    }

    private void redo() {
        EditTextCaption et = field();
        if (et == null) return;
        CharSequence next = bar.history.redo(et.getText());
        if (next == null) {
            toast("Nothing to redo");
            return;
        }
        et.setText(next);
        et.setSelection(et.getText().length());
    }

    // ------------------------------------------------------------------ Telegram AI editor (grammar, clarity, translate)

    private void openAiEditor() {
        EditTextCaption et = field();
        if (et == null || et.getText().length() == 0) {
            toast("Type a message first");
            return;
        }
        try {
            snapshot();
            new AIEditorAlert(context, null)
                    .setText(et.getText())
                    .setOnUse(text -> {
                        et.setText(text);
                        et.setSelection(text.length(), text.length());
                    })
                    .show();
        } catch (Throwable t) {
            toast("The Telegram AI editor is not available right now");
        }
    }

    // ------------------------------------------------------------------ thread splitter

    private void splitThread() {
        final EditTextCaption et = field();
        if (et == null || et.getText().length() < 50) {
            toast("Paste a long message first");
            return;
        }
        final CharSequence source = new SpannableStringBuilder(et.getText());
        ScrollView scroll = new ScrollView(context);
        final LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), AndroidUtilities.dp(20), AndroidUtilities.dp(4));
        scroll.addView(box);

        final CheckBox numbering = new CheckBox(context);
        numbering.setText("Number the parts (1/5, 2/5 ...)");
        numbering.setChecked(true);
        numbering.setTextColor(textColor());
        box.addView(numbering);
        final LinearLayout partsBox = new LinearLayout(context);
        partsBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(partsBox);
        final ArrayList<EditText> edits = new ArrayList<>();

        final Runnable rebuild = () -> {
            partsBox.removeAllViews();
            edits.clear();
            ArrayList<CharSequence> parts = OmnigramComposerTools.splitThread(source, 3900, numbering.isChecked());
            for (int i = 0; i < parts.size(); i++) {
                TextView label = new TextView(context);
                label.setText("Part " + (i + 1) + " of " + parts.size() + "  (" + parts.get(i).length() + " chars, editable)");
                label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                label.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(2));
                partsBox.addView(label);
                EditText e = new EditText(context);
                e.setText(parts.get(i));
                e.setTextColor(textColor());
                e.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                e.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), Theme.getColor(Theme.key_windowBackgroundGray)));
                e.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
                e.setMaxLines(6);
                partsBox.addView(e);
                edits.add(e);
            }
        };
        rebuild.run();
        numbering.setOnCheckedChangeListener((cb, checked) -> rebuild.run());

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Thread splitter");
        b.setView(scroll);
        b.setNegativeButton("Cancel", null);
        b.setPositiveButton("Send all", (d, w) -> {
            final ArrayList<CharSequence> toSend = new ArrayList<>();
            for (EditText e : edits) {
                CharSequence cs = new SpannableStringBuilder(e.getText());
                if (cs.toString().trim().length() > 0) toSend.add(cs);
            }
            for (int i = 0; i < toSend.size(); i++) {
                final CharSequence part = toSend.get(i);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        enter.setFieldText(part);
                        enter.sendMessage();
                    } catch (Throwable ignore) {}
                }, 700L * i);
            }
        });
        b.show();
    }

    // ------------------------------------------------------------------ emoji suggestions + density

    private void emojiSuggestions() {
        final EditTextCaption et = field();
        if (et == null || et.getText().length() == 0) {
            toast("Type a message first");
            return;
        }
        final String text = et.getText().toString();
        ScrollView scroll = new ScrollView(context);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), AndroidUtilities.dp(20), AndroidUtilities.dp(4));
        scroll.addView(box);

        final TextView densityLabel = new TextView(context);
        densityLabel.setTextColor(textColor());
        densityLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        final SeekBar seek = new SeekBar(context);
        seek.setMax(4);
        seek.setProgress(OmnigramComposerPrefs.density());
        final LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        final ArrayList<ArrayList<OmnigramComposerTools.EmojiSuggestion>> current = new ArrayList<>();
        current.add(new ArrayList<>());

        final Runnable rebuild = () -> {
            int d = seek.getProgress();
            densityLabel.setText("Emoji density: " + OmnigramComposerTools.DENSITY_NAMES[d]);
            list.removeAllViews();
            ArrayList<OmnigramComposerTools.EmojiSuggestion> sug = OmnigramComposerTools.suggestEmojis(text, d);
            current.set(0, sug);
            if (sug.isEmpty()) {
                TextView none = new TextView(context);
                none.setText(d == 0 ? "None selected: no emojis will be added." : "No fitting emojis found for this message.");
                none.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                none.setPadding(0, AndroidUtilities.dp(10), 0, 0);
                list.addView(none);
                return;
            }
            for (OmnigramComposerTools.EmojiSuggestion s : sug) {
                CheckBox cb = new CheckBox(context);
                int from = Math.max(0, s.position - 28);
                String around = text.substring(from, Math.min(text.length(), s.position)).replace('\n', ' ');
                cb.setText(s.emoji + "   after \"..." + around + "\"");
                cb.setTextColor(textColor());
                cb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                cb.setChecked(true);
                cb.setOnCheckedChangeListener((v, checked) -> s.selected = checked);
                list.addView(cb);
            }
        };
        if (OmnigramComposerPrefs.on(OmnigramComposerPrefs.DENSITY_SLIDER)) {
            box.addView(densityLabel);
            box.addView(seek);
        } else {
            seek.setVisibility(View.GONE);
        }
        box.addView(list);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) { rebuild.run(); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { OmnigramComposerPrefs.setDensity(s.getProgress()); }
        });
        rebuild.run();

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Emoji suggestions");
        b.setView(scroll);
        b.setNegativeButton("Cancel", null);
        b.setPositiveButton("Apply selected", (dd, w) -> {
            ArrayList<OmnigramComposerTools.EmojiSuggestion> chosen = new ArrayList<>();
            for (OmnigramComposerTools.EmojiSuggestion s : current.get(0)) if (s.selected) chosen.add(s);
            if (chosen.isEmpty()) return;
            snapshot();
            java.util.Collections.sort(chosen, (x, y) -> Integer.compare(y.position, x.position));
            Editable ed = et.getText();
            for (OmnigramComposerTools.EmojiSuggestion s : chosen) {
                int p = Math.min(s.position, ed.length());
                ed.insert(p, " " + s.emoji);
            }
        });
        b.show();
    }

    // ------------------------------------------------------------------ welcome / auto-reply templates

    private Spannable render(OmnigramComposerTools.Converted c) {
        SpannableStringBuilder sb = new SpannableStringBuilder(c.text);
        for (OmnigramComposerTools.Run r : c.runs) {
            int s = Math.max(0, Math.min(r.start, sb.length()));
            int e = Math.max(0, Math.min(r.end, sb.length()));
            if (s >= e) continue;
            Object span = null;
            switch (r.type) {
                case OmnigramComposerTools.BOLD: span = new StyleSpan(Typeface.BOLD); break;
                case OmnigramComposerTools.ITALIC: span = new StyleSpan(Typeface.ITALIC); break;
                case OmnigramComposerTools.UNDERLINE: span = new UnderlineSpan(); break;
                case OmnigramComposerTools.STRIKE: span = new StrikethroughSpan(); break;
                case OmnigramComposerTools.CODE: span = new TypefaceSpan("monospace"); break;
                case OmnigramComposerTools.SPOILER: span = new BackgroundColorSpan(0x66888888); break;
                case OmnigramComposerTools.LINK: span = new UnderlineSpan(); break;
                case OmnigramComposerTools.QUOTE: span = new StyleSpan(Typeface.ITALIC); break;
                default: break;
            }
            if (span != null) sb.setSpan(span, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    private void templates() {
        final JSONArray arr = OmnigramComposerPrefs.templates();
        ScrollView scroll = new ScrollView(context);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), AndroidUtilities.dp(20), AndroidUtilities.dp(4));
        scroll.addView(box);

        TextView note = new TextView(context);
        note.setText("Use {name}, {group} and {link} in your text. Markdown is supported. Omnigram formats and stores your templates. Sending them automatically when someone joins needs a Telegram bot with admin permission in the group.");
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        note.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        box.addView(note);

        if (arr.length() == 0) {
            TextView empty = new TextView(context);
            empty.setText("No templates yet.");
            empty.setTextColor(textColor());
            empty.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(6));
            box.addView(empty);
        }
        for (int i = 0; i < arr.length(); i++) {
            final int index = i;
            final JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = new TextView(context);
            name.setText(o.optString("name", "Template"));
            name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            name.setTextColor(textColor());
            name.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            android.widget.Switch sw = new android.widget.Switch(context);
            sw.setChecked(o.optBoolean("enabled", true));
            sw.setOnCheckedChangeListener((v, checked) -> {
                try {
                    o.put("enabled", checked);
                    arr.put(index, o);
                    OmnigramComposerPrefs.saveTemplates(arr);
                } catch (Throwable ignore) {}
            });
            row.addView(sw);
            row.setOnClickListener(v -> templateMenu(arr, index));
            box.addView(row);
        }

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Welcome / auto-reply templates");
        b.setView(scroll);
        b.setNegativeButton("Close", null);
        b.setPositiveButton("New template", (d, w) -> editTemplate(arr, -1));
        b.show();
    }

    private void templateMenu(final JSONArray arr, final int index) {
        final JSONObject o = arr.optJSONObject(index);
        if (o == null) return;
        String[] items = {"Preview", "Insert into message", "Edit", "Delete"};
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(o.optString("name", "Template"));
        b.setItems(items, (d, which) -> {
            String text = o.optString("text", "");
            if (which == 0) {
                OmnigramComposerTools.Converted c = OmnigramComposerTools.convert(OmnigramComposerTools.fillTemplate(text, "Alex", "My Group", "https://t.me/example"));
                TextView tv = new TextView(context);
                tv.setText(render(c));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                tv.setTextColor(textColor());
                tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));
                AlertDialog.Builder pb = new AlertDialog.Builder(context);
                pb.setTitle("Preview (sample values)");
                pb.setView(tv);
                pb.setPositiveButton("Close", null);
                pb.show();
            } else if (which == 1) {
                snapshot();
                applyConverted(OmnigramComposerTools.convert(text));
            } else if (which == 2) {
                editTemplate(arr, index);
            } else {
                JSONArray next = new JSONArray();
                for (int i = 0; i < arr.length(); i++) if (i != index) next.put(arr.opt(i));
                OmnigramComposerPrefs.saveTemplates(next);
                toast("Template deleted");
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void editTemplate(final JSONArray arr, final int index) {
        JSONObject existing = index >= 0 ? arr.optJSONObject(index) : null;
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(4));
        final EditText name = new EditText(context);
        name.setHint("Template name");
        name.setSingleLine(true);
        name.setTextColor(textColor());
        final EditText body = new EditText(context);
        body.setHint("Welcome {name}! Please read the rules of {group}: {link}");
        body.setMinLines(4);
        body.setTextColor(textColor());
        if (existing != null) {
            name.setText(existing.optString("name"));
            body.setText(existing.optString("text"));
        }
        box.addView(name);
        box.addView(body);
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(index >= 0 ? "Edit template" : "New template");
        b.setView(box);
        b.setNegativeButton("Cancel", null);
        b.setPositiveButton("Save", (d, w) -> {
            String n = name.getText().toString().trim();
            String t = body.getText().toString();
            if (n.isEmpty() || t.trim().isEmpty()) {
                toast("Name and text are required");
                return;
            }
            boolean enabled = existing == null || existing.optBoolean("enabled", true);
            JSONObject o = OmnigramComposerPrefs.template(n, t, enabled);
            try {
                if (index >= 0) arr.put(index, o); else arr.put(o);
            } catch (Throwable ignore) {}
            OmnigramComposerPrefs.saveTemplates(arr);
        });
        b.show();
    }
}
