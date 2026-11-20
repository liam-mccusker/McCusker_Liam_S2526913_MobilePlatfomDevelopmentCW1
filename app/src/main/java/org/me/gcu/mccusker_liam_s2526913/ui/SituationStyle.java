package org.me.gcu.mccusker_liam_s2526913.ui;

import org.me.gcu.mccusker_liam_s2526913.R;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.util.Locale;

// Decides which icon and delay colour each situation gets
public final class SituationStyle {

    private SituationStyle() {
    }

    public static int iconFor(Situation s) {
        String desc = (s.getDescription() == null)
                ? "" : s.getDescription().toLowerCase(Locale.UK);

        if ("roadworks".equals(s.getCategory())) {
            if (desc.contains("lane closure")) {
                return R.drawable.ic_roadworks_lanes;
            }
            if (desc.contains("closure")) {
                return R.drawable.ic_roadworks_closure;
            }
            if (desc.contains("lights") || desc.contains("signals")
                    || desc.matches(".*\\bttl\\b.*")) {
                return R.drawable.ic_roadworks_lights;
            }
            return R.drawable.ic_roadworks;
        }

        // Incidents.
        // 0 open lanes only reliably means "closed" on lane management records;
        // other records (e.g. a fuel notice) can report 0 lanes without the road being closed
        boolean laneManagement = "RoadOrCarriagewayOrLaneManagement".equals(s.getRecordType());
        if ("roadClosed".equals(s.getSubType())
                || (laneManagement && "0".equals(s.getNumberOfOperationalLanes()))) {
            return R.drawable.ic_incident_closed;
        }
        if (s.getNumberOfLanesRestricted() != null) {
            return R.drawable.ic_incident_lanes;
        }
        if ("queuingTraffic".equals(s.getSubType())) {
            return R.drawable.ic_incident_queue;
        }
        return R.drawable.ic_incident;
    }

    public static int delayColour(String delaysType) {
        if ("veryLongDelays".equals(delaysType)) return R.color.delay_very_long;
        if ("longDelays".equals(delaysType)) return R.color.delay_long;
        if ("delays".equals(delaysType)) return R.color.delay_short;
        return R.color.delay_none;
    }

    public static String delayLabel(String delaysType) {
        String text = DisplayFormat.readable(delaysType);
        return (text != null) ? text : "No delay info";
    }
}