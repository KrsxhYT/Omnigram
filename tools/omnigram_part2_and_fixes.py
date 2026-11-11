#!/usr/bin/env python3
"""Omnigram: title always user name, remove DC, inline calc, part2 chat extras."""
from pathlib import Path
import re

ROOT = Path(".")


def fix_stories_title():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Stories/DialogStoriesCell.java"
    if not p.exists():
        print("MISSING DialogStoriesCell")
        return
    t = p.read_text(encoding="utf-8", errors="replace")

    old_vis = """    private void checkUi_titleVisibility() {
        final float progress = MathUtils.clamp(Math.min(collapsedProgress, collapsedProgress2), 0, 1);
        final float titleVisibility = animatorHasTitleText.getFloatValue();
        final float logoVisibility = 1f - titleVisibility;
        final float titleAlpha = titleVisibility * progress;
        final float logoAlpha = logoVisibility * progress;

        if (titleView != null) {
            titleView.setAlpha(titleAlpha);
            titleView.setVisibility(titleAlpha > 0 ? VISIBLE : GONE);
        }
        if (telegramLogoView != null) {
            telegramLogoView.setAlpha(logoAlpha);
            telegramLogoView.setVisibility(logoAlpha > 0 ? VISIBLE : GONE);
        }
        if (emojiStatusView != null) {
            emojiStatusView.setAlpha(logoAlpha);
            emojiStatusView.setVisibility(logoAlpha > 0 ? VISIBLE : GONE);
        }
        if (subtitleOverlayContainer != null) {
            subtitleOverlayContainer.setAlpha(progress);
            subtitleOverlayContainer.setVisibility(progress > 0 ? VISIBLE : GONE);
        }
    }"""
    new_vis = """    private void checkUi_titleVisibility() {
        final float progress = MathUtils.clamp(Math.min(collapsedProgress, collapsedProgress2), 0, 1);
        // Omnigram: never show Telegram word-logo; always show account name as title
        final float titleAlpha = progress;
        final float logoAlpha = 0f;

        if (titleView != null) {
            titleView.setAlpha(titleAlpha);
            titleView.setVisibility(titleAlpha > 0 ? VISIBLE : GONE);
        }
        if (telegramLogoView != null) {
            telegramLogoView.setAlpha(0f);
            telegramLogoView.setVisibility(GONE);
        }
        if (emojiStatusView != null) {
            emojiStatusView.setAlpha(titleAlpha);
            emojiStatusView.setVisibility(titleAlpha > 0 ? VISIBLE : GONE);
        }
        if (subtitleOverlayContainer != null) {
            subtitleOverlayContainer.setAlpha(progress);
            subtitleOverlayContainer.setVisibility(progress > 0 ? VISIBLE : GONE);
        }
    }"""
    if old_vis in t:
        t = t.replace(old_vis, new_vis, 1)
        print("DialogStoriesCell: title visibility fixed")
    elif "telegramLogoView.setAlpha(logoAlpha)" in t:
        t = t.replace(
            "telegramLogoView.setAlpha(logoAlpha);\n            telegramLogoView.setVisibility(logoAlpha > 0 ? VISIBLE : GONE);",
            "telegramLogoView.setAlpha(0f);\n            telegramLogoView.setVisibility(GONE);",
            1,
        )
        print("DialogStoriesCell: soft logo hide")

    helper = """
    private CharSequence omnigramUserTitle() {
        try {
            org.telegram.tgnet.TLRPC.User me = org.telegram.messenger.UserConfig.getInstance(currentAccount).getCurrentUser();
            if (me != null) {
                String n = org.telegram.messenger.UserObject.getUserName(me);
                if (n != null && n.length() > 0) return n;
            }
        } catch (Throwable ignore) {}
        return org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.AppName);
    }
"""
    if "omnigramUserTitle" not in t:
        t = t.replace(
            "    private void checkUi_titleVisibility() {",
            helper + "\n    private void checkUi_titleVisibility() {",
            1,
        )
        t = t.replace(
            "currentTitle = menuItemsOffset < dp(50) ? null :\n                    LocaleController.getString(R.string.MyStory);",
            "currentTitle = menuItemsOffset < dp(50) ? omnigramUserTitle() :\n                    LocaleController.getString(R.string.MyStory);",
        )
        t = t.replace(
            "currentTitle = menuItemsOffset < dp(50) ? null :\n                LocaleController.formatPluralString(\"Stories\", totalCount);",
            "currentTitle = menuItemsOffset < dp(50) ? omnigramUserTitle() :\n                LocaleController.formatPluralString(\"Stories\", totalCount);",
        )
        t = t.replace(
            "titleView.setText(currentTitle, animated && !LocaleController.isRTL);",
            "if (TextUtils.isEmpty(currentTitle)) currentTitle = omnigramUserTitle();\n            titleView.setText(currentTitle, animated && !LocaleController.isRTL);",
            1,
        )
        print("DialogStoriesCell: user name as default title")

    p.write_text(t, encoding="utf-8")


