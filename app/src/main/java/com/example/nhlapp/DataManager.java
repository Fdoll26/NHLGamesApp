package com.example.nhlapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced DataManager with full async support using AsyncApiClient
 */
public class DataManager {
    private static final String TAG = "DataManager";
    private static DataManager instance;
    private AltImageDownloader imageDownloader;
    private boolean isDownloadingLogos = false;

    // Core data storage
    private final HashMap<Integer, Team> teamsById = new HashMap<>();
    private final HashMap<Integer, NHLPlayer> playersById = new HashMap<>();
    private final HashMap<Integer, Game> gamesById = new HashMap<>();
    private final HashMap<String, ArrayList<Integer>> gamesByDate = new HashMap<>();
    private final HashMap<String, List<Integer>> gamesBySeason = new HashMap<>();
    private final List<String> seasons = new ArrayList<>();

    // Async components
    private AsyncApiClient apiClient;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Context context;

    // Data loading flags
    private boolean teamsLoadedFromJson = false;
    private boolean playersLoadedFromJson = false;
    private boolean seasonsLoadedFromJson = false;
    private boolean gamesLoadedFromJson = false;
    private boolean gamesBySeasonLoadedFromJson = false;
    private boolean isInitialLoadComplete = false;

    private DataManager() {
        apiClient = new AsyncApiClient();
        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }


    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        if (settings.isJsonSavingEnabled()) {
            loadDataFromJsonAsync(() -> {
                isInitialLoadComplete = true;
                Log.d(TAG, "Initial data load complete from JSON files");
            });
        } else {
            isInitialLoadComplete = true;
        }
    }

    // Async methods for getting data with API fallback

    public void getTeamsAsync(DataCallback<List<Team>> callback) {
        if (!teamsById.isEmpty()) {
            callback.onSuccess(new ArrayList<>(teamsById.values()));
            return;
        }

        // Load from API
        String teamsUrl = "https://api.nhle.com/stats/rest/en/team";
        apiClient.makeAsyncRequest(
                teamsUrl,
                new AsyncApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String response) {
                        parseTeamsResponse(response, callback);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to load teams: " + error);
                    }
                },
                String.class,
                new AtomicBoolean(false)
        );
    }

    public void getPlayersAsync(DataCallback<List<NHLPlayer>> callback) {
        if (!playersById.isEmpty()) {
            callback.onSuccess(new ArrayList<>(playersById.values()));
            return;
        }

        // First get teams, then get players for each team
        getTeamsAsync(new DataCallback<List<Team>>() {
            @Override
            public void onSuccess(List<Team> teams) {
                loadPlayersForTeamsAsync(teams, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to load teams for player lookup: " + error);
            }
        });
    }

    public void getSeasonsAsync(DataCallback<List<String>> callback) {
        if (!seasons.isEmpty()) {
            callback.onSuccess(new ArrayList<>(seasons));
            return;
        }

        String seasonsUrl = "https://api-web.nhle.com/v1/season";
        apiClient.makeAsyncRequest(
                seasonsUrl,
                new AsyncApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String response) {
                        parseSeasonsResponse(response, callback);
                    }

                    @Override
                    public void onError(String error) {
                        // Fallback to hardcoded seasons
                        seasons.clear();
                        seasons.add("20242025");
                        seasons.add("20232024");
                        seasons.add("20222023");
                        seasons.add("20212022");
                        seasons.add("20202021");
                        callback.onSuccess(new ArrayList<>(seasons));
                    }
                },
                String.class,
                new AtomicBoolean(false)
        );
    }

    public void getGamesForSeasonAsync(String season, boolean forceRefresh, DataCallback<List<Game>> callback) {
        // Check cache first if not forcing refresh
        if (!forceRefresh && gamesBySeason.containsKey(season) && !Objects.requireNonNull(gamesBySeason.get(season)).isEmpty()) {
            List<Game> cachedGames = new ArrayList<>();
            for (Integer gameId : Objects.requireNonNull(gamesBySeason.get(season))) {
                Game game = gamesById.get(gameId);
                if (game != null) {
                    cachedGames.add(game);
                }
            }
            callback.onSuccess(cachedGames);
            return;
        }

        // Load from API
        String scheduleUrl = "https://api-web.nhle.com/v1/schedule/" + season;
        apiClient.makeAsyncRequest(
                scheduleUrl,
                new AsyncApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String response) {
                        parseScheduleResponse(response, season, callback);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to load games for season " + season + ": " + error);
                    }
                },
                String.class,
                new AtomicBoolean(false)
        );
    }

    public void getGamesForDateAsync(String date, DataCallback<List<Game>> callback) {
        if (gamesByDate.containsKey(date)) {
            List<Game> dateGames = new ArrayList<>();
            for (Integer gameId : Objects.requireNonNull(gamesByDate.get(date))) {
                Game game = gamesById.get(gameId);
                if (game != null) {
                    dateGames.add(game);
                }
            }
            callback.onSuccess(dateGames);
            return;
        }

        // Load games for the specific date
        String dateUrl = "https://api-web.nhle.com/v1/schedule/" + date;
        apiClient.makeAsyncRequest(
                dateUrl,
                new AsyncApiClient.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String response) {
                        parseDateGamesResponse(response, date, callback);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to load games for date " + date + ": " + error);
                    }
                },
                String.class,
                new AtomicBoolean(false)
        );
    }

    public void getGameBoxscoreAsync(int gameId, DataCallback<Game> callback) {
        String boxscoreUrl = "https://api-web.nhle.com/v1/gamecenter/" + gameId + "/boxscore";

        apiClient.makeAsyncRequest(
                boxscoreUrl,
                new AsyncApiClient.ApiCallback<Game>() {
                    @Override
                    public void onSuccess(Game result) {
                        // Update game data with boxscore info
                        updateGameFromBoxscore(gameId, result);
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to load boxscore for game " + gameId + ": " + error);
                    }
                },
                Game.class,
                new AtomicBoolean(false)
        );
    }

    // Response parsing methods

    private void parseTeamsResponse(String response, DataCallback<List<Team>> callback) {
        executorService.execute(() -> {
            try {
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                org.json.JSONArray teamsArray = jsonResponse.getJSONArray("data");

                List<Team> teams = new ArrayList<>();
                for (int i = 0; i < teamsArray.length(); i++) {
                    org.json.JSONObject teamJson = teamsArray.getJSONObject(i);
                    Team team = new Team();
                    team.setTeamID(teamJson.getInt("id"));
                    team.setName(teamJson.getString("fullName"));
                    if (teamJson.has("triCode")) {
                        team.setAbreviatedName(teamJson.getString("triCode"));
                    }
                    teams.add(team);
                }

                // Update cache
                teamsById.clear();
                for (Team team : teams) {
                    teamsById.put(team.getTeamID(), team);
                }

                mainHandler.post(() -> callback.onSuccess(teams));

            } catch (Exception e) {
                Log.e(TAG, "Error parsing teams response", e);
                mainHandler.post(() -> callback.onError("Failed to parse teams data"));
            }
        });
    }

    private void loadPlayersForTeamsAsync(List<Team> teams, DataCallback<List<NHLPlayer>> callback) {
        List<AsyncApiClient.ApiRequest<String>> requests = new ArrayList<>();

        for (Team team : teams) {
            if (team.getAbreviatedName() != null) {
                String rosterUrl = "https://api-web.nhle.com/v1/roster/" + team.getAbreviatedName() + "/current";

                AsyncApiClient.ApiRequest<String> request = new AsyncApiClient.ApiRequest<>(
                        rosterUrl,
                        new AsyncApiClient.ApiCallback<String>() {
                            @Override
                            public void onSuccess(String response) {
                                parsePlayersFromRoster(response, team.getTeamID());
                            }

                            @Override
                            public void onError(String error) {
                                Log.w(TAG, "Failed to get roster for team: " + team.getName());
                            }
                        },
                        String.class,
                        new AtomicBoolean(false)
                );

                requests.add(request);
            }
        }

        // Execute all roster requests concurrently
        apiClient.makeAsyncRequests(requests);

        // Use a delay to wait for most requests to complete, then return results
        mainHandler.postDelayed(() -> {
            callback.onSuccess(new ArrayList<>(playersById.values()));
        }, 5000); // Wait 5 seconds for most requests to complete
    }

    private void parsePlayersFromRoster(String response, int teamId) {
        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(response);

            // Parse forwards
            if (jsonResponse.has("forwards")) {
                parsePlayerArray(jsonResponse.getJSONArray("forwards"), teamId, "C");
            }
            // Parse defensemen
            if (jsonResponse.has("defensemen")) {
                parsePlayerArray(jsonResponse.getJSONArray("defensemen"), teamId, "D");
            }
            // Parse goalies
            if (jsonResponse.has("goalies")) {
                parsePlayerArray(jsonResponse.getJSONArray("goalies"), teamId, "G");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing roster for team " + teamId, e);
        }
    }

    private void parsePlayerArray(org.json.JSONArray playerArray, int teamId, String position) {
        try {
            for (int i = 0; i < playerArray.length(); i++) {
                org.json.JSONObject playerJson = playerArray.getJSONObject(i);
                NHLPlayer player = new NHLPlayer();
                player.setPlayerId(playerJson.getInt("id"));
                player.setPosition(position);
                player.setTeamId(teamId);

                // Handle different name formats
                if (playerJson.has("firstName") && playerJson.has("lastName")) {
                    String fullName = playerJson.getString("firstName") + " " + playerJson.getString("lastName");
                    player.setName(fullName);
                } else if (playerJson.has("fullName")) {
                    player.setName(playerJson.getString("fullName"));
                }

                playersById.put(player.getPlayerId(), player);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing player array", e);
        }
    }

    private void parseSeasonsResponse(String response, DataCallback<List<String>> callback) {
        executorService.execute(() -> {
            try {
                org.json.JSONArray seasonsArray = new org.json.JSONArray(response);
                seasons.clear();

                for (int i = 0; i < seasonsArray.length(); i++) {
                    String season = seasonsArray.getString(i);
                    seasons.add(season);
                }

                mainHandler.post(() -> callback.onSuccess(new ArrayList<>(seasons)));

            } catch (Exception e) {
                Log.e(TAG, "Error parsing seasons response", e);
                mainHandler.post(() -> callback.onError("Failed to parse seasons data"));
            }
        });
    }

    private void parseScheduleResponse(String response, String season, DataCallback<List<Game>> callback) {
        executorService.execute(() -> {
            try {
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                List<Game> games = new ArrayList<>();

                if (jsonResponse.has("gameWeek")) {
                    org.json.JSONArray gameWeeks = jsonResponse.getJSONArray("gameWeek");

                    for (int i = 0; i < gameWeeks.length(); i++) {
                        org.json.JSONObject week = gameWeeks.getJSONObject(i);
                        if (week.has("games")) {
                            org.json.JSONArray weekGames = week.getJSONArray("games");

                            for (int j = 0; j < weekGames.length(); j++) {
                                org.json.JSONObject gameJson = weekGames.getJSONObject(j);
                                Game game = parseGameFromJson(gameJson);
                                if (game != null) {
                                    games.add(game);
                                }
                            }
                        }
                    }
                }

                // Update cache
                addSeasonGames(season, games);

                mainHandler.post(() -> callback.onSuccess(games));

            } catch (Exception e) {
                Log.e(TAG, "Error parsing schedule response", e);
                mainHandler.post(() -> callback.onError("Failed to parse schedule data"));
            }
        });
    }

    private void parseDateGamesResponse(String response, String date, DataCallback<List<Game>> callback) {
        executorService.execute(() -> {
            try {
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                List<Game> games = new ArrayList<>();

                if (jsonResponse.has("gameWeek")) {
                    org.json.JSONArray gameWeeks = jsonResponse.getJSONArray("gameWeek");

                    for (int i = 0; i < gameWeeks.length(); i++) {
                        org.json.JSONObject week = gameWeeks.getJSONObject(i);
                        if (week.has("games")) {
                            org.json.JSONArray weekGames = week.getJSONArray("games");

                            for (int j = 0; j < weekGames.length(); j++) {
                                org.json.JSONObject gameJson = weekGames.getJSONObject(j);
                                Game game = parseGameFromJson(gameJson);
                                if (game != null && game.getGameDate().equals(date)) {
                                    games.add(game);
                                }
                            }
                        }
                    }
                }

                // Update cache
                addGamesForDate(date, new ArrayList<>(games));

                mainHandler.post(() -> callback.onSuccess(games));

            } catch (Exception e) {
                Log.e(TAG, "Error parsing date games response", e);
                mainHandler.post(() -> callback.onError("Failed to parse date games data"));
            }
        });
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
                game.setHomeScore(homeTeam.optInt("score", -1));
            }

            if (gameJson.has("awayTeam")) {
                org.json.JSONObject awayTeam = gameJson.getJSONObject("awayTeam");
                game.setAwayTeamId(awayTeam.getInt("id"));
                game.setAwayTeamName(awayTeam.optString("abbrev", ""));
                game.setAwayScore(awayTeam.optInt("score", -1));
            }

            return game;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing game JSON", e);
            return null;
        }
    }

    private void updateGameFromBoxscore(int gameId, Game boxscore) {
        Game game = gamesById.get(gameId);
        if (game != null && boxscore.getAwayScore() > -1 && boxscore.getHomeScore() > -1) {
            game.setHomeScore(boxscore.getHomeScore());
            game.setAwayScore(boxscore.getAwayScore());
            gamesById.put(gameId, game);
        }
    }

    // Existing synchronous methods for backward compatibility and cached data access

    public ArrayList<Game> getGamesForDate(String date) {
        if (!gamesByDate.containsKey(date)) {
            return new ArrayList<>();
        }

        ArrayList<Game> games = new ArrayList<>();
        List<Integer> gameIds = gamesByDate.get(date);

        if (gameIds != null) {
            for (int gameId : gameIds) {
                Game game = gamesById.get(gameId);
                if (game != null) {
                    games.add(game);
                }
            }
        }

        return games;
    }

    public Game getGameById(int gameId) {
        return gamesById.get(gameId);
    }

    public Team getTeamById(int teamId) {
        return teamsById.get(teamId);
    }

    public NHLPlayer getPlayerById(int playerId) {
        return playersById.get(playerId);
    }

    public void addGame(Game game) {
        if (game == null || game.getGameDate() == null) {
            return;
        }

        gamesById.put(game.getGameId(), game);

        String gameDate = game.getGameDate();
        if (!gamesByDate.containsKey(gameDate)) {
            gamesByDate.put(gameDate, new ArrayList<>());
        }

        List<Integer> gamesForDate = gamesByDate.get(gameDate);
        if (gamesForDate != null && !gamesForDate.contains(game.getGameId())) {
            gamesForDate.add(game.getGameId());
        }
    }

