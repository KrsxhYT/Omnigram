package org.telegram.ui.Components;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.R;

/** Message tick style (sent / read). Style 0 keeps Telegram's own ticks. Applied when chat resources are built. */
public final class OmnigramCheckStyle {
    private OmnigramCheckStyle() {}

    public static final String[] NAMES = {"Default", "iOS", "Telegram X", "Pixel", "Heart", "WhatsApp", "Instagram"};
    private static final String[] KEYS = {null, "ios", "tgx", "pixel", "heart", "wa", "ig"};

    private static int res(int style, boolean half) {
        switch (style) {
            case 1: return half ? R.drawable.omni_chk_ios_b : R.drawable.omni_chk_ios_a;
            case 2: return half ? R.drawable.omni_chk_tgx_b : R.drawable.omni_chk_tgx_a;
            case 3: return half ? R.drawable.omni_chk_pixel_b : R.drawable.omni_chk_pixel_a;
            case 4: return half ? R.drawable.omni_chk_heart_b : R.drawable.omni_chk_heart_a;
            case 5: return half ? R.drawable.omni_chk_wa_b : R.drawable.omni_chk_wa_a;
            case 6: return half ? R.drawable.omni_chk_ig_b : R.drawable.omni_chk_ig_a;
            default: return 0;
        }
    }

    /** Drawable for a tick id used by Theme; falls back to the original for style 0. */
    public static Drawable pick(Resources resources, int originalId) {
        try {
            int style = OmnigramPrefs.checkStyle();
            if (style > 0 && style < KEYS.length) {
                if (originalId == R.drawable.msg_check_s) return resources.getDrawable(res(style, false));
                if (originalId == R.drawable.msg_halfcheck || originalId == R.drawable.msg_halfcheck_s) return resources.getDrawable(res(style, true));
            }
        } catch (Throwable ignore) {}
        return resources.getDrawable(originalId);
    }

    public static int previewRes(int style, boolean half) {
        int r = res(style, half);
        return r != 0 ? r : (half ? R.drawable.msg_halfcheck : R.drawable.msg_check_s);
    }
}
