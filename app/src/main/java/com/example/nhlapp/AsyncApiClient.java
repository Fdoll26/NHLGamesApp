package com.example.nhlapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Team;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncApiClient {
    private static final String TAG = "AsyncApiClient";
    private static final int MAX_CONCURRENT_REQUESTS = 10;
    private static final int REQUEST_TIMEOUT = 10000; // 10 seconds

    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Gson gson;
    private final List<Future<?>> activeTasks;

    public AsyncApiClient() {
        executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);
        mainHandler = new Handler(Looper.getMainLooper());
        gson = new Gson();
        activeTasks = new ArrayList<>();
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public static class ApiRequest<T> {
        private final String url;
        private final ApiCallback<T> callback;
        private final Class<T> responseClass;
        private final AtomicBoolean cancellationFlag;

        public ApiRequest(String url, ApiCallback<T> callback, Class<T> responseClass, AtomicBoolean cancellationFlag) {
            this.url = url;
            this.callback = callback;
            this.responseClass = responseClass;
            this.cancellationFlag = cancellationFlag;
        }

        public String getUrl() { return url; }
        public ApiCallback<T> getCallback() { return callback; }
        public Class<T> getResponseClass() { return responseClass; }
        public AtomicBoolean getCancellationFlag() { return cancellationFlag; }
    }

    /**
     * Make a single async API request
     */
    public <T> void makeAsyncRequest(String url, ApiCallback<T> callback, Class<T> responseClass, AtomicBoolean cancellationFlag) {
        ApiRequest<T> request = new ApiRequest<>(url, callback, responseClass, cancellationFlag);
        executeRequest(request);
    }

    /**
     * Make multiple concurrent API requests
     */
    public <T> void makeAsyncRequests(List<ApiRequest<T>> requests) {
        for (ApiRequest<T> request : requests) {
            executeRequest(request);
        }
    }

    private <T> void executeRequest(ApiRequest<T> request) {
        Future<?> task = executorService.submit(() -> {
            try {
                // Check cancellation before starting
                if (request.getCancellationFlag() != null && request.getCancellationFlag().get()) {
                    Log.d(TAG, "Request cancelled before execution: " + request.getUrl());
                    return;
                }

                String response = makeApiCall(request.getUrl(), request.getCancellationFlag());

                // Check cancellation after API call
                if (request.getCancellationFlag() != null && request.getCancellationFlag().get()) {
                    Log.d(TAG, "Request cancelled after API call: " + request.getUrl());
                    return;
                }

                // Parse response based on type
                T parsedResponse = parseResponse(response, request.getResponseClass());

                // Check cancellation before callback
                if (request.getCancellationFlag() != null && request.getCancellationFlag().get()) {
                    Log.d(TAG, "Request cancelled before callback: " + request.getUrl());
                    return;
                }

                // Post success to main thread
                mainHandler.post(() -> {
                    if (request.getCancellationFlag() == null || !request.getCancellationFlag().get()) {
                        request.getCallback().onSuccess(parsedResponse);
                    }
                });

            } catch (Exception e) {
                // Check if cancellation caused the exception
                if (request.getCancellationFlag() != null && request.getCancellationFlag().get()) {
                    Log.d(TAG, "Request cancelled via exception: " + request.getUrl());
                    return;
                }

                Log.e(TAG, "API request failed: " + request.getUrl(), e);

                // Post error to main thread
                mainHandler.post(() -> {
                    if (request.getCancellationFlag() == null || !request.getCancellationFlag().get()) {
                        request.getCallback().onError(e.getMessage());
                    }
                });
            }
        });

        synchronized (activeTasks) {
            activeTasks.add(task);
        }
    }

    private String makeApiCall(String urlString, AtomicBoolean cancellationFlag) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // Check cancellation before connection
            if (cancellationFlag != null && cancellationFlag.get()) {
                throw new Exception("Request cancelled");
            }

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setReadTimeout(REQUEST_TIMEOUT);
            connection.setRequestProperty("User-Agent", "NHL-App/1.0");
            connection.setRequestProperty("Accept", "application/json");

            // Check cancellation before connecting
            if (cancellationFlag != null && cancellationFlag.get()) {
                throw new Exception("Request cancelled");
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("API call failed with response code: " + responseCode);
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // Check cancellation during reading
                if (cancellationFlag != null && cancellationFlag.get()) {
                    throw new Exception("Request cancelled");
                }
                response.append(line);
            }

            return response.toString();

        } catch (java.io.InterruptedIOException e) {
            Log.d(TAG, "API call interrupted: " + urlString);
            throw new Exception("Request cancelled", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T parseResponse(String response, Class<T> responseClass) throws Exception {
        try {
            if (responseClass == String.class) {
                return (T) response;
            }
            // Handle specific parsing for known types
            if (responseClass == Game.class) {
                return (T) parseCurrentGame(response);
            }
            // For other types, use generic Gson parsing
            return gson.fromJson(response, responseClass);
        } catch (JsonSyntaxException e) {
            throw new Exception("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

//    private Game parseCurrentGame(String response) throws Exception {
//        try {
//            // Parse the JSON response into GameBoxscore object
//            org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
//            Game activeGame = new Game();
//
//            // Parse linescore
//            if (jsonResponse.has("linescore")) {
//                org.json.JSONObject linescoreJson = jsonResponse.getJSONObject("linescore");
//
//                    if (linescoreJson.has("totals")) {
//                    GameBoxscore.Totals totals = new GameBoxscore.Totals();
//                    org.json.JSONObject totalsJson = linescoreJson.getJSONObject("totals");
//                    totals.setHome(totalsJson.optInt("home", 0));
//                    totals.setAway(totalsJson.optInt("away", 0));
//                    linescore.setTotals(totals);
//                }
//                boxscore.setLinescore(linescore);
//            }
//
//            // Parse playerByGameStats
//            if (jsonResponse.has("playerByGameStats")) {
//                org.json.JSONObject playerStatsJson = jsonResponse.getJSONObject("playerByGameStats");
//                GameBoxscore.PlayerByGameStats playerStats = new GameBoxscore.PlayerByGameStats();
//
//                // Parse home team
//                if (playerStatsJson.has("homeTeam")) {
//                    Team homeTeam = parseTeamGameData(playerStatsJson.getJSONObject("homeTeam"));
//                    playerStats.setHomeTeam(homeTeam);
//                }
//
//                // Parse away team
//                if (playerStatsJson.has("awayTeam")) {
//                    Team awayTeam = parseTeamGameData(playerStatsJson.getJSONObject("awayTeam"));
//                    playerStats.setAwayTeam(awayTeam);
//                }
//
//                boxscore.setPlayerByGameStats(playerStats);
//            }
//
//            return boxscore;
//
//        } catch (org.json.JSONException e) {
//            throw new Exception("Failed to parse boxscore JSON: " + e.getMessage(), e);
//        }
//    }
        private Game parseCurrentGame(String response) throws Exception {
            try {
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                Game game = new Game();

                // Parse actual fields that exist in NHL API response
                if (jsonResponse.has("id")) {
                    game.setGameId(jsonResponse.getInt("id"));
                }

                // Parse home team data
                if (jsonResponse.has("homeTeam")) {
                    org.json.JSONObject homeTeamJson = jsonResponse.getJSONObject("homeTeam");
                    Team homeTeam = parseTeamGameData(homeTeamJson);
                    game.setHomeTeam(homeTeam);

                    if (homeTeamJson.has("score")) {
                        game.setHomeScore(homeTeamJson.getInt("score"));
                    }
                }

                // Parse away team data
                if (jsonResponse.has("awayTeam")) {
                    org.json.JSONObject awayTeamJson = jsonResponse.getJSONObject("awayTeam");
                    Team awayTeam = parseTeamGameData(awayTeamJson);
                    game.setAwayTeam(awayTeam);

                    if (awayTeamJson.has("score")) {
                        game.setAwayScore(awayTeamJson.getInt("score"));
                    }
                }

                return game;

            } catch (org.json.JSONException e) {
                throw new Exception("Failed to parse boxscore JSON: " + e.getMessage(), e);
            }
        }

    private Team parseTeamGameData(org.json.JSONObject teamJson) throws org.json.JSONException {
        Team teamData = new Team();

        // Parse forwards
        if (teamJson.has("forwards")) {
            org.json.JSONArray forwardsArray = teamJson.getJSONArray("forwards");
            List<NHLPlayer> forwards = new ArrayList<>();
            for (int i = 0; i < forwardsArray.length(); i++) {
                forwards.add(parseNHLPlayer(forwardsArray.getJSONObject(i), false));
            }
            teamData.addPlayers(forwards);
        }

        // Parse defense
        if (teamJson.has("defense")) {
            org.json.JSONArray defenseArray = teamJson.getJSONArray("defense");
            List<NHLPlayer> defense = new ArrayList<>();
            for (int i = 0; i < defenseArray.length(); i++) {
                defense.add(parseNHLPlayer(defenseArray.getJSONObject(i), false));
            }
            teamData.addPlayers(defense);
        }

        // Parse goalies
        if (teamJson.has("goalies")) {
            org.json.JSONArray goaliesArray = teamJson.getJSONArray("goalies");
            List<NHLPlayer> goalies = new ArrayList<>();
            for (int i = 0; i < goaliesArray.length(); i++) {
                goalies.add(parseNHLPlayer(goaliesArray.getJSONObject(i), true));
            }
            teamData.addPlayers(goalies);
        }

        return teamData;
    }

    private NHLPlayer parseNHLPlayer(org.json.JSONObject playerJson, boolean isGoalie) throws org.json.JSONException {
        NHLPlayer player = new NHLPlayer();

        player.setPlayerId(playerJson.optInt("playerId", 0));
        player.setJerseyNumber(playerJson.optInt("sweaterNumber", 0));
        player.setPosition(playerJson.optString("position", ""));
        player.setTimeOnIce((float) playerJson.optDouble("toi", 0.0));

        // Parse name
        if (playerJson.has("firstName") && playerJson.has("lastName")) {
            org.json.JSONObject firstName = playerJson.getJSONObject("firstName");
            org.json.JSONObject lastName = playerJson.getJSONObject("lastName");
            String fullName = firstName.optString("default", "") + " " + lastName.optString("default", "");
            player.setName(fullName.trim());
        } else if (playerJson.has("name")) {
            org.json.JSONObject nameObj = playerJson.getJSONObject("name");
            player.setName(nameObj.optString("default", ""));
        }

        if (isGoalie) {
            // Goalie-specific stats
            player.setShotsAgainst(playerJson.optInt("shotsAgainst", 0));
            player.setSavePercentage(playerJson.optDouble("savePctg", 0.0));
            player.setGoalsAgainst(playerJson.optInt("goalsAgainst", 0));
//            player.setDecision(playerJson.optString("decision", ""));
            // Calculate saves
            player.setSaves((int) Math.round(player.getShotsAgainst() * player.getSavePercentage()));
        } else {
            // Skater stats
            player.setGoals(playerJson.optInt("goals", 0));
            player.setAssists(playerJson.optInt("assists", 0));
            player.setPoints(playerJson.optInt("points", 0));
            player.setShotsOnGoal(playerJson.optInt("shots", 0));
            player.setHits(playerJson.optInt("hits", 0));
            player.setBlocks(playerJson.optInt("blockedShots", 0));
        }

        player.setPenaltyMinutes(playerJson.optInt("pim", 0));

        return player;
    }

    /**
     * Cancel all active requests
     */
    public void cancelAllRequests() {
        synchronized (activeTasks) {
            for (Future<?> task : activeTasks) {
                if (!task.isDone()) {
                    task.cancel(true);
                }
            }
            activeTasks.clear();
        }
        Log.d(TAG, "Cancelled all active API requests");
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        cancelAllRequests();
        executorService.shutdown();
    }
}
