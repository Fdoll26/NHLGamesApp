package com.example.nhlapp.Adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.R;

import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {
    private List<String> dates;
    private OnDateClickListener listener;
    private int selectedPosition = 1; // Default to today

    public interface OnDateClickListener {
        void onDateClick(String date);
    }

    public DateAdapter(List<String> dates, OnDateClickListener listener) {
        this.dates = dates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String selectedDate = dates.get(position);
        holder.dayText.setText(selectedDate.split("-")[2]);
        holder.monthText.setText(selectedDate.split("-")[1]);
        //TODO:
        // Need to rework this "date" to try and get out the month and day,
        // Might make custom day class that shows the games so it can be
        // easier to programically load an dstore the games and days

        // Highlight selected date
        if (position == selectedPosition) {
            holder.dayText.setBackgroundResource(R.drawable.selected_date_background);
            holder.monthText.setBackgroundResource(R.drawable.selected_date_background);
        } else {
            holder.dayText.setBackgroundResource(R.drawable.date_background);
            holder.monthText.setBackgroundResource(R.drawable.date_background);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);
            listener.onDateClick(selectedDate);
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView dayText;
        TextView monthText;

        DateViewHolder(View itemView) {
            super(itemView);
            monthText = itemView.findViewById(R.id.tv_month);
            dayText = itemView.findViewById(R.id.tv_day);
        }
    }
}