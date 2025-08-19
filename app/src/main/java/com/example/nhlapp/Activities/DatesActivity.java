package com.example.nhlapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Adapters.DateAdapter;
import com.example.nhlapp.Adapters.GameAdapter;
import com.example.nhlapp.AltImageDownloader;
import com.example.nhlapp.AsyncApiClient;
import com.example.nhlapp.DataCallback;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.ImageHelper;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DatesActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://api-web.nhle.com/v1";
    private static final String STATS_BASE_URL = "https://api.nhle.com/stats/rest/en";
    private static final String TAG = "DatesActivity";
    private static final long LIVE_UPDATE_INTERVAL = 15000; // 15 seconds

    // UI Components
    private RecyclerView dateRecyclerView;
    private RecyclerView gamesRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView statusText;
    private Spinner seasonSpinner;
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

    private List<String> seasons;
    private List<Team> allTeams;
    private List<Team> currentTeams;

    // Live updates
    private Handler liveUpdateHandler;
    private Runnable liveUpdateRunnable;
    private boolean isLiveUpdatesEnabled = false;
    private boolean isUserSelection = false;
    private boolean isUpdatingSpinner = false;

    private AltImageDownloader imageDownloader;
    private ImageHelper imageHelper;
    private boolean logosDownloaded = false;

    // Logo management
    private final Set<Integer> logoDownloadInProgress = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> logoDownloadCompleted = Collections.synchronizedSet(new HashSet<>());

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
        imageDownloader = new AltImageDownloader(this);
        imageHelper = ImageHelper.getInstance(this);
        seasons = new ArrayList<>();
        currentTeams = new ArrayList<>();
        seasonSpinner = findViewById(R.id.seasonSpinner);
    }

    private void initViews() {
        // Find views
        dateRecyclerView = findViewById(R.id.recycler_dates);
        gamesRecyclerView = findViewById(R.id.recycler_games);
//        loadingProgressBar = findViewById(R.id.loading_progress_bar);
//        statusText = findViewById(R.id.status_text);

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

//        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, seasons);
//        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        seasonSpinner.setAdapter(spinnerAdapter);


        seasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Ignore if we're programmatically updating the spinner
                if (isUpdatingSpinner) {
                    return;
                }

                if (position >= 0 && position < seasons.size()) {
                    String newSeason = seasons.get(position);

                    // Only proceed if season actually changed
                    if (!newSeason.equals(currentSeason)) {
                        currentSeason = newSeason;
                        isUserSelection = true;
                        Log.d(TAG, "User selected new season: " + currentSeason);

                        // Check if we have teams loaded before proceeding
                        if (allTeams != null && !allTeams.isEmpty()) {
                            loadTeamsAndSchedules();
                        } else {
                            Log.d(TAG, "Teams not loaded yet, will load schedules after teams are available");
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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
                // Process logos for cached games
                processGameLogosAsync(seasonGames);
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
//        seasons = new ArrayList<>(getSeasons());
//        if(!seasons.isEmpty()){
//            currentSeason = seasons.get(0);
//        }
//
//        allTeams = new ArrayList<>(getAllTeams());
//
//        // Load teams and schedules
//        loadTeamsAndSchedules();
        loadSeasonsAsync();
    }

    private void loadSeasonsAsync() {
        String seasonsUrl = BASE_URL + "/season";

        apiClient.makeAsyncRequest(seasonsUrl, new AsyncApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray seasonsArray = new JSONArray(response);
                    seasons = new ArrayList<>();

                    for (int i = 0; i < seasonsArray.length(); i++) {
                        String seasonId = seasonsArray.getString(i);
                        seasons.add(seasonId);
                    }

                    if (!seasons.isEmpty()) {
//                        currentSeason = seasons.get(seasons.size()-2);
                        seasons.sort(new Comparator<String>() {
                            @Override
                            public int compare(String s1, String s2) {
                                return s2.compareTo(s1); // Reverse order for most recent first
                            }
                        });
                        runOnUiThread(() -> {
                            updateSpinnerWithSeasons();
                        });
                    }


                    Log.d(TAG, "Got seasons: " + seasons.size());

                    // Now load teams
                    loadTeamsAsync();

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing seasons response", e);
                    // Use fallback seasons
                    seasons = new ArrayList<>(Arrays.asList("20242025", "20232024", "20222223", "20212022", "20202021"));
                    if (currentSeason != null)
                        currentSeason = seasons.get(0);
                    runOnUiThread(() -> {
                        updateSpinnerWithSeasons();
                    });
                    loadTeamsAsync();
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to get seasons from API: " + error);
                // Use fallback seasons
                seasons = new ArrayList<>(Arrays.asList("20242025", "20232024", "20222023", "20212022", "20202021"));
                if (currentSeason != null)
                    currentSeason = seasons.get(0);
                runOnUiThread(() -> {
                    updateSpinnerWithSeasons();
                });
                loadTeamsAsync();
            }
        }, String.class, isLoadingCancelled);
    }

    private void loadTeamsAsync() {
//        runOnUiThread(() -> updateStatusText("Loading team information..."));

        String teamsUrl = STATS_BASE_URL + "/team";

        apiClient.makeAsyncRequest(teamsUrl, new AsyncApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray teamsArray = responseObject.getJSONArray("data");

                    allTeams = new ArrayList<>();

                    for (int i = 0; i < teamsArray.length(); i++) {
                        Team newTeam = new Team();
                        JSONObject teamObj = teamsArray.getJSONObject(i);
                        newTeam.setTeamID(teamObj.getInt("id"));
                        newTeam.setAbreviatedName(teamObj.getString("triCode"));
                        newTeam.setFullName(teamObj.getString("fullName"));
                        allTeams.add(newTeam);
                        DataManager.getInstance().addTeam(newTeam);
                    }

                    Log.d(TAG, "Got teams: " + allTeams.size());

                    // Now load team schedules
                    loadTeamsAndSchedules();

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing teams response", e);
                    // Create fallback teams list if needed
                    createFallbackTeams();
                    loadTeamsAndSchedules();
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to get teams from API: " + error);
                // Create fallback teams list
                createFallbackTeams();
                loadTeamsAndSchedules();
            }
        }, String.class, isLoadingCancelled);
    }


    private void loadTeamsAndSchedules() {

        if (allTeams == null || allTeams.isEmpty()) {
            Log.w(TAG, "allTeams is null or empty, cannot load schedules yet");
            return;
        }

        Log.d(TAG, "Starting to load schedules for all NHL teams");

        List<Game> allGames = Collections.synchronizedList(new ArrayList<>());
        Set<Integer> uniqueGameIds = Collections.synchronizedSet(new HashSet<>());
        AtomicInteger completedRequests = new AtomicInteger(0);
        AtomicInteger successfulRequests = new AtomicInteger(0);

        List<AsyncApiClient.ApiRequest<String>> requests = new ArrayList<>();
        currentTeams.clear();
        for(Team team: allTeams){
            String apiUrl = "https://api-web.nhle.com/v1/club-schedule-season/" + team.getAbreviatedName() + "/" + currentSeason;

            AsyncApiClient.ApiRequest<String> request = new AsyncApiClient.ApiRequest<>(
                    apiUrl,
                    new AsyncApiClient.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String response) {
                            try {
                                List<Game> teamGames = parseTeamScheduleResponse(response, team.getAbreviatedName());
                                Log.d("LoadTeamsAndSchedules", "Parsed " + teamGames.size() + " games for " + team.getAbreviatedName());
                                if(!teamGames.isEmpty())
                                    currentTeams.add(team);

                                // Add unique games to the collection
                                synchronized (uniqueGameIds) {
                                    for (Game game : teamGames) {
                                        if (!uniqueGameIds.contains(game.getGameId())) {
                                            uniqueGameIds.add(game.getGameId());
                                            Log.d("DatesActivity", "Adding game with " + game.getHomeTeamName() + " and " + game.getAwayTeamName());
                                            Log.d("DatesActivity", "Adding game homer team with " + game.getHomeTeam().getAbreviatedName() + " and " +  game.getHomeTeam().getFullName());
                                            Log.d("DatesActivity", "Adding game away team with " + game.getAwayTeam().getAbreviatedName() + " and " +  game.getAwayTeam().getFullName());
                                            allGames.add(game);
                                        }
                                    }
                                }

                                successfulRequests.incrementAndGet();
                                Log.d(TAG, "Loaded " + teamGames.size() + " games for " + team.getAbreviatedName() + " (Total unique games: " + allGames.size() + ")");

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing schedule for " + team.getAbreviatedName() + ": " + e.getMessage());
                            }

                            checkIfAllRequestsComplete(completedRequests, allGames);
                        }

                        @Override
                        public void onError(String error) {
                            Log.w(TAG, "Failed to load schedule for " + team.getAbreviatedName() + ": " + error);
                            checkIfAllRequestsComplete(completedRequests, allGames);
                        }
                    },
                    String.class,
                    isLoadingCancelled
            );

            requests.add(request);
        }

        // Execute all requests concurrently
