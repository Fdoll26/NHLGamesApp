package com.example.nhlapp;

import android.content.Context;
import android.util.Log;

import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Player;
import com.example.nhlapp.Objects.Team;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.HashMap;
import java.util.List;

public class JsonHelper {

    public static void saveTeamsToJson(Context context, List<Team> teams) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Team team : teams) {
                JSONObject teamJson = new JSONObject();
                teamJson.put("id", team.getTeamID());
                teamJson.put("name", team.getName());
                teamJson.put("logoPath", team.getLogoPath() != null ? team.getLogoPath() : "");
                if (team.getRosterPlayerIds() != null) {
                    JSONArray rosterArray = new JSONArray();
                    for (Integer playerId : team.getRosterPlayerIds()) {
                        rosterArray.put(playerId);
                    }
                    teamJson.put("rosterPlayerIds", rosterArray);
                }
                jsonArray.put(teamJson);
            }
            saveJsonToFile(context, "teams.json", jsonArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void savePlayersToJson(Context context, List<NHLPlayer> players) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (NHLPlayer player : players) {
                JSONObject playerJson = new JSONObject();
                playerJson.put("id", player.getPlayerId());
                playerJson.put("name", player.getName());
                playerJson.put("teamId", player.getTeamId());
                playerJson.put("headshotPath", player.getHeadshotPath() != null ? player.getHeadshotPath() : "");
                jsonArray.put(playerJson);
            }
            saveJsonToFile(context, "players.json", jsonArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveGamesToJson(Context context, List<Game> games) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Game game : games) {
                JSONObject gameJson = new JSONObject();
                gameJson.put("id", game.getId());
                gameJson.put("date", game.getDate());
                gameJson.put("homeTeamId", game.getHomeTeamId());
                gameJson.put("awayTeamId", game.getAwayTeamId());
                gameJson.put("homeScore", game.getHomeScore());
                gameJson.put("awayScore", game.getAwayScore());
                jsonArray.put(gameJson);
            }
            saveJsonToFile(context, "games.json", jsonArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveGamesBySeasonToJson(Context context, HashMap<String, List<Game>> gamesBySeason){
        try {
            JSONObject seasonsObj = new JSONObject();
            for(String season: gamesBySeason.keySet()){
                JSONArray jsonArray = new JSONArray();
                List<Game> gameList = gamesBySeason.get(season);
                for (Game game : gameList) {
                    JSONObject gameJson = new JSONObject();
                    gameJson.put("id", game.getId());
                    gameJson.put("date", game.getDate());
                    gameJson.put("homeTeamId", game.getHomeTeamId());
                    gameJson.put("awayTeamId", game.getAwayTeamId());
                    gameJson.put("homeScore", game.getHomeScore());
                    gameJson.put("awayScore", game.getAwayScore());
                    gameJson.put("homeName", game.getHomeTeamName());
                    gameJson.put("awayName", game.getAwayTeamName());
                    jsonArray.put(gameJson);
                }
                seasonsObj.put(season, jsonArray);
            }
            saveJsonToFile(context, "gamesBySeason.json", seasonsObj.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveImagePathsToJson(Context context) {
        try {
            JSONObject imagePathsJson = new JSONObject();

            // Save team logo paths
            JSONObject teamLogos = new JSONObject();
            File teamLogosDir = new File(context.getFilesDir(), "team_logos");
            if (teamLogosDir.exists()) {
                File[] logoFiles = teamLogosDir.listFiles();
                if (logoFiles != null) {
                    for (File logoFile : logoFiles) {
                        String teamId = logoFile.getName().replace(".png", "");
                        teamLogos.put(teamId, logoFile.getAbsolutePath());
                    }
                }
            }
            imagePathsJson.put("teamLogos", teamLogos);

            // Save player headshot paths
            JSONObject playerHeadshots = new JSONObject();
            File playerHeadshotsDir = new File(context.getFilesDir(), "player_headshots");
            if (playerHeadshotsDir.exists()) {
                File[] headshotFiles = playerHeadshotsDir.listFiles();
                if (headshotFiles != null) {
                    for (File headshotFile : headshotFiles) {
                        String playerId = headshotFile.getName().replace(".png", "");
                        playerHeadshots.put(playerId, headshotFile.getAbsolutePath());
                    }
                }
            }
            imagePathsJson.put("playerHeadshots", playerHeadshots);

            saveJsonToFile(context, "image_paths.json", imagePathsJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveJsonToFile(Context context, String filename, String jsonData) {
        try {
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(jsonData.getBytes());

            Log.d("JsonHelper", "Tried to save " + filename + " to " + context.getFilesDir());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String loadJsonFromFile(Context context, String filename) {
        try {
            Log.d("JsonHelper", "Trying to load file " + filename);
            FileInputStream fis = context.openFileInput(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            Log.d("JsonHelper", "Found file at " + context.getFilesDir());
            reader.close();
            fis.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static void deleteAllJsonFiles(Context context) {
        String[] filenames = {"teams.json", "players.json", "games.json", "image_paths.json"};
        for (String filename : filenames) {
            File file = new File(context.getFilesDir(), filename);
            if (file.exists()) {
                file.delete();
            }
        }

        // Also delete season-specific game files
        File filesDir = context.getFilesDir();
        File[] files = filesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("games_") && file.getName().endsWith(".json")) {
                    file.delete();
                }
            }
        }
    }

    public static void saveSeasonsToJson(Context context, List<String> seasons) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (String season : seasons) {
                jsonArray.put(season);
            }
            saveJsonToFile(context, "seasons.json", jsonArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
