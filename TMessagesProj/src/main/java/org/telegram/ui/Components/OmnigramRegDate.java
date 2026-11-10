package org.telegram.ui.Components;

/**
 * Approximate Telegram account registration date from user/chat id ranges.
 * Same approach used by many clients (incl. older Zoxgram builds) when server does not expose create date.
 */
public final class OmnigramRegDate {

    private OmnigramRegDate() {
    }

    public static String estimate(long id) {
        if (id < 0) {
            id = -id;
        }
        if (id == 0) {
            return "Unknown";
        }
        // Chat/channel ids often 10+ digits after channel migration
        if (id < 100_000_000L) {
            return "2013 – 2014";
        }
        if (id < 500_000_000L) {
            return "2015 – 2016";
        }
        if (id < 1_000_000_000L) {
            return "2017 – 2018";
        }
        if (id < 1_500_000_000L) {
            return "2019";
        }
        if (id < 2_000_000_000L) {
            return "2020";
        }
        if (id < 5_000_000_000L) {
            return "2021 – 2022";
        }
        if (id < 6_000_000_000L) {
            return "2023";
        }
        if (id < 7_500_000_000L) {
            return "2024";
        }
        return "2025+";
    }

    public static String label(long id) {
        return "Registered: ~" + estimate(id);
    }
}
