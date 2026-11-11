#!/usr/bin/env python3
"""Force Omnigram UI fixes that previous APKs missed + wire Part2 menus."""
from pathlib import Path
import re

ROOT = Path(".")


def force_title():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Stories/DialogStoriesCell.java"
    t = p.read_text(encoding="utf-8", errors="replace")

    t = t.replace(
        "telegramLogoView.setImageResource(R.drawable.telegram_logo_2);",
        "telegramLogoView.setImageDrawable(null); // Omnigram: no Telegram word logo\n"
        "        telegramLogoView.setVisibility(GONE);",
    )

    if "omnigramUserTitle" not in t:
        t = t.replace(
            "private void checkUi_titleVisibility()",
            """private CharSequence omnigramUserTitle() {
        try {
            org.telegram.tgnet.TLRPC.User me = org.telegram.messenger.UserConfig.getInstance(currentAccount).getCurrentUser();
            if (me != null) {
                String n = org.telegram.messenger.UserObject.getUserName(me);
                if (n != null && n.length() > 0) return n;
            }
        } catch (Throwable ignore) {}
        return org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.AppName);
    }

    private void checkUi_titleVisibility()""",
            1,
        )

    # Replace checkUi body aggressively if logo still can show
    if "telegramLogoView.setVisibility(GONE)" not in t or "setImageDrawable(null)" not in t:
        pass

    t = t.replace(
        "currentTitle = menuItemsOffset < dp(50) ? null :",
        "currentTitle = menuItemsOffset < dp(50) ? omnigramUserTitle() :",
    )

    if "if (TextUtils.isEmpty(currentTitle)) currentTitle = omnigramUserTitle();" not in t:
        t = t.replace(
            "titleView.setText(currentTitle, animated && !LocaleController.isRTL);",
            "if (TextUtils.isEmpty(currentTitle)) currentTitle = omnigramUserTitle();\n"
            "            titleView.setText(currentTitle, animated && !LocaleController.isRTL);",
            1,
        )

    t = t.replace(
        "animatorHasTitleText.setValue(!TextUtils.isEmpty(currentTitle) || hasOverlayText, animated);",
        "animatorHasTitleText.setValue(true, animated); // Omnigram: always title, never logo",
    )

    # Force visibility method body logo path to GONE
    t = t.replace(
        "telegramLogoView.setAlpha(logoAlpha);\n            telegramLogoView.setVisibility(logoAlpha > 0 ? VISIBLE : GONE);",
        "telegramLogoView.setAlpha(0f);\n            telegramLogoView.setVisibility(GONE);",
    )
    t = t.replace(
        "telegramLogoView.setAlpha(0f);\n            telegramLogoView.setVisibility(GONE);",
        "telegramLogoView.setImageDrawable(null);\n            telegramLogoView.setAlpha(0f);\n            telegramLogoView.setVisibility(GONE);",
    )
    # dedupe if already had setImageDrawable
    while "telegramLogoView.setImageDrawable(null);\n            telegramLogoView.setImageDrawable(null);" in t:
        t = t.replace(
            "telegramLogoView.setImageDrawable(null);\n            telegramLogoView.setImageDrawable(null);",
            "telegramLogoView.setImageDrawable(null);",
        )

    p.write_text(t, encoding="utf-8")
    print("DialogStoriesCell forced")

    d = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java"
    if d.exists():
        dt = d.read_text(encoding="utf-8", errors="replace")
        dt2 = dt.replace(
            "logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo_2).mutate();",
            "logoDrawable = null; // Omnigram",
        )
        if dt2 != dt:
            d.write_text(dt2, encoding="utf-8")
            print("DialogsActivity logo null")


