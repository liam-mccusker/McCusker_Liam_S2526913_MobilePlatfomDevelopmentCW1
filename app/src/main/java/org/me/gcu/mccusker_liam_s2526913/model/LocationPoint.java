package org.me.gcu.mccusker_liam_s2526913.model;

import androidx.annotation.NonNull;

public class LocationPoint {

    private double latitude;
    private double longitude;
    private String name;   // e.g. a junction name or description

    // Used by the parser, which fills in the fields one at a time
    public LocationPoint() {
    }

    // Used to create a complete point in one line (e.g. GCU campus, device location)
    public LocationPoint(double latitude, double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return "LocationPoint{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", name='" + name + '\'' +
                '}';
    }
}