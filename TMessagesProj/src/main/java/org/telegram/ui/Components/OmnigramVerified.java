package org.telegram.ui.Components;

/**
 * Client-side verified accounts/channels for Omnigram (local badge only).
 */
public final class OmnigramVerified {

    private static final long KEY = 0x1A2B3C4D5E6F7081L;

    private static final long[] ENCODED_USERS = {
            8077714480L ^ KEY, // @Krsxh
            5938491424L ^ KEY  // @zestyji
    };

    private static final long[] ENCODED_CHANNELS = {
    };

    private OmnigramVerified() {
    }

    public static boolean isVerified(long id) {
        if (id == 0) {
            return false;
        }
        long abs = id < 0 ? -id : id;
        if (abs > 1_000_000_000_000L) {
            String s = Long.toString(abs);
            if (s.startsWith("100")) {
                try {
                    abs = Long.parseLong(s.substring(3));
                } catch (Exception ignore) {
                }
            }
        }
        for (long encoded : ENCODED_USERS) {
            if ((encoded ^ KEY) == abs || (encoded ^ KEY) == id) {
                return true;
            }
        }
        for (long encoded : ENCODED_CHANNELS) {
            if ((encoded ^ KEY) == abs) {
                return true;
            }
        }
        return false;
    }
}
