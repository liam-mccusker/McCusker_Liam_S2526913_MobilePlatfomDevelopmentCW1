package org.me.gcu.mccusker_liam_s2526913.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.me.gcu.mccusker_liam_s2526913.R;
import org.me.gcu.mccusker_liam_s2526913.data.DistanceUtils;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    public interface OnSituationClickListener {
        void onSituationClick(Situation situation);
    }

    private final List<Situation> situations = new ArrayList<>();
    private final OnSituationClickListener listener;

    // True while a distance search is active, so each row shows how far away it is
    private boolean showDistance = false;

    public SituationAdapter(OnSituationClickListener listener) {
        this.listener = listener;
    }

    public void setShowDistance(boolean show) {
        showDistance = show;
    }

    public void setSituations(List<Situation> newList) {
        situations.clear();
        if (newList != null) {
            situations.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_situation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Situation s = situations.get(position);

        // Type, plus the distance when searching by distance
        String typeText = s.getDisplayType();
        if (showDistance) {
            double miles = DistanceUtils.distanceMiles(s,
                    SituationViewModel.HOME_LAT, SituationViewModel.HOME_LON);
            if (miles >= 0) {
                typeText += String.format(Locale.UK, " · %.1f mi", miles);
            }
        }
        holder.type.setText(typeText);
        holder.description.setText(s.getDescription());

        // Icon based on the type of situation
        holder.icon.setImageResource(SituationStyle.iconFor(s));

        // Colour-coded delay label, for roadworks only
        if ("roadworks".equals(s.getCategory())) {
            holder.delay.setVisibility(View.VISIBLE);
            holder.delay.setText(SituationStyle.delayLabel(s.getDelaysType()));
            holder.delay.setBackgroundColor(ContextCompat.getColor(
                    holder.itemView.getContext(), SituationStyle.delayColour(s.getDelaysType())));
        } else {
            holder.delay.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onSituationClick(s));
    }

    @Override
    public int getItemCount() {
        return situations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView type;
        final TextView description;
        final TextView delay;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.itemIcon);
            type = itemView.findViewById(R.id.itemType);
            description = itemView.findViewById(R.id.itemDescription);
            delay = itemView.findViewById(R.id.itemDelay);
        }
    }
}