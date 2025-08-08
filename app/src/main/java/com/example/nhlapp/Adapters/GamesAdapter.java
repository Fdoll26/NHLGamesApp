package com.example.nhlapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.R;

import java.util.List;

public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.GameViewHolder> {
    private List<Game> games;
    private Context context;

    public GamesAdapter(List<Game> games, Context context) {
        this.games = games;
        this.context = context;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = games.get(position);
        holder.gameDate.setText(game.getGameDate());
        holder.gameTeams.setText(game.getHomeTeamName() + " @ Team " + game.getAwayTeamName());

        if (game.getHomeScore() > 0 || game.getAwayScore() > 0) {
            holder.gameScore.setText(game.getHomeScore() + " - " + game.getAwayScore());
            holder.gameScore.setVisibility(View.VISIBLE);
        } else {
            holder.gameScore.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        TextView gameDate, gameTeams, gameScore;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameDate = itemView.findViewById(R.id.gameDate);
            gameTeams = itemView.findViewById(R.id.gameTeams);
            gameScore = itemView.findViewById(R.id.gameScore);
        }
    }
}