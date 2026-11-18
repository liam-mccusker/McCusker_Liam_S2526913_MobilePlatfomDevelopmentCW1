package org.me.gcu.mccusker_liam_s2526913.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SituationRepository {

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SituationParser parser = new SituationParser();

    private final MutableLiveData<List<Situation>> incidents = new MutableLiveData<>();
    private final MutableLiveData<List<Situation>> roadworks = new MutableLiveData<>();
    private final MutableLiveData<String> status = new MutableLiveData<>();

    public SituationRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public LiveData<List<Situation>> getIncidents() { return incidents; }
    public LiveData<List<Situation>> getRoadworks() { return roadworks; }
    public LiveData<String> getStatus() { return status; }

    // Loads and parses both feeds on a background thread
    public void refresh() {
        executor.execute(() -> {
            status.postValue("Loading…");

            // For now this reads the sample files.
            // When DATEX II access arrives, this is where the live download goes.
            String incidentXml = loadFromAssets("unplannedContentExample.xml");
            String roadworksXml = loadFromAssets("roadworksContentExample.xml");

            incidents.postValue(parser.parse(incidentXml, "incident"));
            roadworks.postValue(parser.parse(roadworksXml, "roadworks"));

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            status.postValue("Last updated " + time);
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    private String loadFromAssets(String fileName) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            Log.e("SituationRepository", "Could not read " + fileName, e);
        }
        return sb.toString();
    }
}