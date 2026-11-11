#!/usr/bin/env python3
from pathlib import Path
import re
ROOT = Path(".")

def fix_calculator():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java"
    t = p.read_text(encoding="utf-8", errors="replace")
    m = re.search(r"private void omnigramMaybeEvalMath\(\) \{.*?\n    private double omnigramEvalExpr", t, re.S)
    if not m:
        print("calc missing"); return
    new = r'''private void omnigramMaybeEvalMath() {
        if (omnigramCalcBusy || messageEditText == null) return;
        try {
            CharSequence cs = messageEditText.getText();
            if (cs == null) return;
            String s = cs.toString();
            if (s.isEmpty() || !s.endsWith("=")) return;
            String expr = s.substring(0, s.length() - 1).trim();
            if (expr.isEmpty()) return;
            for (int k = 0; k < expr.length(); k++) {
                char ch = expr.charAt(k);
                if (!(Character.isDigit(ch) || ch=='+'||ch=='-'||ch=='*'||ch=='/'||ch=='x'||ch=='X'||ch=='('||ch==')'||ch=='.'||ch==' '||ch=='%')) return;
            }
            double val = omnigramEvalExpr(expr.replace('x','*').replace('X','*'));
            if (Double.isNaN(val) || Double.isInfinite(val)) return;
            String ans = (Math.abs(val - Math.rint(val)) < 1e-9) ? String.valueOf((long)Math.rint(val)) : String.format(java.util.Locale.US, "%.8f", val).replaceAll("0+$","").replaceAll("\\.$","");
            String next = expr + "=" + ans;
            if (next.equals(s)) return;
            omnigramCalcBusy = true;
            messageEditText.setText(next);
            messageEditText.setSelection(next.length());
        } catch (Throwable ignore) {
        } finally { omnigramCalcBusy = false; }
    }

    private double omnigramEvalExpr'''
    p.write_text(t[:m.start()] + new + t[m.end():], encoding="utf-8")
    print("calc ok")

def fix_verified():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/OmnigramVerified.java"
    p.write_text('''package org.telegram.ui.Components;
public final class OmnigramVerified {
    private static final long KEY = 0x1A2B3C4D5E6F7081L;
    private static final long[] ENCODED_USERS = { 8077714480L ^ KEY, 5938491424L ^ KEY };
    private static final String[] VERIFIED_USERNAMES = { "krsxh", "zestyji", "officalomnigram", "officialomnigram", "omnigramchat" };
    private OmnigramVerified() {}
    public static boolean isVerified(long id) {
        if (id == 0) return false;
        long abs = id < 0 ? -id : id;
        if (abs > 1_000_000_000_000L) {
            String s = Long.toString(abs);
            if (s.startsWith("100")) { try { abs = Long.parseLong(s.substring(3)); } catch (Exception ignore) {} }
        }
        for (long e : ENCODED_USERS) if ((e ^ KEY) == abs || (e ^ KEY) == id) return true;
        return false;
    }
    public static boolean isVerifiedUsername(String username) {
        if (username == null) return false;
        String u = username.toLowerCase(java.util.Locale.US);
        if (u.startsWith("@")) u = u.substring(1);
        for (String v : VERIFIED_USERNAMES) if (v.equals(u)) return true;
        return false;
    }
}
''', encoding="utf-8")
    dc = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java"
    if dc.exists():
        t = dc.read_text(encoding="utf-8", errors="replace")
        t = t.replace(
            "drawVerified = !forbidVerified && (user.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(user.id));",
            "drawVerified = !forbidVerified && (user.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(user.id) || org.telegram.ui.Components.OmnigramVerified.isVerifiedUsername(user.username));")
        t = t.replace(
            "drawVerified = !forbidVerified && (chat.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(chat.id));",
            "drawVerified = !forbidVerified && (chat.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(chat.id) || org.telegram.ui.Components.OmnigramVerified.isVerifiedUsername(chat.username));")
        dc.write_text(t, encoding="utf-8")
    print("verified ok")

