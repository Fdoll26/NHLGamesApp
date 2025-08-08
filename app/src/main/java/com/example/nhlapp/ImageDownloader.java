package com.example.nhlapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Player;
import com.example.nhlapp.Objects.Team;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageDownloader {

    public static String downloadTeamLogo(Context context, Team team) {
        try {
            // Construct NHL team logo URL (official NHL API pattern)
            String logoUrl = "https://www-league.nhlstatic.com/images/logos/teams-current-primary-light/" + team.getTeamID() + ".svg";

            File logoDir = new File(context.getFilesDir(), "team_logos");
            if (!logoDir.exists()) {
                logoDir.mkdirs();
            }

            File logoFile = new File(logoDir, team.getTeamID() + ".png");

            if (logoFile.exists()) {
                return logoFile.getAbsolutePath();
            }

            URL url = new URL(logoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();

            if (bitmap != null) {
                FileOutputStream out = new FileOutputStream(logoFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                return logoFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String downloadPlayerHeadshot(Context context, NHLPlayer player) {
        try {
            // Construct NHL player headshot URL (official NHL API pattern)
            String headshotUrl = "https://cms.nhl.bamgrid.com/images/headshots/current/168x168/" + player.getPlayerId() + ".jpg";

            File headshotDir = new File(context.getFilesDir(), "player_headshots");
            if (!headshotDir.exists()) {
                headshotDir.mkdirs();
            }

            File headshotFile = new File(headshotDir, player.getPlayerId() + ".jpg");

            if (headshotFile.exists()) {
                return headshotFile.getAbsolutePath();
            }

            URL url = new URL(headshotUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();

            if (bitmap != null) {
                FileOutputStream out = new FileOutputStream(headshotFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.close();
                return headshotFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteAllImages(Context context) {
        // Delete team logos
        File teamLogosDir = new File(context.getFilesDir(), "team_logos");
        if (teamLogosDir.exists()) {
            File[] logoFiles = teamLogosDir.listFiles();
            if (logoFiles != null) {
                for (File file : logoFiles) {
                    file.delete();
                }
            }
            teamLogosDir.delete();
        }

        // Delete player headshots
        File playerHeadshotsDir = new File(context.getFilesDir(), "player_headshots");
        if (playerHeadshotsDir.exists()) {
            File[] headshotFiles = playerHeadshotsDir.listFiles();
            if (headshotFiles != null) {
                for (File file : headshotFiles) {
                    file.delete();
                }
            }
            playerHeadshotsDir.delete();
        }
    }
}