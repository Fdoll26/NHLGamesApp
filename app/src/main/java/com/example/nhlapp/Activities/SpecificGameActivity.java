package com.example.nhlapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BuildCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Adapters.GamePlayerStatsAdapter;
import com.example.nhlapp.AsyncApiClient;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.Objects.TeamStats;
import com.example.nhlapp.R;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpecificGameActivity extends AppCompatActivity {
    private static final String TAG = "SpecificGameActivity";

    private int gameId;
    private Game game;
    private Team homeTeam;
    private Team awayTeam;

    // UI Components
    private TextView homeName;
    private TextView awayName;
    private TextView gameDate;
    private TextView gameScore;
    private TextView statusText;
    private Button awayTeamButton;
    private Button generalOverviewButton;
    private Button homeTeamButton;
    private ProgressBar loadingBar;
    private RecyclerView statsRecyclerView;
    private ImageView homeTeamLogo;
    private ImageView awayTeamLogo;
    private TextView teamStatsComparison;

    // Data and API
    private DataManager dataManager;
    private AsyncApiClient apiClient;
    private AtomicBoolean isLoadingCancelled = new AtomicBoolean(false);

    // Current view state
    private enum ViewState {
        LOADING,
        GENERAL_OVERVIEW,
        HOME_TEAM,
        AWAY_TEAM
    }
    private ViewState currentViewState = ViewState.LOADING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specific_game);

        Intent intent = getIntent();
        gameId = intent.getIntExtra("gameId", 0);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                isLoadingCancelled.set(true);
                if (apiClient != null) {
                    apiClient.cancelAllRequests();
                }
            }
        };

        if (gameId == 0) {
            Toast.makeText(this, "Invalid game ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initData();
        setupGame();
        loadBoxscoreData();
    }

    private void initViews() {
//        gameTitle = findViewById(R.id.gameTitle);
        homeName = findViewById(R.id.homeTeamName);
        awayName = findViewById(R.id.awayTeamName);
        gameDate = findViewById(R.id.gameDate);
        gameScore = findViewById(R.id.gameScore);
        statusText = findViewById(R.id.statusText);
        awayTeamButton = findViewById(R.id.awayTeamButton);
        generalOverviewButton = findViewById(R.id.generalOverviewButton);
        homeTeamButton = findViewById(R.id.homeTeamButton);
        loadingBar = findViewById(R.id.loadingBar);
        statsRecyclerView = findViewById(R.id.statsRecyclerView);
        homeTeamLogo = findViewById(R.id.homeTeamLogo);
        awayTeamLogo = findViewById(R.id.awayTeamLogo);
        teamStatsComparison = findViewById(R.id.teamStatsComparison);

        // Setup RecyclerView
        statsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup button listeners
        awayTeamButton.setOnClickListener(v -> showTeamStats("away"));
        generalOverviewButton.setOnClickListener(v -> showGeneralOverview());
        homeTeamButton.setOnClickListener(v -> showTeamStats("home"));

        // Initially hide content until data is loaded
        setViewState(ViewState.LOADING);
    }

    private void initData() {
        dataManager = DataManager.getInstance();
        apiClient = new AsyncApiClient();
    }

    private void setupGame() {
        game = dataManager.getGameById(gameId);
        if (game == null) {
            Toast.makeText(this, "Game not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get team information
        homeTeam = dataManager.getTeamById(game.getHomeTeamId());
        awayTeam = dataManager.getTeamById(game.getAwayTeamId());

        // Setup basic game info
        updateGameHeader();
        setupTeamLogos();
    }

    private void updateGameHeader() {
        String homeTeamName = homeTeam != null ? homeTeam.getAbreviatedName() : String.valueOf(game.getHomeTeamId());
        String awayTeamName = awayTeam != null ? awayTeam.getAbreviatedName() : String.valueOf(game.getAwayTeamId());

        awayName.setText(awayTeamName);
        homeName.setText(homeTeamName);
        gameDate.setText(String.format("Date: %s", game.getDate()));

        // Update button text
        awayTeamButton.setText(String.format("%s Team", awayTeamName));
        homeTeamButton.setText(String.format("%s Team", homeTeamName));

        // Show score if available
        if (game.getHomeScore() >= 0 && game.getAwayScore() >= 0) {
            gameScore.setText(String.format("Score: %s %d - %d %s",
                    awayTeamName, game.getAwayScore(), game.getHomeScore(), homeTeamName));
            gameScore.setVisibility(View.VISIBLE);
        } else {
            gameScore.setVisibility(View.GONE);
        }
    }

    private void setupTeamLogos() {
        // Load team logos from local storage if available
        if (homeTeam != null && homeTeam.getLogoUrl() != null && !homeTeam.getLogoUrl().isEmpty()) {
            // Try to load from local storage first, fallback to URL
            if (homeTeam.getLogoUrl().startsWith("/") || homeTeam.getLogoUrl().startsWith("file://")) {
                // Local file
                Picasso.get().load("file://" + homeTeam.getLogoUrl()).into(homeTeamLogo);
            } else {
                // URL - this will be handled by the async API client to download and cache
                Picasso.get().load(homeTeam.getLogoUrl()).into(homeTeamLogo);
            }
        }

        if (awayTeam != null && awayTeam.getLogoUrl() != null && !awayTeam.getLogoUrl().isEmpty()) {
            if (awayTeam.getLogoUrl().startsWith("/") || awayTeam.getLogoUrl().startsWith("file://")) {
                Picasso.get().load("file://" + awayTeam.getLogoUrl()).into(awayTeamLogo);
            } else {
                Picasso.get().load(awayTeam.getLogoUrl()).into(awayTeamLogo);
            }
        }
    }

    private void loadBoxscoreData() {
        statusText.setText("Loading boxscore data...");
        setViewState(ViewState.LOADING);

        // For now, we'll work with the team data we have
        // If you need to load additional boxscore data from API, implement that here
        statusText.setText("Team data loaded");
        showGeneralOverview(); // Default to general overview
    }

    private void setViewState(ViewState state) {
        currentViewState = state;

        switch (state) {
            case LOADING:
                loadingBar.setVisibility(View.VISIBLE);
                statsRecyclerView.setVisibility(View.GONE);
                teamStatsComparison.setVisibility(View.GONE);
                break;
            case GENERAL_OVERVIEW:
            case HOME_TEAM:
            case AWAY_TEAM:
                loadingBar.setVisibility(View.GONE);
                // Visibility of other views will be set by individual methods
                break;
        }
    }

    private void showGeneralOverview() {
        setViewState(ViewState.GENERAL_OVERVIEW);
        statsRecyclerView.setVisibility(View.GONE);
        teamStatsComparison.setVisibility(View.VISIBLE);

        if (homeTeam == null || awayTeam == null) {
            teamStatsComparison.setText("Team data not available");
            return;
        }

        // Create team stats comparison using the team rosters
        TeamStats homeStats = calculateTeamTotals(homeTeam);
        TeamStats awayStats = calculateTeamTotals(awayTeam);

        String homeTeamName = homeTeam.getAbreviatedName();
        String awayTeamName = awayTeam.getAbreviatedName();

        StringBuilder comparison = new StringBuilder();
        comparison.append("GAME SUMMARY\n\n");

        // Final Score if available
        if (game.getHomeScore() >= 0 && game.getAwayScore() >= 0) {
            comparison.append(String.format("Final Score: %s %d - %d %s\n\n",
                    awayTeamName, game.getAwayScore(), game.getHomeScore(), homeTeamName));
        } else {
            comparison.append("Game Score: TBD\n\n");
        }

        // Team season statistics comparison
        comparison.append("SEASON STATISTICS\n\n");
        comparison.append(String.format("%-20s %10s %10s\n", "Statistic", awayTeamName, homeTeamName));
        comparison.append("─".repeat(40)).append("\n");
        comparison.append(String.format("%-20s %10d %10d\n", "Games Played",
                awayTeam.getGamesPlayed() >= 0 ? awayTeam.getGamesPlayed() : 0,
                homeTeam.getGamesPlayed() >= 0 ? homeTeam.getGamesPlayed() : 0));
        comparison.append(String.format("%-20s %10d %10d\n", "Wins",
                awayTeam.getGamesWon() >= 0 ? awayTeam.getGamesWon() : 0,
                homeTeam.getGamesWon() >= 0 ? homeTeam.getGamesWon() : 0));
        comparison.append(String.format("%-20s %10d %10d\n", "Losses",
                awayTeam.getGamesLost() >= 0 ? awayTeam.getGamesLost() : 0,
                homeTeam.getGamesLost() >= 0 ? homeTeam.getGamesLost() : 0));
        comparison.append(String.format("%-20s %10d %10d\n", "Points",
                awayTeam.getPoints() >= 0 ? awayTeam.getPoints() : 0,
                homeTeam.getPoints() >= 0 ? homeTeam.getPoints() : 0));
        comparison.append(String.format("%-20s %10d %10d\n", "Goals For",
                awayTeam.getGoalsFor() >= 0 ? awayTeam.getGoalsFor() : 0,
                homeTeam.getGoalsFor() >= 0 ? homeTeam.getGoalsFor() : 0));
        comparison.append(String.format("%-20s %10d %10d\n", "Goals Against",
                awayTeam.getGoalsAgainst() >= 0 ? awayTeam.getGoalsAgainst() : 0,
                homeTeam.getGoalsAgainst() >= 0 ? homeTeam.getGoalsAgainst() : 0));

        // Player-based statistics
        comparison.append("\nPLAYER TOTALS\n\n");
        comparison.append(String.format("%-20s %10s %10s\n", "Player Stats", awayTeamName, homeTeamName));
        comparison.append("─".repeat(40)).append("\n");
        comparison.append(String.format("%-20s %10d %10d\n", "Total Goals", awayStats.getGoals(), homeStats.getGoals()));
        comparison.append(String.format("%-20s %10d %10d\n", "Total Assists", awayStats.getAssists(), homeStats.getAssists()));
        comparison.append(String.format("%-20s %10d %10d\n", "Total Points", awayStats.getPoints(), homeStats.getPoints()));
        comparison.append(String.format("%-20s %10d %10d\n", "Total Shots", awayStats.getShots(), homeStats.getShots()));
        comparison.append(String.format("%-20s %10d %10d\n", "Total Hits", awayStats.getHits(), homeStats.getHits()));
        comparison.append(String.format("%-20s %10d %10d\n", "Blocked Shots", awayStats.getBlockedShots(), homeStats.getBlockedShots()));
        comparison.append(String.format("%-20s %10d %10d\n", "Penalty Minutes", awayStats.getPenaltyMinutes(), homeStats.getPenaltyMinutes()));

        teamStatsComparison.setText(comparison.toString());
    }

    private void showTeamStats(String teamSide) {
        ViewState newState = teamSide.equals("home") ? ViewState.HOME_TEAM : ViewState.AWAY_TEAM;
        setViewState(newState);

        statsRecyclerView.setVisibility(View.VISIBLE);
        teamStatsComparison.setVisibility(View.GONE);

        Team selectedTeam = teamSide.equals("home") ? homeTeam : awayTeam;

        if (selectedTeam == null || selectedTeam.getTeamRoster().isEmpty()) {
            // Show empty state
            statsRecyclerView.setAdapter(null);
            return;
        }

        // Create and set adapter for player stats
        GamePlayerStatsAdapter adapter = new GamePlayerStatsAdapter(
                this,
                selectedTeam,
                selectedTeam
        );
        statsRecyclerView.setAdapter(adapter);
    }

    private TeamStats calculateTeamTotals(Team team) {
        TeamStats stats = new TeamStats();

        if (team == null || team.getTeamRoster().isEmpty()) {
            return stats;
        }

        // Sum stats from all players in the roster
        for (NHLPlayer player : team.getTeamRoster().values()) {
            if (player != null) {
                stats.addGoals(player.getGoals());
                stats.addAssists(player.getAssists());
                stats.addPoints(player.getPoints());
                stats.addShots(player.getShotsOnGoal());
                stats.addHits(player.getHits());
                stats.addBlockedShots(player.getBlocks());
                stats.addPenaltyMinutes(player.getPenaltyMinutes());
            }
        }

        return stats;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any ongoing API requests
        isLoadingCancelled.set(true);
        if (apiClient != null) {
            apiClient.cancelAllRequests();
        }
    }

//    @Override
//    public void onBackPressed() {
//        // Cancel ongoing requests before going back
//
//        super.onBackPressed();
//    }
}