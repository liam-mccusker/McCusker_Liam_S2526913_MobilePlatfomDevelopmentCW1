package org.me.gcu.mccusker_liam_s2526913.data;

import android.util.Log;

import org.me.gcu.mccusker_liam_s2526913.model.LocationPoint;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SituationParser {

    // DATEX II puts the specific type of each kind of record in a different tag
    // (e.g. GeneralObstruction -> obstructionType, Accident -> accidentType).
    // Any of these found inside a record is stored as the situation's sub-type.
    private static final Set<String> SUB_TYPE_TAGS = new HashSet<>(Arrays.asList(
            "roadMaintenanceType", "abnormalTrafficType", "roadOrCarriagewayOrLaneManagementType",
            "accidentType", "obstructionType", "vehicleObstructionType",
            "environmentalObstructionType", "animalPresenceType", "infrastructureDamageType",
            "poorEnvironmentType", "nonWeatherRelatedRoadConditionType",
            "weatherRelatedRoadConditionType", "roadsideServiceDisruptionType",
            "disturbanceActivityType", "publicEventType", "authorityOperationType",
            "constructionWorkType", "equipmentOrSystemFaultType", "transitServiceType"));

    public ArrayList<Situation> parse(String xml, String category) {
        ArrayList<Situation> situations = new ArrayList<>();
        Situation current = null;

        // Tracking where we are inside a situation
        boolean inComment = false;          // true while inside <comment>
        LocationPoint currentPoint = null;  // the <from> or <to> point we're inside
        String lastValue = null;            // last <value> read inside a point

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();

                    if (tag.equals("situation")) {
                        current = new Situation();
                        current.setCategory(category);
                        current.setId(parser.getAttributeValue(null, "id"));
                    } else if (current != null && tag.equals("situationRecord")) {
                        current.setRecordType(parser.getAttributeValue(null, "xsi:type"));
                    } else if (current != null && tag.equals("overallStartTime")) {
                        current.setStartTime(parser.nextText());
                    } else if (current != null && tag.equals("overallEndTime")) {
                        current.setEndTime(parser.nextText());
                    } else if (current != null && tag.equals("delaysType")) {
                        current.setDelaysType(parser.nextText());
                    } else if (current != null && tag.equals("numberOfOperationalLanes")) {
                        current.setNumberOfOperationalLanes(parser.nextText());
                    } else if (current != null && tag.equals("numberOfLanesRestricted")) {
                        current.setNumberOfLanesRestricted(parser.nextText());
                    } else if (current != null && tag.equals("tpegDirection")) {
                        current.setDirection(parser.nextText());
                    } else if (current != null && tag.equals("complianceOption")) {
                        current.setComplianceOption(parser.nextText());
                    } else if (current != null && tag.equals("urgentRoadworks")) {
                        current.setUrgentRoadworks(parser.nextText());
                    } else if (current != null && SUB_TYPE_TAGS.contains(tag)) {
                        current.setSubType(parser.nextText());

                        // ---- Description ----
                    } else if (current != null && tag.equals("comment")) {
                        inComment = true;

                        // ---- From / to points ----
                    } else if (current != null && (tag.equals("from") || tag.equals("to"))) {
                        currentPoint = new LocationPoint();
                        if (tag.equals("from")) {
                            current.setFrom(currentPoint);
                        } else {
                            current.setTo(currentPoint);
                        }
                    } else if (currentPoint != null && tag.equals("latitude")) {
                        currentPoint.setLatitude(toDouble(parser.nextText()));
                    } else if (currentPoint != null && tag.equals("longitude")) {
                        currentPoint.setLongitude(toDouble(parser.nextText()));

                        // ---- <value> means different things in different places ----
                    } else if (current != null && tag.equals("value")) {
                        String text = parser.nextText().trim();
                        if (inComment) {
                            current.setDescription(text);
                        } else if (currentPoint != null) {
                            lastValue = text;   // meaning decided by the type tag that follows
                        }

                        // ---- The type tags that say what lastValue was ----
                    } else if (current != null && currentPoint != null
                            && (tag.equals("tpegOtherPointDescriptorType")
                            || tag.equals("tpegIlcPointDescriptorType"))) {
                        String type = parser.nextText();
                        if (type.equals("linkName") || type.equals("tpegIlcName1")) {
                            if (current.getRoadName() == null) {
                                current.setRoadName(lastValue);
                            }
                        } else if (type.equals("nonLinkedPointName")
                                || type.equals("intersectionName")) {
                            currentPoint.setName(lastValue);
                        }
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    String tag = parser.getName();

                    if (tag.equals("comment")) {
                        inComment = false;
                    } else if (tag.equals("from") || tag.equals("to")) {
                        currentPoint = null;
                        lastValue = null;
                    } else if (tag.equals("situation") && current != null) {
                        situations.add(current);
                        current = null;
                    }
                }

                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e("SituationParser", "Parsing failed", e);
        }

        return situations;
    }

    // Converts text to a number without crashing the whole parse if a value is bad
    private double toDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            Log.w("SituationParser", "Bad coordinate: " + text);
            return 0.0;
        }
    }
}