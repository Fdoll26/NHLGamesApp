package com.example.nhlapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Adapters.DateAdapter;
import com.example.nhlapp.Adapters.GameAdapter;
import com.example.nhlapp.AsyncApiClient;
import com.example.nhlapp.DataCallback;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DatesActivity extends AppCompatActivity {
    private static final String TAG = "DatesActivity";
    private static final long LIVE_UPDATE_INTERVAL = 15000; // 15 seconds

    // UI Components
    private RecyclerView dateRecyclerView;
    private RecyclerView gamesRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView statusText;
    private DateAdapter dateAdapter;
    private GameAdapter gameAdapter;

    // Data
    private ArrayList<String> dates;
    private ArrayList<Game> games;
    private DataManager dataManager;
    private AsyncApiClient apiClient;
    private String selectedDate;
    private String currentSeason;
    private Map<String, List<Game>> gamesByDateCache;

    // Live updates
    private Handler liveUpdateHandler;
    private Runnable liveUpdateRunnable;
    private boolean isLiveUpdatesEnabled = false;

    // Cancellation handling
    private final AtomicBoolean isLoadingCancelled = new AtomicBoolean(false);

    // Team abbreviations for NHL API calls (2024-25 season)
    private static final String[] NHL_TEAMS = {
            "ANA", "UTA", "BOS", "BUF", "CGY", "CAR", "CHI", "COL", "CBJ", "DAL",
            "DET", "EDM", "FLA", "LAK", "MIN", "MTL", "NSH", "NJD", "NYI", "NYR",
            "OTT", "PHI", "PIT", "SEA", "SJS", "STL", "TBL", "TOR", "VAN",
            "VGK", "WSH", "WPG"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dates);

        initializeComponents();
        initViews();
        loadInitialData();
    }

    private void initializeComponents() {
        dataManager = DataManager.getInstance();
        apiClient = new AsyncApiClient();
        gamesByDateCache = new HashMap<>();
        liveUpdateHandler = new Handler();
        currentSeason = "20242025";
    }

    private void initViews() {
        // Find views
        dateRecyclerView = findViewById(R.id.recycler_dates);
        gamesRecyclerView = findViewById(R.id.recycler_games);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        statusText = findViewById(R.id.status_text);

        // Setup date RecyclerView
        LinearLayoutManager dateLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        dateRecyclerView.setLayoutManager(dateLayoutManager);

        dates = new ArrayList<>();
        dateAdapter = new DateAdapter(dates, this::selectDate);
        dateRecyclerView.setAdapter(dateAdapter);

        // Setup games RecyclerView
        LinearLayoutManager gamesLayoutManager = new LinearLayoutManager(this);
        gamesRecyclerView.setLayoutManager(gamesLayoutManager);

        games = new ArrayList<>();
        gameAdapter = new GameAdapter(games, this::onGameClicked);
        gamesRecyclerView.setAdapter(gameAdapter);

        // Initially show loading state
        showLoadingState("Loading season data...");
    }

    private void loadInitialData() {
        // Check if we have cached season data
        if (dataManager.hasGamesForSeason(currentSeason)) {
            Log.d(TAG, "Using cached season data");
            loadDatesFromCachedData();
        } else {
            Log.d(TAG, "Loading season data from API");
            loadSeasonDataFromAPI();
        }
    }

    private void loadDatesFromCachedData() {
        // Get games from DataManager and extract unique dates
        dataManager.getGamesForSeason(currentSeason, false, new DataCallback<List<Game>>() {
            @Override
            public void onSuccess(List<Game> seasonGames) {
                processSeasonGames(seasonGames);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading cached season data: " + error);
                loadSeasonDataFromAPI();
            }
        });
    }

    private void loadSeasonDataFromAPI() {
        showLoadingState("Loading games from API...");

        // Load teams first, then load schedules for each team
        loadTeamsAndSchedules();
    }

    private void loadTeamsAndSchedules() {
        Log.d(TAG, "Starting to load schedules for all NHL teams");

        List<Game> allGames = Collections.synchronizedList(new ArrayList<>());
        Set<Integer> uniqueGameIds = Collections.synchronizedSet(new HashSet<>());
        AtomicInteger completedRequests = new AtomicInteger(0);
        AtomicInteger successfulRequests = new AtomicInteger(0);

        List<AsyncApiClient.ApiRequest<String>> requests = new ArrayList<>();

        // Create requests for each team's schedule
        for (String teamAbbr : NHL_TEAMS) {
            String apiUrl = "https://api-web.nhle.com/v1/club-schedule-season/" + teamAbbr + "/" + currentSeason;

            AsyncApiClient.ApiRequest<String> request = new AsyncApiClient.ApiRequest<>(
                    apiUrl,
                    new AsyncApiClient.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String response) {
                            try {
                                List<Game> teamGames = parseTeamScheduleResponse(response, teamAbbr);

                                // Add unique games to the collection
                                synchronized (uniqueGameIds) {
                                    for (Game game : teamGames) {
                                        if (!uniqueGameIds.contains(game.getGameId())) {
                                            uniqueGameIds.add(game.getGameId());
                                            allGames.add(game);
                                        }
                                    }
                                }

                                successfulRequests.incrementAndGet();
                                Log.d(TAG, "Loaded " + teamGames.size() + " games for " + teamAbbr +
                                        " (Total unique games: " + allGames.size() + ")");

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing schedule for " + teamAbbr + ": " + e.getMessage());
                            }

                            checkIfAllRequestsComplete(completedRequests, allGames);
                        }

                        @Override
                        public void onError(String error) {
                            Log.w(TAG, "Failed to load schedule for " + teamAbbr + ": " + error);
                            checkIfAllRequestsComplete(completedRequests, allGames);
                        }
                    },
                    String.class,
                    isLoadingCancelled
            );

            requests.add(request);
        }

        // Execute all requests concurrently
        runOnUiThread(() -> updateStatusText("Loading schedules for " + NHL_TEAMS.length + " teams..."));
        apiClient.makeAsyncRequests(requests);
    }

    private void checkIfAllRequestsComplete(AtomicInteger completedRequests, List<Game> allGames) {
        int completed = completedRequests.incrementAndGet();

        runOnUiThread(() -> {
            updateStatusText("Loaded " + completed + "/" + NHL_TEAMS.length + " team schedules");
        });

        if (completed >= NHL_TEAMS.length) {
            // All requests completed
            Log.d(TAG, "All team schedule requests completed. Total games: " + allGames.size());

            if (!allGames.isEmpty()) {
                // Save to DataManager and process
                dataManager.addSeasonGames(currentSeason, allGames);
                processSeasonGames(allGames);
            } else {
                runOnUiThread(() -> showErrorState("No games found for season " + currentSeason));
            }
        }
    }

    private List<Game> parseTeamScheduleResponse(String response, String teamAbbr) {
        List<Game> games = new ArrayList<>();

        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(response);

            if (jsonResponse.has("games")) {
                org.json.JSONArray gamesArray = jsonResponse.getJSONArray("games");

                for (int i = 0; i < gamesArray.length(); i++) {
                    org.json.JSONObject gameJson = gamesArray.getJSONObject(i);
                    Game game = parseGameFromJson(gameJson);
                    if (game != null) {
                        games.add(game);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing team schedule response for " + teamAbbr, e);
        }

        return games;
    }

    private Game parseGameFromJson(org.json.JSONObject gameJson) {
        try {
            Game game = new Game();
            game.setGameId(gameJson.getInt("id"));
            game.setGameDate(gameJson.getString("gameDate"));

            // Parse teams
            if (gameJson.has("homeTeam")) {
                org.json.JSONObject homeTeam = gameJson.getJSONObject("homeTeam");
                game.setHomeTeamId(homeTeam.getInt("id"));
                game.setHomeTeamName(homeTeam.optString("abbrev", ""));

                // Handle score - may not exist for future games
                if (homeTeam.has("score")) {
                    game.setHomeScore(homeTeam.getInt("score"));
                } else {
                    game.setHomeScore(-1); // Indicates no score yet
                }
            }

            if (gameJson.has("awayTeam")) {
                org.json.JSONObject awayTeam = gameJson.getJSONObject("awayTeam");
                game.setAwayTeamId(awayTeam.getInt("id"));
                game.setAwayTeamName(awayTeam.optString("abbrev", ""));

                // Handle score - may not exist for future games
                if (awayTeam.has("score")) {
                    game.setAwayScore(awayTeam.getInt("score"));
                } else {
                    game.setAwayScore(-1); // Indicates no score yet
                }
            }

            // Parse game state/status
            String gameState = gameJson.optString("gameState", "Unknown");
            String gameStatus = gameJson.optString("gameScheduleState", "Unknown");

            // Determine display status
            if ("FINAL".equalsIgnoreCase(gameState) || "OFF".equalsIgnoreCase(gameState)) {
//                game.setGameStatus("Final");
            } else if ("LIVE".equalsIgnoreCase(gameState) || "ON".equalsIgnoreCase(gameState)) {
//                game.setGameStatus("Live");
            } else if ("FUT".equalsIgnoreCase(gameState) || "PRE".equalsIgnoreCase(gameState)) {
//                game.setGameStatus("Scheduled");
                // For future games, extract start time if available
                if (gameJson.has("startTimeUTC")) {
                    String startTime = gameJson.getString("startTimeUTC");
                    game.setStartTime(startTime);
                }
            }

            return game;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing individual game JSON", e);
            return null;
        }
    }

    private void processSeasonGames(List<Game> seasonGames) {
        // Extract unique dates and cache games by date
        Map<String, List<Game>> gamesByDate = new HashMap<>();

        for (Game game : seasonGames) {
            String gameDate = game.getGameDate();
            if (!gamesByDate.containsKey(gameDate)) {
                gamesByDate.put(gameDate, new ArrayList<>());
            }
            gamesByDate.get(gameDate).add(game);
        }

        // Update cache
        gamesByDateCache.putAll(gamesByDate);

        // Extract and sort dates
        List<String> dateList = new ArrayList<>(gamesByDate.keySet());
        Collections.sort(dateList, Collections.reverseOrder()); // Most recent first

        // Update UI on main thread
        runOnUiThread(() -> {
            dates.clear();
            dates.addAll(dateList);
            dateAdapter.notifyDataSetChanged();

            hideLoadingState();

            // Select closest date to today
            String todayDate = getTodayDateString();
            String closestDate = findClosestDate(dateList, todayDate);
            if (closestDate != null) {
                selectDate(closestDate);
            }

            Log.d(TAG, "Loaded " + seasonGames.size() + " games across " + dateList.size() + " dates");
            showMessage("Loaded " + seasonGames.size() + " games for season " + currentSeason);
        });
    }

    private String findClosestDate(List<String> dateList, String targetDate) {
        if (dateList.isEmpty()) return null;

        String closest = dateList.get(0);
        long minDiff = Long.MAX_VALUE;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long targetTime = sdf.parse(targetDate).getTime();

            for (String date : dateList) {
                long dateTime = sdf.parse(date).getTime();
                long diff = Math.abs(dateTime - targetTime);
                if (diff < minDiff) {
                    minDiff = diff;
                    closest = date;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding closest date", e);
            return dateList.get(0);
        }

        return closest;
    }

    private void selectDate(String date) {
        Log.d(TAG, "Selecting date: " + date);

        // Stop live updates when switching away from today
        if (!getTodayDateString().equals(date)) {
            stopLiveUpdates();
        }

        selectedDate = date;

        // Check cache first
        if (gamesByDateCache.containsKey(date)) {
            List<Game> cachedGames = gamesByDateCache.get(date);
            updateGamesDisplay(cachedGames);
            Log.d(TAG, "Using cached games for " + date + ": " + cachedGames.size() + " games");

            // Start live updates if this is today
            if (getTodayDateString().equals(date)) {
                startLiveUpdates();
            }
            return;
        }

        // Try to get games from DataManager
        ArrayList<Game> gamesForDate = dataManager.getGamesForDate(date);
        if (gamesForDate != null && !gamesForDate.isEmpty()) {
            gamesByDateCache.put(date, gamesForDate);
            updateGamesDisplay(gamesForDate);
            Log.d(TAG, "Using DataManager games for " + date + ": " + gamesForDate.size() + " games");

            // Start live updates if this is today
            if (getTodayDateString().equals(date)) {
                startLiveUpdates();
            }
        } else {
            // No games found for this date
            updateGamesDisplay(new ArrayList<>());
            showMessage("No games found for " + date);
        }
    }

    private void startLiveUpdates() {
        if (!isLiveUpdatesEnabled && getTodayDateString().equals(selectedDate)) {
            Log.d(TAG, "Starting live updates for today's games");
            isLiveUpdatesEnabled = true;

            liveUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isLiveUpdatesEnabled && getTodayDateString().equals(selectedDate)) {
                        updateLiveScores();
                        liveUpdateHandler.postDelayed(this, LIVE_UPDATE_INTERVAL);
                    }
                }
            };

            // Start after initial delay
            liveUpdateHandler.postDelayed(liveUpdateRunnable, LIVE_UPDATE_INTERVAL);
        }
    }

    private void updateLiveScores() {
        if (selectedDate == null || !gamesByDateCache.containsKey(selectedDate)) {
            return;
        }

        List<Game> todaysGames = gamesByDateCache.get(selectedDate);
        if (todaysGames == null || todaysGames.isEmpty()) {
            return;
        }

        Log.d(TAG, "Updating live scores for " + todaysGames.size() + " games");

        // Create multiple requests for live score updates
        List<AsyncApiClient.ApiRequest<String>> requests = new ArrayList<>();

        for (Game game : todaysGames) {
            String liveScoreUrl = "https://api-web.nhle.com/v1/gamecenter/" + game.getGameId() + "/landing";

            AsyncApiClient.ApiRequest<String> request = new AsyncApiClient.ApiRequest<>(
                    liveScoreUrl,
                    new AsyncApiClient.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String response) {
                            updateGameFromLiveScore(game.getGameId(), response);
                        }

                        @Override
                        public void onError(String error) {
                            Log.w(TAG, "Failed to update live score for game " + game.getGameId());
                        }
                    },
                    String.class,
                    isLoadingCancelled
            );

            requests.add(request);
        }

        // Make concurrent requests
        apiClient.makeAsyncRequests(requests);
    }

    private void updateGameFromLiveScore(int gameId, String response) {
        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(response);

            if (jsonResponse.has("gameState") && jsonResponse.has("homeTeam") && jsonResponse.has("awayTeam")) {
                int homeScore = jsonResponse.getJSONObject("homeTeam").optInt("score", -1);
                int awayScore = jsonResponse.getJSONObject("awayTeam").optInt("score", -1);

                // Update game in cache
                for (List<Game> gameList : gamesByDateCache.values()) {
                    for (Game game : gameList) {
                        if (game.getGameId() == gameId) {
                            boolean scoreChanged = game.getHomeScore() != homeScore || game.getAwayScore() != awayScore;

                            game.setHomeScore(homeScore);
                            game.setAwayScore(awayScore);

                            if (scoreChanged) {
                                // Update DataManager
                                dataManager.addGame(game);

                                // Refresh UI on main thread
                                runOnUiThread(() -> {
                                    if (selectedDate != null && gamesByDateCache.containsKey(selectedDate)) {
                                        updateGamesDisplay(gamesByDateCache.get(selectedDate));
                                    }
                                });

                                Log.d(TAG, "Updated live score for game " + gameId + ": " + awayScore + "-" + homeScore);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating live score for game " + gameId, e);
        }
    }

    private void stopLiveUpdates() {
        if (isLiveUpdatesEnabled) {
            Log.d(TAG, "Stopping live updates");
            isLiveUpdatesEnabled = false;
            if (liveUpdateRunnable != null) {
                liveUpdateHandler.removeCallbacks(liveUpdateRunnable);
            }
        }
    }

    private void updateGamesDisplay(List<Game> gamesToShow) {
        games.clear();
        if (gamesToShow != null) {
            games.addAll(gamesToShow);
        }
        gameAdapter.notifyDataSetChanged();
        Log.d(TAG, "Updated games display with " + games.size() + " games");
    }

    private void showLoadingState(String message) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(message);
        dateRecyclerView.setVisibility(View.GONE);
        gamesRecyclerView.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        loadingProgressBar.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        dateRecyclerView.setVisibility(View.VISIBLE);
        gamesRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorState(String message) {
        loadingProgressBar.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(message);
        dateRecyclerView.setVisibility(View.GONE);
        gamesRecyclerView.setVisibility(View.GONE);
        showMessage(message);
    }

    private void updateStatusText(String message) {
        statusText.setText(message);
    }

    private String getTodayDateString() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
    }

    private String getCurrentSeason() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);

        // NHL season runs from September to June
        if (month >= Calendar.SEPTEMBER) {
            return year + String.valueOf(year + 1);
        } else {
            return (year - 1) + String.valueOf(year);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    public void onGameClicked(Game game) {
        Intent intent = new Intent(this, SpecificGameActivity.class);
        intent.putExtra("gameId", game.getGameId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel all ongoing requests
        isLoadingCancelled.set(true);
        stopLiveUpdates();
        if (apiClient != null) {
            apiClient.cancelAllRequests();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop live updates when activity is not visible
        stopLiveUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart live updates if we're on today's date
        if (selectedDate != null && getTodayDateString().equals(selectedDate)) {
            startLiveUpdates();
        }
    }
}