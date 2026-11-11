#!/usr/bin/env python3
"""Remove invalid return-string cases injected into void switch in ChatActivity."""
from pathlib import Path

p = Path("TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java")
t = p.read_text(encoding="utf-8", errors="replace")

bad = '''            case OPTION_OMNI_MSG_DETAILS:
                return "Details";
            case OPTION_OMNI_VOICE_DL:
                return "Download voice";
            case OPTION_OMNI_TO_BEGINNING:
                return "To Beginning";
            case OPTION_COPY:'''

if bad in t:
    t = t.replace(bad, "            case OPTION_COPY:", 1)
    print("Removed bad return cases before OPTION_COPY")
else:
    # try escaped / alternate spacing
    import re
    t2, n = re.subn(
        r"\s*case OPTION_OMNI_MSG_DETAILS:\s*return \"Details\";\s*"
        r"case OPTION_OMNI_VOICE_DL:\s*return \"Download voice\";\s*"
        r"case OPTION_OMNI_TO_BEGINNING:\s*return \"To Beginning\";\s*"
        r"case OPTION_COPY:",
        "            case OPTION_COPY:",
        t,
        count=1,
    )
    if n:
        t = t2
        print("Removed via regex")
    else:
        print("Pattern not found - checking")
        if "return \"Details\"" in t:
            print("still has return Details")
        else:
            print("no return Details left")

p.write_text(t, encoding="utf-8")
print("done")
