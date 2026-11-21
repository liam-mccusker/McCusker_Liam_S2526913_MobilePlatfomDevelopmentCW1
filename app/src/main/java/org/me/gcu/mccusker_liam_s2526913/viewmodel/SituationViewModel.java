package org.me.gcu.mccusker_liam_s2526913.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.mccusker_liam_s2526913.data.DistanceUtils;
import org.me.gcu.mccusker_liam_s2526913.data.SituationRepository;
import org.me.gcu.mccusker_liam_s2526913.model.LocationPoint;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

public class SituationViewModel extends AndroidViewModel {

    // How often the data refreshes automatically.
    // Automatic updates are hourly by default; the user can change this from the menu
    private static final long DEFAULT_UPDATE_INTERVAL_MINUTES = 60;
    private long updateIntervalMinutes = DEFAULT_UPDATE_INTERVAL_MINUTES;
    private ScheduledFuture<?> autoUpdate;   // the currently scheduled repeating update

    // Fallback for distance search when the device location isn't available
    private static final LocationPoint GCU_CAMPUS =
            new LocationPoint(55.8666, -4.2504, "GCU campus");

    private final SituationRepository repository;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final MutableLiveData<String> filter = new MutableLiveData<>("all");   // all / incident / roadworks
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> maxDistanceMiles = new MutableLiveData<>(0); // 0 = any distance
    private final MutableLiveData<LocationPoint> referenceLocation = new MutableLiveData<>(GCU_CAMPUS);
    private final MutableLiveData<Situation> selectedSituation = new MutableLiveData<>();

    // The list the screen shows, rebuilt whenever the data or any search option changes
    private final MediatorLiveData<List<Situation>> visibleSituations = new MediatorLiveData<>();

    // Road names that currently have situations, for the search suggestions
    private final MediatorLiveData<List<String>> roadNames = new MediatorLiveData<>();

    // So the location permission is only asked for once per session
    private boolean deviceLocationRequested = false;

    public SituationViewModel(@NonNull Application application) {
        super(application);
        repository = new SituationRepository(application);

        visibleSituations.addSource(repository.getIncidents(), v -> updateVisible());
        visibleSituations.addSource(repository.getRoadworks(), v -> updateVisible());
        visibleSituations.addSource(filter, v -> updateVisible());
        visibleSituations.addSource(roadQuery, v -> updateVisible());
        visibleSituations.addSource(maxDistanceMiles, v -> updateVisible());
        visibleSituations.addSource(referenceLocation, v -> updateVisible());

        roadNames.addSource(repository.getIncidents(), v -> updateRoadNames());
        roadNames.addSource(repository.getRoadworks(), v -> updateRoadNames());

        scheduleUpdates(0);   // load now, then repeat at the update interval
    }

    private void updateVisible() {
        String f = filter.getValue();
        String query = (roadQuery.getValue() == null) ? "" : roadQuery.getValue().trim();
        int maxMiles = (maxDistanceMiles.getValue() == null) ? 0 : maxDistanceMiles.getValue();
        LocationPoint ref = referenceLocation.getValue();

        // 1. Category filter
        List<Situation> candidates = new ArrayList<>();
        List<Situation> incidents = repository.getIncidents().getValue();
        List<Situation> roadworks = repository.getRoadworks().getValue();
        if (incidents != null && !"roadworks".equals(f)) {
            candidates.addAll(incidents);
        }
        if (roadworks != null && !"incident".equals(f)) {
            candidates.addAll(roadworks);
        }

        // Whole-word pattern for searching descriptions, e.g. "M8" but not "M80"
        Pattern wordPattern = query.matches("[A-Za-z0-9]+")
                ? Pattern.compile("\\b" + query + "\\b", Pattern.CASE_INSENSITIVE)
                : null;

        // 2. Road and distance filters
        List<Situation> result = new ArrayList<>();
        for (Situation s : candidates) {
            if (!matchesRoad(s, query, wordPattern)) {
                continue;
            }
            if (maxMiles > 0) {
                double miles = DistanceUtils.distanceMiles(s, ref.getLatitude(), ref.getLongitude());
                if (miles < 0 || miles > maxMiles) {
                    continue;
                }
            }
            result.add(s);
        }

        // 3. When searching by distance, show the nearest first
        if (maxMiles > 0) {
            result.sort((a, b) -> Double.compare(
                    DistanceUtils.distanceMiles(a, ref.getLatitude(), ref.getLongitude()),
                    DistanceUtils.distanceMiles(b, ref.getLatitude(), ref.getLongitude())));
        }

        visibleSituations.setValue(result);
    }

    private boolean matchesRoad(Situation s, String query, Pattern wordPattern) {
        if (query.isEmpty()) {
            return true;
        }
        String q = normalise(query);
        if (s.getRoadName() != null) {
            for (String part : s.getRoadName().split("/")) {   // e.g. "M90/A90"
                if (normalise(part).equals(q) || normalise(roadNumber(part)).equals(q)) {
                    return true;
                }
            }
        }
        return wordPattern != null && s.getDescription() != null
                && wordPattern.matcher(s.getDescription()).find();
    }

