#!/usr/bin/env python3
"""Fix DialogsActivity null logoDrawable NPE and ensure clean title."""
from pathlib import Path

p = Path("TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java")
t = p.read_text(encoding="utf-8", errors="replace")

old = """                logoDrawable = null; // Omnigram
                logoDrawable.setBounds(0, dp(2), logoDrawable.getIntrinsicWidth(), dp(2) + logoDrawable.getIntrinsicHeight());
                // a new drawable (also when the view is recreated after a theme switch made elsewhere): remember its tint, the
                // theme hook only recolors when the color differs from the last one applied
                logoDrawable.setColorFilter(lastLogoDrawableColor = getThemedColor(Theme.key_telegram_color_dialogsLogo), PorterDuff.Mode.MULTIPLY);
                org.telegram.tgnet.TLRPC.User meTitle = UserConfig.getInstance(currentAccount).getCurrentUser();
                String titleName = meTitle != null ? UserObject.getUserName(meTitle) : getString(R.string.AppName);
                if (titleName == null || titleName.length() == 0) titleName = getString(R.string.AppName);
                actionBar.setTitle(titleName, statusDrawable);
                updateStatus(UserConfig.getInstance(currentAccount).getCurrentUser(), false);"""

new = """                // Omnigram: no Telegram word-logo; show account name only
                logoDrawable = null;
                org.telegram.tgnet.TLRPC.User meTitle = UserConfig.getInstance(currentAccount).getCurrentUser();
                String titleName = meTitle != null ? UserObject.getUserName(meTitle) : getString(R.string.AppName);
                if (titleName == null || titleName.length() == 0) titleName = getString(R.string.AppName);
                actionBar.setTitle(titleName, statusDrawable);
                updateStatus(UserConfig.getInstance(currentAccount).getCurrentUser(), false);"""

if old in t:
    t = t.replace(old, new, 1)
    print("DialogsActivity: removed null logoDrawable method calls")
else:
    if "logoDrawable = null; // Omnigram" in t and "logoDrawable.setBounds" in t:
        t = t.replace(
            "logoDrawable = null; // Omnigram\n"
            "                logoDrawable.setBounds(0, dp(2), logoDrawable.getIntrinsicWidth(), dp(2) + logoDrawable.getIntrinsicHeight());\n"
            "                // a new drawable (also when the view is recreated after a theme switch made elsewhere): remember its tint, the\n"
            "                // theme hook only recolors when the color differs from the last one applied\n"
            "                logoDrawable.setColorFilter(lastLogoDrawableColor = getThemedColor(Theme.key_telegram_color_dialogsLogo), PorterDuff.Mode.MULTIPLY);",
            "logoDrawable = null; // Omnigram: no Telegram logo",
            1,
        )
        print("DialogsActivity: soft null logo fix")
    else:
        print("DialogsActivity: pattern not found")

p.write_text(t, encoding="utf-8")
print("done")
