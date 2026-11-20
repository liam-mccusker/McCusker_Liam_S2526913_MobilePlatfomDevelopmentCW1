package org.me.gcu.mccusker_liam_s2526913.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.mccusker_liam_s2526913.R;
import org.me.gcu.mccusker_liam_s2526913.model.LocationPoint;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

import java.util.Locale;

public class SituationDetailsFragment extends Fragment {

    private LinearLayout rows;

    public SituationDetailsFragment() {
        super(R.layout.fragment_situation_details);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SituationViewModel viewModel =
                new ViewModelProvider(requireActivity()).get(SituationViewModel.class);

        TextView type = view.findViewById(R.id.detailType);
        TextView category = view.findViewById(R.id.detailCategory);
        TextView description = view.findViewById(R.id.detailDescription);
        rows = view.findViewById(R.id.detailRows);
        ImageView icon = view.findViewById(R.id.detailIcon);

        viewModel.getSelectedSituation().observe(getViewLifecycleOwner(), s -> {
            if (s == null) {
                return;
            }
            type.setText(s.getDisplayType());
            icon.setImageResource(SituationStyle.iconFor(s));
            category.setText("roadworks".equals(s.getCategory()) ? "Roadworks" : "Incident");
            description.setText(s.getDescription());

            rows.removeAllViews();

            addHeader("When");
            addRow("Starts", DisplayFormat.dateTime(s.getStartTime()));
            addRow("Ends", DisplayFormat.dateTime(s.getEndTime()));

            addHeader("Impact");
            addRow("Delays", DisplayFormat.readable(s.getDelaysType()));
            addRow("Lanes open", s.getNumberOfOperationalLanes());
            addRow("Lanes restricted", s.getNumberOfLanesRestricted());
            addRow("Compliance", DisplayFormat.readable(s.getComplianceOption()));
            addRow("Urgent", yesNo(s.getUrgentRoadworks()));

            addHeader("Location");
            addRow("Road", s.getRoadName());
            addRow("Direction", DisplayFormat.readable(s.getDirection()));
            addPoint("From", s.getFrom());
            addPoint("To", s.getTo());

            addHeader("Reference");
            addRow("ID", s.getId());
        });
    }

    private void addHeader(String text) {
        TextView header = new TextView(requireContext());
        header.setText(text);
        header.setTextSize(18);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(0, dp(16), 0, dp(4));
        rows.addView(header);
    }

    // Adds a label/value row, but only if there is a value to show
    private void addRow(String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_detail_row, rows, false);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value);
        rows.addView(row);
    }

    private void addPoint(String label, LocationPoint point) {
        if (point == null) {
            return;
        }
        String coords = String.format(Locale.UK, "%.5f, %.5f",
                point.getLatitude(), point.getLongitude());
        String text = (point.getName() != null) ? point.getName() + "\n" + coords : coords;
        addRow(label, text);
    }

    private String yesNo(String value) {
        if ("true".equals(value)) return "Yes";
        if ("false".equals(value)) return "No";
        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}