package com.example.nhlapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.nhlapp.Objects.Team;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AltImageDownloader {
    private static final String TAG = "ImageDownloader";
    private static final String LOGOS_DIR = "team_logos";
    private static final int MAX_CONCURRENT_DOWNLOADS = 5;
    private static final int DOWNLOAD_TIMEOUT = 10000; // 10 seconds
    private static final int MAX_IMAGE_SIZE = 500; // Max width/height in pixels

    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Context context;
    private File logosDirectory;

    public interface DownloadCallback {
        void onComplete(int successful, int failed, int skipped);
        void onProgress(String teamName, boolean success);
    }

    public AltImageDownloader(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.mainHandler = new Handler(Looper.getMainLooper());
        initializeDirectories();
    }

    private void initializeDirectories() {
        logosDirectory = new File(context.getFilesDir(), LOGOS_DIR);
        if (!logosDirectory.exists()) {
            boolean created = logosDirectory.mkdirs();
            Log.d(TAG, "Created logos directory: " + created);
        }
    }

    /**
     * Download team logos for all teams that don't have them locally
     */
    public void downloadTeamLogos(List<Team> teams, DownloadCallback callback) {
        if (teams == null || teams.isEmpty()) {
            if (callback != null) {
                callback.onComplete(0, 0, 0);
            }
            return;
        }

        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        int totalTeams = teams.size();

        for (Team team : teams) {
            executorService.execute(() -> {
                try {
                    boolean result = downloadTeamLogo(team);

                    if (result == true) {
                        successful.incrementAndGet();
                    } else if (hasLocalLogo(team)) {
                        skipped.incrementAndGet();
                        // Update team with existing logo path
                        String logoPath = getLocalLogoPath(team);
                        team.setLogoPath(logoPath);
                    } else {
                        failed.incrementAndGet();
                    }

                    // Update progress on main thread
                    if (callback != null) {
                        mainHandler.post(() -> callback.onProgress(team.getName(), result));
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error downloading logo for team: " + team.getName(), e);
                    failed.incrementAndGet();

                    if (callback != null) {
                        mainHandler.post(() -> callback.onProgress(team.getName(), false));
                    }
                }

                // Check if all downloads completed
                int completedCount = completed.incrementAndGet();
                if (completedCount >= totalTeams && callback != null) {
                    mainHandler.post(() -> callback.onComplete(
                            successful.get(),
                            failed.get(),
                            skipped.get()
                    ));
                }
            });
        }
    }

    /**
     * Download logo for a single team
     */
    private boolean downloadTeamLogo(Team team) {
        if (team == null || team.getAbreviatedName() == null) {
            Log.w(TAG, "Invalid team data for logo download");
            return false;
        }

        // Check if logo already exists
        if (hasLocalLogo(team)) {
            String existingPath = getLocalLogoPath(team);
            team.setLogoPath(existingPath);
            Log.d(TAG, "Logo already exists for " + team.getName() + ": " + existingPath);
            return false; // Not a new download, but logo exists
        }

        String logoUrl = buildLogoUrl(team);
        if (logoUrl == null) {
            Log.w(TAG, "Could not build logo URL for team: " + team.getName());
            return false;
        }

        try {
            Bitmap logoBitmap = downloadBitmap(logoUrl);
            if (logoBitmap != null) {
                String savedPath = saveBitmapToFile(logoBitmap, team);
                if (savedPath != null) {
                    team.setLogoPath(savedPath);
                    Log.d(TAG, "Successfully downloaded and saved logo for " + team.getName());
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download logo for " + team.getName() + " from " + logoUrl, e);
        }

        return false;
    }

    /**
     * Build the NHL logo URL for a team
     */
    private String buildLogoUrl(Team team) {
        // NHL team logos are available at this URL pattern
        // Using the official NHL API logo endpoints
        String teamAbbrev = team.getAbreviatedName();
        if (teamAbbrev == null || teamAbbrev.isEmpty()) {
            return null;
        }

        // Try multiple logo URL formats
        String[] logoFormats = {
                "https://assets.nhle.com/logos/nhl/svg/" + teamAbbrev + "_light.svg",
                "https://assets.nhle.com/logos/nhl/png/" + teamAbbrev + "_logo.png",
                "https://www-league.nhlstatic.com/images/logos/teams-current-primary-light/" + team.getTeamID() + ".svg"
        };

        // For now, use the PNG format as it's more widely supported
        return "https://assets.nhle.com/logos/nhl/png/" + teamAbbrev + "_logo.png";
    }

    /**
     * Download bitmap from URL
     */
    private Bitmap downloadBitmap(String imageUrl) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT);
            connection.setRequestProperty("User-Agent", "NHL-App/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP response code: " + responseCode);
            }

            inputStream = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                // Resize if too large
                return resizeBitmap(bitmap, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
            }

            return null;

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Resize bitmap to fit within max dimensions
     */
    private Bitmap resizeBitmap(Bitmap original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return original; // No resizing needed
        }

        float scaleWidth = (float) maxWidth / width;
        float scaleHeight = (float) maxHeight / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        Bitmap resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        original.recycle(); // Free memory
        return resized;
    }

    /**
     * Save bitmap to internal storage
     */
    private String saveBitmapToFile(Bitmap bitmap, Team team) {
        String filename = generateLogoFilename(team);
        File logoFile = new File(logosDirectory, filename);

        try (FileOutputStream outputStream = new FileOutputStream(logoFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
            outputStream.flush();

            String relativePath = LOGOS_DIR + File.separator + filename;
            Log.d(TAG, "Saved logo to: " + relativePath);
            return relativePath;

        } catch (IOException e) {
            Log.e(TAG, "Failed to save logo file for " + team.getName(), e);
            // Clean up partial file
            if (logoFile.exists()) {
                logoFile.delete();
            }
            return null;
        } finally {
            bitmap.recycle(); // Free memory
        }
    }

    /**
     * Generate filename for team logo
     */
    private String generateLogoFilename(Team team) {
        String teamAbbrev = team.getAbreviatedName();
        if (teamAbbrev == null || teamAbbrev.isEmpty()) {
            teamAbbrev = "team_" + team.getTeamID();
        }
        return teamAbbrev.toLowerCase() + "_logo.png";
    }

    /**
     * Check if team has local logo
     */
    public boolean hasLocalLogo(Team team) {
        String filename = generateLogoFilename(team);
        File logoFile = new File(logosDirectory, filename);
        return logoFile.exists() && logoFile.length() > 0;
    }

    /**
     * Get local logo path for team
     */
    public String getLocalLogoPath(Team team) {
        if (hasLocalLogo(team)) {
            String filename = generateLogoFilename(team);
            return LOGOS_DIR + File.separator + filename;
        }
        return null;
    }

    /**
     * Get absolute file path for team logo
     */
    public File getLogoFile(Team team) {
        String filename = generateLogoFilename(team);
        File logoFile = new File(logosDirectory, filename);
        return logoFile.exists() ? logoFile : null;
    }

    /**
     * Load bitmap from local storage
     */
    public Bitmap loadLocalLogo(Team team) {
        File logoFile = getLogoFile(team);
        if (logoFile != null) {
            try {
                return BitmapFactory.decodeFile(logoFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Failed to load local logo for " + team.getName(), e);
            }
        }
        return null;
    }

    /**
     * Update all teams with their local logo paths
     */
    public void updateTeamsWithLogoPaths(List<Team> teams) {
        if (teams == null) return;

        for (Team team : teams) {
            if (hasLocalLogo(team)) {
                String logoPath = getLocalLogoPath(team);
                team.setLogoPath(logoPath);
            }
        }
    }

    /**
     * Clean up old logo files
     */
    public void cleanupOldLogos() {
        if (!logosDirectory.exists()) return;

        File[] files = logosDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                // Delete files older than 30 days
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);
                if (file.lastModified() < thirtyDaysAgo) {
                    boolean deleted = file.delete();
                    Log.d(TAG, "Deleted old logo file: " + file.getName() + " (" + deleted + ")");
                }
            }
        }
    }

    /**
     * Get total size of logo cache
     */
    public long getCacheSize() {
        if (!logosDirectory.exists()) return 0;

        long totalSize = 0;
        File[] files = logosDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                totalSize += file.length();
            }
        }
        return totalSize;
    }

    /**
     * Clear all cached logos
     */
    public void clearCache() {
        if (!logosDirectory.exists()) return;

        File[] files = logosDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                boolean deleted = file.delete();
                Log.d(TAG, "Cleared logo cache file: " + file.getName() + " (" + deleted + ")");
            }
        }
    }

    /**
     * Shutdown the downloader
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}