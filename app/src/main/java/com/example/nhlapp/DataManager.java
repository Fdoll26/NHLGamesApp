package com.example.nhlapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.nhlapp.AppSettings;
import com.example.nhlapp.JsonHelper;
import com.example.nhlapp.NHLApiClient;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.ImageDownloader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataManager {
    private static DataManager instance;
    private List<String> seasons = new ArrayList<>();
    private Context context;
    public HashMap<String, List<Integer>> gamesBySeason = new HashMap<>();
    private HashMap<String, ArrayList<Integer>> gamesByDate = new HashMap<String, ArrayList<Integer>>();
    private boolean teamsLoadedFromJson = false;
    private boolean playersLoadedFromJson = false;
    private boolean seasonsLoadedFromJson = false;
    private boolean gamesLoadedFromJson = false;
    private boolean gamesBySeasonLoadedFromJson = false;
    private boolean isInitialLoadComplete = false;
    private HashMap<Integer, Team> teamsById = new HashMap<>();
    private HashMap<Integer, NHLPlayer> playersById = new HashMap<>();
    private HashMap<Integer, Game> gamesById = new HashMap<>();
    private HashMap<Integer, Integer> playerIdTeamId = new HashMap<>();
    private HashMap<Integer, Set<Integer>> teamIdPlayerId = new HashMap<>();
    private HashMap<Integer, String> teamIdToLogoFile = new HashMap<>();
    private HashMap<Integer, String> playerIdToLogoFile = new HashMap<>();

    private DataManager() {}

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public void calculateTeamStats(String season) {
        if (!gamesBySeason.containsKey(season)) {
            return;
        }

        // Reset stats for all teams
        for (Team team : teamsById.values()) {
            team.setGamesPlayed(0);
            team.setGamesWon(0);
            team.setGamesLost(0);
            team.setGamesLostOT(0);
            team.setPoints(0);
            team.setGoalsFor(0);
            team.setGoalsAgainst(0);
//            team.setGoalDifferential(0);
            team.setGoalsAgainst(0);
            team.setGoalsFor(0);
        }


        for (int gameId : gamesBySeason.get(season)) {
            if (gamesById.get(gameId).getHomeScore() > -1 || gamesById.get(gameId).getAwayScore() > -1) {
                continue;
            }

            Team homeTeam = teamsById.get(gamesById.get(gameId).getHomeTeamId());
            Team awayTeam = teamsById.get(gamesById.get(gameId).getAwayTeamId());

            if (homeTeam == null || awayTeam == null) {
                continue;
            }

            // Update games played
            homeTeam.setGamesPlayed(homeTeam.getGamesPlayed() + 1);
            awayTeam.setGamesPlayed(awayTeam.getGamesPlayed() + 1);

            // Update goals
            homeTeam.setGoalsFor(homeTeam.getGoalsFor() + gamesById.get(gameId).getHomeScore());
            homeTeam.setGoalsAgainst(homeTeam.getGoalsAgainst() + gamesById.get(gameId).getAwayScore());
            awayTeam.setGoalsFor(awayTeam.getGoalsFor() + gamesById.get(gameId).getAwayScore());
            awayTeam.setGoalsAgainst(awayTeam.getGoalsAgainst() + gamesById.get(gameId).getHomeScore());

            // Determine winner and update wins/losses/points
            if (gamesById.get(gameId).getHomeScore() > gamesById.get(gameId).getAwayScore()) {
                homeTeam.setGamesWon(homeTeam.getGamesWon() + 1);
                homeTeam.setPoints(homeTeam.getPoints() + 2);
                awayTeam.setGamesLost(awayTeam.getGamesLost() + 1);
            } else if (gamesById.get(gameId).getAwayScore() > gamesById.get(gameId).getHomeScore()) {
                awayTeam.setGamesWon(awayTeam.getGamesWon() + 1);
                awayTeam.setPoints(awayTeam.getPoints() + 2);
                homeTeam.setGamesLost(homeTeam.getGamesLost() + 1);
            }
        }

        // Calculate goal differential
        for (Team team : teamsById.values()) {
//            team.setGoalDifferential(team.getGoalsFor() - team.getGoalsAgainst());
            teamsById.put(team.getTeamID(), team);
        }
    }

    public boolean hasSeasonData(String season) {
        return gamesBySeason.containsKey(season) &&
                !Objects.requireNonNull(gamesBySeason.get(season)).isEmpty() &&
                !teamsById.isEmpty();
    }

    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        if (settings.isJsonSavingEnabled()) {
            loadDataFromJson();
            isInitialLoadComplete = true;
            Log.d("DataManager", "Initial data load complete from JSON files");
        }
    }

    public void insertGame(Game game){
        if (gamesById.containsKey(game.getId()))
            gamesById.replace(game.getId(), game);
        else
            gamesById.put(game.getId(), game);
    }

    public void insertGamesByDate(String date, ArrayList<Integer> gameIds){
        for(int gameId: gameIds){
            if(gamesById.containsKey(gameId)){
                if(gamesByDate.containsKey(date)){
                    if(!Objects.requireNonNull(gamesByDate.get(date)).contains(gameId))
                        Objects.requireNonNull(gamesByDate.get(date)).add(gameId);
                }
            }
        }
    }

    public int getTeamSize(){
        return teamsById.size();
    }

    public void addTeam(Team team) {
        if (teamsById.containsKey(team.getTeamID()))
            teamsById.replace(team.getTeamID(), team);
        else
            teamsById.put(team.getTeamID(), team);
    }

    public void addTeams(ArrayList<Team> teams) {
        for (Team team: teams){
            if (teamsById.containsKey(team.getTeamID()))
                teamsById.replace(team.getTeamID(), team);
            else
                teamsById.put(team.getTeamID(), team);
        }
//        if (!teamsById.isEmpty()) {
//            callback.onSuccess(new ArrayList<>(teamsById.values()));
//        } else {
//            new FetchTeamsTask(callback).execute();
//        }
    }

    public ArrayList<Team> getTeams() {
        if (!teamsById.isEmpty()) {
//            callback.onSuccess(new ArrayList<>(teamsById.values()));
            return (ArrayList<Team>) teamsById.values();
        } else {
            return new ArrayList<Team>();
//            new FetchTeamsTask(callback).execute();
        }
    }

    public void getPlayers(DataCallback<List<NHLPlayer>> callback) {
        if (!playersById.isEmpty()) {
            callback.onSuccess(new ArrayList<>(playersById.values()));
        } else {
            new FetchPlayersTask(callback).execute();
        }
    }

    public void getSeasons(DataCallback<List<String>> callback) {
        if (!seasons.isEmpty()) {
            callback.onSuccess(new ArrayList<>(seasons));
        } else {
            new FetchSeasonsTask(callback).execute();
        }
    }

