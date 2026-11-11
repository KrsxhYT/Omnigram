package org.telegram.ui.Components;
import java.util.HashSet;
import java.util.Set;
public final class OmnigramDeleted {
    private static final Set<String> KEYS = new HashSet<>();
    private OmnigramDeleted() {}
    private static String key(long d, int m) { return d + ":" + m; }
    public static void mark(long d, int m) { KEYS.add(key(d, m)); }
    public static boolean isDeleted(long d, int m) { return KEYS.contains(key(d, m)); }
    public static void markMany(long d, java.util.ArrayList<Integer> ids) {
        if (ids == null) return;
        for (int i = 0; i < ids.size(); i++) mark(d, ids.get(i));
    }
}
