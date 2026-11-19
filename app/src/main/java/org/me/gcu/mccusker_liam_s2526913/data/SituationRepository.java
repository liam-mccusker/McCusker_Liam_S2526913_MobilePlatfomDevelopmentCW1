package org.me.gcu.mccusker_liam_s2526913.data;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SituationRepository {

    private static final String TAG = "SituationRepository";

    // true = use the sample XML files in assets (for reproducible testing)
    // false = download live data from Traffic Scotland
    private static final boolean USE_SAMPLE_DATA = false;

    private static final String INCIDENTS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    private static final String ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    // Copies of the last successful download, used when there is no connection
    private static final String INCIDENTS_CACHE = "incidents_cache.xml";
    private static final String ROADWORKS_CACHE = "roadworks_cache.xml";

    private final Context context;
    private final String clientId;
    private final String clientKey;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SituationParser parser = new SituationParser();

    private final MutableLiveData<List<Situation>> incidents = new MutableLiveData<>();
    private final MutableLiveData<List<Situation>> roadworks = new MutableLiveData<>();
    private final MutableLiveData<String> status = new MutableLiveData<>();

    public SituationRepository(Context context) {
        this.context = context.getApplicationContext();
        clientId = readMetaData("TS_CLIENT_ID");
        clientKey = readMetaData("TS_CLIENT_KEY");
    }

    public LiveData<List<Situation>> getIncidents() { return incidents; }
    public LiveData<List<Situation>> getRoadworks() { return roadworks; }
    public LiveData<String> getStatus() { return status; }

    // Loads and parses both feeds on a background thread
    public void refresh() {
        executor.execute(() -> {
            status.postValue("Loading…");

            if (USE_SAMPLE_DATA) {
                publish(readAsset("unplannedContentExample.xml"),
                        readAsset("roadworksContentExample.xml"));
                status.postValue("Sample data loaded " + now());
                return;
            }

            String incidentXml = download(INCIDENTS_URL);
            String roadworksXml = download(ROADWORKS_URL);
            boolean online = (incidentXml != null && roadworksXml != null);

            // Save fresh downloads; fall back to the saved copy when a download fails
            if (incidentXml != null) {
                saveCache(INCIDENTS_CACHE, incidentXml);
            } else {
                incidentXml = readCache(INCIDENTS_CACHE);
            }
            if (roadworksXml != null) {
                saveCache(ROADWORKS_CACHE, roadworksXml);
            } else {
                roadworksXml = readCache(ROADWORKS_CACHE);
            }

            if (incidentXml == null && roadworksXml == null) {
                status.postValue("No connection and no saved data. Check your internet connection.");
                return;
            }

            publish(incidentXml, roadworksXml);

            if (online) {
                status.postValue("Live data · updated " + now());
            } else {
                status.postValue("Offline · showing saved data from " + cacheTime());
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void publish(String incidentXml, String roadworksXml) {
        incidents.postValue(incidentXml != null
                ? parser.parse(incidentXml, "incident") : new ArrayList<>());
        roadworks.postValue(roadworksXml != null
                ? parser.parse(roadworksXml, "roadworks") : new ArrayList<>());
    }

    // ---------- Downloading ----------

    private String download(String address) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            // Authorisation header, as in the skeleton code: Base64 of "clientId:clientKey"
            String credentials = clientId + ":" + clientKey;
            connection.setRequestProperty("Authorization",
                    Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP " + code + " from " + address);
                return null;
            }

            String result;
            try (InputStream in = connection.getInputStream()) {
                result = readAll(in);
            }

            // Remove any leading characters before the XML starts (as in the skeleton)
            int start = result.indexOf("<?");
            return (start >= 0) ? result.substring(start) : null;

        } catch (IOException e) {
            Log.e(TAG, "Download failed: " + address, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ---------- Saved copies (offline support) ----------

    private void saveCache(String fileName, String xml) {
        try (FileOutputStream out = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Could not save " + fileName, e);
        }
    }

    private String readCache(String fileName) {
        if (!new File(context.getFilesDir(), fileName).exists()) {
            return null;
        }
        try (InputStream in = context.openFileInput(fileName)) {
            return readAll(in);
        } catch (IOException e) {
            Log.e(TAG, "Could not read " + fileName, e);
            return null;
        }
    }

    private String cacheTime() {
        File file = new File(context.getFilesDir(), INCIDENTS_CACHE);
        return Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.UK));
    }

    // ---------- Helpers ----------

    private String readAsset(String fileName) {
        try (InputStream in = context.getAssets().open(fileName)) {
            return readAll(in);
        } catch (IOException e) {
            Log.e(TAG, "Could not read " + fileName, e);
            return null;
        }
    }

    // Reads a whole stream into a String, using a StringBuilder (fast for large feeds)
    private String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private String readMetaData(String name) {
        try {
            Bundle meta = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
                    .metaData;
            Object value = (meta != null) ? meta.get(context.getPackageName() + "." + name) : null;
            return (value != null) ? String.valueOf(value) : "";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not read " + name, e);
            return "";
        }
    }

    private String now() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}