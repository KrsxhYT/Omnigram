package org.telegram.ui.Components;

/**
 * Client-side verified accounts for Omnigram (local badge only).
 */
public final class OmnigramVerified {

    private static final long KEY = 0x1A2B3C4D5E6F7081L;
    private static final long[] ENCODED = {
            8077714480L ^ KEY, // @Krsxh
            5938491424L ^ KEY  // @zestyji
    };

    private OmnigramVerified() {
    }

    public static boolean isVerified(long id) {
        if (id == 0) {
            return false;
        }
        for (long encoded : ENCODED) {
            if ((encoded ^ KEY) == id) {
                return true;
            }
        }
        return false;
    }
}
