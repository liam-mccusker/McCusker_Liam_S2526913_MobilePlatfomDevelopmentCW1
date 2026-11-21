package org.me.gcu.mccusker_liam_s2526913;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.me.gcu.mccusker_liam_s2526913.ui.DisplayFormat;

import java.time.OffsetDateTime;

// Checks the text the details screen shows for types, dates and timings
public class DisplayFormatTest {

    @Test
    public void readable_splitsCamelCaseIntoWords() {
        assertEquals("West bound", DisplayFormat.readable("westBound"));
        assertEquals("Very long delays", DisplayFormat.readable("veryLongDelays"));
        assertNull(DisplayFormat.readable(null));
    }

    @Test
    public void dateTime_formatsFeedDates() {
        assertEquals("Sat 22 Jul 2023, 00:00", DisplayFormat.dateTime("2023-07-22T00:00:00+01:00"));
    }

    @Test
    public void dateTime_unreadableValueIsShownUnchanged() {
        assertEquals("not a date", DisplayFormat.dateTime("not a date"));
    }

    // The live feeds only list current situations, so "Starts in" is tested here
    @Test
    public void startStatus_futureStart() {
        String inThreeDays = OffsetDateTime.now().plusDays(3).plusHours(1).toString();
        assertEquals("Starts in 3 days", DisplayFormat.startStatus(inThreeDays));
    }

    @Test
    public void startStatus_pastStart() {
        String fiveHoursAgo = OffsetDateTime.now().minusHours(5).minusMinutes(1).toString();
        assertEquals("Started 5 hours ago", DisplayFormat.startStatus(fiveHoursAgo));
    }

    @Test
    public void endStatus_pastEnd() {
        String tenDaysAgo = OffsetDateTime.now().minusDays(10).minusHours(1).toString();
        assertEquals("Ended 10 days ago", DisplayFormat.endStatus(tenDaysAgo));
    }

    @Test
    public void endStatus_farFutureUsesMonths() {
        String in400Days = OffsetDateTime.now().plusDays(400).toString();
        assertEquals("Ends in about 13 months", DisplayFormat.endStatus(in400Days));
    }

    @Test
    public void status_missingTimeGivesNull() {
        assertNull(DisplayFormat.endStatus(null));
        assertNull(DisplayFormat.startStatus(""));
    }
}