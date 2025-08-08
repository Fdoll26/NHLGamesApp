package com.example.nhlapp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.R;

import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {
    private List<Game> games;
    private OnGameClickListener listener;

    public interface OnGameClickListener {
        void onGameClick(Game game);
    }

    public GameAdapter(List<Game> games, OnGameClickListener listener) {
        this.games = games;
        this.listener = listener;
    }

    @Override
    public GameViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_single_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GameViewHolder holder, int position) {
        Game game = games.get(position);
        holder.bind(game);
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    class GameViewHolder extends RecyclerView.ViewHolder {
        private TextView awayTeamText;
        private TextView homeTeamText;
        private ImageView awayTeamLogo;
        private ImageView homeTeamLogo;
        private TextView scoreAwayText;
        private TextView scoreHomeText;
        private TextView statusText;

        private TextView awayTeamRecord;
        private TextView homeTeamRecord;

        public GameViewHolder(View itemView) {
            super(itemView);

            awayTeamText = itemView.findViewById(R.id.tv_away_name);
            homeTeamText = itemView.findViewById(R.id.tv_home_name);

            awayTeamLogo = itemView.findViewById(R.id.iv_away_logo);
            homeTeamLogo = itemView.findViewById(R.id.iv_home_logo);

            awayTeamRecord = itemView.findViewById(R.id.tv_away_record);
            homeTeamRecord = itemView.findViewById(R.id.tv_home_record);

            scoreAwayText = itemView.findViewById(R.id.tv_away_score);
            scoreHomeText = itemView.findViewById(R.id.tv_home_score);
            statusText = itemView.findViewById(R.id.tv_game_status);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGameClick(games.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Game game) {
//            awayTeamText.setText(game.getAwayTeam().name);
//            homeTeamText.setText(game.getHomeTeam().name);
            awayTeamText.setText(game.getAwayTeamName());
            homeTeamText.setText(game.getHomeTeamName());
            scoreHomeText.setText(game.getHomeScore());
            scoreAwayText.setText(game.getAwayScore());
            statusText.setText(game.getTimeStatus());
        }
    }
}

