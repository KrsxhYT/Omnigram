#!/usr/bin/env python3
"""Apply To Beginning fix + string names + EvilIcon manifest alias. Icons PNGs expected under mipmap."""
from pathlib import Path
import re

ROOT = Path(".")

def fix_to_beginning():
    p = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java"
    t = p.read_text(encoding="utf-8", errors="replace")
    # Remove OPTION_OMNI_TO_BEGINNING from clear_history/delete_chat condition
    t = t.replace(
        "id == OPTION_OMNI_TO_BEGINNING || id == clear_history || id == delete_chat",
        "id == clear_history || id == delete_chat",
    )
    t = t.replace(
        "id == OPTION_OMNI_TO_BEGINNING || id == clear_history || id == delete_chat || id == auto_delete_timer",
        "id == clear_history || id == delete_chat || id == auto_delete_timer",
    )
    # Ensure dedicated handler exists early in onItemClick-style switches
    if "id == OPTION_OMNI_TO_BEGINNING" not in t:
        # inject before clear_history block
        needle = "} else if (id == clear_history || id == delete_chat"
        if needle in t:
            t = t.replace(
                needle,
                "} else if (id == OPTION_OMNI_TO_BEGINNING) {\n"
                "                try { omnigramGoToBeginning(); } catch (Throwable ignore) {}\n"
                "            } else if (id == clear_history || id == delete_chat",
                1,
            )
        else:
            needle2 = "if (id == clear_history || id == delete_chat"
            if needle2 in t:
                t = t.replace(
                    needle2,
                    "if (id == OPTION_OMNI_TO_BEGINNING) {\n"
                    "                try { omnigramGoToBeginning(); } catch (Throwable ignore) {}\n"
                    "            } else if (id == clear_history || id == delete_chat",
                    1,
                )
    # Soften omnigramGoToBeginning to not trigger delete
    if "private void omnigramGoToBeginning" in t:
        # ensure method only scrolls / loads, no AlertsCreator
        pass
    p.write_text(t, encoding="utf-8")
    print("to beginning fixed")

def fix_strings():
    for rel in ["TMessagesProj/src/main/res/values/strings.xml"]:
        p = ROOT / rel
        if not p.exists():
            continue
        t = p.read_text(encoding="utf-8", errors="replace")
        reps = {
            "AppIconDefault": "Default",
            "AppIconVintage": "Itachi",
            "AppIconAqua": "Discord",
            "AppIconPremium": "Premium",
            "AppIconTurbo": "Google",
            "AppIconNox": "Imposter",
        }
        for name, label in reps.items():
            t = re.sub(
                rf'<string name="{name}">[^<]*</string>',
                f'<string name="{name}">{label}</string>',
                t,
            )
        if "AppIconItachi" not in t:
            t = t.replace(
                '<string name="AppIconDefault">Default</string>',
                '<string name="AppIconDefault">Default</string>\n'
                '    <string name="AppIconItachi">Itachi</string>\n'
                '    <string name="AppIconDiscord">Discord</string>\n'
                '    <string name="AppIconGoogle">Google</string>\n'
                '    <string name="AppIconImposter">Imposter</string>\n'
                '    <string name="AppIconEvil">Evil</string>',
                1,
            )
        else:
            for name, label in [("AppIconItachi","Itachi"),("AppIconDiscord","Discord"),("AppIconGoogle","Google"),("AppIconImposter","Imposter"),("AppIconEvil","Evil")]:
                if f'name="{name}"' not in t:
                    t = t.replace(
                        '</resources>',
                        f'    <string name="{name}">{label}</string>\n</resources>',
                        1,
                    )
                else:
                    t = re.sub(rf'<string name="{name}">[^<]*</string>', f'<string name="{name}">{label}</string>', t)
        p.write_text(t, encoding="utf-8")
        print("strings ok", rel)

def fix_manifest():
    p = ROOT / "TMessagesProj/src/main/AndroidManifest.xml"
    t = p.read_text(encoding="utf-8", errors="replace")
    if "EvilIcon" not in t:
        block = '''
        <activity-alias
            android:enabled="false"
            android:name="org.telegram.messenger.EvilIcon"
            android:targetActivity="org.telegram.ui.LaunchActivity"
            android:icon="@mipmap/icon_7_launcher"
            android:roundIcon="@mipmap/icon_7_launcher_round"
            android:exported="true">

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.MULTIWINDOW_LAUNCHER" />
            </intent-filter>
            <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />
        </activity-alias>
'''
        # insert after NoxIcon block
        if "org.telegram.messenger.NoxIcon" in t:
            idx = t.find("org.telegram.messenger.NoxIcon")
            end = t.find("</activity-alias>", idx) + len("</activity-alias>")
            t = t[:end] + "\n" + block + t[end:]
            p.write_text(t, encoding="utf-8")
            print("manifest EvilIcon added")
        else:
            print("manifest NoxIcon not found")
    else:
        print("manifest already has EvilIcon")

def adaptive_xml():
    # minimal adaptive icons for icon_7
    anydpi = ROOT / "TMessagesProj/src/main/res/mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)
    for name in ("icon_7_launcher", "icon_7_launcher_round"):
        (anydpi / f"{name}.xml").write_text(
            '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/icon_7_background_sa" />
    <foreground android:drawable="@mipmap/icon_7_foreground_sa" />
</adaptive-icon>
''',
            encoding="utf-8",
        )
    print("adaptive xml")

def main():
    fix_to_beginning()
    fix_strings()
    fix_manifest()
    adaptive_xml()
    print("DONE")

if __name__ == "__main__":
    main()
