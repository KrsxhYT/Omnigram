#!/usr/bin/env python3
"""Repair Omnigram compile break + apply branding quick fixes."""
from pathlib import Path
import re

PA = Path("TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java")
CMC = Path("TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java")
DA = Path("TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java")
SA = Path("TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java")
DC = Path("TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java")
STR = Path("TMessagesProj/src/main/res/values/strings.xml")


def fix_profile_click(t: str) -> str:
    start = t.find("            } else if (position == addMemberRow) {\n                openAddMember();\n            } else if (position == userOrChatIdRow) {")
    if start < 0:
        print("Profile click: start marker not found")
        return t

    end_marker = "            } else if (position == usernameRow) {\n                processOnClickOrPress(position, view, x, y);"
    end = t.find(end_marker, start)
    if end < 0:
        print("Profile click: end marker not found")
        return t

    replacement = (
        "            } else if (position == addMemberRow) {\n"
        "                openAddMember();\n"
        "            } else if (position == userOrChatIdRow) {\n"
        "                long idv = userId != 0 ? userId : chatId;\n"
        "                AndroidUtilities.addToClipboard(String.valueOf(idv));\n"
        "                BulletinFactory.of(ProfileActivity.this).createSimpleBulletin(R.raw.copy, \"ID copied\").show();\n"
        "            } else if (position == regDateRow) {\n"
        "                long idv = userId != 0 ? userId : chatId;\n"
        "                String name = userId != 0 ? \"This user\" : \"This channel\";\n"
        "                try {\n"
        "                    if (userId != 0) {\n"
        "                        org.telegram.tgnet.TLRPC.User u = getMessagesController().getUser(userId);\n"
        "                        if (u != null) name = org.telegram.messenger.UserObject.getFirstName(u);\n"
        "                    } else if (currentChat != null && currentChat.title != null) {\n"
        "                        name = currentChat.title;\n"
        "                    }\n"
        "                } catch (Throwable ignore) {}\n"
        "                BulletinFactory.of(ProfileActivity.this).createSimpleBulletin(R.raw.contact_check,\n"
        "                        org.telegram.ui.Components.OmnigramRegDate.approximateSentence(name, idv)).show();\n"
        "            } else if (position == usernameRow) {\n"
        "                processOnClickOrPress(position, view, x, y);"
    )
    t = t[:start] + replacement + t[end + len(end_marker):]
    print("ProfileActivity: fixed click handler")
    return t


def improve_bind(t: str) -> str:
    if "OmnigramRegDate.dcSubtitle" in t:
        print("Profile bind: already has dcSubtitle")
        return t
    old = (
        "                    } else if (position == userOrChatIdRow) {\n"
        "                        long idv = userId != 0 ? userId : chatId;\n"
        "                        detailCell.setTextAndValue(String.valueOf(idv), userId != 0 ? \"User ID\" : \"Chat ID\", true);\n"
        "                    } else if (position == regDateRow) {\n"
        "                        long idv = userId != 0 ? userId : chatId;\n"
        "                        detailCell.setTextAndValue(org.telegram.ui.Components.OmnigramRegDate.estimate(idv), \"Registered\", true);\n"
        "                    } else if (position == usernameRow) {"
    )
    new = (
        "                    } else if (position == userOrChatIdRow) {\n"
        "                        long idv = userId != 0 ? userId : chatId;\n"
        "                        int dc = 0;\n"
        "                        try {\n"
        "                            if (userId != 0) {\n"
        "                                org.telegram.tgnet.TLRPC.User u = getMessagesController().getUser(userId);\n"
        "                                if (u != null && u.photo != null) dc = u.photo.dc_id;\n"
        "                            } else if (currentChat != null && currentChat.photo != null) {\n"
        "                                dc = currentChat.photo.dc_id;\n"
        "                            }\n"
        "                        } catch (Throwable ignore) {}\n"
        "                        String sub = org.telegram.ui.Components.OmnigramRegDate.dcSubtitle(dc);\n"
        "                        if (sub == null || sub.length() == 0) sub = (userId != 0 ? \"User ID\" : \"Chat ID\");\n"
        "                        detailCell.setTextAndValue(String.valueOf(idv), sub, true);\n"
        "                        try {\n"
        "                            android.graphics.drawable.Drawable cal = getContext().getResources().getDrawable(R.drawable.msg_calendar2).mutate();\n"
        "                            detailCell.setImage(cal);\n"
        "                            detailCell.setImageClickListener(v -> {\n"
        "                                String name = userId != 0 ? \"This user\" : \"This channel\";\n"
        "                                try {\n"
        "                                    if (userId != 0) {\n"
        "                                        org.telegram.tgnet.TLRPC.User u = getMessagesController().getUser(userId);\n"
        "                                        if (u != null) name = org.telegram.messenger.UserObject.getFirstName(u);\n"
        "                                    } else if (currentChat != null && currentChat.title != null) {\n"
        "                                        name = currentChat.title;\n"
        "                                    }\n"
        "                                } catch (Throwable ignore) {}\n"
        "                                BulletinFactory.of(ProfileActivity.this).createSimpleBulletin(R.raw.contact_check,\n"
        "                                    org.telegram.ui.Components.OmnigramRegDate.approximateSentence(name, idv)).show();\n"
        "                            });\n"
        "                        } catch (Throwable ignore) {}\n"
        "                    } else if (position == regDateRow) {\n"
        "                        long idv = userId != 0 ? userId : chatId;\n"
        "                        detailCell.setTextAndValue(org.telegram.ui.Components.OmnigramRegDate.estimate(idv), \"Registered\", true);\n"
        "                    } else if (position == usernameRow) {"
    )
    if old in t:
        t = t.replace(old, new, 1)
        print("Profile bind: upgraded")
    return t