//    public void addSeasonGames(String season, List<Game> recievedGames){
//
//        gamesByDate.clear();
//        ArrayList<Integer> gotGameIds = new ArrayList<>();
//        for(Game game: recievedGames){
//            gotGameIds.add(game.getGameId());
//            if(gamesById.containsKey(game.getGameId()))
//                gamesById.replace(game.getGameId(), game);
//            else
//                gamesById.put(game.getGameId(), game);
//            if(gamesByDate.containsKey(game.getGameDate())){
//                Objects.requireNonNull(gamesByDate.get(game.getGameDate())).add(game.getGameId());
//            }
//            else{
//                gamesByDate.put(game.getGameDate(), new ArrayList<>(game.getGameId()));
//            }
//        }
    ////        games = recievedGames;
//        if(gamesBySeason.containsKey(season)){
//            gamesBySeason.replace(season, gotGameIds);
//        } else{
//            gamesBySeason.put(season, gotGameIds);
//        }
//    }

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

    public void addPlayer(NHLPlayer recievedPlayer){
        if(playersById.containsKey(recievedPlayer.getPlayerId())){
            // Realistically this is where the combine code comes in handy, really just a way to update whats new
            // Good way to combine different data avenues into one location (overlay lol)
            playersById.replace(recievedPlayer.getPlayerId(), recievedPlayer);
        }else{
            playersById.put(recievedPlayer.getPlayerId(), recievedPlayer);
        }
    }

    public int getPlayerSize(){
        return playersById.size();
    }

//    public void getGamesForSeason(String season, DataCallback<List<Game>> callback) {
//        new FetchGamesTask(callback).execute(season);
//    }

    public Game getGameById(int gameId){
        if(gamesById.containsKey(gameId))
            return gamesById.get(gameId);
        return null;
    }

//    public ArrayList<String> getAllDates(){
//        return (ArrayList<String>) gamesByDate.keySet();
//    }

    public void addGamesForDate(String date, ArrayList<Game> games){
        if(!gamesByDate.containsKey(date))
            gamesByDate.put(date, new ArrayList<Integer>());
        for(Game game: games){
            if(gamesById.containsKey(game.getId()))
                gamesById.replace(game.getId(), game);
            else
                gamesById.put(game.getId(), game);
            if(!gamesByDate.get(date).contains(game.getId()))
                gamesByDate.get(date).add(game.getId());
        }

    }

