package com.example.nhlapp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nhlapp.R;
import java.util.List;

public class GeneralOverviewAdapter extends RecyclerView.Adapter<GeneralOverviewAdapter.OverviewViewHolder> {

    private List<String> overviewData;

    public GeneralOverviewAdapter(List<String> overviewData) {
        this.overviewData = overviewData;
    }

    @NonNull
    @Override
    public OverviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_overview_text, parent, false);
        return new OverviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OverviewViewHolder holder, int position) {
        String text = overviewData.get(position);
        holder.textView.setText(text);

        // Style headers differently
        if (text.equals("GAME SUMMARY") || text.equals("GAME STATISTICS") || text.equals("SEASON STATISTICS")) {
            holder.textView.setTextSize(18f);
            holder.textView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (text.contains("────────")) {
            holder.textView.setTextSize(12f);
            holder.textView.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else if (text.isEmpty()) {
            // Empty line for spacing - make it smaller
            holder.textView.setTextSize(8f);
        } else {
            holder.textView.setTextSize(14f);
            holder.textView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Use monospace font for stats formatting
        if (text.contains("Final Score:") || text.contains("Game Stats") ||
                text.contains("Season Stats") || text.matches(".*\\d+.*\\d+.*")) {
            holder.textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        }
    }

    @Override
    public int getItemCount() {
        return overviewData != null ? overviewData.size() : 0;
    }

    public void updateData(List<String> newData) {
        this.overviewData = newData;
        notifyDataSetChanged();
    }

    static class OverviewViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public OverviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.overviewText);
        }
    }
}