    // "a74(m)" -> "A74M", so capitals and punctuation don't matter
    private static String normalise(String text) {
        return text.toUpperCase(Locale.UK).replaceAll("[^A-Z0-9]", "");
    }

    private void updateRoadNames() {
        Set<String> names = new TreeSet<>(SituationViewModel::compareRoads);
        addRoadNames(names, repository.getIncidents().getValue());
        addRoadNames(names, repository.getRoadworks().getValue());
        roadNames.setValue(new ArrayList<>(names));
    }

    private static void addRoadNames(Set<String> names, List<Situation> situations) {
        if (situations == null) {
            return;
        }
        for (Situation s : situations) {
            if (s.getRoadName() == null) {
                continue;
            }
            for (String part : s.getRoadName().split("/")) {   // e.g. "M90/A90"
                String name = roadNumber(part);
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
    }

    // Sorts roads as drivers expect: by letter, then by number (A9 before A82),
// then alphabetically for anything else (e.g. "A74(M)")
    private static final Pattern ROAD_PARTS = Pattern.compile("^([A-Za-z]+)(\\d*)(.*)$");

    private static int compareRoads(String a, String b) {
        Matcher ma = ROAD_PARTS.matcher(a);
        Matcher mb = ROAD_PARTS.matcher(b);
        if (ma.matches() && mb.matches()) {
            int letters = ma.group(1).compareToIgnoreCase(mb.group(1));
            if (letters != 0) {
                return letters;
            }
            int na = ma.group(2).isEmpty() ? 0 : Integer.parseInt(ma.group(2));
            int nb = mb.group(2).isEmpty() ? 0 : Integer.parseInt(mb.group(2));
            if (na != nb) {
                return Integer.compare(na, nb);
            }
        }
        int text = a.compareToIgnoreCase(b);
        return (text != 0) ? text : a.compareTo(b);
    }

    // Pulls the road number out of a feed road name:
    // "A726 (West Mains Road)" -> "A726", "M8MONKLANDS" -> "M8", "a74(m)" -> "A74(M)".
    // Names without a road number (e.g. a bridge name) are returned unchanged.
    private static final Pattern ROAD_NUMBER = Pattern.compile("^([A-Za-z]{1,2}\\d+(\\([A-Za-z]\\))?)");

    public static String roadNumber(String name) {
        String trimmed = name.trim();
        Matcher m = ROAD_NUMBER.matcher(trimmed);
        return m.find() ? m.group(1).toUpperCase(Locale.UK) : trimmed;
    }

    public LiveData<List<Situation>> getVisibleSituations() { return visibleSituations; }
    public LiveData<String> getFilter() { return filter; }
    public LiveData<String> getRoadQuery() { return roadQuery; }
    public LiveData<Integer> getMaxDistanceMiles() { return maxDistanceMiles; }
    public LiveData<LocationPoint> getReferenceLocation() { return referenceLocation; }
    public LiveData<Situation> getSelectedSituation() { return selectedSituation; }
    public LiveData<String> getStatus() { return repository.getStatus(); }
    public LiveData<List<String>> getRoadNames() { return roadNames; }

    public void setFilter(String value) { filter.setValue(value); }
    public void setRoadQuery(String value) { roadQuery.setValue(value); }
    public void setMaxDistanceMiles(int miles) { maxDistanceMiles.setValue(miles); }
    public void selectSituation(Situation situation) { selectedSituation.setValue(situation); }

    public void setDeviceLocation(double latitude, double longitude) {
        referenceLocation.setValue(new LocationPoint(latitude, longitude, "your location"));
    }

    // Returns true only the first time, so the user isn't asked repeatedly
    public boolean shouldRequestDeviceLocation() {
        if (deviceLocationRequested) {
            return false;
        }
        deviceLocationRequested = true;
        return true;
    }

    // Starts (or restarts) the repeating update with the current interval
    private void scheduleUpdates(long initialDelayMinutes) {
        if (autoUpdate != null) {
            autoUpdate.cancel(false);   // stop the old schedule before starting a new one
        }
        autoUpdate = scheduler.scheduleWithFixedDelay(repository::refresh,
                initialDelayMinutes, updateIntervalMinutes, TimeUnit.MINUTES);
    }

    public long getUpdateIntervalMinutes() {
        return updateIntervalMinutes;
    }

    public void setUpdateIntervalMinutes(long minutes) {
        updateIntervalMinutes = minutes;
        scheduleUpdates(minutes);   // next update one new interval from now
    }

    public void refresh() {
        repository.refresh();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        scheduler.shutdownNow();
        repository.shutdown();
    }
}