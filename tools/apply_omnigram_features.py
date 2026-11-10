#!/usr/bin/env python3
"""Apply Omnigram UI features to ProfileActivity, SettingsActivity, ChatMessageCell."""
from pathlib import Path


def patch_profile(t: str) -> str:
    if "private int userOrChatIdRow" not in t:
        t = t.replace(
            "    private int usernameRow;",
            "    private int usernameRow;\n    private int userOrChatIdRow;\n    private int regDateRow;",
            1,
        )
    if "userOrChatIdRow = -1" not in t:
        t = t.replace(
            "        usernameRow = -1;",
            "        usernameRow = -1;\n        userOrChatIdRow = -1;\n        regDateRow = -1;",
            1,
        )

    lines = t.splitlines(keepends=True)
    out = []
    for i, line in enumerate(lines):
        out.append(line)
        if "usernameRow = rowCount++" in line and "userOrChatIdRow" not in line:
            nxt = lines[i + 1] if i + 1 < len(lines) else ""
            if "userOrChatIdRow" not in nxt:
                ind = line[: len(line) - len(line.lstrip())]
                out.append(f"{ind}userOrChatIdRow = rowCount++;\n")
                out.append(f"{ind}regDateRow = rowCount++;\n")
    t = "".join(out)

    if t.count("detailCell.setTextAndValue(String.valueOf(idv)") == 0:
        anchor = "                    } else if (position == usernameRow) {"
        insert = (
            "                    } else if (position == userOrChatIdRow) {\n"
            "                        long idv = userId != 0 ? userId : chatId;\n"
            '                        detailCell.setTextAndValue(String.valueOf(idv), userId != 0 ? "User ID" : "Chat ID", true);\n'
            "                    } else if (position == regDateRow) {\n"
            "                        long idv = userId != 0 ? userId : chatId;\n"
            '                        detailCell.setTextAndValue(org.telegram.ui.Components.OmnigramRegDate.estimate(idv), "Registered", true);\n'
            "                    } else if (position == usernameRow) {"
        )
        if anchor in t:
            t = t.replace(anchor, insert, 1)

    if "ID copied" not in t:
        anchor = "            } else if (position == usernameRow) {"
        insert = (
            "            } else if (position == userOrChatIdRow) {\n"
            "                long idv = userId != 0 ? userId : chatId;\n"
            "                AndroidUtilities.addToClipboard(String.valueOf(idv));\n"
            '                BulletinFactory.of(ProfileActivity.this).createSimpleBulletin(R.raw.copy, "ID copied").show();\n'
            "            } else if (position == regDateRow) {\n"
            "                long idv = userId != 0 ? userId : chatId;\n"
            "                new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity())\n"
            '                        .setTitle("Registered")\n'
            '                        .setMessage("Approximate registration: " + org.telegram.ui.Components.OmnigramRegDate.estimate(idv) + "\\n\\nBased on Telegram ID ranges.")\n'
            '                        .setPositiveButton("OK", null)\n'
            "                        .show();\n"
            "            } else if (position == usernameRow) {"
        )
        if anchor in t:
            t = t.replace(anchor, insert, 1)

    if "OmnigramVerified" not in t:
        old = (
            "                } else if (user.verified) {\n"
            "                    nameTextView.setRightDrawable2(getVerifiedCrossfadeDrawable());\n"
            "                    nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.AccDescrVerified);\n"
            "                } else {\n"
            "                    nameTextView.setRightDrawable2(null);\n"
            "                    nameTextViewRightDrawable2ContentDescription = null;\n"
            "                }"
        )
        new = (
            "                } else if (user.verified || org.telegram.ui.Components.OmnigramVerified.isVerified(userId)) {\n"
            "                    if (user.verified) {\n"
            "                        nameTextView.setRightDrawable2(getVerifiedCrossfadeDrawable());\n"
            "                    } else {\n"
            "                        android.graphics.drawable.Drawable omniBadge = getContext().getResources().getDrawable(R.drawable.ic_verified_omnigram).mutate();\n"
            "                        omniBadge.setBounds(0, 0, org.telegram.messenger.AndroidUtilities.dp(16), org.telegram.messenger.AndroidUtilities.dp(16));\n"
            "                        nameTextView.setRightDrawable2(omniBadge);\n"
            "                    }\n"
            "                    nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.AccDescrVerified);\n"
            "                } else {\n"
            "                    nameTextView.setRightDrawable2(null);\n"
            "                    nameTextViewRightDrawable2ContentDescription = null;\n"
            "                }"
        )
        if old in t:
            t = t.replace(old, new)

    if "0xFF38506A" not in t:
        t = t.replace(
            "musicView = new ProfileMusicView(context, resourcesProvider);",
            "musicView = new ProfileMusicView(context, resourcesProvider);\n            try { musicView.setBackgroundColor(0xFF38506A); } catch (Throwable ignore) {}",
            1,
        )

    pairs = [
        (
            "position == usernameRow || position == setUsernameRow",
            "position == usernameRow || position == setUsernameRow || position == userOrChatIdRow || position == regDateRow",
        ),
        (
            "position == phoneRow || position == locationRow || position == numberRow",
            "position == phoneRow || position == locationRow || position == numberRow || position == userOrChatIdRow || position == regDateRow",
        ),
    ]
    for a, b in pairs:
        if a in t and b not in t:
            t = t.replace(a, b)

    t = t.replace(
        "position == versionRow || position == dataRow",
        "position == versionRow || position == userOrChatIdRow || position == regDateRow || position == dataRow",
        1,
    )

    return t


def patch_settings(t: str) -> str:
    if "case 9999" in t:
        return t
    old = 'items.add(SettingCell.Factory.of(19, IconBackgroundColors.GREEN.top, IconBackgroundColors.GREEN.bottom, R.drawable.settings_policy, getString(R.string.PrivacyPolicy)));'
    new = old + '\n        items.add(SettingCell.Factory.of(9999, 0xFF62B0F0, 0xFF1B242F, R.drawable.msg_info, "Developers", "Omnigram Team"));'
    if old in t:
        t = t.replace(old, new, 1)
    old2 = (
        "            case 19:\n"
        "                Browser.openUrl(getParentActivity(), LocaleController.getString(R.string.PrivacyPolicyUrl));\n"
        "                break;"
    )
    new2 = old2 + (
        "\n            case 9999:\n"
        "                presentFragment(new DevelopersActivity());\n"
        "                break;"
    )
    if old2 in t:
        t = t.replace(old2, new2, 1)
    return t


def patch_cmc(t: str) -> str:
    old = 'return getString(R.string.EditedMessage) + " " + LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);'
    new = 'return "✎ " + LocaleController.getInstance().getFormatterDay().format((long) (message.messageOwner.date) * 1000);'
    if old in t:
        t = t.replace(old, new, 1)
    return t


def main():
    mapping = [
        ("TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java", patch_profile),
        ("TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java", patch_settings),
        ("TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java", patch_cmc),
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


if __name__ == "__main__":
    main()
