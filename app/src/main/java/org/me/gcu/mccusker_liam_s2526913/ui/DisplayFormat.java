package org.me.gcu.mccusker_liam_s2526913.ui;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

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
}