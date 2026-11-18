package org.me.gcu.mccusker_liam_s2526913.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.mccusker_liam_s2526913.data.SituationRepository;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.util.ArrayList;
import java.util.List;

public class SituationViewModel extends AndroidViewModel {

    private final SituationRepository repository;

    // "all", "incident" or "roadworks"
    private final MutableLiveData<String> filter = new MutableLiveData<>("all");

    // The list the screen actually shows, rebuilt whenever data or filter changes
    private final MediatorLiveData<List<Situation>> visibleSituations = new MediatorLiveData<>();

    public SituationViewModel(@NonNull Application application) {
        super(application);
        repository = new SituationRepository(application);

        visibleSituations.addSource(repository.getIncidents(), list -> updateVisible());
        visibleSituations.addSource(repository.getRoadworks(), list -> updateVisible());
        visibleSituations.addSource(filter, value -> updateVisible());

        repository.refresh();   // load data when the app starts
    }

    private void updateVisible() {
        String f = filter.getValue();
        List<Situation> incidents = repository.getIncidents().getValue();
        List<Situation> roadworks = repository.getRoadworks().getValue();

        List<Situation> result = new ArrayList<>();
        if (incidents != null && !"roadworks".equals(f)) {
            result.addAll(incidents);
        }
        if (roadworks != null && !"incident".equals(f)) {
            result.addAll(roadworks);
        }
        visibleSituations.setValue(result);
    }

    public LiveData<List<Situation>> getVisibleSituations() { return visibleSituations; }
    public LiveData<String> getFilter() { return filter; }
    public LiveData<String> getStatus() { return repository.getStatus(); }

    public void setFilter(String value) {
        filter.setValue(value);
    }

    public void refresh() {
        repository.refresh();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}