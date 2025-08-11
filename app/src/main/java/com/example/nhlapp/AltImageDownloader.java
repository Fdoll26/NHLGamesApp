package com.example.nhlapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.example.nhlapp.Objects.Team;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AltImageDownloader {
    private static final String TAG = "ImageDownloader";
    private static final String LOGOS_DIR = "team_logos";
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    private static final int DOWNLOAD_TIMEOUT = 15000; // 15 seconds
    private static final int MAX_IMAGE_SIZE = 200; // Max width/height in pixels
    private static final String LOGO_FILE_EXTENSION = ".png";

    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Context context;
    private File logosDirectory;

    // Track downloads in progress to prevent duplicates
    private final Set<String> downloadsInProgress = ConcurrentHashMap.newKeySet();
    private final Set<String> downloadsFailed = ConcurrentHashMap.newKeySet();

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
        // Use getFilesDir() which points to /data/data/com.example.nhlapp/files/
        logosDirectory = new File(context.getFilesDir(), LOGOS_DIR);
        if (!logosDirectory.exists()) {
            boolean created = logosDirectory.mkdirs();
            Log.d(TAG, "Created logos directory: " + created + " at " + logosDirectory.getAbsolutePath());
        } else {
            Log.d(TAG, "Logos directory exists at: " + logosDirectory.getAbsolutePath());
        }

        // Debug: Check if we can write to this directory
        File testFile = new File(logosDirectory, "test.txt");
        try {
            boolean testCreated = testFile.createNewFile();
            if (testCreated) {
                testFile.delete();
                Log.d(TAG, "Directory is writable");
            }
        } catch (IOException e) {
            Log.e(TAG, "Directory is not writable: " + e.getMessage());
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

        // Filter out teams that already have logos or are currently downloading
        List<Team> teamsToDownload = teams.stream()
                .filter(this::shouldDownloadLogo)
                .collect(java.util.stream.Collectors.toList());

        if (teamsToDownload.isEmpty()) {
            Log.d(TAG, "No teams need logo downloads");
            // Update all teams with their existing logo paths
            for (Team team : teams) {
                updateTeamWithLocalPath(team);
            }
            if (callback != null) {
                callback.onComplete(0, 0, teams.size());
            }
            return;
        }

        Log.d(TAG, "Starting logo downloads for " + teamsToDownload.size() + " teams");

        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        int totalTeams = teamsToDownload.size();

        for (Team team : teamsToDownload) {
            String teamKey = getTeamKey(team);

            // Mark as in progress
            downloadsInProgress.add(teamKey);

            executorService.execute(() -> {
                try {
                    boolean result = downloadTeamLogo(team);

                    if (result) {
                        successful.incrementAndGet();
                        Log.d(TAG, "Successfully downloaded logo for " + team.getAbreviatedName());

                        // IMPORTANT: Update the team object with the local path immediately
                        String localPath = getLocalLogoPath(team);
                        if (localPath != null) {
                            team.setLogoPath(localPath);
                            Log.d(TAG, "Set logoPath for " + team.getAbreviatedName() + ": " + localPath);
                        }

                    } else if (hasLocalLogo(team)) {
                        skipped.incrementAndGet();
                        updateTeamWithLocalPath(team);
                        Log.d(TAG, "Logo already exists for " + team.getAbreviatedName());
                    } else {
                        failed.incrementAndGet();
                        downloadsFailed.add(teamKey);
                        Log.w(TAG, "Failed to download logo for " + team.getAbreviatedName());
                    }

                    // Update progress on main thread
                    if (callback != null) {
                        mainHandler.post(() -> callback.onProgress(
                                team.getAbreviatedName() != null ? team.getAbreviatedName() : "Unknown Team",
                                result
                        ));
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error downloading logo for team: " + team.getAbreviatedName(), e);
                    failed.incrementAndGet();
                    downloadsFailed.add(teamKey);

                    if (callback != null) {
                        mainHandler.post(() -> callback.onProgress(
                                team.getAbreviatedName() != null ? team.getAbreviatedName() : "Unknown Team",
                                false
                        ));
                    }
                } finally {
                    // Remove from in-progress tracking
                    downloadsInProgress.remove(teamKey);
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

        // Update teams that already have logos with their paths
        for (Team team : teams) {
            if (!teamsToDownload.contains(team)) {
                updateTeamWithLocalPath(team);
            }
        }
    }

    /**
     * Check if a team should have its logo downloaded
     */
    private boolean shouldDownloadLogo(Team team) {
        if (team == null || team.getAbreviatedName() == null || team.getAbreviatedName().trim().isEmpty()) {
            return false;
        }

        String teamKey = getTeamKey(team);

        // Don't download if already in progress or recently failed
        if (downloadsInProgress.contains(teamKey) || downloadsFailed.contains(teamKey)) {
            return false;
        }

        // Don't download if we already have the logo locally
        return !hasLocalLogo(team);
    }

    /**
     * Generate a unique key for tracking team downloads
     */
    private String getTeamKey(Team team) {
        if (team.getAbreviatedName() != null && !team.getAbreviatedName().trim().isEmpty()) {
            return team.getAbreviatedName().toUpperCase();
        }
        return "TEAM_" + team.getTeamID();
    }

    /**
     * Download logo for a single team
     */
    private boolean downloadTeamLogo(Team team) {
        if (team == null || team.getAbreviatedName() == null || team.getAbreviatedName().trim().isEmpty()) {
            Log.w(TAG, "Invalid team data for logo download");
            return false;
        }

        // Double-check if logo already exists (race condition protection)
        if (hasLocalLogo(team)) {
            updateTeamWithLocalPath(team);
            return false; // Not a new download, but logo exists
        }

        // Use logoUrl from team if available, otherwise build URL
        String logoUrl = team.logoUrl;
        if (logoUrl == null || logoUrl.trim().isEmpty()) {
            logoUrl = buildLogoUrl(team);
        }

        if (logoUrl == null) {
            Log.w(TAG, "Could not determine logo URL for team: " + team.getAbreviatedName());
            return false;
        }

        Log.d(TAG, "Downloading logo for " + team.getAbreviatedName() + " from: " + logoUrl);

        try {
            Bitmap logoBitmap = downloadAndConvertSvg(logoUrl);
            if (logoBitmap != null) {
                String savedPath = saveBitmapToFile(logoBitmap, team);
                if (savedPath != null) {
                    // CRITICAL: Set the logoPath immediately after successful save
                    team.setLogoPath(savedPath);
                    Log.d(TAG, "Successfully downloaded and saved logo for " + team.getAbreviatedName() + " to " + savedPath);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download logo for " + team.getAbreviatedName() + " from " + logoUrl, e);
        }

        return false;
    }

    /**
     * Build the NHL logo URL for a team using their abbreviation
     */
    private String buildLogoUrl(Team team) {
        String teamAbbrev = team.getAbreviatedName();
        if (teamAbbrev == null || teamAbbrev.isEmpty()) {
            return null;
        }

        // Use the NHL assets URL format
        return "https://assets.nhle.com/logos/nhl/svg/" + teamAbbrev + "_light.svg";
    }

    /**
     * Download SVG and convert to bitmap
     */
    private Bitmap downloadAndConvertSvg(String imageUrl) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT);
            connection.setRequestProperty("User-Agent", "NHL-App/1.0");
            connection.setRequestProperty("Accept", "image/svg+xml,image/*,*/*");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP error " + responseCode + " for URL: " + imageUrl);
                return null;
            }

            inputStream = connection.getInputStream();

            // Check if it's actually an SVG by trying to parse it
            if (imageUrl.toLowerCase().contains(".svg")) {
                return convertSvgToBitmap(inputStream);
            } else {
                // Fallback for non-SVG images
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    return resizeBitmap(bitmap, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
                }
            }

            return null;

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing input stream", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Convert SVG InputStream to Bitmap
     */
    private Bitmap convertSvgToBitmap(InputStream svgInputStream) {
        try {
            // Try to use AndroidSVG library if available
            SVG svg = SVG.getFromInputStream(svgInputStream);

            // Set a reasonable size for the SVG
            if (svg.getDocumentWidth() <= 0) {
                svg.setDocumentWidth(MAX_IMAGE_SIZE);
            }
            if (svg.getDocumentHeight() <= 0) {
                svg.setDocumentHeight(MAX_IMAGE_SIZE);
            }

            // Create bitmap and render SVG to it
            Bitmap bitmap = Bitmap.createBitmap(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            svg.renderToCanvas(canvas);

            return bitmap;

        } catch (SVGParseException e) {
            Log.e(TAG, "Failed to parse SVG", e);
            return createPlaceholderBitmap();
        } catch (Exception e) {
            Log.w(TAG, "AndroidSVG library not available, creating placeholder", e);
            return createPlaceholderBitmap();
        }
    }

    /**
     * Create a simple placeholder bitmap for teams without logos
     */
    private Bitmap createPlaceholderBitmap() {
        Bitmap placeholder = Bitmap.createBitmap(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(placeholder);

        // Fill with a simple background color (light gray)
        canvas.drawColor(0xFFE0E0E0);

        return placeholder;
    }

    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private Bitmap resizeBitmap(Bitmap original, int maxWidth, int maxHeight) {
        if (original == null) return null;

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
        if (resized != original) {
            original.recycle(); // Free memory only if we created a new bitmap
        }
        return resized;
    }

    /**
     * Save bitmap to internal storage
     */
    private String saveBitmapToFile(Bitmap bitmap, Team team) {
        if (bitmap == null) return null;

        String filename = generateLogoFilename(team);
        File logoFile = new File(logosDirectory, filename);

        // Ensure directory exists
        if (!logosDirectory.exists()) {
            boolean created = logosDirectory.mkdirs();
            Log.d(TAG, "Created logos directory: " + created);
            if (!created) {
                Log.e(TAG, "Failed to create logos directory");
                return null;
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(logoFile)) {
            boolean compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
            if (!compressed) {
                Log.e(TAG, "Failed to compress bitmap for " + team.getAbreviatedName());
                return null;
            }

            outputStream.flush();

            String absolutePath = logoFile.getAbsolutePath();
            Log.d(TAG, "Saved logo to: " + absolutePath + " (size: " + logoFile.length() + " bytes)");

            // Verify the file was actually created
            if (!logoFile.exists() || logoFile.length() == 0) {
                Log.e(TAG, "Logo file was not created properly for " + team.getAbreviatedName());
                return null;
            }

            return absolutePath;

        } catch (IOException e) {
            Log.e(TAG, "Failed to save logo file for " + team.getAbreviatedName(), e);
            // Clean up partial file
            if (logoFile.exists()) {
                logoFile.delete();
            }
            return null;
        } finally {
            // Don't recycle bitmap here as it might be used elsewhere
        }
    }

    /**
     * Generate filename for team logo based on team abbreviation
     */
    private String generateLogoFilename(Team team) {
        String teamAbbrev = team.getAbreviatedName();
        if (teamAbbrev == null || teamAbbrev.trim().isEmpty()) {
            teamAbbrev = "team_" + team.getTeamID();
        }
        return teamAbbrev.toLowerCase().trim() + LOGO_FILE_EXTENSION;
    }

    /**
     * Check if team has local logo file
     */
    public boolean hasLocalLogo(Team team) {
        if (team == null) return false;

        String filename = generateLogoFilename(team);
        File logoFile = new File(logosDirectory, filename);
        boolean exists = logoFile.exists() && logoFile.length() > 0;

        if (exists) {
            Log.d(TAG, "Local logo exists for " + team.getAbreviatedName() + ": " + logoFile.getAbsolutePath());
        }

        return exists;
    }

    /**
     * Get local logo path for team (absolute path)
     */
    public String getLocalLogoPath(Team team) {
        if (team == null) return null;

        String filename = generateLogoFilename(team);
        File logoFile = new File(logosDirectory, filename);

        if (logoFile.exists() && logoFile.length() > 0) {
            String absolutePath = logoFile.getAbsolutePath();
            Log.d(TAG, "Found local logo for " + team.getAbreviatedName() + ": " + absolutePath);
            return absolutePath;
        }

        return null;
    }

    /**
     * Update team with local logo path if it exists
     */
    private void updateTeamWithLocalPath(Team team) {
        if (team == null) return;

        String localPath = getLocalLogoPath(team);
        if (localPath != null) {
            team.setLogoPath(localPath);
            Log.d(TAG, "Updated " + team.getAbreviatedName() + " with local logo path: " + localPath);
        }
    }

    /**
     * Get absolute file object for team logo
     */
    public File getLogoFile(Team team) {
        if (team == null) return null;

        String filename = generateLogoFilename(team);
        File logoFile = new File(logosDirectory, filename);
        return logoFile.exists() && logoFile.length() > 0 ? logoFile : null;
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
                Log.e(TAG, "Failed to load local logo for " + team.getAbreviatedName(), e);
            }
        }
        return null;
    }

    /**
     * Update all teams with their local logo paths
     */
    public void updateTeamsWithLogoPaths(List<Team> teams) {
        if (teams == null) return;

        int updatedCount = 0;
        for (Team team : teams) {
            String oldPath = team.getLogoPath();
            updateTeamWithLocalPath(team);
            if (team.getLogoPath() != null && !team.getLogoPath().equals(oldPath)) {
                updatedCount++;
            }
        }
        Log.d(TAG, "Updated " + updatedCount + " teams with local logo paths");
    }

    /**
     * Debug method to list all downloaded logos
     */
    public void listDownloadedLogos() {
        if (!logosDirectory.exists()) {
            Log.d(TAG, "Logos directory does not exist");
            return;
        }

        File[] files = logosDirectory.listFiles();
        if (files != null && files.length > 0) {
            Log.d(TAG, "Downloaded logos (" + files.length + "):");
            for (File file : files) {
                Log.d(TAG, "  - " + file.getName() + " (" + file.length() + " bytes)");
            }
        } else {
            Log.d(TAG, "No downloaded logos found in: " + logosDirectory.getAbsolutePath());
        }
    }

    /**
     * Clean up old logo files (older than specified days)
     */
    public void cleanupOldLogos(int daysOld) {
        if (!logosDirectory.exists()) return;

        File[] files = logosDirectory.listFiles();
        if (files != null) {
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
            int deletedCount = 0;

            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        deletedCount++;
                        Log.d(TAG, "Deleted old logo file: " + file.getName());
                    }
                }
            }

            Log.d(TAG, "Cleanup completed: deleted " + deletedCount + " old logo files");
        }
    }

    /**
     * Get total size of logo cache in bytes
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

        Log.d(TAG, "Logo cache size: " + totalSize + " bytes (" + (files != null ? files.length : 0) + " files)");
        return totalSize;
    }

    /**
     * Clear all cached logos
     */
    public void clearCache() {
        if (!logosDirectory.exists()) return;

        File[] files = logosDirectory.listFiles();
        if (files != null) {
            int deletedCount = 0;
            for (File file : files) {
                boolean deleted = file.delete();
                if (deleted) {
                    deletedCount++;
                    Log.d(TAG, "Cleared logo cache file: " + file.getName());
                }
            }
            Log.d(TAG, "Cache cleared: deleted " + deletedCount + " files");
        }

        // Clear tracking sets
        downloadsInProgress.clear();
        downloadsFailed.clear();
    }

    /**
     * Reset failed downloads tracking (call this when you want to retry failed downloads)
     */
    public void resetFailedDownloads() {
        downloadsFailed.clear();
        Log.d(TAG, "Reset failed downloads tracking");
    }

    /**
     * Get the number of downloads currently in progress
     */
    public int getDownloadsInProgress() {
        return downloadsInProgress.size();
    }

    /**
     * Shutdown the downloader and clean up resources
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "Image downloader shutdown");
        }

        // Clear tracking sets
        downloadsInProgress.clear();
        downloadsFailed.clear();
    }
}