def fix_profile_dc():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java"
    if not p.exists():
        return
    t = p.read_text(encoding="utf-8", errors="replace")
    if "OmnigramRegDate.dcSubtitle" in t or ", sub, true)" in t:
        t = t.replace(
            'String sub = org.telegram.ui.Components.OmnigramRegDate.dcSubtitle(dc);\n                        if (sub == null || sub.length() == 0) sub = (userId != 0 ? "User ID" : "Chat ID");\n                        detailCell.setTextAndValue(String.valueOf(idv), sub, true);',
            'detailCell.setTextAndValue(String.valueOf(idv), userId != 0 ? "User ID" : "Chat ID", true);',
        )
        lines = []
        for line in t.splitlines(True):
            if "OmnigramRegDate.dcSubtitle" in line:
                continue
            if 'sub = (userId != 0 ? "User ID"' in line:
                continue
            if "detailCell.setTextAndValue(String.valueOf(idv), sub, true)" in line:
                lines.append(line.replace(", sub,", ', userId != 0 ? "User ID" : "Chat ID",'))
            else:
                lines.append(line)
        t = "".join(lines)
        print("ProfileActivity: removed DC subtitle")
    p.write_text(t, encoding="utf-8")


def add_calculator():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java"
    if not p.exists():
        print("MISSING ChatActivityEnterView")
        return
    t = p.read_text(encoding="utf-8", errors="replace")
    if "omnigramMaybeEvalMath" in t:
        print("Calculator already present")
        return

    helper = r'''
    /** Omnigram: if user types e.g. 7+8= show answer after = */
    private void omnigramMaybeEvalMath() {
        try {
            if (messageEditText == null) return;
            CharSequence cs = messageEditText.getText();
            if (cs == null) return;
            String s = cs.toString();
            int eq = s.lastIndexOf('=');
            if (eq < 0 || eq != s.length() - 1) return;
            String expr = s.substring(0, eq).trim();
            if (expr.isEmpty()) return;
            if (!expr.matches("[0-9+\\-*/().\\s]+")) return;
            double val = omnigramEvalExpr(expr);
            if (Double.isNaN(val) || Double.isInfinite(val)) return;
            String ans;
            if (Math.abs(val - Math.rint(val)) < 1e-9) {
                ans = String.valueOf((long) Math.rint(val));
            } else {
                ans = String.valueOf(val);
            }
            String next = expr + "=" + ans;
            if (next.equals(s)) return;
            messageEditText.setText(next);
            messageEditText.setSelection(next.length());
        } catch (Throwable ignore) {}
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
                if (!ops.isEmpty() && ops.peek() == '(') ops.pop();
                i++; continue;
            }
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (c == '-' && (output.isEmpty() || (!ops.isEmpty() && (ops.peek() == '(')))) {
                    output.add("0");
                }
                while (!ops.isEmpty()) {
                    char top = ops.peek();
                    if ((c == '+' || c == '-') && (top == '+' || top == '-' || top == '*' || top == '/')) {
                        output.add(String.valueOf(ops.pop()));
                    } else if ((c == '*' || c == '/') && (top == '*' || top == '/')) {
                        output.add(String.valueOf(ops.pop()));
                    } else break;
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
                st.push(Double.parseDouble(tok));
            }
        }
        return st.isEmpty() ? Double.NaN : st.pop();
    }

'''
    anchor = "    public void addTextChangedListener(TextWatcher textWatcher) {"
    if anchor in t:
        t = t.replace(anchor, helper + "\n" + anchor, 1)
    else:
        print("Calculator: helper anchor missing")
        return

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
        print("ChatActivityEnterView: calculator added")
    p.write_text(t, encoding="utf-8")


