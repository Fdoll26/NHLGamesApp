package com.example.nhlapp.Activities;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.concurrent.atomic.AtomicBoolean;

public class GamesActivity extends AppCompatActivity {
    private Spinner seasonSpinner;
    private ToggleButton saveGamesBtn;
    private RecyclerView recyclerView;
    private GamesAdapter adapter;
    private List<Game> gameItems = new ArrayList<>();
    private List<String> seasons = new ArrayList<>();

    // Keep track of current async task to cancel if needed
    private FetchGamesTask currentGamesTask;
    private FetchSeasonsTask currentSeasonsTask;
    private boolean isUserSelection = false;
    private String lastSeason = "";

    // Flag to prevent spinner listener from triggering during programmatic changes
    private boolean isUpdatingSpinner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games);

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
        recyclerView = findViewById(R.id.recyclerViewGames);
    }

    private void setupRecyclerView() {
        adapter = new GamesAdapter(gameItems, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSpinnerAndBtn() {
        Context context = this;
        saveGamesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataManager manager = DataManager.getInstance();
                if(!gameItems.isEmpty()){
                    String selectedSeason = seasonSpinner.getSelectedItem().toString();
                    manager.addSeasonGames(selectedSeason, gameItems);
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
                // Cancel any existing seasons task
                if (currentSeasonsTask != null && !currentSeasonsTask.isCancelled()) {
                    currentSeasonsTask.cancel(true);
                }
                currentSeasonsTask = new FetchSeasonsTask();
                currentSeasonsTask.execute();
            }
        }
    }
    private void updateSpinnerAndLoadLatestSeason() {
        if (seasons.isEmpty()) {
            return;
        }

        // Sort seasons in descending order (most recent first)
        Collections.sort(seasons, new Comparator<String>() {
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
//    private void updateSpinnerAndLoadLatestSeason() {
//        if (seasons.isEmpty()) {
//            return;
//        }
//
//        // Sort seasons in descending order (most recent first)
//        Collections.sort(seasons, new Comparator<String>() {
//            @Override
//            public int compare(String s1, String s2) {
//                return s2.compareTo(s1); // Reverse order for most recent first
//            }
//        });
//
//        isUpdatingSpinner = true;
//        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, seasons);
//        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        seasonSpinner.setAdapter(spinnerAdapter);
//
//        // Set selection to most recent season (index 0)
//        seasonSpinner.setSelection(0);
//        lastSeason = seasonSpinner.getSelectedItem().toString();
//        isUpdatingSpinner = false;
//
//        // Only automatically load games if this is the initial setup (not a user selection)
//        if (!seasons.isEmpty() && !isUserSelection) {
//            String latestSeason = seasons.get(0);
//            lastSeason = latestSeason;
//            DataManager dataManager = DataManager.getInstance();
//
//            // Check if we already have games for this season from JSON
//            if (dataManager.hasGamesForSeason(latestSeason)) {
//                Log.d("GamesActivity", "Using games data loaded from JSON for season: " + latestSeason);
//                List<Game> cachedGames = dataManager.getGamesForSeason().get(latestSeason);
//                dataManager.addSeasonGames(latestSeason, cachedGames);
//                updateGamesList(cachedGames);
//            } else {
//                Log.d("GamesActivity", "No cached games found for season: " + latestSeason + ", waiting for user selection");
//                // Don't automatically load - wait for user to select
//            }
//        }
//    }

    private void loadGamesForSeason(String requestSeason) {
        // Cancel any existing games task
        if (currentGamesTask != null && !currentGamesTask.isCancelled()) {
            currentGamesTask.cancel(true);
        }

        AppSettings settings = AppSettings.getInstance(this);

        if (settings.useSingleton()) {
            if (settings.isJsonSavingEnabled()) {
                loadGamesFromJson(requestSeason);
                if(!gameItems.isEmpty())
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
                currentGamesTask = new FetchGamesTask();
                currentGamesTask.execute(requestSeason);
            }
        }
    }

    private void updateGamesList(List<Game> games) {
        gameItems.clear();

        if (games.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

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
            gameItems.add(game);
        }
        Log.d("GamesActivity", "updateGamesList with " + gameItems.size() + " new games added");

        adapter.notifyDataSetChanged();
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

    private class FetchSeasonsTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            // Check if task was cancelled
            if (isCancelled()) {
                return null;
            }
            return NHLApiClient.getSeasons();
        }

        @Override
        protected void onPostExecute(List<String> result) {
            // Only proceed if task wasn't cancelled and activity still exists
            if (!isCancelled() && result != null && !isFinishing()) {
                seasons.clear();
                seasons.addAll(result);
                updateSpinnerAndLoadLatestSeason();
            }
            // Clear reference
            if (currentSeasonsTask == this) {
                currentSeasonsTask = null;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            // Clear reference
            if (currentSeasonsTask == this) {
                currentSeasonsTask = null;
            }
        }
    }

    private class FetchGamesTask extends AsyncTask<String, Void, List<Game>> {
        private String seasonRequested;
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);
        private boolean wasInterrupted = false;

        @Override
        protected void onCancelled() {
            super.onCancelled();
            // Signal to API client that this task is cancelled
            isCancelled.set(true);
            if (currentGamesTask == this) {
                currentGamesTask = null;
            }
            Log.d("GamesActivity", "FetchGamesTask cancelled for season: " + seasonRequested);
        }

        @Override
        protected List<Game> doInBackground(String... seasons) {
            // Check if task was cancelled before starting
            if (isCancelled()) {
                return null;
            }

            seasonRequested = seasons[0];
            Log.d("GamesActivity", "Starting fetch for season: " + seasonRequested);

            try {
                // Pass the cancellation flag to the API client
                return NHLApiClient.getGamesForSeason(seasons[0], isCancelled);
            } catch (Exception e) {
                // Check if the exception is due to interruption/cancellation
                if (e.getMessage() != null && e.getMessage().contains("Request cancelled")) {
                    wasInterrupted = true;
                    Log.d("GamesActivity", "Games fetch was cancelled due to interruption for season: " + seasonRequested);
                } else {
                    Log.e("GamesActivity", "Error fetching games for season " + seasonRequested + ": " + e.getMessage());
                }
                return new ArrayList<>(); // Return empty list on error
            }
        }

        @Override
        protected void onPostExecute(List<Game> result) {
            // Don't update UI if task was cancelled or interrupted
            if (isCancelled() || wasInterrupted) {
                Log.d("GamesActivity", "Task was cancelled/interrupted, not updating UI for season: " + seasonRequested);
                return;
            }

            // Only proceed if activity still exists
            if (result != null && !isFinishing()) {
                // Verify this is still the season we want (user might have changed selection)
                String currentSelectedSeason = null;
                if (seasonSpinner.getSelectedItem() != null) {
                    currentSelectedSeason = seasonSpinner.getSelectedItem().toString();
                }

                // Only update if this matches the currently selected season
                if (seasonRequested.equals(currentSelectedSeason)) {
                    Log.d("GamesActivity", "Updating games list for season: " + seasonRequested + " with " + result.size() + " games");
                    updateGamesList(result);
                    DataManager dataManager = DataManager.getInstance();
                    dataManager.addSeasonGames(currentSelectedSeason, result);
                } else {
                    Log.d("GamesActivity", "Discarding results for season: " + seasonRequested + " (current selection: " + currentSelectedSeason + ")");
                }
            }

            // Clear reference
            if (currentGamesTask == this) {
                currentGamesTask = null;
            }
        }

//        @Override
//        protected void onCancelled() {
//            super.onCancelled();
//            Log.d("GamesActivity", "FetchGamesTask cancelled in onCancelled()");
//            // Clear reference
//            if (currentGamesTask == this) {
//                currentGamesTask = null;
//            }
//        }
    }
}