package org.me.gcu.mccusker_liam_s2526913.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import org.me.gcu.mccusker_liam_s2526913.R;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

public class SituationListFragment extends Fragment {

    // Matches the order of the "distance_options" list in strings.xml
    private static final int[] DISTANCE_MILES = {0, 1, 5, 10, 25, 50};

    private SituationViewModel viewModel;

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

            adapter.setShowDistance(nearSearch);
            adapter.setSituations(list);

            // getQuantityString picks "situation" or "situations" based on the number
            if (nearSearch) {
                countText.setText(getResources().getQuantityString(R.plurals.list_count_near,
                        list.size(), list.size(), miles, "GCU campus"));
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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}