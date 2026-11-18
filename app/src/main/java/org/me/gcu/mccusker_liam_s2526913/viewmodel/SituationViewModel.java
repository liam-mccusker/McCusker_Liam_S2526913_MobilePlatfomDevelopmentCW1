package org.me.gcu.mccusker_liam_s2526913.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.me.gcu.mccusker_liam_s2526913.data.SituationRepository;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.util.List;

public class SituationViewModel extends AndroidViewModel {

    private final SituationRepository repository;

    public SituationViewModel(@NonNull Application application) {
        super(application);
        repository = new SituationRepository(application);
        repository.refresh();   // load data when the app starts
    }

    public LiveData<List<Situation>> getIncidents() { return repository.getIncidents(); }
    public LiveData<List<Situation>> getRoadworks() { return repository.getRoadworks(); }
    public LiveData<String> getStatus() { return repository.getStatus(); }

    public void refresh() {
        repository.refresh();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}