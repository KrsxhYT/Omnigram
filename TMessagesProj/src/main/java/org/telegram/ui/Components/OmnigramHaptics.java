package org.telegram.ui.Components;

import android.view.View;

/** Every haptic tap in the app goes through here so "In-App Vibration" can switch them all off (off by default). */
public final class OmnigramHaptics {
    private OmnigramHaptics() {}

    public static boolean perform(View v, int constant) {
        if (v == null || !OmnigramPrefs.vibration()) return false;
        return v.performHapticFeedback(constant);
    }

    public static boolean perform(View v, int constant, int flags) {
        if (v == null || !OmnigramPrefs.vibration()) return false;
        return v.performHapticFeedback(constant, flags);
    }
}
