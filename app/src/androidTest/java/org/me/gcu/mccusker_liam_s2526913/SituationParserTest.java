package org.me.gcu.mccusker_liam_s2526913;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.me.gcu.mccusker_liam_s2526913.data.DistanceUtils;
import org.me.gcu.mccusker_liam_s2526913.data.SituationParser;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Runs on the emulator: parses the four sample files from the app's assets
// and checks the results against values taken from the XML
@RunWith(AndroidJUnit4.class)
public class SituationParserTest {

    private static final double GCU_LAT = 55.8666;
    private static final double GCU_LON = -4.2504;

    private List<Situation> parseAsset(String fileName, String category) throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(fileName), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return new SituationParser().parse(sb.toString(), category);
    }

    @Test
    public void sampleFiles_giveTheRightNumberOfSituations() throws IOException {
        assertEquals(405, parseAsset("roadworksContentExample.xml", "roadworks").size());
        assertEquals(452, parseAsset("roadworksContentExample1.xml", "roadworks").size());
        assertEquals(4, parseAsset("unplannedContentExample.xml", "incident").size());
        assertEquals(7, parseAsset("unplannedContentExample1.xml", "incident").size());
    }

    @Test
    public void firstRoadworks_matchesTheXml() throws IOException {
        Situation s = parseAsset("roadworksContentExample.xml", "roadworks").get(0);
        assertEquals("D_159301_S", s.getId());
        assertEquals("MaintenanceWorks", s.getRecordType());
        assertEquals("roadworks", s.getSubType());
        assertEquals("M8 WB Woodside - A804 Phoenix Rd - Closure", s.getDescription());
        assertEquals("2023-07-22T00:00:00+01:00", s.getStartTime());
        assertEquals("2027-12-31T23:59:00+00:00", s.getEndTime());
        assertEquals("longDelays", s.getDelaysType());
        assertEquals("westBound", s.getDirection());
        assertEquals("M8", s.getRoadName());
        assertEquals("false", s.getUrgentRoadworks());
        assertEquals(55.869923, s.getFrom().getLatitude(), 1e-6);
        assertEquals(-4.261365, s.getFrom().getLongitude(), 1e-6);
        assertEquals("M8 (A81 Underpass to Jct 18 Off Slip)", s.getFrom().getName());
        assertEquals(55.87038, s.getTo().getLatitude(), 1e-6);
        assertEquals("M8 (Jct 18 Off Slip to Entry A82)", s.getTo().getName());
    }

    @Test
    public void firstIncident_matchesTheXml() throws IOException {
        Situation s = parseAsset("unplannedContentExample.xml", "incident").get(0);
        assertEquals("I_20260810_248_S", s.getId());
        assertEquals("RoadOrCarriagewayOrLaneManagement", s.getRecordType());
        assertEquals("roadClosed", s.getSubType());
        assertEquals("0", s.getNumberOfOperationalLanes());
        assertEquals("southBound", s.getDirection());
        assertEquals("A74(M)", s.getRoadName());
        assertEquals("mandatory", s.getComplianceOption());
        assertEquals("A74(M) J18 Ecclefechan", s.getFrom().getName());
        assertEquals("A74(M) J19 Eaglesfield", s.getTo().getName());
    }

    // The spec says new fields may appear: the app should ignore them, not crash
    @Test
    public void unknownTags_areIgnored() {
        String xml = "<d2LogicalModel><situation id=\"X1\">"
                + "<situationRecord xsi:type=\"Accident\">"
                + "<someFutureField>new data</someFutureField>"
                + "<accidentType>collision</accidentType>"
                + "<generalPublicComment><comment><values>"
                + "<value lang=\"en\">Test incident</value>"
                + "</values></comment></generalPublicComment>"
                + "</situationRecord></situation></d2LogicalModel>";
        List<Situation> result = new SituationParser().parse(xml, "incident");
        assertEquals(1, result.size());
        assertEquals("Accident", result.get(0).getRecordType());
        assertEquals("collision", result.get(0).getSubType());
        assertEquals("Test incident", result.get(0).getDescription());
    }

    @Test
    public void brokenXml_doesNotCrash() {
        List<Situation> result = new SituationParser().parse("<situation id=\"X1\"><broken", "incident");
        assertTrue(result.isEmpty());
    }

    // Expected counts calculated independently from the XML (see T12)
    @Test
    public void distanceSearchFromGcu_matchesExpectedCounts() throws IOException {
        List<Situation> all = new ArrayList<>(parseAsset("unplannedContentExample.xml", "incident"));
        all.addAll(parseAsset("roadworksContentExample.xml", "roadworks"));

        int withinOneMile = 0;
        int withinFiftyMiles = 0;
        for (Situation s : all) {
            double miles = DistanceUtils.distanceMiles(s, GCU_LAT, GCU_LON);
            if (miles >= 0 && miles <= 1) withinOneMile++;
            if (miles >= 0 && miles <= 50) withinFiftyMiles++;
        }
        assertEquals(11, withinOneMile);
        assertEquals(215, withinFiftyMiles);
    }
}