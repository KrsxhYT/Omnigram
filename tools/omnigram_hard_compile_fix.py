#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(".")

def main():
    # 1) ChatActivity drawable
    ca = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java"
    t = ca.read_text(encoding="utf-8", errors="replace")
    # Only replace the To Beginning line, not other msg_rate_up uses if any are valid
    t = t.replace(
        'headerItem.lazilyAddSubItem(OPTION_OMNI_TO_BEGINNING, R.drawable.msg_rate_up, "To Beginning")',
        'headerItem.lazilyAddSubItem(OPTION_OMNI_TO_BEGINNING, R.drawable.msg_arrow_back, "To Beginning")',
    )
    t = t.replace(
        'headerItem.lazilyAddSubItem(OPTION_OMNI_TO_BEGINNING, R.drawable.msg_up, "To Beginning")',
        'headerItem.lazilyAddSubItem(OPTION_OMNI_TO_BEGINNING, R.drawable.msg_arrow_back, "To Beginning")',
    )
    ca.write_text(t, encoding="utf-8")
    print("ChatActivity drawable ok")

    # 2) SettingsActivity - remove @Override from private method if any left
    sa = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java"
    st = sa.read_text(encoding="utf-8", errors="replace")
    st = st.replace(
        "    @Override\n    private void omnigramRefreshAvatar()",
        "    private void omnigramRefreshAvatar()",
    )
    # if omnigramRefreshAvatar somehow still broken, rewrite clean block
    if "private void omnigramRefreshAvatar" not in st:
        pass
    sa.write_text(st, encoding="utf-8")
    print("Settings ok")

    # 3) PremiumAppIconsPreviewView - no icon.premium
    pv = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/Premium/PremiumAppIconsPreviewView.java"
    pt = pv.read_text(encoding="utf-8", errors="replace")
    if "icon.premium" in pt:
        pt = re.sub(
            r"for \(LauncherIconController\.LauncherIcon icon : LauncherIconController\.LauncherIcon\.values\(\)\) \{.*?if \(icons\.size\(\) == 3\) \{.*?\}.*?\}",
            "for (LauncherIconController.LauncherIcon icon : LauncherIconController.LauncherIcon.values()) {\n"
            "            icons.add(icon);\n"
            "            if (icons.size() == 3) {\n"
            "                break;\n"
            "            }\n"
            "        }",
            pt,
            count=1,
            flags=re.S,
        )
        pt = pt.replace("icon.premium", "true")  # leftover safety
        pv.write_text(pt, encoding="utf-8")
        print("Premium icons fixed")
    else:
        print("Premium icons already ok")

    # 4) Any remaining icon.premium in AppIconsSelectorCell
    for path in (ROOT / "TMessagesProj").rglob("*AppIcon*.java"):
        txt = path.read_text(encoding="utf-8", errors="replace")
        if "icon.premium" in txt or ".premium" in txt:
            # only LauncherIcon premium field
            nt = re.sub(r"\bicon\.premium\b", "false", txt)
            nt = re.sub(r"\b(?<!\.)premium\s*\?", "false ?", nt)
            if nt != txt:
                path.write_text(nt, encoding="utf-8")
                print("fixed", path)

    print("DONE")

if __name__ == "__main__":
    main()
