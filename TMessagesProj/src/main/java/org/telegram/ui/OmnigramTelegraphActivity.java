package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/** Telegraph: create a Telegraph page (published on graph.org) from a title, alias, author and text. */
public class OmnigramTelegraphActivity extends OmnigramSettingsBase {

    private EditText titleField;
    private EditText aliasField;
    private EditText authorField;
    private EditText contentField;
    private TextView createButton;
    private TextView status;
    private LinearLayout resultCard;
    private TextView resultLink;
    private String lastUrl;
    private boolean busy;

    private SharedPreferences sp() {
        return ApplicationLoader.applicationContext.getSharedPreferences("omnigram_telegraph", Context.MODE_PRIVATE);
    }

    private EditText field(Context context, String hint, boolean multiline) {
        EditText e = new EditText(context);
        e.setHint(hint);
        e.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        e.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        e.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        e.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_windowBackgroundWhite)));
        e.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(8);
            e.setGravity(Gravity.TOP);
        } else {
            e.setSingleLine(true);
        }
        return e;
    }

    private void label(Context context, String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        t.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(14), 0, AndroidUtilities.dp(6));
        list.addView(t);
    }

    private TextView button(Context context, String text, boolean filled) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(filled ? 0xFFFFFFFF : getThemedColor(Theme.key_featuredStickers_addButton));
        t.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        t.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), filled ? getThemedColor(Theme.key_featuredStickers_addButton) : getThemedColor(Theme.key_windowBackgroundWhite)));
        return t;
    }

    @Override
    public View createView(Context context) {
        View root = buildRoot(context, "Telegraph");

        label(context, "PAGE TITLE");
        titleField = field(context, "Page title", false);
        list.addView(titleField, matchWrap());

        label(context, "LINK ALIAS");
        aliasField = field(context, "Made-by-Krsxh", false);
        list.addView(aliasField, matchWrap());

        label(context, "AUTHOR NAME");
        authorField = field(context, "Your name", false);
        authorField.setText(sp().getString("author", ""));
        list.addView(authorField, matchWrap());

        label(context, "CONTENT");
        contentField = field(context, "Write the text of your page. Every new line becomes a paragraph.", true);
        list.addView(contentField, matchWrap());

        createButton = button(context, "Create", true);
        createButton.setOnClickListener(v -> create(context));
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = AndroidUtilities.dp(18);
        list.addView(createButton, bp);

        status = new TextView(context);
        status.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        status.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(10), 0, 0);
        list.addView(status);

        resultCard = new LinearLayout(context);
        resultCard.setOrientation(LinearLayout.VERTICAL);
        resultCard.setVisibility(View.GONE);
        resultCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), getThemedColor(Theme.key_windowBackgroundWhite)));
        resultCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        TextView done = new TextView(context);
        done.setText("Page created");
        done.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        done.setTypeface(AndroidUtilities.bold());
        done.setTextColor(0xFF2E9E4F);
        resultCard.addView(done);
        resultLink = new TextView(context);
        resultLink.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        resultLink.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteValueText));
        resultLink.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(10));
        resultLink.setTextIsSelectable(true);
        resultCard.addView(resultLink);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView copy = button(context, "Copy link", true);
        TextView open = button(context, "Open", false);
        copy.setOnClickListener(v -> {
            if (lastUrl != null) {
                AndroidUtilities.addToClipboard(lastUrl);
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show();
            }
        });
        open.setOnClickListener(v -> {
            if (lastUrl != null) open(lastUrl);
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cp.rightMargin = AndroidUtilities.dp(8);
        row.addView(copy, cp);
        row.addView(open, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        resultCard.addView(row);
        LinearLayout.LayoutParams rp = matchWrap();
        rp.topMargin = AndroidUtilities.dp(14);
        list.addView(resultCard, rp);
        return root;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void setStatus(String text, int color) {
        status.setText(text);
        status.setTextColor(color);
    }

    private void create(Context context) {
        if (busy) return;
        final String title = titleField.getText().toString().trim();
        final String alias = aliasField.getText().toString().trim();
        final String author = authorField.getText().toString().trim();
        final String content = contentField.getText().toString().trim();
        if (title.isEmpty() || title.length() > 256) {
            setStatus("Enter a page title (up to 256 characters)", 0xFFE53935);
            return;
        }
        if (content.isEmpty()) {
            setStatus("Content is empty", 0xFFE53935);
            return;
        }
        if (author.length() > 128) {
            setStatus("Author name is too long (up to 128 characters)", 0xFFE53935);
            return;
        }
        busy = true;
        createButton.setAlpha(0.5f);
        resultCard.setVisibility(View.GONE);
        setStatus("Creating page...", getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        sp().edit().putString("author", author).apply();

        Utilities.globalQueue.postRunnable(() -> {
            String error = null;
            String url = null;
            try {
                JSONArray nodes = new JSONArray();
                for (String line : content.split("\\n")) {
                    String t = line.trim();
                    if (t.isEmpty()) continue;
                    JSONObject p = new JSONObject();
                    p.put("tag", "p");
                    JSONArray children = new JSONArray();
                    children.put(t);
                    p.put("children", children);
                    nodes.put(p);
                }
                String token = sp().getString("token", null);
                if (token == null) {
                    Map<String, String> acc = new LinkedHashMap<>();
                    acc.put("short_name", "Omnigram");
                    acc.put("author_name", author);
                    JSONObject r = new JSONObject(post("createAccount", acc));
                    if (!r.optBoolean("ok")) throw new Exception(r.optString("error", "Could not create a Telegraph account"));
                    token = r.getJSONObject("result").getString("access_token");
                    sp().edit().putString("token", token).apply();
                }
                // The link path comes from the title Telegraph is given, so the alias is used first and the real title is set right after.
                boolean useAlias = !alias.isEmpty() && !alias.equals(title);
                Map<String, String> page = new LinkedHashMap<>();
                page.put("access_token", token);
                page.put("title", useAlias ? alias : title);
                page.put("author_name", author);
                page.put("content", nodes.toString());
                JSONObject cr = new JSONObject(post("createPage", page));
                if (!cr.optBoolean("ok")) throw new Exception(cr.optString("error", "Could not create the page"));
                String path = cr.getJSONObject("result").getString("path");
                if (useAlias) {
                    Map<String, String> edit = new LinkedHashMap<>();
                    edit.put("access_token", token);
                    edit.put("path", path);
                    edit.put("title", title);
                    edit.put("author_name", author);
                    edit.put("content", nodes.toString());
                    post("editPage", edit);
                }
                url = "https://graph.org/" + path;
            } catch (Throwable t) {
                error = t.getMessage() == null ? "Network error" : t.getMessage();
            }
            final String fError = error;
            final String fUrl = url;
            AndroidUtilities.runOnUIThread(() -> {
                busy = false;
                createButton.setAlpha(1f);
                if (fUrl != null) {
                    lastUrl = fUrl;
                    resultLink.setText(fUrl);
                    resultCard.setVisibility(View.VISIBLE);
                    setStatus("", 0);
                } else {
                    setStatus(fError, 0xFFE53935);
                }
            });
        });
    }

    private static String post(String method, Map<String, String> params) throws Exception {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
        }
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.telegra.ph/" + method).openConnection();
        try {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(25000);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            OutputStream os = c.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();
            InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }
}