def pin_official():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java"
    t = p.read_text(encoding="utf-8", errors="replace")
    if "omnigramPinOfficial" in t:
        print("pin skip"); return
    if "public void sortDialogsList(ArrayList<TLRPC.Dialog> dialogs)" not in t:
        print("pin missing"); return
    helper = '''
    private void omnigramPinOfficial(ArrayList<TLRPC.Dialog> dialogs) {
        if (dialogs == null || dialogs.isEmpty()) return;
        try {
            int found = -1;
            for (int i = 0; i < dialogs.size(); i++) {
                TLRPC.Dialog d = dialogs.get(i);
                if (d == null) continue;
                TLRPC.Chat chat = getChat(d.id < 0 ? -d.id : d.id);
                String un = chat != null ? chat.username : null;
                if (un == null && chat != null && chat.usernames != null) {
                    for (int u = 0; u < chat.usernames.size(); u++) {
                        if (chat.usernames.get(u) != null && chat.usernames.get(u).active) { un = chat.usernames.get(u).username; break; }
                    }
                }
                if (un != null) {
                    String low = un.toLowerCase();
                    if (low.equals("officalomnigram") || low.equals("officialomnigram") || low.equals("omnigramchat")) { found = i; break; }
                }
            }
            if (found > 0) { TLRPC.Dialog d = dialogs.remove(found); dialogs.add(0, d); }
        } catch (Throwable ignore) {}
    }

'''
    t = t.replace("public void sortDialogsList(ArrayList<TLRPC.Dialog> dialogs) {",
                  helper + "    public void sortDialogsList(ArrayList<TLRPC.Dialog> dialogs) {\n        omnigramPinOfficial(dialogs);", 1)
    p.write_text(t, encoding="utf-8")
    print("pin ok")

def deleted_ui():
    (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/OmnigramDeleted.java").write_text(
        '''package org.telegram.ui.Components;
import java.util.HashSet;
import java.util.Set;
public final class OmnigramDeleted {
    private static final Set<String> KEYS = new HashSet<>();
    private OmnigramDeleted() {}
    private static String key(long d, int m) { return d + ":" + m; }
    public static void mark(long d, int m) { KEYS.add(key(d, m)); }
    public static boolean isDeleted(long d, int m) { return KEYS.contains(key(d, m)); }
    public static void markMany(long d, java.util.ArrayList<Integer> ids) {
        if (ids == null) return;
        for (int i = 0; i < ids.size(); i++) mark(d, ids.get(i));
    }
}
''', encoding="utf-8")
    cmc = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java"
    t = cmc.read_text(encoding="utf-8", errors="replace")
    if "OmnigramDeleted.isDeleted" not in t:
        t = t.replace(
            'return "\\u270E " + LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);',
            'String omniT = LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);\n'
            '            try { if (message != null && org.telegram.ui.Components.OmnigramDeleted.isDeleted(message.getDialogId(), message.getId())) return "\\uD83D\\uDDD1 " + omniT; } catch (Throwable ignore) {}\n'
            '            return "\\u270E " + omniT;', 1)
        if "protected void onDraw(Canvas canvas)" in t:
            t = t.replace("protected void onDraw(Canvas canvas) {",
                "protected void onDraw(Canvas canvas) {\n"
                "        try { if (currentMessageObject != null && org.telegram.ui.Components.OmnigramDeleted.isDeleted(currentMessageObject.getDialogId(), currentMessageObject.getId())) setAlpha(0.70f); } catch (Throwable ignore) {}\n", 1)
        cmc.write_text(t, encoding="utf-8")
    mc = ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java"
    mt = mc.read_text(encoding="utf-8", errors="replace")
    if "OmnigramDeleted.markMany" not in mt:
        n = "if (update instanceof TLRPC.TL_updateDeleteMessages)"
        if n in mt:
            i = mt.find(n); b = mt.find("{", i)
            mt = mt[:b+1] + "\n                    try { TLRPC.TL_updateDeleteMessages ud = (TLRPC.TL_updateDeleteMessages) update; org.telegram.ui.Components.OmnigramDeleted.markMany(0, new java.util.ArrayList<>(ud.messages)); } catch (Throwable ignore) {}\n" + mt[b+1:]
            mc.write_text(mt, encoding="utf-8")
    print("deleted ok")

def android_paths():
    p = ROOT / ".github/workflows/android.yml"
    t = p.read_text(encoding="utf-8", errors="replace")
    if "TMessagesProj/**" in t: return
    t = t.replace("on:\n  push:\n    branches:\n      - master\n  workflow_dispatch:",
                  "on:\n  push:\n    branches:\n      - master\n    paths:\n      - 'TMessagesProj/**'\n      - 'TMessagesProj_App/**'\n      - 'build.gradle'\n      - 'settings.gradle'\n      - 'gradle/**'\n  workflow_dispatch:", 1)
    p.write_text(t, encoding="utf-8")
    print("android paths ok")

def main():
    fix_calculator(); fix_verified(); pin_official(); deleted_ui(); android_paths()
    print("ALL DONE")
if __name__ == "__main__":
    main()