//        runOnUiThread(() -> updateStatusText("Loading schedules for " + allTeams.size() + " teams..."));
        Log.d(TAG, "Loading schedules for " + allTeams.size() + " teams...");
        apiClient.makeAsyncRequests(requests);

    }

    private void checkIfAllRequestsComplete(AtomicInteger completedRequests, List<Game> allGames) {
        int completed = completedRequests.incrementAndGet();

//        runOnUiThread(() -> {
//            updateStatusText("Loaded " + completed + "/" + allTeams.size() + " team schedules");
//        });
        Log.d(TAG, "Loaded " + completed + "/" + allTeams.size() + " team schedules");

        if (completed >= allTeams.size()) {
            Log.d(TAG, "All team schedule requests completed. Total games: " + allGames.size());

            if (!allGames.isEmpty()) {
                dataManager.addSeasonGames(currentSeason, allGames);
                processSeasonGames(allGames);
//                for(Game game: allGames){
//                    if (gamesByDateCache.containsKey(game.getGameDate())) {
//                        Objects.requireNonNull(gamesByDateCache.get(game.getGameDate())).add(game);
//                    } else{
//                        List<Game> newList = new ArrayList<>();
//                        newList.add(game);
//                        gamesByDateCache.put(game.getGameDate(), newList);
//                    }
//                }

                // Process logos for all games asynchronously
                processGameLogosAsync(allGames);
            } else {
                runOnUiThread(() -> showErrorState("No games found for season " + currentSeason));
            }
        }
    }

    private void processGameLogosAsync(List<Game> games) {
        if (games == null || games.isEmpty()) {
            Log.d(TAG, "No games to process for logos");
            return;
        }

        Log.d(TAG, "Starting logo processing for " + games.size() + " games");

        // Collect all unique teams that need logos, avoiding duplicates
        Map<String, Team> uniqueTeams = new HashMap<>();

        for (Game game : games) {
            // Process home team
            if (game.homeTeam != null && isTeamValidForProcessing(game.homeTeam)) {
                String teamKey = getTeamKey(game.homeTeam);
                if (!uniqueTeams.containsKey(teamKey)) {
                    // Ensure team has logoUrl set from the game data
                    if (game.homeTeam.logoUrl == null || game.homeTeam.logoUrl.trim().isEmpty()) {
                        game.homeTeam.logoUrl = buildLogoUrlForTeam(game.homeTeam);
                    }
                    uniqueTeams.put(teamKey, game.homeTeam);
                }
            }

            // Process away team
            if (game.awayTeam != null && isTeamValidForProcessing(game.awayTeam)) {
                String teamKey = getTeamKey(game.awayTeam);
                if (!uniqueTeams.containsKey(teamKey)) {
                    // Ensure team has logoUrl set from the game data
                    if (game.awayTeam.logoUrl == null || game.awayTeam.logoUrl.trim().isEmpty()) {
                        game.awayTeam.logoUrl = buildLogoUrlForTeam(game.awayTeam);
                    }
                    uniqueTeams.put(teamKey, game.awayTeam);
                }
            }
        }

        Log.d(TAG, "Found " + uniqueTeams.size() + " unique teams to process for logos");

        if (uniqueTeams.isEmpty()) {
            Log.d(TAG, "No valid teams found for logo processing");
            return;
        }

        // First, update all teams with existing local logo paths
        List<Team> teamsWithoutLogos = new ArrayList<>();
        int teamsWithExistingLogos = 0;

        for (Team team : uniqueTeams.values()) {
            if (imageDownloader.hasLocalLogo(team)) {
                // Update team with existing logo path
                String localPath = imageDownloader.getLocalLogoPath(team);
                team.setLogoPath(localPath);
                teamsWithExistingLogos++;
                Log.d(TAG, "Team " + team.getAbreviatedName() + " already has logo: " + localPath);

                // IMPORTANT: Update the team in DataManager and game objects
                updateTeamInAllGames(team);

            } else {
                teamsWithoutLogos.add(team);
                Log.d(TAG, "Team " + team.getAbreviatedName() + " needs logo download");
            }
        }

        Log.d(TAG, "Teams with existing logos: " + teamsWithExistingLogos + ", Teams needing download: " + teamsWithoutLogos.size());

        // Update UI immediately for teams that already have logos
        if (teamsWithExistingLogos > 0) {
            runOnUiThread(() -> {
                if (gameAdapter != null) {
                    gameAdapter.notifyDataSetChanged();
                }
            });
        }

        // Download logos for teams that don't have them
        if (!teamsWithoutLogos.isEmpty()) {
            Log.d(TAG, "Starting downloads for " + teamsWithoutLogos.size() + " teams");

            imageDownloader.downloadTeamLogos(teamsWithoutLogos, new AltImageDownloader.DownloadCallback() {
                @Override
                public void onComplete(int successful, int failed, int skipped) {
                    Log.d(TAG, "Logo downloads completed: " + successful + " successful, " + failed + " failed, " + skipped + " skipped");

                    // Update all teams with their new logo paths
                    for (Team team : teamsWithoutLogos) {
                        String localPath = imageDownloader.getLocalLogoPath(team);
                        if (localPath != null) {
                            team.setLogoPath(localPath);
                            updateTeamInAllGames(team);
                            Log.d(TAG, "Updated team " + team.getAbreviatedName() + " with new logo path: " + localPath);
                        }
                    }

                    runOnUiThread(() -> {
                        // Final refresh to show all logos
                        if (gameAdapter != null) {
                            gameAdapter.notifyDataSetChanged();
                        }

                        // Show completion message
                        if (successful > 0) {
                            showMessage("Downloaded " + successful + " team logos");
                        } else if (failed > 0 && successful == 0) {
                            showMessage("Failed to download team logos");
                        }

                        // Debug: List all downloaded logos
                        imageDownloader.listDownloadedLogos();
                    });
                }

                @Override
                public void onProgress(String teamName, boolean success) {
                    if (success) {
                        Log.d(TAG, "Successfully downloaded logo for: " + teamName);

                        // Find the team object and update it with the new logo path
                        for (Team team : teamsWithoutLogos) {
                            if (teamName.equals(team.getAbreviatedName())) {
                                String localPath = imageDownloader.getLocalLogoPath(team);
                                if (localPath != null) {
                                    team.setLogoPath(localPath);
                                    updateTeamInAllGames(team);
                                    Log.d(TAG, "Updated team " + team.getAbreviatedName() + " with logo path: " + localPath);
                                }
                                break;
                            }
                        }
                    } else {
                        Log.w(TAG, "Failed to download logo for: " + teamName);
                    }

                    // Update UI immediately when each logo is downloaded
                    runOnUiThread(() -> {
                        if (gameAdapter != null) {
                            gameAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        } else {
            Log.d(TAG, "All teams already have logos, no downloads needed");
        }
    }

    // Add this helper method to update team data across all game objects
    private void updateTeamInAllGames(Team updatedTeam) {
        if (updatedTeam == null) return;

        // Update the team in DataManager
        DataManager.getInstance().addTeam(updatedTeam);

        // Update team references in all cached games
        for (List<Game> gameList : gamesByDateCache.values()) {
            for (Game game : gameList) {
                if (game.homeTeam != null && game.homeTeam.getTeamID() == updatedTeam.getTeamID()) {
                    // Update home team logo path
                    game.homeTeam.setLogoPath(updatedTeam.getLogoPath());
                    if (updatedTeam.getLogoUrl() != null) {
                        game.homeTeam.setLogoUrl(updatedTeam.getLogoUrl());
                    }
                }
                if (game.awayTeam != null && game.awayTeam.getTeamID() == updatedTeam.getTeamID()) {
                    // Update away team logo path
                    game.awayTeam.setLogoPath(updatedTeam.getLogoPath());
                    if (updatedTeam.getLogoUrl() != null) {
                        game.awayTeam.setLogoUrl(updatedTeam.getLogoUrl());
                    }
                }
            }
        }
    }

    // Helper method to validate if team can be processed for logos
    private boolean isTeamValidForProcessing(Team team) {
        if (team == null) {
            return false;
        }

        // Must have abbreviation or team ID
        boolean hasAbbrev = team.getAbreviatedName() != null && !team.getAbreviatedName().trim().isEmpty();
        boolean hasTeamId = team.getTeamID() > 0;

        if (!hasAbbrev && !hasTeamId) {
            Log.w(TAG, "Team has no abbreviation or valid team ID: " + getTeamDebugString(team));
            return false;
        }

        return true;
    }

    // Helper method to generate consistent team keys
    private String getTeamKey(Team team) {
        if (team.getAbreviatedName() != null && !team.getAbreviatedName().trim().isEmpty()) {
            return team.getAbreviatedName().toUpperCase().trim();
        }
        return "TEAM_" + team.getTeamID();
    }

    // Helper method to build logo URL for a team
    private String buildLogoUrlForTeam(Team team) {
        if (team == null || team.getAbreviatedName() == null || team.getAbreviatedName().trim().isEmpty()) {
            return null;
        }
        return "https://assets.nhle.com/logos/nhl/svg/" + team.getAbreviatedName() + "_light.svg";
    }

    // Helper method for debug logging
    private String getTeamDebugString(Team team) {
        if (team == null) return "null team";

        return String.format("Team{ID: %d, Abbrev: '%s', Full: '%s', LogoURL: '%s'}",
                team.getTeamID(),
                team.getAbreviatedName(),
                team.getFullName(),
                team.logoUrl);
    }


    private List<Game> parseTeamScheduleResponse(String response, String teamAbbr) {
        List<Game> addingGames = new ArrayList<>();

        try {
            JSONObject jsonResponse = new JSONObject(response);
            Log.d("parseTeamScheduleResponse", response);
            if (jsonResponse.has("games") && jsonResponse.has("currentSeason")) {
                JSONArray gamesArray = jsonResponse.getJSONArray("games");

                for (int i = 0; i < gamesArray.length(); i++) {
                    JSONObject gameJson = gamesArray.getJSONObject(i);
                    Game game = parseGameFromJson(gameJson);
                    if (game != null) {
                        addingGames.add(game);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing team schedule response for " + teamAbbr, e);
        }

        return addingGames;
    }

//    private Game parseGameFromJson(org.json.JSONObject gameJson) {
//        try {
//            Game game = new Game();
//            game.setGameId(gameJson.getInt("id"));
//            game.setGameDate(gameJson.getString("gameDate"));
//
//            // Parse home team
//            if (gameJson.has("homeTeam")) {
//                org.json.JSONObject homeTeamJson = gameJson.getJSONObject("homeTeam");
//                Team homeTeam = parseTeamFromGameJson(homeTeamJson);
//                game.homeTeam = homeTeam;
//
//                // Set score
//                if (homeTeamJson.has("score")) {
//                    game.setHomeScore(homeTeamJson.getInt("score"));
//                }
//            }
//
//            // Parse away team
//            if (gameJson.has("awayTeam")) {
//                org.json.JSONObject awayTeamJson = gameJson.getJSONObject("awayTeam");
//                Team awayTeam = parseTeamFromGameJson(awayTeamJson);
//                game.awayTeam = awayTeam;
//
//                // Set score
//                if (awayTeamJson.has("score")) {
//                    game.setAwayScore(awayTeamJson.getInt("score"));
//                }
//            }
//
//            // Parse game state/status
//            String gameState = gameJson.optString("gameState", "Unknown");
//            if ("FUT".equalsIgnoreCase(gameState) || "PRE".equalsIgnoreCase(gameState)) {
//                if (gameJson.has("startTimeUTC")) {
//                    String startTime = gameJson.getString("startTimeUTC");
//                    game.setStartTime(startTime);
//                }
//            }
//
//            return game;
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing individual game JSON", e);
//            return null;
//        }
//    }

    private Team parseTeamFromGameJson(org.json.JSONObject teamJson) {
        Team team = new Team();

        try {
            // Set basic team info
            team.setTeamID(teamJson.getInt("id"));
            team.setAbreviatedName(teamJson.optString("abbrev", ""));

            // Extract and set logo URL from API response
            if (teamJson.has("logo")) {
                String logoUrl = teamJson.getString("logo");
                team.setLogoUrl(logoUrl);
                Log.d(TAG, "Set logoUrl for " + team.getAbreviatedName() + ": " + logoUrl);
            } else {
                // Build logo URL if not provided
                String builtUrl = buildLogoUrlForTeam(team);
                team.setLogoUrl(builtUrl);
                Log.d(TAG, "Built logoUrl for " + team.getAbreviatedName() + ": " + builtUrl);
            }

            // Add to DataManager for future reference
            DataManager.getInstance().addTeam(team);

            // Find and merge with full team info if available
            Team fullTeamInfo = findTeamById(team.getTeamID());
            if (fullTeamInfo != null) {
                // Copy full name and other details
                team.setFullName(fullTeamInfo.getFullName());

                // Update abbreviation if it wasn't set properly
                if (team.getAbreviatedName().isEmpty()) {
                    team.setAbreviatedName(fullTeamInfo.getAbreviatedName());
                }

                // Preserve existing logo URL if the full team info doesn't have one
                if (fullTeamInfo.getLogoUrl() == null || fullTeamInfo.getLogoUrl().trim().isEmpty()) {
                    fullTeamInfo.setLogoUrl(team.getLogoUrl());
                }
            } else {
                // Fallback: set full name based on abbreviation
                if (!team.getAbreviatedName().isEmpty()) {
                    team.setFullName(getFullTeamName(team.getAbreviatedName()));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing team from game JSON", e);
        }

        return team;
    }

    private Game parseGameFromJson(org.json.JSONObject gameJson) {
        try {
            Game game = new Game();
            game.setGameId(gameJson.getInt("id"));
            game.setGameDate(gameJson.getString("gameDate"));

            // Parse teams with logo URLs
            if (gameJson.has("homeTeam")) {
                org.json.JSONObject homeTeamJson = gameJson.getJSONObject("homeTeam");

                Team temp = new Team();
                temp.setTeamID(homeTeamJson.getInt("id"));
                temp.setAbreviatedName(homeTeamJson.optString("abbrev", ""));


                // Extract logo URL if available
                if (homeTeamJson.has("logo")) {
                    temp.logoUrl = homeTeamJson.getString("logo");
                    if(temp.logoUrl.contains("_secondary"))
                        temp.logoUrl = temp.logoUrl.replace("_secondary", "");
                }

                Team fullTeamInfo = findTeamById(temp.getTeamID());
                if (fullTeamInfo != null) {
                    temp.setFullName(fullTeamInfo.getFullName());
                    // Also copy abbreviation if it wasn't set properly
                    if (temp.getAbreviatedName().isEmpty()) {
                        temp.setAbreviatedName(fullTeamInfo.getAbreviatedName());
                    }
                } else {
                    // Fallback: try to set a reasonable full name based on abbreviation
                    if (!temp.getAbreviatedName().isEmpty()) {
                        temp.setFullName(getFullTeamName(temp.getAbreviatedName()));
                    }
                }
                game.setHomeTeam(temp);
                DataManager.getInstance().addTeam(temp);

                game.setHomeTeamId(temp.getTeamID());
                game.setHomeTeamName(temp.getAbreviatedName());

                // Set score
                if (homeTeamJson.has("score")) {
                    game.setHomeScore(homeTeamJson.getInt("score"));
                } else {
                    game.setHomeScore(-1);
                }

                // Find full team info from allTeams list

            }

            if (gameJson.has("awayTeam")) {
                org.json.JSONObject awayTeamJson = gameJson.getJSONObject("awayTeam");
                StringBuilder keys = new StringBuilder();
                for (Iterator<String> it = awayTeamJson.keys(); it.hasNext(); ) {
                    String key = it.next();
                    keys.append(" ").append(key);
                }
//                Log.d("DatesActivity", "awayTeamJson keys " + keys);

                Team temp = new Team();
                temp.setTeamID(awayTeamJson.getInt("id"));
                temp.setAbreviatedName(awayTeamJson.optString("abbrev", ""));


                // Extract logo URL if available
                if (awayTeamJson.has("logo")) {
                    temp.logoUrl = awayTeamJson.getString("logo");
                    if(temp.logoUrl.contains("_secondary"))
                        temp.logoUrl = temp.logoUrl.replace("_secondary", "");
                }

                // Find full team info from allTeams list
                Team fullTeamInfo = findTeamById(temp.getTeamID());
                if (fullTeamInfo != null) {
                    temp.setFullName(fullTeamInfo.getFullName());
                    // Also copy abbreviation if it wasn't set properly
                    if (temp.getAbreviatedName().isEmpty()) {
                        temp.setAbreviatedName(fullTeamInfo.getAbreviatedName());
                    }
                } else {
                    // Fallback: try to set a reasonable full name based on abbreviation
                    if (!temp.getAbreviatedName().isEmpty()) {
                        temp.setFullName(getFullTeamName(temp.getAbreviatedName()));
                    }
                }

                game.setAwayTeam(temp);
                DataManager.getInstance().addTeam(temp);

                game.setAwayTeamId(temp.getTeamID());
                game.setAwayTeamName(temp.getAbreviatedName());

                // Set score
                if (awayTeamJson.has("score")) {
                    game.setAwayScore(awayTeamJson.getInt("score"));
                } else {
                    game.setAwayScore(-1);
                }


            }
            Log.d("DatesActivity", "awayTeam name " + game.getAwayTeam().getAbreviatedName() + " " + game.getAwayTeam().getFullName());
            Log.d("DatesActivity", "homeTeam name " + game.getHomeTeam().getAbreviatedName() + " " + game.getHomeTeam().getFullName());
            // Parse game state/status
            String gameState = gameJson.optString("gameState", "Unknown");
            if ("FUT".equalsIgnoreCase(gameState) || "PRE".equalsIgnoreCase(gameState)) {
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

    private Team findTeamById(int teamId) {
        for (Team team : allTeams) {
            if (team.getTeamID() == teamId) {
                return team;
            }
        }
        return null;
    }

    private void processSeasonGames(List<Game> seasonGames) {
        // Extract unique dates and cache games by date
        Map<String, List<Game>> gamesByDate = new HashMap<>();

        for (Game game : seasonGames) {
            String gameDate = game.getGameDate();
            if (!gamesByDate.containsKey(gameDate)) {
                gamesByDate.put(gameDate, new ArrayList<>());
            }
            Objects.requireNonNull(gamesByDate.get(gameDate)).add(game);
        }

        // Update cache
        gamesByDateCache.putAll(gamesByDate);
        Log.d("DatesActivty", "GamesByCache size " + gamesByDateCache.size());
        // Extract and sort dates
        List<String> dateList = new ArrayList<>(gamesByDate.keySet());
//        dateList.sort(Collections.reverseOrder());
        Collections.sort(dateList);


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

        if (!getTodayDateString().equals(date)) {
            stopLiveUpdates();
        }

        selectedDate = date;

        // Update the date adapter to highlight the selected date and scroll to it
        updateDateSelection(date);

        if (gamesByDateCache.containsKey(date)) {
            List<Game> cachedGames = gamesByDateCache.get(date);
            updateGamesDisplay(cachedGames);
            Log.d(TAG, "Using cached games for " + date + ": " + cachedGames.size() + " games");

            if (getTodayDateString().equals(date)) {
                startLiveUpdates();
            }
            return;
        }

        ArrayList<Game> gamesForDate = dataManager.getGamesForDate(date);
        if (gamesForDate != null && !gamesForDate.isEmpty()) {
            if(!gamesByDateCache.containsKey(date))
                gamesByDateCache.put(date, gamesForDate);
            updateGamesDisplay(gamesByDateCache.get(date));
            Log.d(TAG, "Using DataManager games for " + date + ": " + gamesForDate.size() + " games");

            if (getTodayDateString().equals(date)) {
                startLiveUpdates();
            }
        } else {
            updateGamesDisplay(new ArrayList<>());
            showMessage("No games found for " + date);
        }
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
//                dateRecyclerView.smoothScrollToPosition(selectedIndex);
                dateRecyclerView.scrollToPosition(selectedIndex);

                Log.d(TAG, "Updated date selection to: " + selectedDate + " at index: " + selectedIndex);
            } else {
                Log.w(TAG, "Selected date not found in dates list: " + selectedDate);
            }
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

        apiClient.makeAsyncRequests(requests);
    }

    private void updateGameFromLiveScore(int gameId, String response) {
        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(response);

            if (jsonResponse.has("gameState") && jsonResponse.has("homeTeam") && jsonResponse.has("awayTeam")) {
                int homeScore = jsonResponse.getJSONObject("homeTeam").optInt("score", -1);
                int awayScore = jsonResponse.getJSONObject("awayTeam").optInt("score", -1);

                for (List<Game> gameList : gamesByDateCache.values()) {
                    for (Game game : gameList) {
                        if (game.getGameId() == gameId) {
                            boolean scoreChanged = game.getHomeScore() != homeScore || game.getAwayScore() != awayScore;

                            game.setHomeScore(homeScore);
                            game.setAwayScore(awayScore);

                            if (scoreChanged) {
                                dataManager.addGame(game);

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

    private void showLoadingState(String message) {
//        loadingProgressBar.setVisibility(View.VISIBLE);
//        statusText.setVisibility(View.VISIBLE);
//        statusText.setText(message);
        dateRecyclerView.setVisibility(View.GONE);
        gamesRecyclerView.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
//        loadingProgressBar.setVisibility(View.GONE);
//        statusText.setVisibility(View.GONE);
        dateRecyclerView.setVisibility(View.VISIBLE);
        gamesRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorState(String message) {
//        loadingProgressBar.setVisibility(View.GONE);
//        statusText.setVisibility(View.VISIBLE);
//        statusText.setText(message);
        dateRecyclerView.setVisibility(View.GONE);
        gamesRecyclerView.setVisibility(View.GONE);
        showMessage(message);
    }

//    private void updateStatusText(String message) {
//        statusText.setText(message);
//    }

    private String getTodayDateString() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
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
        // Shutdown image downloader
        if (imageDownloader != null) {
            imageDownloader.shutdown();
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

    private void createFallbackTeams() {
        // Create a minimal fallback teams list using the NHL_TEAMS array
        allTeams = new ArrayList<>();
        int teamId = 1;

        for (String teamAbbr : NHL_TEAMS) {
            Team team = new Team();
            team.setTeamID(teamId++);
            team.setAbreviatedName(teamAbbr);
            team.setFullName(getFullTeamName(teamAbbr)); // Helper method
            allTeams.add(team);
        }

        Log.d(TAG, "Created fallback teams list: " + allTeams.size());
    }

    private void updateSpinnerWithSeasons() {
        if (seasons.isEmpty()) {
            return;
        }

        isUpdatingSpinner = true;

        // Create new adapter with populated seasons
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, seasons);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seasonSpinner.setAdapter(spinnerAdapter);

        // Set selection to most recent season (index 0)
        if (!seasons.isEmpty()) {
            seasonSpinner.setSelection(0);
            currentSeason = seasons.get(0);
        }

        isUpdatingSpinner = false;

        Log.d(TAG, "Updated spinner with " + seasons.size() + " seasons, selected: " + currentSeason);
    }

    private String getFullTeamName(String abbreviation) {
        // Map of abbreviations to full names for fallback
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
}