package org.telegram.ui.Components;
public final class OmnigramVerified {
    private static final long KEY = 0x1A2B3C4D5E6F7081L;
    private static final long[] ENCODED_USERS = { 8077714480L ^ KEY, 5938491424L ^ KEY };
    private static final String[] VERIFIED_USERNAMES = { "krsxh", "zestyji", "officalomnigram", "officialomnigram", "omnigramchat" };
    private OmnigramVerified() {}
    public static boolean isVerified(long id) {
        if (id == 0) return false;
        long abs = id < 0 ? -id : id;
        if (abs > 1_000_000_000_000L) {
            String s = Long.toString(abs);
            if (s.startsWith("100")) { try { abs = Long.parseLong(s.substring(3)); } catch (Exception ignore) {} }
        }
        for (long e : ENCODED_USERS) if ((e ^ KEY) == abs || (e ^ KEY) == id) return true;
        return false;
    }
    public static boolean isVerifiedUsername(String username) {
        if (username == null) return false;
        String u = username.toLowerCase(java.util.Locale.US);
        if (u.startsWith("@")) u = u.substring(1);
        for (String v : VERIFIED_USERNAMES) if (v.equals(u)) return true;
        return false;
    }
}
