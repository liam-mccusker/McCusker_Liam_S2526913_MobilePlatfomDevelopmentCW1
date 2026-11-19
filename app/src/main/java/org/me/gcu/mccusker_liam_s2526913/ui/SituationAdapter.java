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
import org.me.gcu.mccusker_liam_s2526913.model.Situation;

import java.util.ArrayList;
import java.util.List;

public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    public interface OnSituationClickListener {
        void onSituationClick(Situation situation);
    }

    private final List<Situation> situations = new ArrayList<>();
    private final OnSituationClickListener listener;

    public SituationAdapter(OnSituationClickListener listener) {
        this.listener = listener;
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
        holder.type.setText(s.getDisplayType());
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