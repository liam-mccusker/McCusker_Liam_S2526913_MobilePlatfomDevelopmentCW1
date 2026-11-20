package org.me.gcu.mccusker_liam_s2526913.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import org.me.gcu.mccusker_liam_s2526913.R;
import org.me.gcu.mccusker_liam_s2526913.model.LocationPoint;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

public class SituationListFragment extends Fragment {

    // Matches the order of the "distance_options" list in strings.xml
    private static final int[] DISTANCE_MILES = {0, 1, 5, 10, 25, 50};

    // A saved location older than this is ignored and a fresh one is requested
    private static final long MAX_LOCATION_AGE_MS = 10 * 60 * 1000;   // 10 minutes

    private SituationViewModel viewModel;

    // Asks the user for location permission, then finds the device location if allowed
    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    findDeviceLocation();
                } else {
                    showLocationUnavailable();
                }
            });

    public SituationListFragment() {
        super(R.layout.fragment_situation_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Share the Activity's ViewModel, so the fragment sees the same data
        viewModel = new ViewModelProvider(requireActivity()).get(SituationViewModel.class);

        setUpList(view);
        setUpFilterButtons(view);
        setUpRoadSearch(view);
        setUpDistanceSpinner(view);
    }

    private void setUpList(View view) {
        RecyclerView recycler = view.findViewById(R.id.situationRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        SituationAdapter adapter = new SituationAdapter(situation -> {
            viewModel.selectSituation(situation);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SituationDetailsFragment())
                    .addToBackStack(null)
                    .commit();
        });
        recycler.setAdapter(adapter);

        TextView countText = view.findViewById(R.id.listCount);
        viewModel.getVisibleSituations().observe(getViewLifecycleOwner(), list -> {
            Integer miles = viewModel.getMaxDistanceMiles().getValue();
            boolean nearSearch = (miles != null && miles > 0);
            LocationPoint reference = viewModel.getReferenceLocation().getValue();

            adapter.setDistanceFrom(nearSearch ? reference : null);
            adapter.setSituations(list);

            // getQuantityString picks "situation" or "situations" based on the number
            if (nearSearch && reference != null) {
                countText.setText(getResources().getQuantityString(R.plurals.list_count_near,
                        list.size(), list.size(), miles, reference.getName()));
            } else {
                countText.setText(getResources().getQuantityString(R.plurals.list_count,
                        list.size(), list.size()));
            }
        });
    }

    private void setUpFilterButtons(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.filterToggle);
        String current = viewModel.getFilter().getValue();
        if ("incident".equals(current)) {
            toggle.check(R.id.filterIncidents);
        } else if ("roadworks".equals(current)) {
            toggle.check(R.id.filterRoadworks);
        } else {
            toggle.check(R.id.filterAll);
        }

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.filterIncidents) {
                viewModel.setFilter("incident");
            } else if (checkedId == R.id.filterRoadworks) {
                viewModel.setFilter("roadworks");
            } else {
                viewModel.setFilter("all");
            }
        });
    }

    private void setUpRoadSearch(View view) {
        EditText search = view.findViewById(R.id.roadSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setRoadQuery(s.toString());   // list updates as the user types
            }
        });
    }

    private void setUpDistanceSpinner(View view) {
        Spinner spinner = view.findViewById(R.id.distanceSpinner);
        ArrayAdapter<CharSequence> options = ArrayAdapter.createFromResource(
                requireContext(), R.array.distance_options, android.R.layout.simple_spinner_item);
        options.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(options);

        // Show the saved choice (e.g. after rotation or coming back from details)
        Integer currentMiles = viewModel.getMaxDistanceMiles().getValue();
        for (int i = 0; i < DISTANCE_MILES.length; i++) {
            if (currentMiles != null && DISTANCE_MILES[i] == currentMiles) {
                spinner.setSelection(i);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                viewModel.setMaxDistanceMiles(DISTANCE_MILES[position]);
                if (DISTANCE_MILES[position] > 0 && viewModel.shouldRequestDeviceLocation()) {
                    requestDeviceLocation();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // ---------- Device location (falls back to GCU campus) ----------

    private void requestDeviceLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            findDeviceLocation();
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")   // only called once permission has been granted
    private void findDeviceLocation() {
        LocationManager manager =
                (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        // GPS first (on the emulator this is the one "Set location" changes), then network
        String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};

        // 1. Use a recent location the phone already knows, if there is one
        for (String provider : providers) {
            try {
                Location location = manager.getLastKnownLocation(provider);
                if (location != null
                        && System.currentTimeMillis() - location.getTime() < MAX_LOCATION_AGE_MS) {
                    useDeviceLocation(location);
                    return;
                }
            } catch (SecurityException | IllegalArgumentException ignored) {
                // provider not allowed with coarse permission, or not on this device
            }
        }

        // 2. Otherwise ask for one fresh location update
        for (String provider : providers) {
            try {
                if (!manager.isProviderEnabled(provider)) {
                    continue;
                }
                manager.requestLocationUpdates(provider, 0, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        manager.removeUpdates(this);
                        useDeviceLocation(location);
                    }

                    // Needed on older Android versions
                    @Override
                    public void onStatusChanged(String p, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String p) {
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String p) {
                    }
                }, Looper.getMainLooper());
                return;
            } catch (SecurityException | IllegalArgumentException ignored) {
                // try the next provider
            }
        }

        showLocationUnavailable();
    }

    private void useDeviceLocation(Location location) {
        Log.d("Location", "Using device location: "
                + location.getLatitude() + ", " + location.getLongitude());
        viewModel.setDeviceLocation(location.getLatitude(), location.getLongitude());
    }

    private void showLocationUnavailable() {
        if (isAdded()) {
            Toast.makeText(requireContext(), R.string.location_unavailable, Toast.LENGTH_LONG).show();
        }
    }
}