//    public void addGame(Game game){
//        if(!gamesByDate.containsKey(game.getGameDate()))
//            gamesByDate.put(game.getGameDate(), new ArrayList<Integer>());
//        if(!gamesByDate.get(game.getGameDate()).contains(game.getGameId()))
//            gamesByDate.get(game.getGameDate()).add(game.getGameId());
//        if(gamesById.containsKey(game.getGameId()))
//            gamesById.replace(game.getGameId(), game);
//        else
//            gamesById.put(game.getGameId(), game);
//    }

    public void saveAllDataToJson(Context context) {
        new SaveDataTask(context).execute();
    }

    public void saveSpecificDataToJson(Context context, String dataToSave) {
        new SaveSpecificDataTask(context, dataToSave).execute();
    }

    public void setTeamLogoFile(int teamId, String file_location){
        if(teamsById.containsKey(teamId)){
            teamsById.get(teamId).setLogoUrl(file_location);
        }
    }

    private void loadDataFromJson() {
        // Load teams from JSON
        teamsLoadedFromJson = loadTeamsFromJson();
        // Load players from JSON
        playersLoadedFromJson = loadPlayersFromJson();
        // Load seasons from JSON (if available)
        seasonsLoadedFromJson = loadSeasonsFromJson();
        // Load games from JSON (if available)
        gamesLoadedFromJson = loadGamesFromJson();
        // Load games by season from JSON
        gamesBySeasonLoadedFromJson = loadGamesBySeasonFromJson();

        Log.d("DataManager", String.format("Data loaded from JSON - Teams: %b, Players: %b, Seasons: %b, Games: %b, GamesBySeason: %b", teamsLoadedFromJson, playersLoadedFromJson, seasonsLoadedFromJson, gamesLoadedFromJson, gamesBySeasonLoadedFromJson));
    }

    private boolean loadTeamsFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "teams.json");
        if (jsonData != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                List<Team> loadedTeams = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject teamJson = jsonArray.getJSONObject(i);
                    Team team = new Team();
                    team.setTeamID(teamJson.getInt("id"));
                    team.setName(teamJson.getString("name"));
                    team.setLogoUrl(teamJson.optString("logoPath", ""));

                    // Load roster player IDs if available
                    if (teamJson.has("rosterPlayerIds")) {
                        JSONArray rosterArray = teamJson.getJSONArray("rosterPlayerIds");
                        List<Integer> rosterIds = new ArrayList<>();
                        for (int j = 0; j < rosterArray.length(); j++) {
                            rosterIds.add(rosterArray.getInt(j));
                        }
                        team.setRosterPlayerIds(rosterIds);
                    }

                    loadedTeams.add(team);
                }

                teamsById.clear();
                for(Team team: loadedTeams){
                    teamsById.put(team.getTeamID(), team);
                }
//                teamsById.addAll(loadedTeams);
                Log.d("DataManager", "Loaded " + teamsById.size() + " teams from JSON");
                return true;
            } catch (Exception e) {
                Log.e("DataManager", "Error loading teams from JSON", e);
            }
        }
        return false;
    }

    private boolean loadPlayersFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "players.json");
        if (jsonData != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                List<NHLPlayer> loadedPlayers = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject playerJson = jsonArray.getJSONObject(i);
                    NHLPlayer player = new NHLPlayer();
                    player.setPlayerId(playerJson.getInt("id"));
                    player.setName(playerJson.getString("name"));
                    player.setTeamId(playerJson.getInt("teamId"));
                    player.setHeadshotPath(playerJson.optString("headshotPath", ""));
                    loadedPlayers.add(player);
                }

                playersById.clear();
                for (NHLPlayer player: loadedPlayers){
                    playersById.put(player.getPlayerId(), player);
                }
//                players.addAll(loadedPlayers);
                Log.d("DataManager", "Loaded " + playersById.size() + " players from JSON");
                return true;
            } catch (Exception e) {
                Log.e("DataManager", "Error loading players from JSON", e);
            }
        }
        return false;
    }

    private boolean loadSeasonsFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "seasons.json");
        if (jsonData != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                List<String> loadedSeasons = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    loadedSeasons.add(jsonArray.getString(i));
                }

                seasons.clear();
                seasons.addAll(loadedSeasons);
                Log.d("DataManager", "Loaded " + seasons.size() + " seasons from JSON");
                return true;
            } catch (Exception e) {
                Log.e("DataManager", "Error loading seasons from JSON", e);
            }
        }
        return false;
    }

    // Be aware that there is a seasons JSON that we can load in as well that might superseed this.
    // This might turn into that and the games json just selects a season to load in at the very end
    // And then if the seasons json exists but the games one doesn't then load in the most recent season
    private boolean loadGamesFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "games.json");
        if (jsonData != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                List<Game> loadedGames = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject gameJson = jsonArray.getJSONObject(i);
                    Game game = new Game();
                    game.setId(gameJson.getInt("id"));
                    game.setDate(gameJson.getString("date"));
                    game.setHomeTeamId(gameJson.getInt("homeTeamId"));
                    game.setAwayTeamId(gameJson.getInt("awayTeamId"));
                    game.setHomeScore(gameJson.optInt("homeScore", 0));
                    game.setAwayScore(gameJson.optInt("awayScore", 0));
                    loadedGames.add(game);
                }

