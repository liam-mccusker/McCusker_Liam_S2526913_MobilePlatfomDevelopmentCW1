package org.me.gcu.mccusker_liam_s2526913.ui;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.time.Duration;

// Turns raw feed values into text suitable for showing to users
public final class DisplayFormat {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);

    private DisplayFormat() {
    }

    // "westBound" -> "West bound", "longDelays" -> "Long delays"
    public static String readable(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String spaced = raw.replaceAll("([A-Z])", " $1").toLowerCase(Locale.UK);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    // "2023-07-22T00:00:00+01:00" -> "Sat 22 Jul 2023, 00:00"
    public static String dateTime(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).format(DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return iso;   // show the raw value rather than nothing
        }
    }

    // "Started 3 days ago" / "Starts in 2 days", or null if the time is missing or unreadable
    public static String startStatus(String iso) {
        OffsetDateTime time = parse(iso);
        if (time == null) {
            return null;
        }
        OffsetDateTime now = OffsetDateTime.now();
        return time.isAfter(now)
                ? "Starts in " + describeGap(Duration.between(now, time))
                : "Started " + describeGap(Duration.between(time, now)) + " ago";
    }

    // "Ends in about 13 months" / "Ended 2 hours ago", or null if there is no end time
    public static String endStatus(String iso) {
        OffsetDateTime time = parse(iso);
        if (time == null) {
            return null;
        }
        OffsetDateTime now = OffsetDateTime.now();
        return time.isAfter(now)
                ? "Ends in " + describeGap(Duration.between(now, time))
                : "Ended " + describeGap(Duration.between(time, now)) + " ago";
    }

    private static OffsetDateTime parse(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Describes a length of time at a sensible scale for drivers
    private static String describeGap(Duration gap) {
        if (gap.toMinutes() < 60) {
            return "under an hour";
        }
        long hours = gap.toHours();
        if (hours < 48) {
            return plural(hours, "hour");
        }
        long days = gap.toDays();
        if (days < 60) {
            return plural(days, "day");
        }
        if (days < 730) {
            return "about " + plural(days / 30, "month");
        }
        return "about " + plural(days / 365, "year");
    }

    private static String plural(long n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }
}