package com.example.nhlapp.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.DataManager;
import com.example.nhlapp.ImageHelper;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {
    private static final String TAG = "GameAdapter";

    private List<Game> games;
    private Context context;
    private OnGameClickListener onGameClickListener;
    private ImageHelper imageHelper;
    private DataManager dataManager;

    public interface OnGameClickListener {
        void onGameClick(Game game);
    }

    public GameAdapter(List<Game> games, OnGameClickListener listener) {
        this.games = games;
        this.onGameClickListener = listener;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        imageHelper = ImageHelper.getInstance(context);
        dataManager = DataManager.getInstance();

        View view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = games.get(position);
        holder.bind(game);
    }

    @Override
    public int getItemCount() {
        return games != null ? games.size() : 0;
    }

    public void updateGames(List<Game> newGames) {
        this.games = newGames;
        notifyDataSetChanged();
    }

    class GameViewHolder extends RecyclerView.ViewHolder {
        private ImageView awayTeamLogo;
        private ImageView homeTeamLogo;
        private TextView awayTeamName;
        private TextView homeTeamName;
//        private TextView awayScore;
//        private TextView homeScore;
        private TextView score;
        private TextView gameTime;
//        private TextView gameStatus;
        private View vsIndicator;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);

            awayTeamLogo = itemView.findViewById(R.id.awayTeamLogo);
            homeTeamLogo = itemView.findViewById(R.id.homeTeamLogo);
            awayTeamName = itemView.findViewById(R.id.awayTeamName);
            homeTeamName = itemView.findViewById(R.id.homeTeamName);
            score = itemView.findViewById(R.id.gameScore);
//            homeScore = itemView.findViewById(R.id.text_home_score);
            gameTime = itemView.findViewById(R.id.gameTime);
//            gameStatus = itemView.findViewById(R.id.text_game_status);
//            vsIndicator = itemView.findViewById(R.id.vs_indicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onGameClickListener != null) {
                    onGameClickListener.onGameClick(games.get(position));
                }
            });
        }

        public void bind(Game game) {
            if (game == null) return;


            // Load team data
//            Team homeTeam = dataManager.getTeamById(game.getHomeTeamId());
//            Team awayTeam = dataManager.getTeamById(game.getAwayTeamId());
            Team homeTeam = game.getHomeTeam();
            Team awayTeam = game.getAwayTeam();
            Log.d("GameAdapter", "Loading game in with away team name " + awayTeam.getAbreviatedName() + " and other name " + awayTeam.getName());
            Log.d("GameAdapter", "Loading game in with home team name " + homeTeam.getAbreviatedName() + " and other name " + homeTeam.getName());
            Log.d("GameAdapter", "Loading game in with game home team name " + game.getHomeTeamName() + " and away team " + game.getAwayTeamName());
            // Set team names (use abbreviation if full name not available)
            setTeamName(awayTeamName, awayTeam, game.getAwayTeamName());
            setTeamName(homeTeamName, homeTeam, game.getHomeTeamName());

            // Load team logos
            loadTeamLogo(awayTeamLogo, awayTeam);
            loadTeamLogo(homeTeamLogo, homeTeam);

            // Set scores
            setScores(game);

            // Set game time/status
            setGameTimeAndStatus(game);
        }

        private void setTeamName(TextView textView, Team team, String fallbackName) {
            if (team != null && team.getAbreviatedName() != null) {
                textView.setText(team.getAbreviatedName() != null ?
                        team.getAbreviatedName() : team.getName());
                textView.setVisibility(View.VISIBLE);
            } else if (fallbackName != null && !fallbackName.isEmpty()) {
                textView.setText(fallbackName);
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setText("TBD");
            }
        }

        private void loadTeamLogo(ImageView logoView, Team team) {
            Log.d("IDK WHY", String.valueOf(team));
            String logoPath = team.getLogoPath();
            Log.d("Logo master", "Got a logo path of " + logoPath);
            Log.d("Logo URL master", "Got a logo path of " + team.getLogoUrl());

            if (logoPath != null && !logoPath.isEmpty()) {
                // Try to load the downloaded logo
                File logoFile = new File(logoPath);
                if (logoFile.exists()) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(logoPath);
                        if (bitmap != null) {
                            logoView.setImageBitmap(bitmap);
                            return;
                        }
                    } catch (Exception e) {
                        // If loading fails, fall through to default
                    }
                }
            }


            // Use default logo if no downloaded logo available
            logoView.setImageResource(R.drawable.ic_team_placeholder);
        }

        private void setScores(Game game) {
            boolean hasScores = game.getHomeScore() >= 0 && game.getAwayScore() >= 0;

            if (hasScores) {
//                Log.d("GameAdapter", "Setting scores of " + game.getHomeScore())
                // Game has scores - show them
//                awayScore.setText(String.valueOf(game.getAwayScore()));
                score.setText(String.format("%d - %d", game.getAwayScore(), game.getHomeScore()));
                score.setVisibility(View.VISIBLE);
//                vsIndicator.setVisibility(View.GONE);
            } else {
                // No scores - show VS indicator
                score.setText(" VS ");
                score.setVisibility(View.VISIBLE);
//                awayScore.setVisibility(View.GONE);
//                homeScore.setVisibility(View.GONE);
//                vsIndicator.setVisibility(View.VISIBLE);
            }
        }

        private void setGameTimeAndStatus(Game game) {
            // Determine game status and time
            boolean hasScores = game.getHomeScore() >= 0 && game.getAwayScore() >= 0;

            if (hasScores) {
                // Game is completed or in progress
//                gameStatus.setText("Final");
//                gameStatus.setVisibility(View.VISIBLE);
                gameTime.setText("Final");
//                gameTime.setVisibility(View.GONE);
            } else {
                // Future game - show start time if available
//                gameStatus.setVisibility(View.GONE);

                if (game.getStartTime() != null) {
                    try {
                        // Parse and format start time
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                        SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.US);
                        java.util.Date startTime = inputFormat.parse(game.getStartTime());
                        if (startTime != null) {
                            gameTime.setText(outputFormat.format(startTime));
                        } else {
                            gameTime.setText("TBD");
                        }
                    } catch (Exception e) {
                        gameTime.setText("TBD");
                    }
                    gameTime.setVisibility(View.VISIBLE);
                } else {
                    gameTime.setText("TBD");
                    gameTime.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}