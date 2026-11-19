package org.me.gcu.mccusker_liam_s2526913.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.mccusker_liam_s2526913.data.DistanceUtils;
import org.me.gcu.mccusker_liam_s2526913.data.SituationRepository;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SituationViewModel extends AndroidViewModel {

    // Reference point for distance search: GCU campus, Cowcaddens Road, Glasgow
    public static final double HOME_LAT = 55.8666;
    public static final double HOME_LON = -4.2504;

    private final SituationRepository repository;

    private final MutableLiveData<String> filter = new MutableLiveData<>("all");   // all / incident / roadworks
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> maxDistanceMiles = new MutableLiveData<>(0); // 0 = any distance
    private final MutableLiveData<Situation> selectedSituation = new MutableLiveData<>();

    // The list the screen shows, rebuilt whenever the data or any search option changes
    private final MediatorLiveData<List<Situation>> visibleSituations = new MediatorLiveData<>();

    public SituationViewModel(@NonNull Application application) {
        super(application);
        repository = new SituationRepository(application);

        visibleSituations.addSource(repository.getIncidents(), v -> updateVisible());
        visibleSituations.addSource(repository.getRoadworks(), v -> updateVisible());
        visibleSituations.addSource(filter, v -> updateVisible());
        visibleSituations.addSource(roadQuery, v -> updateVisible());
        visibleSituations.addSource(maxDistanceMiles, v -> updateVisible());

        repository.refresh();   // load data when the app starts
    }

    private void updateVisible() {
        String f = filter.getValue();
        String query = (roadQuery.getValue() == null) ? "" : roadQuery.getValue().trim();
        int maxKm = (maxDistanceMiles.getValue() == null) ? 0 : maxDistanceMiles.getValue();

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
            if (maxKm > 0) {
                double km = DistanceUtils.distanceMiles(s, HOME_LAT, HOME_LON);
                if (km < 0 || km > maxKm) {
                    continue;
                }
            }
            result.add(s);
        }

        // 3. When searching by distance, show the nearest first
        if (maxKm > 0) {
            result.sort((a, b) -> Double.compare(
                    DistanceUtils.distanceMiles(a, HOME_LAT, HOME_LON),
                    DistanceUtils.distanceMiles(b, HOME_LAT, HOME_LON)));
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
                if (normalise(part).equals(q)) {
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

    public LiveData<List<Situation>> getVisibleSituations() { return visibleSituations; }
    public LiveData<String> getFilter() { return filter; }
    public LiveData<String> getRoadQuery() { return roadQuery; }
    public LiveData<Integer> getMaxDistanceMiles() { return maxDistanceMiles; }
    public LiveData<Situation> getSelectedSituation() { return selectedSituation; }
    public LiveData<String> getStatus() { return repository.getStatus(); }

    public void setFilter(String value) { filter.setValue(value); }
    public void setRoadQuery(String value) { roadQuery.setValue(value); }
    public void setMaxDistanceMiles(int km) { maxDistanceMiles.setValue(km); }
    public void selectSituation(Situation situation) { selectedSituation.setValue(situation); }

    public void refresh() {
        repository.refresh();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}