//                gamesByDate.clear();
//                gamesByDate.clear();
//                for(Game game: loadedGames){
//                    if(gamesByDate.containsKey(game.getGameDate())){
//                        Objects.requireNonNull(gamesByDate.get(game.getGameDate())).add(game);
//                    }
//                }
                gamesByDate.clear();
                ArrayList<Integer> gotGameIds = new ArrayList<>();
                for(Game game: loadedGames){
                    gotGameIds.add(game.getId());
                    if(gamesById.containsKey(game.getId()))
                        gamesById.replace(game.getId(), game);
                    else
                        gamesById.put(game.getId(), game);
                    if(gamesByDate.containsKey(game.getDate())){
                        Objects.requireNonNull(gamesByDate.get(game.getDate())).add(game.getId());
                    }
                    else{
                        gamesByDate.put(game.getDate(), new ArrayList<>(game.getId()));
                    }
                }
//                games.addAll(loadedGames);
                Log.d("DataManager", "Loaded " + gamesByDate.size() + " games from JSON");
                return true;
            } catch (Exception e) {
                Log.e("DataManager", "Error loading games from JSON", e);
            }
        }
        return false;
    }

    private boolean loadGamesBySeasonFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(context, "gamesBySeason.json");
        if (jsonData != null) {
            try {
                JSONObject seasonObject = new JSONObject(jsonData);
                HashMap<String, List<Game>> seasonGame = new HashMap<>();

                for (Iterator<String> it = seasonObject.keys(); it.hasNext(); ) {
                    String season = it.next();
                    JSONArray gamesArray = seasonObject.getJSONArray(season);
                    List<Game> loadedGames = new ArrayList<>();

                    for (int i = 0; i < gamesArray.length(); i++) {
                        JSONObject gameJson = gamesArray.getJSONObject(i);
//                        Log.d("DataManager", "gameJson: " + gameJson);
                        Game game = new Game();
                        game.setId(gameJson.getInt("id"));
                        game.setDate(gameJson.getString("date"));
                        game.setHomeTeamId(gameJson.getInt("homeTeamId"));
                        game.setAwayTeamId(gameJson.getInt("awayTeamId"));
                        game.setHomeScore(gameJson.optInt("homeScore", -1));
                        game.setAwayScore(gameJson.optInt("awayScore", -1));
                        game.setHomeTeamName(gameJson.optString("homeName", ""));
                        game.setAwayTeamName(gameJson.optString("awayName", ""));
                        loadedGames.add(game);
                    }
                    seasonGame.put(season, loadedGames);
                }
                for(String season: seasonGame.keySet())
                    addSeasonGames(season, Objects.requireNonNull(seasonGame.get(season)));
//                gamesBySeason.clear();
//                gamesBySeason = new HashMap<>(seasonGame);
                Log.d("DataManager", "Loaded games for " + gamesBySeason.size() + " seasons from JSON");
                return true;
            } catch (Exception e) {
                Log.e("DataManager", "Error loading gamesBySeason from JSON", e);
            }
        }
        return false;
    }

    public Team getTeamByAbbreviation(String abreviatedName) {
        for(Team team: teamsById.values()){
            if(Objects.equals(team.getAbreviatedName(), abreviatedName))
                return team;
        }
        return null;
    }

