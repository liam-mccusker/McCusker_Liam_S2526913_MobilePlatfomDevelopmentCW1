package org.me.gcu.mccusker_liam_s2526913;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

// Feed road names are tidied to road numbers for suggestions and searching
public class RoadNumberTest {

    @Test
    public void descriptionsAndSuffixesAreRemoved() {
        assertEquals("A726", SituationViewModel.roadNumber("A726 (West Mains Road)"));
        assertEquals("M8", SituationViewModel.roadNumber("M8MONKLANDS"));
        assertEquals("M8", SituationViewModel.roadNumber(" M8 "));
    }

    @Test
    public void motorwayStandardRoadsKeepTheirSuffix() {
        assertEquals("A74(M)", SituationViewModel.roadNumber("A74(M)"));
        assertEquals("A823(M)", SituationViewModel.roadNumber("a823(m)"));
    }

    @Test
    public void namesWithoutANumberAreUnchanged() {
        assertEquals("Kessock Bridge", SituationViewModel.roadNumber("Kessock Bridge"));
    }
}