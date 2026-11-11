#!/usr/bin/env python3
"""Fix compile errors blocking Omnigram Android Build."""
from pathlib import Path

ROOT = Path(".")


def fix_msg_up():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java"
    t = p.read_text(encoding="utf-8", errors="replace")
    # R.drawable.msg_up does not exist in resources
    t2 = t.replace("R.drawable.msg_up", "R.drawable.msg_rate_up")
    t2 = t2.replace("R.drawable.msg_go_up", "R.drawable.msg_rate_up")
    if t2 != t:
        p.write_text(t2, encoding="utf-8")
        print("ChatActivity: msg_up -> msg_rate_up")
    else:
        print("ChatActivity: no msg_up")


def fix_settings_override():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java"
    t = p.read_text(encoding="utf-8", errors="replace")
    # Broken: @Override on private omnigramRefreshAvatar
    bad = '''    @Override
    private void omnigramRefreshAvatar() {'''
    good = '''    private void omnigramRefreshAvatar() {'''
    if bad in t:
        t = t.replace(bad, good, 1)
        print("SettingsActivity: removed bad @Override")
    # Ensure didReceivedNotification has @Override if implementing delegate
    if "public void didReceivedNotification(int id, int account, Object... args)" in t:
        if "@Override\n    public void didReceivedNotification" not in t and "@Override\r\n    public void didReceivedNotification" not in t:
            t = t.replace(
                "    public void didReceivedNotification(int id, int account, Object... args) {",
                "    @Override\n    public void didReceivedNotification(int id, int account, Object... args) {",
                1,
            )
            print("SettingsActivity: @Override on didReceivedNotification")
    p.write_text(t, encoding="utf-8")


def fix_premium_icons():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/Premium/PremiumAppIconsPreviewView.java"
    t = p.read_text(encoding="utf-8", errors="replace")
    old = '''        for (LauncherIconController.LauncherIcon icon : LauncherIconController.LauncherIcon.values()) {
            if (icon.premium) {
                icons.add(icon);
            }
            if (icons.size() == 3) {
                break;
            }
        }

        if (icons.size() < 3) {
            FileLog.e(new IllegalArgumentException("There should be at least 3 premium icons!"));
            isEmpty = true;
            return;
        }'''
    new = '''        for (LauncherIconController.LauncherIcon icon : LauncherIconController.LauncherIcon.values()) {
            icons.add(icon);
            if (icons.size() == 3) {
                break;
            }
        }

        if (icons.size() < 3) {
            FileLog.e(new IllegalArgumentException("There should be at least 3 icons!"));
            isEmpty = true;
            return;
        }'''
    if old in t:
        t = t.replace(old, new, 1)
        p.write_text(t, encoding="utf-8")
        print("PremiumAppIconsPreviewView: no premium field")
    elif "icon.premium" in t:
        t = t.replace("if (icon.premium) {\n                icons.add(icon);\n            }", "icons.add(icon);")
        p.write_text(t, encoding="utf-8")
        print("PremiumAppIconsPreviewView: soft fix")
    else:
        print("PremiumAppIconsPreviewView: already ok")

    # Any other icon.premium references in project java under TMessagesProj
    for path in (ROOT / "TMessagesProj/src/main/java").rglob("*.java"):
        txt = path.read_text(encoding="utf-8", errors="replace")
        if "icon.premium" in txt or ".premium)" in txt and "LauncherIcon" in txt:
            # only fix LauncherIcon.premium usages carefully
            if "LauncherIcon" in txt and "icon.premium" in txt:
                nt = txt.replace("icon.premium", "false /*premium removed*/")
                if nt != txt:
                    path.write_text(nt, encoding="utf-8")
                    print("also fixed", path)


def main():
    fix_msg_up()
    fix_settings_override()
    fix_premium_icons()
    print("COMPILE FIXES DONE")


if __name__ == "__main__":
    main()
