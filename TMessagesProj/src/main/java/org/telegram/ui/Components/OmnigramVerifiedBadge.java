package org.telegram.ui.Components;

import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/** The normal blue verified badge (same one used in the chat list) as a drawable for title bars. */
public final class OmnigramVerifiedBadge {
    private OmnigramVerifiedBadge() {}

    public static Drawable make() {
        try {
            if (Theme.dialogs_verifiedDrawable == null || Theme.dialogs_verifiedCheckDrawable == null) return null;
            Drawable combined = new CombinedDrawable(Theme.dialogs_verifiedDrawable, Theme.dialogs_verifiedCheckDrawable);
            return new AnimatedEmojiDrawable.WrapSizeDrawable(combined, AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        } catch (Throwable t) {
            return null;
        }
    }
}