def fix_cmc(t: str) -> str:
    if "return LocaleController.formatPmEditedDate(editDate);" in t:
        t = t.replace(
            "return LocaleController.formatPmEditedDate(editDate);",
            'return "\\u270E " + LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);',
        )
        print("CMC: pencil")
    t = t.replace('getString(R.string.EditedMessage) + " "', '"\\u270E "')
    return t


def fix_dialogs(t: str) -> str:
    if "titleName" in t and "UserObject.getUserName" in t:
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
        print("Dialogs: user title")
    return t


def fix_settings(t: str) -> str:
    old = 'return formatString(R.string.TelegramVersion, String.format(Locale.US, "v%s (%d)\\n%s", pInfo.versionName, code, abi));'
    new = 'return String.format(Locale.US, "Omnigram for Android v%s (%d)\\n%s", pInfo.versionName, code, abi);'
    if "Omnigram for Android v%s" not in t:
        old2 = 'return formatString(R.string.TelegramVersion, String.format(Locale.US, "v%s (%d)\n%s", pInfo.versionName, code, abi));'
        new2 = 'return String.format(Locale.US, "Omnigram for Android v%s (%d)\n%s", pInfo.versionName, code, abi);'
        if old2 in t:
            t = t.replace(old2, new2, 1)
            print("Settings: version")
        elif old in t:
            t = t.replace(old, new, 1)
    t = t.replace("Telegram for Android", "Omnigram for Android")
    return t


def fix_dialog_cell(t: str) -> str:
    a = "drawVerified = !forbidVerified && chat.verified;"
    b = "drawVerified = !forbidVerified && (chat.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(chat.id));"
    if a in t and "OmnigramVerified.isVerified(chat.id)" not in t:
        t = t.replace(a, b)
    a2 = "drawVerified = !forbidVerified && user.verified;"
    b2 = "drawVerified = !forbidVerified && (user.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(user.id));"
    if a2 in t and "OmnigramVerified.isVerified(user.id)" not in t:
        t = t.replace(a2, b2)
        print("DialogCell: verified")
    return t


def fix_strings(s: str) -> str:
    s2 = s.replace(">Telegram for Android", ">Omnigram for Android")
    s2 = re.sub(r'<string name="TelegramVersion">.*?</string>', '<string name="TelegramVersion">Omnigram for Android %1$s</string>', s2, count=1)
    s2 = re.sub(r'<string name="Page1Title">.*?</string>', '<string name="Page1Title">Omnigram</string>', s2, count=1)
    s2 = re.sub(r'<string name="AppName">.*?</string>', '<string name="AppName">Omnigram</string>', s2, count=1)
    s2 = re.sub(r'<string name="EditedMessage">.*?</string>', '<string name="EditedMessage"></string>', s2, count=1)
    return s2


def main():
    t = PA.read_text(encoding="utf-8", errors="replace")
    t = fix_profile_click(t)
    t = improve_bind(t)
    PA.write_text(t, encoding="utf-8")

    if CMC.exists():
        CMC.write_text(fix_cmc(CMC.read_text(encoding="utf-8", errors="replace")), encoding="utf-8")
    if DA.exists():
        DA.write_text(fix_dialogs(DA.read_text(encoding="utf-8", errors="replace")), encoding="utf-8")
    if SA.exists():
        SA.write_text(fix_settings(SA.read_text(encoding="utf-8", errors="replace")), encoding="utf-8")
    if DC.exists():
        DC.write_text(fix_dialog_cell(DC.read_text(encoding="utf-8", errors="replace")), encoding="utf-8")
    if STR.exists():
        s = STR.read_text(encoding="utf-8", errors="replace")
        s2 = fix_strings(s)
        if s2 != s:
            STR.write_text(s2, encoding="utf-8")
            print("strings updated")

    t = PA.read_text(encoding="utf-8", errors="replace")
    idx = t.find("} else if (position == addMemberRow)")
    chunk = t[idx:idx + 2000]
    if "detailCell.setTextAndValue" in chunk:
        print("ERROR: detailCell still in click handler")
        raise SystemExit(1)
    print("SANITY OK")


if __name__ == "__main__":
    main()
