package org.me.gcu.mccusker_liam_s2526913;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;
import org.me.gcu.mccusker_liam_s2526913.ui.SituationStyle;

// Checks which icon and delay colour each kind of situation gets
public class SituationStyleTest {

    private Situation roadworks(String description) {
        Situation s = new Situation();
        s.setCategory("roadworks");
        s.setDescription(description);
        return s;
    }

    private Situation incident(String recordType, String subType) {
        Situation s = new Situation();
        s.setCategory("incident");
        s.setRecordType(recordType);
        s.setSubType(subType);
        return s;
    }

    @Test
    public void roadworksIcons_comeFromTheDescription() {
        assertEquals(R.drawable.ic_roadworks_lanes,
                SituationStyle.iconFor(roadworks("M8 WB JCT 15 - JCT 19 - Lane Closures")));
        assertEquals(R.drawable.ic_roadworks_closure,
                SituationStyle.iconFor(roadworks("M8 WB Woodside - A804 Phoenix Rd - Closure")));
        assertEquals(R.drawable.ic_roadworks_lights,
                SituationStyle.iconFor(roadworks("A835 Tarvie East of Garve EB - Emergency TTL")));
        assertEquals(R.drawable.ic_roadworks,
                SituationStyle.iconFor(roadworks("A83 Tarbet to Arrochar - Signage Only")));
    }

    @Test
    public void ttl_onlyMatchesAsAWholeWord() {
        // "Battlefield" contains the letters "ttl" but is not temporary traffic lights
        assertEquals(R.drawable.ic_roadworks,
                SituationStyle.iconFor(roadworks("A9 Battlefield Rd - Signage")));
    }

    @Test
    public void incidentIcons() {
        assertEquals(R.drawable.ic_incident_closed,
                SituationStyle.iconFor(incident("RoadOrCarriagewayOrLaneManagement", "roadClosed")));

        Situation lanes = incident("AbnormalTraffic", "queuingTraffic");
        lanes.setNumberOfLanesRestricted("2");
        assertEquals(R.drawable.ic_incident_lanes, SituationStyle.iconFor(lanes));

        assertEquals(R.drawable.ic_incident_queue,
                SituationStyle.iconFor(incident("AbnormalTraffic", "queuingTraffic")));
    }

    // Regression test for the live-data finding in T22
    @Test
    public void zeroLanesOnAnObstruction_isNotShownAsRoadClosed() {
        Situation fuelNotice = incident("GeneralObstruction", "obstructionOnTheRoad");
        fuelNotice.setNumberOfOperationalLanes("0");
        assertEquals(R.drawable.ic_incident, SituationStyle.iconFor(fuelNotice));

        Situation laneManagement = incident("RoadOrCarriagewayOrLaneManagement", null);
        laneManagement.setNumberOfOperationalLanes("0");
        assertEquals(R.drawable.ic_incident_closed, SituationStyle.iconFor(laneManagement));
    }

    @Test
    public void delayColours() {
        assertEquals(R.color.delay_very_long, SituationStyle.delayColour("veryLongDelays"));
        assertEquals(R.color.delay_long, SituationStyle.delayColour("longDelays"));
        assertEquals(R.color.delay_short, SituationStyle.delayColour("delays"));
        assertEquals(R.color.delay_none, SituationStyle.delayColour(null));
    }
}