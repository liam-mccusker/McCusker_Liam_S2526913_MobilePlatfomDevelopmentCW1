package org.me.gcu.mccusker_liam_s2526913.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

    private SituationViewModel viewModel;

    public SituationListFragment() {
        super(R.layout.fragment_situation_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Share the Activity's ViewModel, so the fragment sees the same data
        viewModel = new ViewModelProvider(requireActivity()).get(SituationViewModel.class);

        // The list
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

        // Show the list and how many items are in it
        TextView countText = view.findViewById(R.id.listCount);
        viewModel.getVisibleSituations().observe(getViewLifecycleOwner(), list -> {
            adapter.setSituations(list);
            countText.setText(getString(R.string.list_count, list.size()));
        });

        // The filter buttons: show the saved choice, then listen for changes
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
}