def force_no_dc():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java"
    t = p.read_text(encoding="utf-8", errors="replace")
    t = re.sub(
        r"int dc = 0;.*?detailCell\.setTextAndValue\(String\.valueOf\(idv\), sub, true\);",
        'detailCell.setTextAndValue(String.valueOf(idv), userId != 0 ? "User ID" : "Chat ID", true);',
        t,
        flags=re.S,
    )
    t = t.replace(
        "detailCell.setTextAndValue(String.valueOf(idv), sub, true);",
        'detailCell.setTextAndValue(String.valueOf(idv), userId != 0 ? "User ID" : "Chat ID", true);',
    )
    lines = []
    for line in t.splitlines(True):
        if "OmnigramRegDate.dcSubtitle" in line:
            continue
        if 'sub = (userId != 0 ? "User ID"' in line:
            continue
        lines.append(line)
    t = "".join(lines)
    p.write_text(t, encoding="utf-8")
    print("Profile DC scrubbed")


def force_calculator():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java"
    t = p.read_text(encoding="utf-8", errors="replace")

    if "omnigramMaybeEvalMath" in t:
        t = re.sub(
            r"/\*\* Omnigram: if user types.*?private double omnigramEvalExpr\(String expr\) \{.*?\n    \}\n",
            "",
            t,
            count=1,
            flags=re.S,
        )
        t = re.sub(
            r"private void omnigramMaybeEvalMath\(\) \{.*?\n    private double omnigramEvalExpr\(String expr\) \{.*?\n    \}\n",
            "",
            t,
            count=1,
            flags=re.S,
        )
        t = re.sub(
            r"private boolean omnigramCalcBusy;.*?private double omnigramEvalExpr\(String expr\) \{.*?\n    \}\n",
            "",
            t,
            count=1,
            flags=re.S,
        )

    helper = r'''
    private boolean omnigramCalcBusy;

    private void omnigramMaybeEvalMath() {
        if (omnigramCalcBusy || messageEditText == null) return;
        try {
            CharSequence cs = messageEditText.getText();
            if (cs == null) return;
            String s = cs.toString();
            int eq = s.lastIndexOf('=');
            if (eq <= 0) return;
            if (eq != s.length() - 1) return;
            String expr = s.substring(0, eq).trim();
            if (expr.length() == 0) return;
            for (int k = 0; k < expr.length(); k++) {
                char ch = expr.charAt(k);
                if (!(Character.isDigit(ch) || ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == 'x' || ch == 'X'
                        || ch == '(' || ch == ')' || ch == '.' || ch == ' ' || ch == '%')) {
                    return;
                }
            }
            String norm = expr.replace('x', '*').replace('X', '*');
            double val = omnigramEvalExpr(norm);
            if (Double.isNaN(val) || Double.isInfinite(val)) return;
            String ans;
            if (Math.abs(val - Math.rint(val)) < 1e-9) ans = String.valueOf((long) Math.rint(val));
            else {
                ans = String.format(java.util.Locale.US, "%.8f", val).replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            String next = expr + "=" + ans;
            if (next.equals(s)) return;
            omnigramCalcBusy = true;
            messageEditText.setText(next);
            messageEditText.setSelection(next.length());
        } catch (Throwable ignore) {
        } finally {
            omnigramCalcBusy = false;
        }
    }

    private double omnigramEvalExpr(String expr) {
        java.util.ArrayList<String> output = new java.util.ArrayList<>();
        java.util.ArrayDeque<Character> ops = new java.util.ArrayDeque<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(') { ops.push(c); i++; continue; }
            if (c == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') output.add(String.valueOf(ops.pop()));
                if (!ops.isEmpty()) ops.pop();
                i++; continue;
            }
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (c == '-' && (output.isEmpty() || (!ops.isEmpty() && ops.peek() == '('))) {
                    output.add("0");
                }
                while (!ops.isEmpty()) {
                    char top = ops.peek();
                    boolean pop = false;
                    if ((c == '+' || c == '-') && (top == '+' || top == '-' || top == '*' || top == '/')) pop = true;
                    if ((c == '*' || c == '/') && (top == '*' || top == '/')) pop = true;
                    if (!pop) break;
                    output.add(String.valueOf(ops.pop()));
                }
                ops.push(c); i++; continue;
            }
            if (Character.isDigit(c) || c == '.') {
                int j = i + 1;
                while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) j++;
                output.add(expr.substring(i, j));
                i = j; continue;
            }
            i++;
        }
        while (!ops.isEmpty()) output.add(String.valueOf(ops.pop()));
        java.util.ArrayDeque<Double> st = new java.util.ArrayDeque<>();
        for (String tok : output) {
            if (tok.length() == 1 && "+-*/".indexOf(tok.charAt(0)) >= 0) {
                if (st.size() < 2) return Double.NaN;
                double b = st.pop(), a = st.pop();
                switch (tok.charAt(0)) {
                    case '+': st.push(a + b); break;
                    case '-': st.push(a - b); break;
                    case '*': st.push(a * b); break;
                    case '/': st.push(b == 0 ? Double.NaN : a / b); break;
                }
            } else {
                try { st.push(Double.parseDouble(tok)); } catch (Exception e) { return Double.NaN; }
            }
        }
        return st.isEmpty() ? Double.NaN : st.pop();
    }

'''
    anchor = "    public void addTextChangedListener(TextWatcher textWatcher) {"
    if anchor not in t:
        print("calc anchor missing")
        return
    t = t.replace(anchor, helper + "\n" + anchor, 1)

    if "afterTextChanged(android.text.Editable s) { omnigramMaybeEvalMath(); }" not in t:
        inject_at = "messageEditText.addTextChangedListener(new EditTextSuggestionsFix());"
        if inject_at in t:
            t = t.replace(
                inject_at,
                inject_at
                + "\n        messageEditText.addTextChangedListener(new TextWatcher() {\n"
                + "            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}\n"
                + "            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}\n"
                + "            @Override public void afterTextChanged(android.text.Editable s) { omnigramMaybeEvalMath(); }\n"
                + "        });",
                1,
            )
    p.write_text(t, encoding="utf-8")
    print("Calculator forced")


