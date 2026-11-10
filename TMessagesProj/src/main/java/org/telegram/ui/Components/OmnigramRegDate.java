package org.telegram.ui.Components;

/**
 * Approximate Telegram account registration from user/channel id.
 * Shown like AyuGram: "created their account approximately on December 1, 2023."
 */
public final class OmnigramRegDate {

    private OmnigramRegDate() {
    }

    /** Short label for profile row, e.g. "Dec 2023". */
    public static String estimate(long id) {
        long[] parts = approxYearMonthDay(id);
        if (parts == null) {
            return "Unknown";
        }
        String[] months = {
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };
        int y = (int) parts[0];
        int m = (int) parts[1];
        if (m < 1) m = 1;
        if (m > 12) m = 12;
        return months[m - 1] + " " + y;
    }

    /** Full sentence for bulletin / dialog. */
    public static String approximateSentence(String displayName, long id) {
        long[] parts = approxYearMonthDay(id);
        if (parts == null) {
            return displayName + " account age is unknown.";
        }
        String[] months = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };
        int y = (int) parts[0];
        int m = (int) parts[1];
        int d = (int) parts[2];
        if (m < 1) m = 1;
        if (m > 12) m = 12;
        if (d < 1) d = 1;
        if (d > 28) d = 28;
        return displayName + " created their account approximately on "
                + months[m - 1] + " " + d + ", " + y + ".";
    }

    public static long[] approxYearMonthDay(long id) {
        if (id < 0) {
            id = -id;
        }
        if (id > 1_000_000_000_000L) {
            String s = Long.toString(id);
            if (s.startsWith("100")) {
                try {
                    id = Long.parseLong(s.substring(3));
                } catch (Exception ignore) {
                }
            }
        }
        if (id == 0) {
            return null;
        }

        int year;
        int month;
        int day;
        if (id < 100_000_000L) {
            year = 2013; month = 10; day = 14;
        } else if (id < 250_000_000L) {
            year = 2014; month = 6; day = 1;
        } else if (id < 500_000_000L) {
            year = 2015; month = 8; day = 15;
        } else if (id < 750_000_000L) {
            year = 2016; month = 7; day = 1;
        } else if (id < 1_000_000_000L) {
            year = 2017; month = 9; day = 1;
        } else if (id < 1_250_000_000L) {
            year = 2018; month = 6; day = 1;
        } else if (id < 1_500_000_000L) {
            year = 2019; month = 3; day = 1;
        } else if (id < 1_750_000_000L) {
            year = 2019; month = 10; day = 1;
        } else if (id < 2_000_000_000L) {
            year = 2020; month = 5; day = 1;
        } else if (id < 3_000_000_000L) {
            year = 2021; month = 4; day = 1;
        } else if (id < 5_000_000_000L) {
            year = 2022; month = 2; day = 1;
        } else if (id < 6_000_000_000L) {
            year = 2022; month = 11; day = 1;
        } else if (id < 6_500_000_000L) {
            year = 2023; month = 4; day = 1;
        } else if (id < 7_000_000_000L) {
            year = 2023; month = 9; day = 1;
        } else if (id < 7_500_000_000L) {
            year = 2024; month = 2; day = 1;
        } else if (id < 8_000_000_000L) {
            year = 2024; month = 8; day = 1;
        } else if (id < 8_500_000_000L) {
            year = 2025; month = 2; day = 1;
        } else {
            year = 2025; month = 9; day = 1;
        }
        day = 1 + (int) ((id / 97) % 27);
        return new long[]{year, month, day};
    }

    public static String dcSubtitle(int dcId) {
        switch (dcId) {
            case 1: return "DC1, Miami, US";
            case 2: return "DC2, Amsterdam, NL";
            case 3: return "DC3, Miami, US";
            case 4: return "DC4, Amsterdam, NL";
            case 5: return "DC5, Singapore, SG";
            default: return dcId > 0 ? ("DC" + dcId) : "";
        }
    }
}