//    public ArrayList<Game> getGamesForDate(String date) {
//        if(!gamesByDate.containsKey(date))
//            return null;
//        ArrayList<Game> games = new ArrayList<>();
//        for(int gameId: Objects.requireNonNull(gamesByDate.get(date))){
//            games.add(gamesById.get(gameId));
//        }
//        return games;
//    }

    public int getGamesSize() {
        return gamesById.size();
    }
    public int getDatesSize() {
        return gamesByDate.size();
    }

    public ArrayList<String> getDates(){
        return (ArrayList<String>) gamesByDate.values().iterator();
    }

    public void setPlayerPhotoFile(int playerId, String relativePath) {
        if(playersById.containsKey(playerId))
            Objects.requireNonNull(playersById.get(playerId)).setHeadshotPath(relativePath);
    }

    public ArrayList<Game> getGamesForDate(String date) {
        if (!gamesByDate.containsKey(date)) {
            Log.d("DataManager", "No games found for date: " + date);
            return new ArrayList<>(); // Return empty list instead of null for safety
        }

        ArrayList<Game> games = new ArrayList<>();
        List<Integer> gameIds = gamesByDate.get(date);

        if (gameIds != null) {
            for (int gameId : gameIds) {
                Game game = gamesById.get(gameId);
                if (game != null) {
                    games.add(game);
                } else {
                    Log.w("DataManager", "Game with ID " + gameId + " not found in gamesById map");
                }
            }
        }

        Log.d("DataManager", "Retrieved " + games.size() + " games for date: " + date);
        return games;
    }

    /**
     * Add or update a single game and ensure it's properly indexed by date
     */
    public void addGame(Game game) {
        if (game == null || game.getDate() == null) {
            Log.w("DataManager", "Cannot add null game or game with null date");
            return;
        }

        // Add to games by ID
        if (gamesById.containsKey(game.getId())) {
            gamesById.replace(game.getId(), game);
            Log.d("DataManager", "Updated existing game: " + game.getId());
        } else {
            gamesById.put(game.getId(), game);
            Log.d("DataManager", "Added new game: " + game.getId());
        }

        // Add to games by date
        String gameDate = game.getDate();
        if (!gamesByDate.containsKey(gameDate)) {
            gamesByDate.put(gameDate, new ArrayList<>());
            Log.d("DataManager", "Created new date entry: " + gameDate);
        }

        List<Integer> gamesForDate = gamesByDate.get(gameDate);
        if (gamesForDate != null && !gamesForDate.contains(game.getId())) {
            gamesForDate.add(game.getId());
            Log.d("DataManager", "Added game " + game.getId() + " to date " + gameDate);
        }
    }

    /**
     * Get all available dates that have games, sorted in descending order (most recent first)
     */
    public ArrayList<String> getAllDates() {
        ArrayList<String> dates = new ArrayList<>(gamesByDate.keySet());
        Collections.sort(dates, Collections.reverseOrder());
        Log.d("DataManager", "Retrieved " + dates.size() + " dates with games");
        return dates;
    }

    /**
     * Check if we have any games for the specified season
     */
    public boolean hasGamesForSeason(String season) {
        boolean hasGames = gamesBySeason.containsKey(season) &&
                gamesBySeason.get(season) != null &&
                !gamesBySeason.get(season).isEmpty();
        Log.d("DataManager", "Season " + season + " has games: " + hasGames);
        return hasGames;
    }

    /**
     * Get the count of games for a specific season
     */
    public int getGamesCountForSeason(String season) {
        if (!gamesBySeason.containsKey(season)) {
            return 0;
        }
        List<Integer> gameIds = gamesBySeason.get(season);
        return gameIds != null ? gameIds.size() : 0;
    }

    /**
     * Add season games and properly index them by date
     */
    public void addSeasonGames(String season, List<Game> receivedGames) {
        if (receivedGames == null || receivedGames.isEmpty()) {
            Log.w("DataManager", "No games to add for season: " + season);
            return;
        }

        Log.d("DataManager", "Adding " + receivedGames.size() + " games for season: " + season);

        ArrayList<Integer> gameIds = new ArrayList<>();

        for (Game game : receivedGames) {
            if (game == null) continue;

            gameIds.add(game.getId());

            // Add to main games collection
            if (gamesById.containsKey(game.getId())) {
                gamesById.replace(game.getId(), game);
            } else {
                gamesById.put(game.getId(), game);
            }

            // Add to games by date
            String gameDate = game.getDate();
            if (gameDate != null) {
                if (!gamesByDate.containsKey(gameDate)) {
                    gamesByDate.put(gameDate, new ArrayList<>());
                }
                List<Integer> gamesForDate = gamesByDate.get(gameDate);
                if (gamesForDate != null && !gamesForDate.contains(game.getId())) {
                    gamesForDate.add(game.getId());
                }
            }
        }

        // Update season games mapping
        if (gamesBySeason.containsKey(season)) {
            gamesBySeason.replace(season, gameIds);
        } else {
            gamesBySeason.put(season, gameIds);
        }

        Log.d("DataManager", "Successfully added season games. Total games by date: " + gamesByDate.size());
    }

    /**
     * Get debug info about current data state
     */
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

    /**
     * Clear all cached game data (useful for debugging)
     */
    public void clearGameCache() {
        Log.d("DataManager", "Clearing game cache");
        gamesById.clear();
        gamesByDate.clear();
        gamesBySeason.clear();
    }

    /**
     * Force refresh data for a specific season
     */
    public void refreshSeasonData(String season, DataCallback<List<Game>> callback) {
        // Clear existing data for this season
        if (gamesBySeason.containsKey(season)) {
            List<Integer> oldGameIds = gamesBySeason.get(season);
            if (oldGameIds != null) {
                for (Integer gameId : oldGameIds) {
                    Game game = gamesById.get(gameId);
                    if (game != null) {
                        // Remove from gamesByDate
                        String gameDate = game.getDate();
                        if (gamesByDate.containsKey(gameDate)) {
                            List<Integer> dateGames = gamesByDate.get(gameDate);
                            if (dateGames != null) {
                                dateGames.remove(gameId);
                                if (dateGames.isEmpty()) {
                                    gamesByDate.remove(gameDate);
                                }
                            }
                        }
                    }
                    gamesById.remove(gameId);
                }
            }
            gamesBySeason.remove(season);
        }

        // Request fresh data
        getGamesForSeason(season, true, callback);
    }

    // AsyncTask implementations for singleton data fetching
    private class FetchTeamsTask extends AsyncTask<Void, Void, List<Team>> {
        private DataCallback<List<Team>> callback;

        public FetchTeamsTask(DataCallback<List<Team>> callback) {
            this.callback = callback;
        }

        @Override
        protected List<Team> doInBackground(Void... voids) {
            AppSettings settings = AppSettings.getInstance(context);
            if (settings.isOnlineMode()) {
                return NHLApiClient.getTeams();
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<Team> result) {
            if (result != null && !result.isEmpty()) {
                teamsById.clear();
                for (Team team: result){
                    teamsById.put(team.getTeamID(), team);
                }
//                teams.addAll(result);
                callback.onSuccess(new ArrayList<>(teamsById.values()));

                // Download team logos in background
                for (Team team : teamsById.values()) {
                    new DownloadTeamLogoTask(team).execute();
                }
            } else {
                // Return cached data if available
                if (!teamsById.isEmpty()) {
                    callback.onSuccess(new ArrayList<>(teamsById.values()));
                } else {
                    callback.onError("Failed to fetch teams and no cached data available");
                }
            }
        }
    }

    private class FetchPlayersTask extends AsyncTask<Void, Void, List<NHLPlayer>> {
        private DataCallback<List<NHLPlayer>> callback;

        public FetchPlayersTask(DataCallback<List<NHLPlayer>> callback) {
            this.callback = callback;
        }

        @Override
        protected List<NHLPlayer> doInBackground(Void... voids) {
            AppSettings settings = AppSettings.getInstance(context);
            if (settings.isOnlineMode()) {
                return NHLApiClient.getPlayers();
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<NHLPlayer> result) {
            if (result != null && !result.isEmpty()) {
                playersById.clear();
                for (NHLPlayer player: result){
                    playersById.put(player.getPlayerId(), player);
                }
//                players.addAll(result);
                callback.onSuccess(new ArrayList<>(playersById.values()));

                // Download player headshots in background
                for (int playerId : playersById.keySet()) {
                    new DownloadPlayerHeadshotTask(playersById.get(playerId)).execute();
                }
            } else {
                // Return cached data if available
                if (!playersById.isEmpty()) {
                    callback.onSuccess(new ArrayList<>(playersById.values()));
                } else {
                    callback.onError("Failed to fetch players and no cached data available");
                }
            }
        }
    }

    private class FetchSeasonsTask extends AsyncTask<Void, Void, List<String>> {
        private DataCallback<List<String>> callback;

        public FetchSeasonsTask(DataCallback<List<String>> callback) {
            this.callback = callback;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            AppSettings settings = AppSettings.getInstance(context);
            if (settings.isOnlineMode()) {
                return NHLApiClient.getSeasons();
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<String> result) {
            if (result != null && !result.isEmpty()) {
                seasons.clear();
                seasons.addAll(result);
                callback.onSuccess(new ArrayList<>(seasons));
            } else {
                // Return cached data if available
                if (!seasons.isEmpty()) {
                    callback.onSuccess(new ArrayList<>(seasons));
                } else {
                    callback.onError("Failed to fetch seasons and no cached data available");
                }
            }
        }
    }

    // New method to get games for a season with force refresh option
    public void getGamesForSeason(String season, boolean forceRefresh, DataCallback<List<Game>> callback) {
        // If force refresh is requested or no data exists for this season
        if (forceRefresh || !gamesBySeason.containsKey(season) || Objects.requireNonNull(gamesBySeason.get(season)).isEmpty()) {
            Log.d("DataManager", "Fetching games from API for season: " + season + " (force: " + forceRefresh + ")");
            new FetchGamesTask(callback, season).execute(season);
        } else {
            // Return cached data
            List<Integer> cachedGameIds = gamesBySeason.get(season);
            List<Game> cachedGames = new ArrayList<>();
            assert cachedGameIds != null;
            for(int gameId: cachedGameIds){
                if(gamesById.containsKey(gameId))
                    cachedGames.add(gamesById.get(gameId));
            }

            Log.d("DataManager", "Returning cached games for season: " + season + " (" + cachedGames.size() + " games)");
            callback.onSuccess(new ArrayList<>(cachedGames));
        }
    }

    // Keep the old method for backward compatibility
    public void getGamesForSeason(String season, DataCallback<List<Game>> callback) {
        getGamesForSeason(season, false, callback);
    }

    // Modified FetchGamesTask to store results by season
    private class FetchGamesTask extends AsyncTask<String, Void, List<Game>> {
        private DataCallback<List<Game>> callback;
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);
        private String season;

        public FetchGamesTask(DataCallback<List<Game>> callback, String season) {
            this.callback = callback;
            this.season = season;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            // Signal to API client that this task is cancelled
            isCancelled.set(true);
        }

        @Override
        protected List<Game> doInBackground(String... seasons) {
            AppSettings settings = AppSettings.getInstance(context);
            if (settings.isOnlineMode()) {
                // Pass the cancellation flag to the API client
                return NHLApiClient.getGamesForSeason(seasons[0], isCancelled);
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<Game> result) {
            if (result != null && !result.isEmpty()) {
                // Store in both the main games list and the season-specific map
//                games.clear();
//                games.addAll(result);
//                gamesByDate.clear();
//                for(Game game: result){
//                    if(gamesByDate.containsKey(game.getGameDate())){
//                        Objects.requireNonNull(gamesByDate.get(game.getGameDate())).add(game);
//                    }
//                }
                gamesByDate.clear();
                ArrayList<Integer> gotGameIds = new ArrayList<>();
                for(Game game: result){
                    gotGameIds.add(game.getId());
                    if(gamesById.containsKey(game.getId()))
                        gamesById.replace(game.getId(), game);
                    else
                        gamesById.put(game.getId(), game);
                    if(gamesByDate.containsKey(game.getDate())){
                        Objects.requireNonNull(gamesByDate.get(game.getDate())).add(game.getId());
                    }
                    else{
                        gamesByDate.put(game.getDate(), new ArrayList<>(game.getId()));
                    }
                }
                gamesBySeason.put(season, gotGameIds);

                Log.d("DataManager", "Fetched and cached " + result.size() + " games for season: " + season);
                callback.onSuccess(new ArrayList<>(result));
            } else {
                // Return cached data if available
                if (gamesBySeason.containsKey(season) && !gamesBySeason.get(season).isEmpty()) {
                    ArrayList<Game> games = new ArrayList<>();
                    for(int gameId: gamesBySeason.get(season)){
                        if (gamesById.containsKey(gameId))
                            games.add(gamesById.get(gameId));
                    }
                    callback.onSuccess(games);
                } else {
                    callback.onError("Failed to fetch games and no cached data available");
                }
            }
        }
    }

    // Add getter methods for the flags
    public boolean isTeamsLoadedFromJson() {
        return teamsLoadedFromJson;
    }

    public boolean isPlayersLoadedFromJson() {
        return playersLoadedFromJson;
    }

    public boolean isSeasonsLoadedFromJson() {
        return seasonsLoadedFromJson;
    }

    public boolean isGamesLoadedFromJson() {
        return gamesLoadedFromJson;
    }

    public boolean isGamesBySeasonLoadedFromJson() {
        return gamesBySeasonLoadedFromJson;
    }

    public boolean isInitialLoadComplete() {
        return isInitialLoadComplete;
    }

    // Method to check if we have games for a specific season
//    public boolean hasGamesForSeason(String season) {
//        return gamesBySeason.containsKey(season) && !gamesBySeason.get(season).isEmpty();
//    }

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
                JsonHelper.saveTeamsToJson(context, (List<Team>) teamsById.values().stream().iterator());
            if(Objects.equals(dataToSave, "Players"))
                JsonHelper.savePlayersToJson(context, (List<NHLPlayer>) playersById.values().stream().iterator());
            if(Objects.equals(dataToSave, "Games"))
                JsonHelper.saveGamesToJson(context, (List<Game>) gamesByDate.values().stream().iterator());// forgot we cant use rust iterators and collapse
            if(Objects.equals(dataToSave, "Seasons"))
                JsonHelper.saveSeasonsToJson(context, seasons);
            if(Objects.equals(dataToSave, "Images"))
                JsonHelper.saveImagePathsToJson(context);
            if(Objects.equals(dataToSave, "GamesBySeason")){
                HashMap<String, List<Game>> gameHash = new HashMap<>();
                for(String season: gamesBySeason.keySet()){
                    List<Game> games = new ArrayList<>();
                    for(int gameId: gamesBySeason.get(season)){
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

    private class SaveDataTask extends AsyncTask<Void, Void, Void> {
        private Context context;

        public SaveDataTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Save all data to JSON files
            JsonHelper.saveTeamsToJson(context, (List<Team>) teamsById.values().stream().iterator());
            JsonHelper.savePlayersToJson(context, (List<NHLPlayer>) playersById.values().stream().iterator());
            JsonHelper.saveGamesToJson(context, (List<Game>) gamesByDate.values().stream().iterator());
            JsonHelper.saveSeasonsToJson(context, seasons);
            JsonHelper.saveImagePathsToJson(context);
            HashMap<String, List<Game>> gameHash = new HashMap<>();
            for(String season: gamesBySeason.keySet()){
                List<Game> games = new ArrayList<>();
                for(int gameId: gamesBySeason.get(season)){
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

    private class DownloadTeamLogoTask extends AsyncTask<Void, Void, String> {
        private Team team;

        public DownloadTeamLogoTask(Team team) {
            this.team = team;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return ImageDownloader.downloadTeamLogo(context, team);
        }

        @Override
        protected void onPostExecute(String imagePath) {
            if (imagePath != null) {
                team.setLogoUrl(imagePath);
            }
        }
    }

    private class DownloadPlayerHeadshotTask extends AsyncTask<Void, Void, String> {
        private NHLPlayer player;

        public DownloadPlayerHeadshotTask(NHLPlayer player) {
            this.player = player;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return ImageDownloader.downloadPlayerHeadshot(context, player);
        }

        @Override
        protected void onPostExecute(String imagePath) {
            if (imagePath != null) {
                player.setHeadshotPath(imagePath);
            }
        }
    }

    // Utility methods
    public void clearCache() {
        teamsById.clear();
        playersById.clear();
//        games.clear(); // Still don't super know which one I want to have wiped here, think both?
        gamesBySeason.clear();
        gamesByDate.clear();
        seasons.clear();
    }

    public boolean hasTeamsData() {
        return !teamsById.isEmpty();
    }

    public boolean hasPlayersData() {
        return !playersById.isEmpty();
    }

    public boolean hasSeasonsData() {
        return !seasons.isEmpty();
    }

    public boolean hasGamesData() {
        return !gamesBySeason.isEmpty() && !gamesByDate.isEmpty();
    }

    // Method to get cached teams without callback (synchronous)
    public List<Team> getCachedTeams() {
        return new ArrayList<>(teamsById.values());
    }

    // Method to get cached players without callback (synchronous)
    public List<NHLPlayer> getCachedPlayers() {
        return new ArrayList<>(playersById.values());
    }

    // Method to get cached games without callback (synchronous)
    public ArrayList<Game> getCachedGames() {
        return new ArrayList<>(gamesById.values());
    }

    // Method to get cached seasons without callback (synchronous)
    public List<String> getCachedSeasons() {
        return new ArrayList<>(seasons);
    }

    // Method to add or update a team
    public void updateTeam(Team team) {
        for (int i = 0; i < teamsById.size(); i++) {
            if (teamsById.get(i).getTeamID() == team.getTeamID()) {
                teamsById.replace(i, team);
                return;
            }
        }
        teamsById.put(team.getTeamID(), team);
    }

    // Method to add or update a player
    public void updatePlayer(NHLPlayer player) {
        for (int i = 0; i < playersById.size(); i++) {
            if (playersById.get(i).getPlayerId() == player.getPlayerId()) {
                playersById.replace(i, player);
                return;
            }
        }
        playersById.put(player.getPlayerId(), player);
    }

    // Method to get team by ID
    public Team getTeamById(int teamId) {
        if (teamsById.containsKey(teamId))
            return teamsById.get(teamId);
//        for (Team team : teamsById) {
//            if (team.getId() == teamId) {
//                return team;
//            }
//        }
        return null;
    }

    // Method to get player by ID
    public NHLPlayer getPlayerById(int playerId) {
        if (playersById.containsKey(playerId))
            return playersById.get(playerId);
//        for (Player player : players) {
//            if (player.getId() == playerId) {
//                return player;
//            }
//        }
        return null;
    }

    // Method to get players by team ID
    public List<NHLPlayer> getPlayersByTeamId(int teamId) {
        List<NHLPlayer> teamPlayers = new ArrayList<>();
        for (NHLPlayer player : playersById.values()) {
            if (player.getTeamId() == teamId) {
                teamPlayers.add(player);
            }
        }
        return teamPlayers;
    }

    // Method to force refresh data from API
    public void forceRefresh() {
        clearCache();
        // Optionally trigger new API calls
    }
}