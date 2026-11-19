package org.me.gcu.mccusker_liam_s2526913.data;

import android.location.Location;

import org.me.gcu.mccusker_liam_s2526913.model.LocationPoint;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;

public final class DistanceUtils {

    private DistanceUtils() {
    }

    // Distance in miles from (lat, lon) to the nearest end of the situation,
    // or -1 if the situation has no usable location
    public static double distanceMiles(Situation s, double lat, double lon) {
        double best = -1;
        for (LocationPoint p : new LocationPoint[]{s.getFrom(), s.getTo()}) {
            if (p == null || (p.getLatitude() == 0 && p.getLongitude() == 0)) {
                continue;
            }
            float[] results = new float[1];
            Location.distanceBetween(lat, lon, p.getLatitude(), p.getLongitude(), results);
            double miles = results[0] / 1609.344;   // metres to miles
            if (best < 0 || miles < best) {
                best = miles;
            }
        }
        return best;
    }
}