def part2_chat_activity():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java"
    if not p.exists():
        print("MISSING ChatActivity")
        return
    t = p.read_text(encoding="utf-8", errors="replace")
    if "OPTION_OMNI_TO_BEGINNING" not in t:
        t = t.replace(
            "public final static int OPTION_REPLY = 8;",
            "public final static int OPTION_REPLY = 8;\n"
            "    public final static int OPTION_OMNI_TO_BEGINNING = 9101;\n"
            "    public final static int OPTION_OMNI_MSG_DETAILS = 9102;\n"
            "    public final static int OPTION_OMNI_VOICE_DL = 9103;",
            1,
        )
        print("ChatActivity: constants")

    if "OPTION_OMNI_MSG_DETAILS" in t and "option == OPTION_OMNI_MSG_DETAILS" not in t:
        m = re.search(r"(void processSelectedOption\(int option\)\s*\{)", t)
        if m:
            insert = (
                m.group(1)
                + "\n        if (option == OPTION_OMNI_TO_BEGINNING) {\n"
                + "            try {\n"
                + "                if (chatListView != null && messages != null && !messages.isEmpty()) {\n"
                + "                    chatListView.scrollToPosition(messages.size() - 1);\n"
                + "                }\n"
                + "            } catch (Throwable ignore) {}\n"
                + "            return;\n"
                + "        }\n"
                + "        if (option == OPTION_OMNI_VOICE_DL) {\n"
                + "            try {\n"
                + "                if (selectedObject != null && selectedObject.isVoice()) {\n"
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
                + "                    sb.append(\"Message ID: \").append(selectedObject.getId()).append(\"\\n\");\n"
                + "                    sb.append(\"Date: \").append(LocaleController.formatDateAudio(selectedObject.messageOwner.date, true)).append(\"\\n\");\n"
                + "                    if (selectedObject.messageOwner.views > 0) sb.append(\"Views: \").append(selectedObject.messageOwner.views).append(\"\\n\");\n"
                + "                    sb.append(\"From ID: \").append(selectedObject.getFromChatId()).append(\"\\n\");\n"
                + "                    sb.append(\"Dialog ID: \").append(dialog_id);\n"
                + "                    new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity())\n"
                + "                        .setTitle(\"Message Details\")\n"
                + "                        .setMessage(sb.toString())\n"
                + "                        .setPositiveButton(\"OK\", null)\n"
                + "                        .show();\n"
                + "                }\n"
                + "            } catch (Throwable ignore) {}\n"
                + "            return;\n"
                + "        }\n"
            )
            t = t.replace(m.group(1), insert, 1)
            print("ChatActivity: handlers")

        if "items.add(OPTION_COPY" in t and "OPTION_OMNI_MSG_DETAILS" not in t[t.find("items.add(OPTION_COPY"): t.find("items.add(OPTION_COPY") + 200]:
            t = t.replace("items.add(OPTION_COPY", "items.add(OPTION_OMNI_MSG_DETAILS);\n                    items.add(OPTION_COPY", 1)
            print("ChatActivity: details menu item")

        if "case OPTION_COPY:" in t and "case OPTION_OMNI_MSG_DETAILS:" not in t:
            t = t.replace(
                "case OPTION_COPY:",
                "case OPTION_OMNI_MSG_DETAILS:\n"
                "                return \"Details\";\n"
                "            case OPTION_OMNI_VOICE_DL:\n"
                "                return \"Download voice\";\n"
                "            case OPTION_OMNI_TO_BEGINNING:\n"
                "                return \"To Beginning\";\n"
                "            case OPTION_COPY:",
                1,
            )
            print("ChatActivity: labels")

    p.write_text(t, encoding="utf-8")


def main():
    fix_stories_title()
    fix_profile_dc()
    add_calculator()
    part2_chat_activity()
    print("DONE")


if __name__ == "__main__":
    main()