//    public void addPlayer(NHLPlayer player) {
//        if (player != null) {
//            playersById.put(player.getPlayerId(), player);
//        }
//    }
    public void saveAllDataToJson(Context context) {
        new SaveDataTask(context).execute();
    }

    public void saveSpecificDataToJson(Context context, String dataToSave) {
        new SaveSpecificDataTask(context, dataToSave).execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class SaveSpecificDataTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private String dataToSave;

        public SaveSpecificDataTask(Context context, String dataToSave) {
            this.context = context;
            this.dataToSave = dataToSave;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Save all data to JSON files
            if(Objects.equals(dataToSave, "Teams"))
                JsonHelper.saveTeamsToJson(context, new ArrayList<>(teamsById.values()));
//                JsonHelper.saveTeamsToJson(context, (List<Team>) teamsById.values().stream().iterator());
            if(Objects.equals(dataToSave, "Players"))
                JsonHelper.savePlayersToJson(context, (List<NHLPlayer>) playersById.values());
            if(Objects.equals(dataToSave, "Games"))
                JsonHelper.saveGamesToJson(context, (List<Game>) gamesById.values());// forgot we cant use rust iterators and collapse
            if(Objects.equals(dataToSave, "Seasons"))
                JsonHelper.saveSeasonsToJson(context, seasons);
            if(Objects.equals(dataToSave, "Images"))
                JsonHelper.saveImagePathsToJson(context);
            if(Objects.equals(dataToSave, "GamesBySeason")){
                HashMap<String, List<Game>> gameHash = new HashMap<>();
                for(String season: gamesBySeason.keySet()){
                    List<Game> games = new ArrayList<>();
                    for(int gameId: Objects.requireNonNull(gamesBySeason.get(season))){
                        if (gamesById.containsKey(gameId))
                            games.add(gamesById.get(gameId));
                    }
                    if(!games.isEmpty())
                        gameHash.put(season, games);
                }

                JsonHelper.saveGamesBySeasonToJson(context, gameHash);
            }

            Log.d("DataManager","Finished background saving for " + dataToSave);
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SaveDataTask extends AsyncTask<Void, Void, Void> {
        private Context context;

        public SaveDataTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Save all data to JSON files
            JsonHelper.saveTeamsToJson(context, (List<Team>) teamsById.values());
            JsonHelper.savePlayersToJson(context, (List<NHLPlayer>) playersById.values());
            JsonHelper.saveGamesToJson(context, (List<Game>) gamesById.values());
            JsonHelper.saveSeasonsToJson(context, seasons);
            JsonHelper.saveImagePathsToJson(context);
            HashMap<String, List<Game>> gameHash = new HashMap<>();
            for(String season: gamesBySeason.keySet()){
                List<Game> games = new ArrayList<>();
                for(int gameId: Objects.requireNonNull(gamesBySeason.get(season))){
                    if (gamesById.containsKey(gameId))
                        games.add(gamesById.get(gameId));
                }
                if(!games.isEmpty())
                    gameHash.put(season, games);
            }
            JsonHelper.saveGamesBySeasonToJson(context, gameHash);
            return null;
        }
    }

    public boolean isSeasonsLoadedFromJson() {
        return seasonsLoadedFromJson;
    }

    public Map<String, List<Game>> getGamesForSeason() {
        Map<String, List<Game>> result = new HashMap<>();

        for (String season : gamesBySeason.keySet()) {
            List<Integer> gameIds = gamesBySeason.get(season);
            if (gameIds != null) {
                List<Game> games = new ArrayList<>();
                for (Integer gameId : gameIds) {
                    Game game = gamesById.get(gameId);
                    if (game != null) {
                        games.add(game);
                    }
                }
                result.put(season, games);
            }
        }

        return result;
    }

    // Or better yet, add a more specific method:
    public List<Game> getCachedGamesForSeason(String season) {
        if (!gamesBySeason.containsKey(season)) {
            return new ArrayList<>();
        }

        List<Integer> gameIds = gamesBySeason.get(season);
        List<Game> games = new ArrayList<>();

        if (gameIds != null) {
            for (Integer gameId : gameIds) {
                Game game = gamesById.get(gameId);
                if (game != null) {
                    games.add(game);
                }
            }
        }

        return games;
    }

    public void addTeam(Team team) {
        if (team != null) {
            teamsById.put(team.getTeamID(), team);
        }
    }

    public void addSeasonGames(String season, List<Game> games) {
        if (games == null || games.isEmpty()) {
            return;
        }

        ArrayList<Integer> gameIds = new ArrayList<>();

        for (Game game : games) {
            if (game == null) continue;

            gameIds.add(game.getGameId());
            gamesById.put(game.getGameId(), game);

            // Add to games by date
            String gameDate = game.getGameDate();
            if (gameDate != null) {
                if (!gamesByDate.containsKey(gameDate)) {
                    gamesByDate.put(gameDate, new ArrayList<>());
                }
                List<Integer> gamesForDate = gamesByDate.get(gameDate);
                if (gamesForDate != null && !gamesForDate.contains(game.getGameId())) {
                    gamesForDate.add(game.getGameId());
                }
            }
        }

        // Update season games mapping
        gamesBySeason.put(season, gameIds);
        Log.d(TAG, "Added " + games.size() + " games for season " + season);
    }

    public void addGamesForDate(String date, ArrayList<Game> games) {
        if (!gamesByDate.containsKey(date)) {
            gamesByDate.put(date, new ArrayList<>());
        }

        for (Game game : games) {
            gamesById.put(game.getGameId(), game);
            if (!Objects.requireNonNull(gamesByDate.get(date)).contains(game.getGameId())) {
                Objects.requireNonNull(gamesByDate.get(date)).add(game.getGameId());
            }
        }
    }

    public ArrayList<String> getAllDates() {
        ArrayList<String> dates = new ArrayList<>(gamesByDate.keySet());
        dates.sort(Collections.reverseOrder());
        return dates;
    }

    public boolean hasGamesForSeason(String season) {
        return gamesBySeason.containsKey(season) &&
                gamesBySeason.get(season) != null &&
                !Objects.requireNonNull(gamesBySeason.get(season)).isEmpty();
    }

    public int getGamesCountForSeason(String season) {
        if (!gamesBySeason.containsKey(season)) {
            return 0;
        }
        List<Integer> gameIds = gamesBySeason.get(season);
        return gameIds != null ? gameIds.size() : 0;
    }

    public List<Team> getCachedTeams() {
        return new ArrayList<>(teamsById.values());
    }

    public List<NHLPlayer> getCachedPlayers() {
        return new ArrayList<>(playersById.values());
    }

    public List<String> getCachedSeasons() {
        return new ArrayList<>(seasons);
    }

    public boolean isInitialLoadComplete() {
        return isInitialLoadComplete;
    }

    // JSON loading methods (async)
    private void loadDataFromJsonAsync(Runnable onComplete) {
        executorService.execute(() -> {
            teamsLoadedFromJson = loadTeamsFromJson();
            playersLoadedFromJson = loadPlayersFromJson();
            seasonsLoadedFromJson = loadSeasonsFromJson();
            gamesLoadedFromJson = loadGamesFromJson();
            gamesBySeasonLoadedFromJson = loadGamesBySeasonFromJson();

            Log.d(TAG, String.format("Data loaded from JSON - Teams: %b, Players: %b, Seasons: %b, Games: %b, GamesBySeason: %b",
                    teamsLoadedFromJson, playersLoadedFromJson, seasonsLoadedFromJson, gamesLoadedFromJson, gamesBySeasonLoadedFromJson));

            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    private boolean loadTeamsFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "teams.json");
        if (jsonData != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonData);

                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject teamJson = jsonArray.getJSONObject(i);
                    Team team = new Team();
                    team.setTeamID(teamJson.getInt("id"));
                    team.setName(teamJson.getString("name"));
                    team.setLogoUrl(teamJson.optString("logoPath", ""));

                    if (teamJson.has("rosterPlayerIds")) {
                        org.json.JSONArray rosterArray = teamJson.getJSONArray("rosterPlayerIds");
                        List<Integer> rosterIds = new ArrayList<>();
                        for (int j = 0; j < rosterArray.length(); j++) {
                            rosterIds.add(rosterArray.getInt(j));
                        }
                        team.setRosterPlayerIds(rosterIds);
                    }

                    teamsById.put(team.getTeamID(), team);
                }

                Log.d(TAG, "Loaded " + teamsById.size() + " teams from JSON");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error loading teams from JSON", e);
            }
        }
        return false;
    }

    private boolean loadPlayersFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "players.json");
        if (jsonData != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonData);

                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject playerJson = jsonArray.getJSONObject(i);
                    NHLPlayer player = new NHLPlayer();
                    player.setPlayerId(playerJson.getInt("id"));
                    player.setName(playerJson.getString("name"));
                    player.setTeamId(playerJson.getInt("teamId"));
                    player.setHeadshotPath(playerJson.optString("headshotPath", ""));
                    playersById.put(player.getPlayerId(), player);
                }

                Log.d(TAG, "Loaded " + playersById.size() + " players from JSON");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error loading players from JSON", e);
            }
        }
        return false;
    }

    private boolean loadSeasonsFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "seasons.json");
        if (jsonData != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonData);
                seasons.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    seasons.add(jsonArray.getString(i));
                }

                Log.d(TAG, "Loaded " + seasons.size() + " seasons from JSON");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error loading seasons from JSON", e);
            }
        }
        return false;
    }

    private boolean loadGamesFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "games.json");
        if (jsonData != null) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonData);

                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject gameJson = jsonArray.getJSONObject(i);
                    Game game = new Game();
                    game.setGameId(gameJson.getInt("id"));
                    game.setGameDate(gameJson.getString("date"));
                    game.setHomeTeamId(gameJson.getInt("homeTeamId"));
                    game.setAwayTeamId(gameJson.getInt("awayTeamId"));
                    game.setHomeScore(gameJson.optInt("homeScore", -1));
                    game.setAwayScore(gameJson.optInt("awayScore", -1));

                    addGame(game);
                }

                Log.d(TAG, "Loaded " + gamesById.size() + " games from JSON");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error loading games from JSON", e);
            }
        }
        return false;
    }

    private boolean loadGamesBySeasonFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "gamesBySeason.json");
        if (jsonData != null) {
            try {
                org.json.JSONObject seasonObject = new org.json.JSONObject(jsonData);

                for (java.util.Iterator<String> it = seasonObject.keys(); it.hasNext(); ) {
                    String season = it.next();
                    org.json.JSONArray gamesArray = seasonObject.getJSONArray(season);
                    List<Game> seasonGames = new ArrayList<>();

                    for (int i = 0; i < gamesArray.length(); i++) {
                        org.json.JSONObject gameJson = gamesArray.getJSONObject(i);
                        Game game = new Game();
                        game.setGameId(gameJson.getInt("id"));
                        game.setGameDate(gameJson.getString("date"));
                        game.setHomeTeamId(gameJson.getInt("homeTeamId"));
                        game.setAwayTeamId(gameJson.getInt("awayTeamId"));
                        game.setHomeScore(gameJson.optInt("homeScore", -1));
                        game.setAwayScore(gameJson.optInt("awayScore", -1));
                        game.setHomeTeamName(gameJson.optString("homeName", ""));
                        game.setAwayTeamName(gameJson.optString("awayName", ""));
                        seasonGames.add(game);
                    }

                    addSeasonGames(season, seasonGames);
                }

                Log.d(TAG, "Loaded games for " + gamesBySeason.size() + " seasons from JSON");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error loading gamesBySeason from JSON", e);
            }
        }
        return false;
    }

    // Cleanup methods
    public void shutdown() {
        if (apiClient != null) {
            apiClient.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        if (imageDownloader != null) {
            imageDownloader.shutdown();
        }
    }

    public void clearCache() {
        teamsById.clear();
        playersById.clear();
        gamesById.clear();
        gamesByDate.clear();
        gamesBySeason.clear();
        seasons.clear();
    }

    // Keep old callback-based methods for backward compatibility
    public void getGamesForSeason(String season, boolean forceRefresh, DataCallback<List<Game>> callback) {
        getGamesForSeasonAsync(season, forceRefresh, callback);
    }

    public void getGamesForSeason(String season, DataCallback<List<Game>> callback) {
        getGamesForSeasonAsync(season, false, callback);
    }

    public void getTeams(DataCallback<List<Team>> callback) {
        getTeamsAsync(callback);
    }

    public void getPlayers(DataCallback<List<NHLPlayer>> callback) {
        getPlayersAsync(callback);
    }

    public void getSeasons(DataCallback<List<String>> callback) {
        getSeasonsAsync(callback);
    }

    // Debug and utility methods
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("DataManager Debug Info:\n");
        info.append("Teams: ").append(teamsById.size()).append("\n");
        info.append("Players: ").append(playersById.size()).append("\n");
        info.append("Total Games: ").append(gamesById.size()).append("\n");
        info.append("Games by Date: ").append(gamesByDate.size()).append(" dates\n");
        info.append("Seasons: ").append(gamesBySeason.size()).append("\n");

        for (String season : gamesBySeason.keySet()) {
            List<Integer> seasonGames = gamesBySeason.get(season);
            info.append("  ").append(season).append(": ");
            info.append(seasonGames != null ? seasonGames.size() : 0).append(" games\n");
        }

        return info.toString();
    }

    public void addPlayer(NHLPlayer player){
        if(playersById.containsKey(player.getPlayerId())){
            // Realistically this is where the combine code comes in handy, really just a way to update whats new
            // Good way to combine different data avenues into one location (overlay lol)
            playersById.replace(player.getPlayerId(), player);
        }else{
            playersById.put(player.getPlayerId(), player);
        }


    }

    public void addPlayers(List<NHLPlayer> recievedPlayers){
        for (NHLPlayer player: recievedPlayers){
            if(playersById.containsKey(player.getPlayerId())){
                // Realistically this is where the combine code comes in handy, really just a way to update whats new
                // Good way to combine different data avenues into one location (overlay lol)
                playersById.replace(player.getPlayerId(), player);
            }else{
                playersById.put(player.getPlayerId(), player);
            }
        }

    }

    public void downloadTeamLogos(DataCallback<String> callback) {
        if (isDownloadingLogos) {
            if (callback != null) {
                callback.onError("Logo download already in progress");
            }
            return;
        }

        getTeamsAsync(new DataCallback<List<Team>>() {
            @Override
            public void onSuccess(List<Team> teams) {
                if (teams == null || teams.isEmpty()) {
                    if (callback != null) {
                        callback.onError("No teams available for logo download");
                    }
                    return;
                }

                isDownloadingLogos = true;
                Log.d(TAG, "Starting logo download for " + teams.size() + " teams");

                imageDownloader.downloadTeamLogos(teams, new AltImageDownloader.DownloadCallback() {
                    @Override
                    public void onComplete(int successful, int failed, int skipped) {
                        isDownloadingLogos = false;
                        String result = String.format("Logo download complete: %d successful, %d failed, %d skipped", successful, failed, skipped);
                        Log.d(TAG, result);

                        // Update teams in cache with logo paths
                        for (Team team : teams) {
                            if (imageDownloader.hasLocalLogo(team)) {
                                String logoPath = imageDownloader.getLocalLogoPath(team);
                                team.setLogoPath(logoPath);
                                teamsById.put(team.getTeamID(), team);
                            }
                        }

                        // Save updated team data
                        if (context != null) {
                            AppSettings settings = AppSettings.getInstance(context);
                            if (settings.isJsonSavingEnabled()) {
                                saveSpecificDataToJson(context, "Teams");
                            }
                        }

                        if (callback != null) {
                            callback.onSuccess(result);
                        }
                    }

                    @Override
                    public void onProgress(String teamName, boolean success) {
                        Log.d(TAG, "Logo download progress - " + teamName + ": " + (success ? "success" : "failed/skipped"));
                    }
                });
            }

            @Override
            public void onError(String error) {
                isDownloadingLogos = false;
                Log.e(TAG, "Failed to get teams for logo download: " + error);
                if (callback != null) {
                    callback.onError("Failed to get teams: " + error);
                }
            }
        });
    }

    /**
     * Update teams with existing logo paths
     */
    public void updateTeamsWithLogoPaths() {
        if (imageDownloader == null) {
            imageDownloader = new AltImageDownloader(context);
        }

        List<Team> teams = new ArrayList<>(teamsById.values());
        imageDownloader.updateTeamsWithLogoPaths(teams);

        // Update cache
        for (Team team : teams) {
            teamsById.put(team.getTeamID(), team);
        }
    }

    /**
     * Check if logos are currently being downloaded
     */
    public boolean isDownloadingLogos() {
        return isDownloadingLogos;
    }

    /**
     * Get logo cache size
     */
    public long getLogoCacheSize() {
        if (imageDownloader == null) {
            imageDownloader = new AltImageDownloader(context);
        }
        return imageDownloader.getCacheSize();
    }

    /**
     * Clear logo cache
     */
    public void clearLogoCache() {
        if (imageDownloader == null) {
            imageDownloader = new AltImageDownloader(context);
        }
        imageDownloader.clearCache();

        // Clear logo paths from teams
        for (Team team : teamsById.values()) {
            team.setLogoPath(null);
        }
    }

    /**
     * Clean up old logos
     */
    public void cleanupOldLogos() {
        if (imageDownloader == null) {
            imageDownloader = new AltImageDownloader(context);
        }
        imageDownloader.cleanupOldLogos();
    }

    // Update the shutdown method to include imageDownloader:
//    public void shutdown() {
//        if (apiClient != null) {
//            apiClient.shutdown();
//        }
//        if (executorService != null) {
//            executorService.shutdown();
//        }
//        if (imageDownloader != null) {
//            imageDownloader.shutdown();
//        }
//    }
}