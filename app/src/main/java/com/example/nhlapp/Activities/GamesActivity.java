package com.example.nhlapp.Activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.nhlapp.Adapters.DateAdapter;
import com.example.nhlapp.AppSettings;
import com.example.nhlapp.DataCallback;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.Adapters.GamesAdapter;
import com.example.nhlapp.JsonHelper;
import com.example.nhlapp.NHLApiClient;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.R;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class GamesActivity extends AppCompatActivity {
    private static final String TAG = "GamesActivity";
    private Spinner seasonSpinner;
    private ToggleButton saveGamesBtn;
    private RecyclerView gamesRecyclerView;
    private RecyclerView datesRecyclerView;
    private GamesAdapter gameAdapter;
    private DateAdapter dateAdapter;
    private List<String> dates = new ArrayList<>();
    private List<Game> games = new ArrayList<>();
    private List<String> seasons = new ArrayList<>();
    private Map<String, List<Game>> gamesByDateCache = new HashMap<>();

    private String selectedDate;

    private boolean isUserSelection = false;
    private String lastSeason = "";

    private ExecutorService executorService;
    private Handler mainHandler;

    // Keep track of current tasks to cancel if needed
    private Future<?> currentGamesTask;
    private Future<?> currentSeasonsTask;

    // Flag to prevent spinner listener from triggering during programmatic changes
    private boolean isUpdatingSpinner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games);
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        initViews();
        setupRecyclerView();
        setupSpinnerAndBtn();
        loadSeasons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any ongoing tasks when activity is destroyed
        cancelCurrentTasks();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void cancelCurrentTasks() {
        if (currentGamesTask != null && !currentGamesTask.isCancelled()) {
            currentGamesTask.cancel(true);
            currentGamesTask = null;
        }
        if (currentSeasonsTask != null && !currentSeasonsTask.isCancelled()) {
            currentSeasonsTask.cancel(true);
            currentSeasonsTask = null;
        }
    }

    private void initViews() {
        saveGamesBtn = findViewById(R.id.saveGamesBtn);
        seasonSpinner = findViewById(R.id.seasonSpinner);
        gamesRecyclerView = findViewById(R.id.recyclerViewGames);
        datesRecyclerView = findViewById(R.id.recyclerViewDates);
    }

    private void setupRecyclerView() {
        gameAdapter = new GamesAdapter(games, this);
        gamesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        gamesRecyclerView.setAdapter(gameAdapter);

        dateAdapter = new DateAdapter(dates, this::selectDate);
        datesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        gamesRecyclerView.setAdapter(dateAdapter);

    }

    private void setupSpinnerAndBtn() {
        Context context = this;
        saveGamesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataManager manager = DataManager.getInstance();
                if(!games.isEmpty()){
                    String selectedSeason = seasonSpinner.getSelectedItem().toString();
                    manager.addSeasonGames(selectedSeason, games);
                }
                manager.saveSpecificDataToJson(context, "Games");
            }
        });

        seasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Ignore if we're programmatically updating the spinner
                if (isUpdatingSpinner) {
                    return;
                }

                if (position >= 0 && position < seasons.size()) {
                    String selectedSeason = seasons.get(position);
                    lastSeason = selectedSeason;
                    // Set flag to indicate this is a user selection
                    isUserSelection = true;
                    loadGamesForSeason(selectedSeason);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadSeasons() {
        AppSettings settings = AppSettings.getInstance(this);

        if (settings.useSingleton()) {
            DataManager dataManager = DataManager.getInstance();

            // Check if seasons were already loaded from JSON on startup
            if (dataManager.isSeasonsLoadedFromJson() && dataManager.isInitialLoadComplete()) {
                Log.d("GamesActivity", "Using seasons data loaded from JSON on startup");
                seasons.clear();
                seasons.addAll(dataManager.getCachedSeasons());
                updateSpinnerAndLoadLatestSeason();
                return;
            }

            // If not loaded from JSON, fetch from API or singleton cache
            dataManager.getSeasons(new DataCallback<List<String>>() {
                @Override
                public void onSuccess(List<String> data) {
                    seasons.clear();
                    seasons.addAll(data);
                    updateSpinnerAndLoadLatestSeason();
                }

                @Override
                public void onError(String error) {
                    Log.e("GamesActivity", "Error loading seasons: " + error);
                }
            });
        } else {
            if (settings.isOnlineMode()) {
                fetchSeasonsAsync();
            }
        }
    }
    private void updateSpinnerAndLoadLatestSeason() {
        if (seasons.isEmpty()) {
            return;
        }

        // Sort seasons in descending order (most recent first)
        seasons.sort(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s2.compareTo(s1); // Reverse order for most recent first
            }
        });

        isUpdatingSpinner = true;
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, seasons);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seasonSpinner.setAdapter(spinnerAdapter);

        // Set selection to most recent season (index 0)
        seasonSpinner.setSelection(0);
        lastSeason = seasonSpinner.getSelectedItem().toString();
        isUpdatingSpinner = false;

        // Only automatically load games if this is the initial setup (not a user selection)
        if (!seasons.isEmpty() && !isUserSelection) {
            String latestSeason = seasons.get(0);
            lastSeason = latestSeason;
            DataManager dataManager = DataManager.getInstance();

            // Check if we already have games for this season from JSON
            if (dataManager.hasGamesForSeason(latestSeason)) {
                Log.d("GamesActivity", "Using games data loaded from JSON for season: " + latestSeason);
                // Use the new method to get cached games
                List<Game> cachedGames = dataManager.getCachedGamesForSeason(latestSeason);
                updateGamesList(cachedGames);
            } else {
                Log.d("GamesActivity", "No cached games found for season: " + latestSeason + ", waiting for user selection");
                // Don't automatically load - wait for user to select
            }
        }
    }

    private String getTodayDateString() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
    }

    private void updateDateSelection(String selectedDate) {
        if (dateAdapter != null && dates != null) {
            // Find the index of the selected date
            int selectedIndex = dates.indexOf(selectedDate);

            if (selectedIndex != -1) {
                // Update the adapter with the selected date
                dateAdapter.setSelectedDate(selectedDate);
                dateAdapter.notifyDataSetChanged();

                // Scroll to the selected date to make it visible
                datesRecyclerView.smoothScrollToPosition(selectedIndex);

                Log.d(TAG, "Updated date selection to: " + selectedDate + " at index: " + selectedIndex);
            } else {
                Log.w(TAG, "Selected date not found in dates list: " + selectedDate);
            }
        }
    }

    private void updateGamesDisplay(List<Game> gamesToShow) {
        for(Game games: gamesToShow){
            Log.d("GameAdapter", "Loading game in with away team name " + games.getAwayTeam().getAbreviatedName() + " and other name " + games.getAwayTeam().getName());
            Log.d("GameAdapter", "Loading game in with home team name " + games.getHomeTeam().getAbreviatedName() + " and other name " + games.getHomeTeam().getName());
            Log.d("GameAdapter", "Loading game in with game home team name " + games.getHomeTeamName() + " and away team " + games.getAwayTeamName());

        }
        games.clear();
        if (gamesToShow != null) {
            games.addAll(gamesToShow);
        }
        gameAdapter.notifyDataSetChanged();
        Log.d(TAG, "Updated games display with " + games.size() + " games");
    }

    private void selectDate(String date) {
        Log.d(TAG, "Selecting date: " + date);

//        if (!getTodayDateString().equals(date)) {
//            stopLiveUpdates();
//        }

        selectedDate = date;

        // Update the date adapter to highlight the selected date and scroll to it
        updateDateSelection(date);

        if (gamesByDateCache.containsKey(date)) {
            List<Game> cachedGames = gamesByDateCache.get(date);
            updateGamesDisplay(cachedGames);
            Log.d(TAG, "Using cached games for " + date + ": " + cachedGames.size() + " games");

//            if (getTodayDateString().equals(date)) {
//                startLiveUpdates();
//            }
            return;
        }
        DataManager dataManager = DataManager.getInstance();
        ArrayList<Game> gamesForDate = dataManager.getGamesForDate(date);
        if (gamesForDate != null && !gamesForDate.isEmpty()) {
            if(!gamesByDateCache.containsKey(date))
                gamesByDateCache.put(date, gamesForDate);
            updateGamesDisplay(gamesByDateCache.get(date));
            Log.d(TAG, "Using DataManager games for " + date + ": " + gamesForDate.size() + " games");

//            if (getTodayDateString().equals(date)) {
//                startLiveUpdates();
//            }
        } else {
            updateGamesDisplay(new ArrayList<>());
            Log.d(TAG, "No games found for " + date);
        }
    }

    private void loadGamesForSeason(String requestSeason) {
        // Cancel any existing games task
        if (currentGamesTask != null && !currentGamesTask.isCancelled()) {
            currentGamesTask.cancel(true);
        }

        AppSettings settings = AppSettings.getInstance(this);

        if (settings.useSingleton()) {
            if (settings.isJsonSavingEnabled()) {
                loadGamesFromJson(requestSeason);
                if(!games.isEmpty())
                    return;
            }

            DataManager dataManager = DataManager.getInstance();

            // Always force refresh when user makes a selection (including same season)
            boolean forceRefresh = isUserSelection;

            Log.d("GamesActivity", "Loading games for season: " + requestSeason + " (force refresh: " + forceRefresh + ")");

            dataManager.getGamesForSeason(requestSeason, forceRefresh, new DataCallback<List<Game>>() {
                @Override
                public void onSuccess(List<Game> data) {
                    updateGamesList(data);
                    dataManager.addSeasonGames(requestSeason, data);
                    // Reset the user selection flag after successful load
                    isUserSelection = false;
                }

                @Override
                public void onError(String error) {
                    Log.e("GamesActivity", "Error loading games: " + error);
                    // Reset the user selection flag even on error
                    isUserSelection = false;
                }
            });
        } else {
            if (settings.isJsonSavingEnabled()) {
                loadGamesFromJson(requestSeason);
            }

            if (settings.isOnlineMode()) {
                fetchGamesAsync(requestSeason);
            }
        }
    }

    private void updateGamesList(List<Game> games) {


        if (games.isEmpty()) {
            gameAdapter.notifyDataSetChanged();
            return;
        }
        games.clear();

        String currentDate = null;

        for (Game game : games) {
            // Extract date part (remove time if present)
            String gameDate = game.getGameDate();
            if (gameDate.contains("T")) {
                gameDate = gameDate.split("T")[0];
            }

            // Add date separator when date changes
            if (!gameDate.equals(currentDate)) {
                currentDate = gameDate;
                // Note: You'll need to implement date separator logic here
                // For now, just adding the game
            }

            // Add the game
            games.add(game);
        }
        Log.d("GamesActivity", "updateGamesList with " + games.size() + " new games added");

        gameAdapter.notifyDataSetChanged();
    }

    private void loadGamesFromJson(String season) {
        String jsonData = JsonHelper.loadJsonFromFile(this, "gamesBySeason.json");
        if (jsonData != null) {
            try {
                JSONObject seasonsObj = new JSONObject(jsonData);
                if (!seasonsObj.has(season)){
                    return;
                }
                JSONArray jsonArray = new JSONArray(seasonsObj.get(season));
                Log.d("GamesActivity", "Got " + jsonArray.length() + " games from gamesBySeason.json");
                List<Game> loadedGames = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject gameJson = jsonArray.getJSONObject(i);
                    Game game = new Game();
                    game.setGameId(gameJson.getInt("id"));
                    game.setGameDate(gameJson.getString("date"));
                    game.setHomeTeamId(gameJson.getInt("homeTeamId"));
                    game.setAwayTeamId(gameJson.getInt("awayTeamId"));

                    // Handle optional fields safely
                    if (gameJson.has("homeScore") && !gameJson.isNull("homeScore")) {
                        game.setHomeScore(gameJson.getInt("homeScore"));
                    }
                    if (gameJson.has("awayScore") && !gameJson.isNull("awayScore")) {
                        game.setAwayScore(gameJson.getInt("awayScore"));
                    }
                    if (gameJson.has("homeTeamName") && !gameJson.isNull("homeTeamName")) {
                        game.setHomeTeamName(gameJson.getString("homeTeamName"));
                    }
                    if (gameJson.has("awayTeamName") && !gameJson.isNull("awayTeamName")) {
                        game.setAwayTeamName(gameJson.getString("awayTeamName"));
                    }

                    loadedGames.add(game);
                }
                Log.d("GamesActivity", "loadGamesFromJson Got " + loadedGames.size() + " games from the JSON");
                updateGamesList(loadedGames);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void fetchSeasonsAsync() {
        // Cancel any existing seasons task
        if (currentSeasonsTask != null && !currentSeasonsTask.isDone()) {
            currentSeasonsTask.cancel(true);
        }

        currentSeasonsTask = executorService.submit(new Runnable() {
            @Override
            public void run() {
                // Check if task was cancelled
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                try {
                    List<String> result = NHLApiClient.getSeasons();

                    // Post result back to main thread
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Only proceed if activity still exists and task wasn't cancelled
                            if (result != null && !isFinishing() &&
                                    currentSeasonsTask != null && !currentSeasonsTask.isCancelled()) {
                                seasons.clear();
                                seasons.addAll(result);
                                updateSpinnerAndLoadLatestSeason();
                            }
                            // Clear reference
                            currentSeasonsTask = null;
                        }
                    });
                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        Log.e("GamesActivity", "Error fetching seasons: " + e.getMessage());
                    }
                    // Clear reference on main thread
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            currentSeasonsTask = null;
                        }
                    });
                }
            }
        });
    }

    private void fetchGamesAsync(String requestSeason) {
        // Cancel any existing games task
        if (currentGamesTask != null && !currentGamesTask.isDone()) {
            currentGamesTask.cancel(true);
        }

        final AtomicBoolean isCancelled = new AtomicBoolean(false);

        currentGamesTask = executorService.submit(new Runnable() {
            @Override
            public void run() {
                // Check if task was cancelled before starting
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                Log.d("GamesActivity", "Starting fetch for season: " + requestSeason);

                List<Game> result = new ArrayList<>();
                boolean wasInterrupted = false;

                try {
                    // Pass the cancellation flag to the API client
                    result = NHLApiClient.getGamesForSeason(requestSeason, isCancelled);
                } catch (Exception e) {
                    // Check if the exception is due to interruption/cancellation
                    if (e.getMessage() != null && e.getMessage().contains("Request cancelled")) {
                        wasInterrupted = true;
                        Log.d("GamesActivity", "Games fetch was cancelled due to interruption for season: " + requestSeason);
                    } else {
                        Log.e("GamesActivity", "Error fetching games for season " + requestSeason + ": " + e.getMessage());
                    }
                }

                final boolean finalWasInterrupted = wasInterrupted;
                final List<Game> finalResult = result;

                // Post result back to main thread
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Don't update UI if task was cancelled or interrupted
                        if (isCancelled.get() || finalWasInterrupted || Thread.currentThread().isInterrupted()) {
                            Log.d("GamesActivity", "Task was cancelled/interrupted, not updating UI for season: " + requestSeason);
                            return;
                        }

                        // Only proceed if activity still exists
                        if (!isFinishing()) {
                            // Verify this is still the season we want (user might have changed selection)
                            String currentSelectedSeason = null;
                            if (seasonSpinner.getSelectedItem() != null) {
                                currentSelectedSeason = seasonSpinner.getSelectedItem().toString();
                            }

                            // Only update if this matches the currently selected season
                            if (requestSeason.equals(currentSelectedSeason)) {
                                Log.d("GamesActivity", "Updating games list for season: " + requestSeason + " with " + finalResult.size() + " games");
                                updateGamesList(finalResult);
                                DataManager dataManager = DataManager.getInstance();
                                dataManager.addSeasonGames(currentSelectedSeason, finalResult);
                            } else {
                                Log.d("GamesActivity", "Discarding results for season: " + requestSeason + " (current selection: " + currentSelectedSeason + ")");
                            }
                        }

                        // Clear reference
                        currentGamesTask = null;
                    }
                });
            }
        });

        // Set up cancellation callback for the current task
        if (currentGamesTask != null) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        currentGamesTask.get(); // Wait for completion
                    } catch (Exception e) {
                        // Task was cancelled or failed
                        isCancelled.set(true);
                        Log.d("GamesActivity", "FetchGamesTask cancelled for season: " + requestSeason);
                    }
                }
            });
        }
    }



}