def force_part2_menu():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java"
    t = p.read_text(encoding="utf-8", errors="replace")

    if "OPTION_OMNI_MSG_DETAILS" not in t:
        t = t.replace(
            "public final static int OPTION_COPY = 3;",
            "public final static int OPTION_COPY = 3;\n"
            "    public final static int OPTION_OMNI_TO_BEGINNING = 9101;\n"
            "    public final static int OPTION_OMNI_MSG_DETAILS = 9102;\n"
            "    public final static int OPTION_OMNI_VOICE_DL = 9103;",
            1,
        )

    needle = (
        "                if ((selectedObject.type == MessageObject.TYPE_TEXT || selectedObject.type == MessageObject.TYPE_ARTICLE || selectedObject.isDice() || selectedObject.isAnimatedEmoji() || selectedObject.isAnimatedEmojiStickers() || getMessageCaption(selectedObject, selectedObjectGroup) != null) && (!noforwardsOrPaidMedia || isEphemeral) && !selectedObject.sponsoredCanReport) {\n"
        "                    items.add(LocaleController.getString(R.string.Copy));\n"
        "                    options.add(OPTION_COPY);\n"
        "                    icons.add(R.drawable.msg_copy);\n"
        "                }"
    )
    inject = needle + (
        "\n                // Omnigram Part2\n"
        "                if (!selectedObject.isSponsored()) {\n"
        '                    items.add("Details");\n'
        "                    options.add(OPTION_OMNI_MSG_DETAILS);\n"
        "                    icons.add(R.drawable.msg_info);\n"
        "                }\n"
        "                if (selectedObject.isVoice() || selectedObject.isRoundVideo()) {\n"
        '                    items.add("Download voice");\n'
        "                    options.add(OPTION_OMNI_VOICE_DL);\n"
        "                    icons.add(R.drawable.msg_download);\n"
        "                }"
    )
    if "options.add(OPTION_OMNI_MSG_DETAILS)" not in t and needle in t:
        t = t.replace(needle, inject, 1)
        print("ChatActivity: menu items added")
    elif "options.add(OPTION_OMNI_MSG_DETAILS)" in t:
        print("ChatActivity: menu already wired")
    else:
        soft = "options.add(OPTION_COPY);\n                    icons.add(R.drawable.msg_copy);"
        if soft in t:
            t = t.replace(
                soft,
                soft
                + "\n                }\n                if (!selectedObject.isSponsored()) {\n"
                + '                    items.add("Details");\n'
                + "                    options.add(OPTION_OMNI_MSG_DETAILS);\n"
                + "                    icons.add(R.drawable.msg_info);",
                1,
            )
            print("ChatActivity: soft menu inject")

    if "option == OPTION_OMNI_MSG_DETAILS" not in t:
        m = re.search(r"(void processSelectedOption\(int option\)\s*\{)", t)
        if m:
            t = t.replace(
                m.group(1),
                m.group(1)
                + "\n"
                + "        if (option == OPTION_OMNI_TO_BEGINNING) {\n"
                + "            try {\n"
                + "                if (chatListView != null && messages != null && !messages.isEmpty()) {\n"
                + "                    scrollToMessageId(messages.get(messages.size()-1).getId(), 0, false, 0, true, 0);\n"
                + "                }\n"
                + "            } catch (Throwable ignore) {}\n"
                + "            return;\n"
                + "        }\n"
                + "        if (option == OPTION_OMNI_VOICE_DL) {\n"
                + "            try {\n"
                + "                if (selectedObject != null && (selectedObject.isVoice() || selectedObject.isRoundVideo()) && selectedObject.getDocument() != null) {\n"
                + "                    FileLoader.getInstance(currentAccount).loadFile(selectedObject.getDocument(), selectedObject, FileLoader.PRIORITY_HIGH, 0);\n"
                + "                    BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, \"Downloading voice...\").show();\n"
                + "                }\n"
                + "            } catch (Throwable ignore) {}\n"
                + "            return;\n"
                + "        }\n"
                + "        if (option == OPTION_OMNI_MSG_DETAILS) {\n"
                + "            try {\n"
                + "                if (selectedObject != null) {\n"
                + "                    StringBuilder sb = new StringBuilder();\n"
                + "                    sb.append(\"Message ID: \").append(selectedObject.getId()).append('\\n');\n"
                + "                    sb.append(\"Date: \").append(LocaleController.formatDateAudio(selectedObject.messageOwner.date, true)).append('\\n');\n"
                + "                    if (selectedObject.messageOwner.views > 0) sb.append(\"Views: \").append(selectedObject.messageOwner.views).append('\\n');\n"
                + "                    sb.append(\"From ID: \").append(selectedObject.getFromChatId()).append('\\n');\n"
                + "                    sb.append(\"Dialog ID: \").append(dialog_id);\n"
                + "                    new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity())\n"
                + "                        .setTitle(\"Message Details\")\n"
                + "                        .setMessage(sb.toString())\n"
                + "                        .setPositiveButton(\"OK\", null).show();\n"
                + "                }\n"
                + "            } catch (Throwable ignore) {}\n"
                + "            return;\n"
                + "        }\n",
                1,
            )
            print("ChatActivity: handlers")

    if 'return "Details"' not in t and "case OPTION_COPY:" in t:
        t = t.replace(
            "case OPTION_COPY:",
            'case OPTION_OMNI_MSG_DETAILS:\n                return "Details";\n'
            '            case OPTION_OMNI_VOICE_DL:\n                return "Download voice";\n'
            '            case OPTION_OMNI_TO_BEGINNING:\n                return "To Beginning";\n'
            "            case OPTION_COPY:",
            1,
        )

    p.write_text(t, encoding="utf-8")
    print("ChatActivity done")


def main():
    force_title()
    force_no_dc()
    force_calculator()
    force_part2_menu()
    print("ALL FORCED")


if __name__ == "__main__":
    main()
