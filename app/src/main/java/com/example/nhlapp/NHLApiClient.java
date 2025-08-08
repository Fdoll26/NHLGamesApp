package com.example.nhlapp;

import android.util.Log;

import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Player;
import com.example.nhlapp.Objects.Team;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/// TODO: Optimize this code so I can execute multiple API calls at the same time and handle multiple requests
/// I am not entirely sure how this will be done because of threads and the phone archetecture are not best friends
/// So we have 8 cores but at different cycle speeds, I don't know how to do parallelism for that lol
/// It would be cool to see it in action tho
///
/// TODO: Set up a queue system with call backs being the place in the queue that the item is in
/// Make it so that the function will get the queue item and once it does it removes it
/// This also gives way to automatic garbage collection for queue results that we are still waiting on but decieded to cancel
public class NHLApiClient {
    private static final String BASE_URL = "https://api-web.nhle.com/v1";
    private static final String STATS_BASE_URL = "https://api.nhle.com/stats/rest/en";

    public static List<Team> getTeams() {
        List<Team> teams = new ArrayList<>();
        try {
            // Use the stats API for team information
            String response = makeApiCall(STATS_BASE_URL + "/team");
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray teamsArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < teamsArray.length(); i++) {
                JSONObject teamJson = teamsArray.getJSONObject(i);
                Team team = new Team();
                team.setTeamID(teamJson.getInt("id"));
                team.setName(teamJson.getString("fullName"));
                // Also store the team abbreviation if needed
                if (teamJson.has("triCode")) {
                    team.setAbreviatedName(teamJson.getString("triCode"));
                }
                teams.add(team);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teams;
    }

    public static List<NHLPlayer> getPlayers() {
        List<NHLPlayer> players = new ArrayList<>();
        try {
            // Get all teams first
            List<Team> teams = getTeams();

            for (Team team : teams) {
                try {
                    // Use the current roster endpoint with team abbreviation
                    String teamCode = team.getAbreviatedName();
                    if (teamCode != null) {
                        String response = makeApiCall(BASE_URL + "/roster/" + teamCode + "/current");
                        JSONObject jsonResponse = new JSONObject(response);

                        // Check different possible array names in response
                        JSONArray rosterArray = null;
                        if (jsonResponse.has("forwards")) {
                            addPlayersFromArray(jsonResponse.getJSONArray("forwards"), players, team.getTeamID(), "forwards");
                        }
                        if (jsonResponse.has("defensemen")) {
                            addPlayersFromArray(jsonResponse.getJSONArray("defensemen"), players, team.getTeamID(), "defensemen");
                        }
                        if (jsonResponse.has("goalies")) {
                            addPlayersFromArray(jsonResponse.getJSONArray("goalies"), players, team.getTeamID(), "goalies");
                        }
                    }
                } catch (Exception teamException) {
                    Log.w("NHLAPI", "Failed to get roster for team: " + team.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return players;
    }

    private static void addPlayersFromArray(JSONArray playerArray, List<NHLPlayer> players, int teamId, String positionType) {
        try {
            for (int i = 0; i < playerArray.length(); i++) {
                JSONObject playerJson = playerArray.getJSONObject(i);
                NHLPlayer player = new NHLPlayer();
                player.setPlayerId(playerJson.getInt("id"));
                if(Objects.equals(positionType, "goalies"))
                    player.setPosition("G");
                if(Objects.equals(positionType, "forwards"))
                    player.setPosition("C");
                if(Objects.equals(positionType, "defensemen"))
                    player.setPosition("D");

                // Handle different name formats
                if (playerJson.has("firstName") && playerJson.has("lastName")) {
                    String fullName = playerJson.getString("firstName") + " " + playerJson.getString("lastName");
                    player.setName(fullName);
                } else if (playerJson.has("fullName")) {
                    player.setName(playerJson.getString("fullName"));
                }

                player.setTeamId(teamId);
                players.add(player);
            }
        } catch (Exception e) {
            Log.w("NHLAPI", "Error parsing player array: " + e.getMessage());
        }
    }

    public static List<String> getSeasons() {
        List<String> seasons = new ArrayList<>();
        try {
            String response = makeApiCall(BASE_URL + "/season");
            JSONArray seasonsArray = new JSONArray(response);

            for (int i = 0; i < seasonsArray.length(); i++) {
                String seasonId = seasonsArray.getString(i);
                seasons.add(seasonId);
            }
            Log.d("NHLAPI", "Got seasons with the addition of " + seasons.size());
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("NHLAPI", "Failed to get seasons from API, using fallback");
            // Fallback to hardcoded recent seasons
            seasons.addAll(Arrays.asList("20242025", "20232024", "20222023", "20212022", "20202021"));
        }
        return seasons;
    }

    // NEW: Overloaded method that accepts a cancellation flag
    public static List<Game> getGamesForSeason(String season, AtomicBoolean isCancelled) {
        List<Game> games = new ArrayList<>();

        // Since there's no single endpoint for all games in a season,
        // we'll need to get games for each team
        try {
            List<Team> teams = getTeams();

            for (Team team : teams) {
                // Check for cancellation before each team request
                if (isCancelled != null && isCancelled.get()) {
                    Log.d("NHLAPI", "Games fetch cancelled for season: " + season);
                    return new ArrayList<>(); // Return empty list if cancelled
                }

                try {
                    String teamCode = team.getAbreviatedName();
                    if (teamCode != null) {
                        String response = makeApiCall(BASE_URL + "/club-schedule-season/" + teamCode + "/" + season);
                        JSONObject jsonResponse = new JSONObject(response);

                        if (jsonResponse.has("games")) {
                            JSONArray gamesArray = jsonResponse.getJSONArray("games");

                            for (int i = 0; i < gamesArray.length(); i++) {
                                // Check for cancellation during game processing
                                if (isCancelled != null && isCancelled.get()) {
                                    Log.d("NHLAPI", "Games fetch cancelled during processing for season: " + season);
                                    return new ArrayList<>();
                                }

                                JSONObject gameJson = gamesArray.getJSONObject(i);

                                // Check if we already have this game (avoid duplicates)
                                int gameId = gameJson.getInt("id");
                                boolean gameExists = false;
                                for (Game existingGame : games) {
                                    if (existingGame.getGameId() == gameId) {
                                        gameExists = true;
                                        break;
                                    }
                                }

                                if (!gameExists) {
                                    Game game = new Game();
                                    game.setGameId(gameId);
                                    game.setGameDate(gameJson.getString("gameDate"));

                                    // Set team IDs
                                    if (gameJson.has("homeTeam")) {
                                        game.setHomeTeamId(gameJson.getJSONObject("homeTeam").getInt("id"));
                                        game.setHomeTeamName(gameJson.getJSONObject("homeTeam").getString("abbrev"));
                                    }
                                    if (gameJson.has("awayTeam")) {
                                        game.setAwayTeamId(gameJson.getJSONObject("awayTeam").getInt("id"));
                                        game.setAwayTeamName(gameJson.getJSONObject("awayTeam").getString("abbrev"));
                                    }

                                    // Set scores if available
                                    if (gameJson.has("homeTeam") && gameJson.getJSONObject("homeTeam").has("score")) {
                                        game.setHomeScore(gameJson.getJSONObject("homeTeam").getInt("score"));
                                    }
                                    if (gameJson.has("awayTeam") && gameJson.getJSONObject("awayTeam").has("score")) {
                                        game.setAwayScore(gameJson.getJSONObject("awayTeam").getInt("score"));
                                    }

                                    games.add(game);
                                }
                            }
                        }
                    }
                } catch (Exception teamException) {
                    // Check for cancellation even during exceptions
                    if (isCancelled != null && isCancelled.get()) {
                        Log.d("NHLAPI", "Games fetch cancelled during exception handling for season: " + season);
                        return new ArrayList<>();
                    }

                    // Check if the exception is due to cancellation
                    if (teamException.getMessage() != null && teamException.getMessage().contains("Request cancelled")) {
                        Log.d("NHLAPI", "Games fetch cancelled via interrupted connection for season: " + season);
                        return new ArrayList<>();
                    }

                    Log.w("NHLAPI", "Failed to get games for team: " + team.getName() + " - " + teamException.getMessage());
                }
            }

            // Final cancellation check before sorting
            if (isCancelled != null && isCancelled.get()) {
                Log.d("NHLAPI", "Games fetch cancelled before sorting for season: " + season);
                return new ArrayList<>();
            }

            // Sort games by date
            Collections.sort(games, new Comparator<Game>() {
                @Override
                public int compare(Game g1, Game g2) {
                    return g1.getGameDate().compareTo(g2.getGameDate());
                }
            });

            Log.d("NHLAPI", "Retrieved " + games.size() + " games for season " + season);

        } catch (Exception e) {
            // Check if the main exception is due to cancellation
            if (e.getMessage() != null && e.getMessage().contains("Request cancelled")) {
                Log.d("NHLAPI", "Games fetch cancelled via interrupted connection for season: " + season);
                return new ArrayList<>();
            }

            e.printStackTrace();
            Log.e("NHLAPI", "Error getting games for season: " + e.getMessage());
        }

        return games;
    }

    // Keep the original method for backward compatibility
    public static List<Game> getGamesForSeason(String season) {
        return getGamesForSeason(season, null);
    }

    private static String makeApiCall(String urlString) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        // Should do a check here to make sure there is some form of a connection to the database

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // Increased timeout
            connection.setReadTimeout(10000);    // Increased timeout

            // Add user agent header
            connection.setRequestProperty("User-Agent", "NHL-App/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("API call failed with response code: " + responseCode);
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();

        } catch (java.io.InterruptedIOException e) {
            // Task was cancelled - this is expected behavior
            Log.d("NHLAPI", "API call interrupted (task cancelled): " + urlString);
            throw new Exception("Request cancelled", e);
        } catch (Exception e) {
            Log.e("NHLAPI", "API call failed for: " + urlString + " - " + e.getMessage());
            throw e;
        } finally {
            // Clean up resources
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}