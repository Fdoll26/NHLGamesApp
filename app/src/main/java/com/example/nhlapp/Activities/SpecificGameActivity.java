package com.example.nhlapp.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Adapters.GamePlayerStatsAdapter;
import com.example.nhlapp.Adapters.GeneralOverviewAdapter;
import com.example.nhlapp.AltImageDownloader;
import com.example.nhlapp.AsyncApiClient;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.Objects.TeamStats;
import com.example.nhlapp.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpecificGameActivity extends AppCompatActivity {
    private static final String TAG = "SpecificGameActivity";

    private int gameId;
    private Game game;
    private Team homeTeam;
    private Team awayTeam;
    private Game gameResult;
    private AltImageDownloader imageDownloader;

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
    private RecyclerView.Adapter currentAdapter;

    // Data and API
    private DataManager dataManager;
    private AsyncApiClient apiClient;
    private final AtomicBoolean isLoadingCancelled = new AtomicBoolean(false);

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

        // Setup back press handling
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelAndFinish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

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

        // Setup RecyclerView
        statsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup button listeners
        awayTeamButton.setOnClickListener(v -> showTeamStats("away"));
        generalOverviewButton.setOnClickListener(v -> showGeneralOverview());
        homeTeamButton.setOnClickListener(v -> showTeamStats("home"));

        imageDownloader = new AltImageDownloader(this);

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

        // Fixed the null check for game date
        if (game.getGameDate() != null) {
            String setting = String.format("Date: %s", game.getGameDate());
            Log.d("Trial", setting);
            gameDate.setText(setting);
        } else {
            gameDate.setText("Date: TBD");
        }

        // Update button text
        awayTeamButton.setText(String.format("%s Team", awayTeamName));
        homeTeamButton.setText(String.format("%s Team", homeTeamName));

        // Show score if available
        if (game.getHomeScore() >= 0 && game.getAwayScore() >= 0) {
            gameScore.setText(String.format("Score: %s %d - %d %s", awayTeamName, game.getAwayScore(), game.getHomeScore(), homeTeamName));
            gameScore.setVisibility(View.VISIBLE);
        } else {
            gameScore.setVisibility(View.GONE);
        }
    }

    private void setupTeamLogos() {
        // Setup home team logo
        if (homeTeam != null) {
            loadTeamLogo(homeTeam, homeTeamLogo);
        } else {
            homeTeamLogo.setImageResource(R.drawable.ic_team_placeholder);
        }

        // Setup away team logo
        if (awayTeam != null) {
            loadTeamLogo(awayTeam, awayTeamLogo);
        } else {
            awayTeamLogo.setImageResource(R.drawable.ic_team_placeholder);
        }
    }

    private void loadTeamLogo(Team team, ImageView logoImageView) {
        if (team == null || logoImageView == null) {
            logoImageView.setImageResource(R.drawable.ic_team_placeholder);
            return;
        }

        Log.d(TAG, "Loading logo for team: " + team.getAbreviatedName() +
                " (logoPath: " + team.getLogoPath() + ", logoUrl: " + team.getLogoUrl() + ")");

        // First priority: Load from local storage if available
        String localLogoPath = team.getLogoPath();
        if (localLogoPath != null && !localLogoPath.trim().isEmpty()) {
            File logoFile = new File(localLogoPath);
            if (logoFile.exists() && logoFile.length() > 0) {
                try {
                    // Load local file using Picasso
                    Picasso.get()
                            .load(logoFile)
                            .placeholder(R.drawable.ic_team_placeholder)
                            .error(R.drawable.ic_team_placeholder)
                            .into(logoImageView);
                    Log.d(TAG, "Successfully loaded local logo for " + team.getAbreviatedName() + ": " + localLogoPath);
                    return;
                } catch (Exception e) {
                    Log.w(TAG, "Failed to load local logo file: " + localLogoPath, e);
                    // Clear invalid path
                    team.setLogoPath(null);
                }
            } else {
                Log.w(TAG, "Local logo file does not exist or is empty: " + localLogoPath);
                // Clear invalid path
                team.setLogoPath(null);
            }
        }

        // Second priority: Try to load from remote URL if available
        String logoUrl = team.getLogoUrl();
        if (logoUrl != null && !logoUrl.trim().isEmpty()) {
            try {
                Picasso.get()
                        .load(logoUrl)
                        .placeholder(R.drawable.ic_team_placeholder)
                        .error(R.drawable.ic_team_placeholder)
                        .into(logoImageView);
                Log.d(TAG, "Loading remote logo for " + team.getAbreviatedName() + ": " + logoUrl);
                return;
            } catch (Exception e) {
                Log.w(TAG, "Failed to load remote logo: " + logoUrl, e);
            }
        }

        // Third priority: Build URL as last resort
        String builtUrl = buildLogoUrlForTeam(team);
        if (builtUrl != null) {
            team.setLogoUrl(builtUrl); // Save the URL for future use
            try {
                Picasso.get()
                        .load(builtUrl)
                        .placeholder(R.drawable.ic_team_placeholder)
                        .error(R.drawable.ic_team_placeholder)
                        .into(logoImageView);
                Log.d(TAG, "Loading built logo URL for " + team.getAbreviatedName() + ": " + builtUrl);
                return;
            } catch (Exception e) {
                Log.w(TAG, "Failed to load built logo URL: " + builtUrl, e);
            }
        }

        // Fallback: Show placeholder
        logoImageView.setImageResource(R.drawable.ic_team_placeholder);
        Log.d(TAG, "No logo available for " + team.getAbreviatedName() + ", using placeholder");
    }

    // Helper method to build logo URL (same as in DatesActivity)
    private String buildLogoUrlForTeam(Team team) {
        if (team == null || team.getAbreviatedName() == null || team.getAbreviatedName().trim().isEmpty()) {
            return null;
        }
        return "https://assets.nhle.com/logos/nhl/svg/" + team.getAbreviatedName() + "_light.svg";
    }

    // Add this method to refresh logos if they become available
    public void refreshTeamLogos() {
        // Update team data from DataManager in case logos were downloaded
        if (homeTeam != null) {
            Team updatedHomeTeam = dataManager.getTeamById(homeTeam.getTeamID());
            if (updatedHomeTeam != null && updatedHomeTeam.getLogoPath() != null) {
                homeTeam.setLogoPath(updatedHomeTeam.getLogoPath());
            }
            loadTeamLogo(homeTeam, homeTeamLogo);
        }

        if (awayTeam != null) {
            Team updatedAwayTeam = dataManager.getTeamById(awayTeam.getTeamID());
            if (updatedAwayTeam != null && updatedAwayTeam.getLogoPath() != null) {
                awayTeam.setLogoPath(updatedAwayTeam.getLogoPath());
            }
            loadTeamLogo(awayTeam, awayTeamLogo);
        }
    }

    private void loadBoxscoreData() {
        statusText.setText("Loading boxscore data...");
        setViewState(ViewState.LOADING);

        // Make async API call to get boxscore data
        String boxscoreUrl = "https://api-web.nhle.com/v1/gamecenter/" + gameId + "/boxscore";

        apiClient.makeAsyncRequest(
                boxscoreUrl,
                new AsyncApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        try {
                            gameResult = parseBoxScore(result);
                            updateTeamsWithBoxscoreData();
                            runOnUiThread(() -> {
                                statusText.setText("Boxscore data loaded");
                                showGeneralOverview();
                            });
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing boxscore data", e);
                            runOnUiThread(() -> {
                                statusText.setText("Error loading boxscore data");
                                showGeneralOverview();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to load boxscore: " + error);
                        // Fall back to showing general overview without boxscore data
                        runOnUiThread(() -> {
                            statusText.setText("Using cached team data");
                            showGeneralOverview();
                        });
                    }
                },
                String.class,  // Changed from Game.class to String.class
                isLoadingCancelled
        );
    }

    private Game parseBoxScore(String responseToParse) throws JSONException {
        JSONObject boxScore = new JSONObject(responseToParse);
        int season = boxScore.getInt("season");
        Game returnGame = new Game();

        // Set game ID to match current game
        returnGame.setGameId(gameId);

        // Parse teams
        if (boxScore.has("awayTeam")) {
            returnGame.setAwayTeam(parseTeam(boxScore.getJSONObject("awayTeam")));
            returnGame.setAwayTeamName(returnGame.getAwayTeam().getAbreviatedName());
            returnGame.setAwayTeamId(returnGame.getAwayTeam().getTeamID());
            returnGame.setAwayScore(returnGame.getAwayTeam().getGoalsFor());
        }

        if (boxScore.has("homeTeam")) {
            returnGame.setHomeTeam(parseTeam(boxScore.getJSONObject("homeTeam")));
            returnGame.setHomeTeamName(returnGame.getHomeTeam().getAbreviatedName());
            returnGame.setHomeTeamId(returnGame.getHomeTeam().getTeamID());
            returnGame.setHomeScore(returnGame.getHomeTeam().getGoalsFor());
        }

        // Parse player stats if available
        if (boxScore.has("playerByGameStats")) {
            JSONObject playerByGameStats = boxScore.getJSONObject("playerByGameStats");

            if (playerByGameStats.has("awayTeam") && returnGame.getAwayTeam() != null) {
                HashMap<Integer, NHLPlayer> awayPlayers = parsePlayers(playerByGameStats.getJSONObject("awayTeam"), returnGame.getAwayTeam().getAbreviatedName(), season);
                returnGame.getAwayTeam().setTeamRoster(awayPlayers);
            }

            if (playerByGameStats.has("homeTeam") && returnGame.getHomeTeam() != null) {
                HashMap<Integer, NHLPlayer> homePlayers = parsePlayers(playerByGameStats.getJSONObject("homeTeam"),  returnGame.getHomeTeam().getAbreviatedName(), season);
                returnGame.getHomeTeam().setTeamRoster(homePlayers);
            }
        }

        return returnGame;
    }

    private Team parseTeam(JSONObject teamObj) throws JSONException {
        Team returnTeam = new Team();
        returnTeam.setTeamID(teamObj.getInt("id"));
        returnTeam.setAbreviatedName(teamObj.optString("abbrev", ""));
        returnTeam.setGoalsFor(teamObj.optInt("score", 0));
        returnTeam.setShotsOnGoal(teamObj.optInt("sog", 0));

        // Handle logo URL
        String logoUrl = teamObj.optString("logo", "");
        if (logoUrl.isEmpty()) {
            logoUrl = buildLogoUrlForTeam(returnTeam);
        }
        returnTeam.setLogoUrl(logoUrl);

        // Handle team name
        if (teamObj.has("commonName")) {
            JSONObject commonNameObj = teamObj.getJSONObject("commonName");
            returnTeam.setFullName(commonNameObj.optString("default", ""));
        }

        // If full name is still empty, try to get it from existing team data
        if (returnTeam.getFullName() == null || returnTeam.getFullName().isEmpty()) {
            Team existingTeam = dataManager.getTeamById(returnTeam.getTeamID());
            if (existingTeam != null && existingTeam.getFullName() != null) {
                returnTeam.setFullName(existingTeam.getFullName());
            } else {
                returnTeam.setFullName(getFullTeamName(returnTeam.getAbreviatedName()));
            }
        }

        return returnTeam;
    }

    private int parseTimeOnIceToSeconds(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return 0;
        }

        try {
            // Handle formats like "12:34" or "1:23:45"
            String[] parts = timeString.split(":");
            int totalSeconds = 0;

            if (parts.length == 2) {
                // MM:SS format
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                totalSeconds = minutes * 60 + seconds;
            } else if (parts.length == 3) {
                // HH:MM:SS format
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                totalSeconds = hours * 3600 + minutes * 60 + seconds;
            } else {
                // Try to parse as integer (already in seconds)
                totalSeconds = Integer.parseInt(timeString);
            }

            return totalSeconds;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse time on ice: " + timeString, e);
            return 0;
        }
    }





    private HashMap<Integer, NHLPlayer> parsePlayers(JSONObject playersList, String abbrevName, int season) throws JSONException {
        HashMap<Integer, NHLPlayer> returnPlayers = new HashMap<>();
        String[] playerTypes = {"forwards", "defense", "goalies"};

        for (String playerType : playerTypes) {
            if (playersList.has(playerType)) {
                JSONArray playersArray = playersList.getJSONArray(playerType);
                for (int i = 0; i < playersArray.length(); i++) {
                    JSONObject playerObj = playersArray.getJSONObject(i);
                    NHLPlayer newPlayer = parseNewPlayer(playerObj);
                    if (newPlayer != null) {
                        newPlayer.setLogoURL("https://assets/nhle.com/mugs/nhl/"+season+"/"+abbrevName+"/"+newPlayer.getPlayerId()+".png");
                        returnPlayers.put(newPlayer.getPlayerId(), newPlayer);
                    }
                }
            }
        }

        return returnPlayers;
    }

    private NHLPlayer parseNewPlayer(JSONObject playerObj) {
        NHLPlayer newPlayer = new NHLPlayer();
        try {
            newPlayer.setPlayerId(playerObj.getInt("playerId"));
            newPlayer.setPosition(playerObj.optString("position", ""));
            newPlayer.setJerseyNumber(playerObj.optInt("sweaterNumber", 0));

            // Parse player name
            if (playerObj.has("name")) {
                JSONObject nameObj = playerObj.getJSONObject("name");
                newPlayer.setName(nameObj.optString("default", ""));
            }


            // Skater stats (forwards and defense)
            newPlayer.setGoals(playerObj.optInt("goals", 0));
            newPlayer.setAssists(playerObj.optInt("assists", 0));
            newPlayer.setPoints(playerObj.optInt("points", 0));
            newPlayer.setPlusMinus(playerObj.optInt("plusMinus", 0));
            newPlayer.setPenaltyMinutes(playerObj.optInt("pim", 0));
            newPlayer.setHits(playerObj.optInt("hits", 0));
            newPlayer.setPowerplayGoals(playerObj.optInt("powerPlayGoals", 0));
            newPlayer.setShotsOnGoal(playerObj.optInt("sog", 0));
//            newPlayer.setTimeOnIce(playerObj.optInt("toi", 0));
            if (playerObj.has("toi")) {
                Object toiValue = playerObj.get("toi");
                if (toiValue instanceof String) {
                    // If it's a string like "12:34", convert to seconds
                    String toiString = (String) toiValue;
                    newPlayer.setTimeOnIce(parseTimeOnIceToSeconds(toiString));
                } else if (toiValue instanceof Integer) {
                    // If it's already in seconds
                    newPlayer.setTimeOnIce(playerObj.getInt("toi"));
                } else {
                    newPlayer.setTimeOnIce(0);
                }
            } else {
                newPlayer.setTimeOnIce(0);
            }
            newPlayer.setBlocks(playerObj.optInt("blockedShots", 0));
            newPlayer.setGiveaways(playerObj.optInt("giveaways", 0));
            newPlayer.setTakeaways(playerObj.optInt("takeaways", 0));
            newPlayer.setShifts(playerObj.optInt("shifts", 0));
            newPlayer.setFaceoffWinPercentage(playerObj.optDouble("faceoffWinningPctg", 0.0));

            // Goalie stats
            newPlayer.setSaves(playerObj.optInt("saves", 0));
//            newPlayer.setTotalShots(playerObj.optInt("shotsAgainst", 0));
            int shotsAgainst = 0;
            if (playerObj.has("shotsAgainst")) {
                shotsAgainst = playerObj.getInt("shotsAgainst");
            } else if (playerObj.has("shots")) {
                shotsAgainst = playerObj.getInt("shots");
            } else if (playerObj.has("sa")) {
                shotsAgainst = playerObj.getInt("sa");
            }
            newPlayer.setTotalShots(shotsAgainst);
            newPlayer.setGoalsAgainst(playerObj.optInt("goalsAgainst", 0));
            newPlayer.setSavePercentage(playerObj.optDouble("savePctg", 0.0));
            newPlayer.setEvenStrengthGoalsAgainst(playerObj.optInt("evenStrengthGoalsAgainst", 0));
            newPlayer.setPowerPlayGoalsAgainst(playerObj.optInt("powerPlayGoalsAgainst", 0));

            return newPlayer;
        } catch (Exception e) {
            Log.w(TAG, "Error parsing player: " + e.getMessage());
            return null;
        }
    }

    private void updateTeamsWithBoxscoreData() {
        Log.d(TAG, "Updating teams with boxscore data");

        if (gameResult == null) {
            Log.w(TAG, "No gameResult data to merge");
            return;
        }

        // Update home team with boxscore data
        if (gameResult.getHomeTeam() != null) {
            Team boxscoreHomeTeam = gameResult.getHomeTeam();
            if (homeTeam != null) {
                // Merge boxscore data with existing team data
                mergeTeamData(homeTeam, boxscoreHomeTeam);
                Log.d(TAG, "Merged home team data. Players: " + homeTeam.getTeamRoster().size());
            } else {
                homeTeam = boxscoreHomeTeam;
                Log.d(TAG, "Set home team from boxscore. Players: " + homeTeam.getTeamRoster().size());
            }
        }

        // Update away team with boxscore data
        if (gameResult.getAwayTeam() != null) {
            Team boxscoreAwayTeam = gameResult.getAwayTeam();
            if (awayTeam != null) {
                // Merge boxscore data with existing team data
                mergeTeamData(awayTeam, boxscoreAwayTeam);
                Log.d(TAG, "Merged away team data. Players: " + awayTeam.getTeamRoster().size());
            } else {
                awayTeam = boxscoreAwayTeam;
                Log.d(TAG, "Set away team from boxscore. Players: " + awayTeam.getTeamRoster().size());
            }
        }

        // Update game score from boxscore if available
        if (gameResult.getAwayScore() >= 0 && gameResult.getHomeScore() >= 0) {
            game.setHomeScore(gameResult.getHomeScore());
            game.setAwayScore(gameResult.getAwayScore());
            dataManager.addGame(game); // Update in DataManager

            runOnUiThread(this::updateGameHeader);
            Log.d(TAG, "Updated game scores: " + game.getAwayScore() + "-" + game.getHomeScore());
        }
    }

    private void mergeTeamData(Team existingTeam, Team boxscoreTeam) {
        // Merge player data from boxscore with existing team
        if (boxscoreTeam.getTeamRoster() != null && !boxscoreTeam.getTeamRoster().isEmpty()) {
            // Initialize roster if needed
            if (existingTeam.getTeamRoster() == null) {
                existingTeam.setTeamRoster(new HashMap<>());
            }

            for (NHLPlayer boxscorePlayer : boxscoreTeam.getTeamRoster().values()) {
                // Add or update player in existing team
                existingTeam.addPlayer(boxscorePlayer);

                // Also update in DataManager
                dataManager.addPlayer(boxscorePlayer);
            }

            Log.d(TAG, "Merged " + boxscoreTeam.getTeamRoster().size() +
                    " players into " + existingTeam.getAbreviatedName());
        }

        // Merge other team stats if available
        if (boxscoreTeam.getGoalsFor() > 0) {
            existingTeam.setGoalsFor(boxscoreTeam.getGoalsFor());
        }
        if (boxscoreTeam.getShotsOnGoal() > 0) {
            existingTeam.setShotsOnGoal(boxscoreTeam.getShotsOnGoal());
        }
    }

    private void setViewState(ViewState state) {
        currentViewState = state;

        switch (state) {
            case LOADING:
                loadingBar.setVisibility(View.VISIBLE);
                statsRecyclerView.setVisibility(View.GONE);
                break;
            case GENERAL_OVERVIEW:
            case HOME_TEAM:
            case AWAY_TEAM:
                loadingBar.setVisibility(View.GONE);
                statsRecyclerView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showGeneralOverview() {
        setViewState(ViewState.GENERAL_OVERVIEW);

        if (homeTeam == null || awayTeam == null) {
            // Create empty adapter with error message
            List<String> errorMessage = new ArrayList<>();
            errorMessage.add("Team data not available");
            GeneralOverviewAdapter adapter = new GeneralOverviewAdapter(errorMessage);
            statsRecyclerView.setAdapter(adapter);
            return;
        }

        // Create data for general overview
        List<String> overviewData = createGeneralOverviewData();
        GeneralOverviewAdapter adapter = new GeneralOverviewAdapter(overviewData);
        statsRecyclerView.setAdapter(adapter);
        currentAdapter = adapter;

        Log.d(TAG, "Showing general overview with " + overviewData.size() + " items");
    }

    @SuppressLint("DefaultLocale")
    private List<String> createGeneralOverviewData() {
        List<String> data = new ArrayList<>();

        String homeTeamName = homeTeam.getAbreviatedName();
        String awayTeamName = awayTeam.getAbreviatedName();

        // Game Summary Header
        data.add("GAME SUMMARY");
        data.add(""); // Empty line for spacing

        // Final Score if available
        if (game.getHomeScore() >= 0 && game.getAwayScore() >= 0) {
            data.add(String.format("Final Score: %s %d - %d %s",
                    awayTeamName, game.getAwayScore(), game.getHomeScore(), homeTeamName));
        } else {
            data.add("Game Score: TBD");
        }
        data.add(""); // Empty line

        // Game-specific statistics from boxscore if available
        if (gameResult != null) {
            data.add("GAME STATISTICS");
            data.add(""); // Empty line
            data.add(String.format("%-20s %10s %10s", "Game Stats", awayTeamName, homeTeamName));
            data.add("────────────────────────────────────────");

            // Add game-specific stats
            TeamStats awayGameStats = calculateGameStats(gameResult.getAwayTeam());
            TeamStats homeGameStats = calculateGameStats(gameResult.getHomeTeam());

            data.add(String.format("%-20s %10d %10d", "Goals", awayGameStats.getGoals(), homeGameStats.getGoals()));
            data.add(String.format("%-20s %10d %10d", "Assists", awayGameStats.getAssists(), homeGameStats.getAssists()));
            data.add(String.format("%-20s %10d %10d", "Points", awayGameStats.getPoints(), homeGameStats.getPoints()));
            data.add(String.format("%-20s %10d %10d", "Shots on Goal", awayGameStats.getShots(), homeGameStats.getShots()));
            data.add(String.format("%-20s %10d %10d", "Hits", awayGameStats.getHits(), homeGameStats.getHits()));
            data.add(String.format("%-20s %10d %10d", "Blocked Shots", awayGameStats.getBlockedShots(), homeGameStats.getBlockedShots()));
            data.add(String.format("%-20s %10d %10d", "Penalty Minutes", awayGameStats.getPenaltyMinutes(), homeGameStats.getPenaltyMinutes()));
            data.add(""); // Empty line
        }

        // Team season statistics comparison
        data.add("SEASON STATISTICS");
        data.add(""); // Empty line
        data.add(String.format("%-20s %10s %10s", "Season Stats", awayTeamName, homeTeamName));
        data.add("────────────────────────────────────────");
        data.add(String.format("%-20s %10d %10d", "Games Played",
                Math.max(awayTeam.getGamesPlayed(), 0),
                Math.max(homeTeam.getGamesPlayed(), 0)));
        data.add(String.format("%-20s %10d %10d", "Wins", Math.max(awayTeam.getGamesWon(), 0), Math.max(homeTeam.getGamesWon(), 0)));
        data.add(String.format("%-20s %10d %10d", "Losses",
                Math.max(awayTeam.getGamesLost(), 0),
                Math.max(homeTeam.getGamesLost(), 0)));
        data.add(String.format("%-20s %10d %10d", "Points",
                Math.max(awayTeam.getPoints(), 0),
                Math.max(homeTeam.getPoints(), 0)));
        data.add(String.format("%-20s %10d %10d", "Goals For",
                Math.max(awayTeam.getGoalsFor(), 0),
                Math.max(homeTeam.getGoalsFor(), 0)));
        data.add(String.format("%-20s %10d %10d", "Goals Against",
                Math.max(awayTeam.getGoalsAgainst(), 0),
                Math.max(homeTeam.getGoalsAgainst(), 0)));

        return data;
    }

    private void showTeamStats(String teamSide) {
        ViewState newState = teamSide.equals("home") ? ViewState.HOME_TEAM : ViewState.AWAY_TEAM;
        setViewState(newState);

        Team selectedTeam = teamSide.equals("home") ? homeTeam : awayTeam;
        Team gameTeam = null;

        // Get game-specific team data if available from boxscore
        if (gameResult != null) {
            gameTeam = teamSide.equals("home") ?
                    gameResult.getHomeTeam() :
                    gameResult.getAwayTeam();
        }

        if (selectedTeam == null || selectedTeam.getTeamRoster() == null || selectedTeam.getTeamRoster().isEmpty()) {
            // Show empty state or try to use game team data
            if (gameTeam != null && gameTeam.getTeamRoster() != null && !gameTeam.getTeamRoster().isEmpty()) {
                selectedTeam = gameTeam;
            } else {
                statsRecyclerView.setAdapter(null);
                Toast.makeText(this, "No player data available for this team", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create and set adapter for player stats
        // Use game team if available for game-specific stats, otherwise use season team
        Team teamToDisplay = (gameTeam != null && gameTeam.getTeamRoster() != null && !gameTeam.getTeamRoster().isEmpty()) ? gameTeam : selectedTeam;

        GamePlayerStatsAdapter adapter = new GamePlayerStatsAdapter(
                this,
                teamToDisplay,
                selectedTeam
//                forwards,
//                defense,
//                goalies
        );
        statsRecyclerView.setAdapter(adapter);
        currentAdapter = adapter;

        Log.d(TAG, "Showing stats for " + teamSide + " team: " + teamToDisplay.getAbreviatedName() );
//                " with " + forwards.size() + " forwards, " + defense.size() + " defense, " + goalies.size() + " goalies");
    }

    private TeamStats calculateTeamTotals(Team team) {
        TeamStats stats = new TeamStats();

        if (team == null || team.getTeamRoster() == null || team.getTeamRoster().isEmpty()) {
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

    private TeamStats calculateGameStats(Team gameTeam) {
        // Calculate stats specifically from game data (boxscore)
        TeamStats stats = new TeamStats();

        if (gameTeam == null || gameTeam.getTeamRoster() == null || gameTeam.getTeamRoster().isEmpty()) {
            return stats;
        }

        // Sum game-specific stats from all players
        for (NHLPlayer player : gameTeam.getTeamRoster().values()) {
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

    // Helper method to get full team name (same as in DatesActivity)
    private String getFullTeamName(String abbreviation) {
        switch (abbreviation) {
            case "ANA": return "Anaheim Ducks";
            case "UTA": return "Utah Hockey Club";
            case "BOS": return "Boston Bruins";
            case "BUF": return "Buffalo Sabres";
            case "CGY": return "Calgary Flames";
            case "CAR": return "Carolina Hurricanes";
            case "CHI": return "Chicago Blackhawks";
            case "COL": return "Colorado Avalanche";
            case "CBJ": return "Columbus Blue Jackets";
            case "DAL": return "Dallas Stars";
            case "DET": return "Detroit Red Wings";
            case "EDM": return "Edmonton Oilers";
            case "FLA": return "Florida Panthers";
            case "LAK": return "Los Angeles Kings";
            case "MIN": return "Minnesota Wild";
            case "MTL": return "Montréal Canadiens";
            case "NSH": return "Nashville Predators";
            case "NJD": return "New Jersey Devils";
            case "NYI": return "New York Islanders";
            case "NYR": return "New York Rangers";
            case "OTT": return "Ottawa Senators";
            case "PHI": return "Philadelphia Flyers";
            case "PIT": return "Pittsburgh Penguins";
            case "SEA": return "Seattle Kraken";
            case "SJS": return "San Jose Sharks";
            case "STL": return "St. Louis Blues";
            case "TBL": return "Tampa Bay Lightning";
            case "TOR": return "Toronto Maple Leafs";
            case "VAN": return "Vancouver Canucks";
            case "VGK": return "Vegas Golden Knights";
            case "WSH": return "Washington Capitals";
            case "WPG": return "Winnipeg Jets";
            default: return abbreviation + " Hockey Club";
        }
    }

    // Helper methods that were referenced but missing
    private boolean isTeamValidForProcessing(Team team) {
        if (team == null) {
            return false;
        }

        // Must have abbreviation or team ID
        boolean hasAbbrev = team.getAbreviatedName() != null && !team.getAbreviatedName().trim().isEmpty();
        boolean hasTeamId = team.getTeamID() > 0;

        return hasAbbrev || hasTeamId;
    }

    private String getTeamKey(Team team) {
        if (team.getAbreviatedName() != null && !team.getAbreviatedName().trim().isEmpty()) {
            return team.getAbreviatedName().toUpperCase().trim();
        }
        return "TEAM_" + team.getTeamID();
    }

    private void updateTeamInAllGames(Team updatedTeam) {
        if (updatedTeam == null) return;

        // Update the team in DataManager
        DataManager.getInstance().addTeam(updatedTeam);

        // Update current game's team references
        if (game != null) {
            if (game.getHomeTeamId() == updatedTeam.getTeamID()) {
                homeTeam = updatedTeam;
                game.setHomeTeam(updatedTeam);
            }
            if (game.getAwayTeamId() == updatedTeam.getTeamID()) {
                awayTeam = updatedTeam;
                game.setAwayTeam(updatedTeam);
            }
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void cancelAndFinish() {
        isLoadingCancelled.set(true);
        if (apiClient != null) {
            apiClient.cancelAllRequests();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any ongoing API requests
        isLoadingCancelled.set(true);
        if (apiClient != null) {
            apiClient.cancelAllRequests();
        }
        if (imageDownloader != null) {
            imageDownloader.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh logos in case they were downloaded while away
        refreshTeamLogos();
    }
}