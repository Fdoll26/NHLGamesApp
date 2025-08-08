package com.example.nhlapp.Adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.R;

import java.io.File;
import java.util.List;

public class TeamsAdapter extends RecyclerView.Adapter<TeamsAdapter.TeamViewHolder> {
    private List<Team> teams;
    private Context context;

    public TeamsAdapter(List<Team> teams, Context context) {
        this.teams = teams;
        this.context = context;
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_team, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = teams.get(position);
        holder.teamName.setText(team.getName());

        // Load team logo if available
        if (team.getLogoPath() != null && !team.getLogoPath().isEmpty()) {
            File logoFile = new File(team.getLogoPath());
            if (logoFile.exists()) {
                holder.teamLogo.setImageBitmap(BitmapFactory.decodeFile(team.getLogoPath()));
            } else {
                holder.teamLogo.setImageResource(R.drawable.ic_team_placeholder);
            }
        } else {
            holder.teamLogo.setImageResource(R.drawable.ic_team_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    static class TeamViewHolder extends RecyclerView.ViewHolder {
        ImageView teamLogo;
        TextView teamName;

        public TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            teamLogo = itemView.findViewById(R.id.teamLogo);
            teamName = itemView.findViewById(R.id.teamName);
        }
    }
}