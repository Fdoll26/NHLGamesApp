package com.example.nhlapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.DataManager;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.Team;
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
        holder.homeName.setText(game.getHomeTeam().getAbreviatedName());
        holder.awayName.setText(game.getAwayTeam().getAbreviatedName());

        if (game.getHomeScore() > 0 || game.getAwayScore() > 0) {
            holder.gameScore.setText(String.format("%d - %d", game.getHomeScore(), game.getAwayScore()));
            holder.gameScore.setVisibility(View.VISIBLE);
        } else {
            holder.gameScore.setVisibility(View.GONE);
        }
//        DataManager dataManager = DataManager.getInstance();
//        if (game.getHomeTeam() != null) {
//            Team homeTeam = dataManager.getTeamById(game.getHomeTeamId());
//            if (homeTeam != null && holder.homeTeamLogo != null) {
//                imageHelper.loadTeamLogo(homeTeam, holder.homeTeamLogo, R.drawable.ic_hockey_placeholder);
//            }
//        }
//
//// Load away team logo
//        if (game.getAwayTeam() != null) {
//            Team awayTeam = dataManager.getTeamById(game.getAwayTeamId());
//            if (awayTeam != null && holder.awayTeamLogo != null) {
//                imageHelper.loadTeamLogo(awayTeam, holder.awayTeamLogo, R.drawable.ic_hockey_placeholder);
//            }
//        }
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        TextView gameDate, homeName, awayName, gameScore;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameDate = itemView.findViewById(R.id.gameTime);
            homeName = itemView.findViewById(R.id.homeTeamName);
            awayName = itemView.findViewById(R.id.awayTeamName);
            gameScore = itemView.findViewById(R.id.gameScore);
        }
    }
}