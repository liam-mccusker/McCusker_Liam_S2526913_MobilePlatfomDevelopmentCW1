package org.me.gcu.mccusker_liam_s2526913.model;

import androidx.annotation.NonNull;

public class Situation {

    // Which feed it came from
    private String category;        // "incident" or "roadworks"

    // Identity and type
    private String id;              // id attribute on <situation>
    private String recordType;      // xsi:type on <situationRecord>, e.g. MaintenanceWorks
    private String subType;         // roadMaintenanceType / abnormalTrafficType /
    // roadOrCarriagewayOrLaneManagementType
    private String description;     // <value> inside <comment>

    // Validity
    private String startTime;       // <overallStartTime>
    private String endTime;         // <overallEndTime> (roadworks only)

    // Impact (often missing, so these can be null)
    private String delaysType;              // delays / longDelays / veryLongDelays
    private String numberOfOperationalLanes;
    private String numberOfLanesRestricted;

    // Location
    private String direction;       // <tpegDirection>, e.g. southBound
    private String roadName;        // linkName (roadworks) or ilc descriptor (incidents)
    private LocationPoint from;
    private LocationPoint to;

    // Extras
    private String complianceOption;   // incidents, e.g. mandatory
    private String urgentRoadworks;    // roadworks, true/false

    public Situation() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType;
    }

    public String getNumberOfOperationalLanes() {
        return numberOfOperationalLanes;
    }

    public void setNumberOfOperationalLanes(String numberOfOperationalLanes) {
        this.numberOfOperationalLanes = numberOfOperationalLanes;
    }

    public String getNumberOfLanesRestricted() {
        return numberOfLanesRestricted;
    }

    public void setNumberOfLanesRestricted(String numberOfLanesRestricted) {
        this.numberOfLanesRestricted = numberOfLanesRestricted;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public LocationPoint getFrom() {
        return from;
    }

    public void setFrom(LocationPoint from) {
        this.from = from;
    }

    public LocationPoint getTo() {
        return to;
    }

    public void setTo(LocationPoint to) {
        this.to = to;
    }

    public String getComplianceOption() {
        return complianceOption;
    }

    public void setComplianceOption(String complianceOption) {
        this.complianceOption = complianceOption;
    }

    public String getUrgentRoadworks() {
        return urgentRoadworks;
    }

    public void setUrgentRoadworks(String urgentRoadworks) {
        this.urgentRoadworks = urgentRoadworks;
    }

    // Turns e.g. "roadClosed" into "Road closed" for display.
    // Uses the sub-type if there is one, then the record type (e.g. "Accident"),
    // and only then the category, so unfamiliar live record types still get a meaningful label.
    public String getDisplayType() {
        String raw = subType;
        if (raw == null || raw.isEmpty()) {
            raw = recordType;
        }
        if (raw == null || raw.isEmpty()) {
            raw = category;
        }
        if (raw == null || raw.isEmpty()) {
            return "Unknown";
        }
        String spaced = raw.replaceAll("([A-Z])", " $1").trim().toLowerCase();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    @NonNull
    @Override
    public String toString() {
        return "Situation{" +
                "category='" + category + '\'' +
                ", id='" + id + '\'' +
                ", recordType='" + recordType + '\'' +
                ", subType='" + subType + '\'' +
                ", description='" + description + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", delaysType='" + delaysType + '\'' +
                ", numberOfOperationalLanes='" + numberOfOperationalLanes + '\'' +
                ", numberOfLanesRestricted='" + numberOfLanesRestricted + '\'' +
                ", direction='" + direction + '\'' +
                ", roadName='" + roadName + '\'' +
                ", from=" + from +
                ", to=" + to +
                ", complianceOption='" + complianceOption + '\'' +
                ", urgentRoadworks='" + urgentRoadworks + '\'' +
                '}';
    }
}