#!/usr/bin/env python3
"""Quick UI/branding fixes for Omnigram."""
from pathlib import Path
import re


def patch_cmc(t: str) -> str:
    old = """        } else if (edited) {
            if (AppGlobalConfig.getInstance(current.currentAccount).messagePrimaryEditedDate.get()) {
                if (editDate == 0 && current.isEditing()) {
                    editDate = ConnectionsManager.getInstance(current.currentAccount).getCurrentTime();
                }
                return LocaleController.formatPmEditedDate(editDate);
            }
            return \"\u270E \" + LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);
        }"""
    # handle both escaped variants
    if "formatPmEditedDate" in t:
        t = t.replace(
            "return LocaleController.formatPmEditedDate(editDate);",
            'return "\\u270E " + LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);',
        )
        print("CMC: formatPmEditedDate -> pencil")
    t = t.replace('getString(R.string.EditedMessage) + " "', '"\\u270E "')
    t = t.replace('getString(R.string.EditedMessage) + " "', '"\\u270E "')
    return t


def patch_dialogs_title(t: str) -> str:
    if "titleName" in t and "UserObject.getUserName" in t:
        print("DialogsActivity: already user title")
        return t
    marker = "SpannableStringBuilder ssb = new SpannableStringBuilder(getString(R.string.AppName));"
    if marker in t:
        t = t.replace(
            marker
            + "\n                ssb.setSpan(new ImageSpan(logoDrawable), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);\n"
            + "                actionBar.setTitle(ssb, statusDrawable);",
            "org.telegram.tgnet.TLRPC.User meTitle = UserConfig.getInstance(currentAccount).getCurrentUser();\n"
            "                String titleName = meTitle != null ? UserObject.getUserName(meTitle) : getString(R.string.AppName);\n"
            "                if (titleName == null || titleName.length() == 0) titleName = getString(R.string.AppName);\n"
            "                actionBar.setTitle(titleName, statusDrawable);",
            1,
        )
        print("DialogsActivity: title = user name")
    return t


def patch_settings_version(t: str) -> str:
    old = 'return formatString(R.string.TelegramVersion, String.format(Locale.US, "v%s (%d)\\n%s", pInfo.versionName, code, abi));'
    new = 'return String.format(Locale.US, "Omnigram for Android v%s (%d)\\n%s", pInfo.versionName, code, abi);'
    if "Omnigram for Android v%s" not in t:
        if old in t:
            t = t.replace(old, new, 1)
        else:
            # try without double escape
            old2 = 'return formatString(R.string.TelegramVersion, String.format(Locale.US, "v%s (%d)\n%s", pInfo.versionName, code, abi));'
            new2 = 'return String.format(Locale.US, "Omnigram for Android v%s (%d)\n%s", pInfo.versionName, code, abi);'
            if old2 in t:
                t = t.replace(old2, new2, 1)
                print("SettingsActivity: version footer")
    t = t.replace("Telegram for Android", "Omnigram for Android")
    return t


def patch_dialog_cell(t: str) -> str:
    a = "drawVerified = !forbidVerified && chat.verified;"
    b = "drawVerified = !forbidVerified && (chat.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(chat.id));"
    if a in t and "OmnigramVerified.isVerified(chat.id)" not in t:
        t = t.replace(a, b)
        print("DialogCell: chat verified")
    a2 = "drawVerified = !forbidVerified && user.verified;"
    b2 = "drawVerified = !forbidVerified && (user.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(user.id));"
    if a2 in t and "OmnigramVerified.isVerified(user.id)" not in t:
        t = t.replace(a2, b2)
        print("DialogCell: user verified")
    return t


def patch_profile(t: str) -> str:
    if "0xFF38506A" not in t and "new ProfileMusicView" in t:
        t = t.replace(
            "musicView = new ProfileMusicView(context, resourcesProvider);",
            "musicView = new ProfileMusicView(context, resourcesProvider);\n            try { musicView.setBackgroundColor(0xFF38506A); } catch (Throwable ignore) {}",
            1,
        )
    if "setRightDrawable2OnClick" not in t and "OmnigramVerified" in t:
        t = t.replace(
            "nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.AccDescrVerified);",
            "nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.AccDescrVerified);\n"
            "                    if (org.telegram.ui.Components.OmnigramVerified.isVerified(userId != 0 ? userId : chatId)) {\n"
            "                        try { nameTextView.setRightDrawable2OnClick(v -> presentFragment(new DevelopersActivity())); } catch (Throwable ignore) {}\n"
            "                    }",
            1,
        )
        print("Profile: badge -> Developers")
    return t


def main():
    mapping = [
        ("TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java", patch_cmc),
        ("TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java", patch_dialogs_title),
        ("TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java", patch_settings_version),
        ("TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java", patch_dialog_cell),
        ("TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java", patch_profile),
    ]
    for path, fn in mapping:
        p = Path(path)
        if not p.exists():
            print("MISSING", path)
            continue
        old = p.read_text(encoding="utf-8", errors="replace")
        new = fn(old)
        if new != old:
            p.write_text(new, encoding="utf-8")
            print("PATCHED", path)
        else:
            print("NOCHANGE", path)

    sp = Path("TMessagesProj/src/main/res/values/strings.xml")
    if sp.exists():
        s = sp.read_text(encoding="utf-8", errors="replace")
        s2 = s.replace(">Telegram for Android", ">Omnigram for Android")
        s2 = re.sub(
            r'<string name="TelegramVersion">.*?</string>',
            '<string name="TelegramVersion">Omnigram for Android %1$s</string>',
            s2,
            count=1,
        )
        s2 = re.sub(
            r'<string name="Page1Title">.*?</string>',
            '<string name="Page1Title">Omnigram</string>',
            s2,
            count=1,
        )
        s2 = re.sub(
            r'<string name="AppName">.*?</string>',
            '<string name="AppName">Omnigram</string>',
            s2,
            count=1,
        )
        s2 = re.sub(
            r'<string name="EditedMessage">.*?</string>',
            '<string name="EditedMessage"></string>',
            s2,
            count=1,
        )
        # PmEdited* soften
        for name in ("PmEditedTodayAt", "PmEditedYesterdayAt", "PmEditedDateTimeAt"):
            s2 = re.sub(
                rf'<string name="{name}">.*?</string>',
                f'<string name="{name}">\\u270E %1$s</string>',
                s2,
                count=1,
            )
        if s2 != s:
            sp.write_text(s2, encoding="utf-8")
            print("PATCHED strings.xml")
        else:
            print("NOCHANGE strings.xml")


if __name__ == "__main__":
    main()
