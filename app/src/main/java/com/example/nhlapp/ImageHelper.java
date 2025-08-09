package com.example.nhlapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.example.nhlapp.Objects.Team;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageHelper {
    private static final String TAG = "ImageHelper";
    private static final String LOGOS_DIR = "team_logos";

    private static ImageHelper instance;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Context context;

    public interface ImageLoadCallback {
        void onImageLoaded(Bitmap bitmap);
        void onImageFailed();
    }

    private ImageHelper(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ImageHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ImageHelper(context);
        }
        return instance;
    }

    /**
     * Load team logo into ImageView with fallback
     */
    public void loadTeamLogo(Team team, ImageView imageView, int fallbackDrawableId) {
        if (team == null || imageView == null) {
            return;
        }

        // Set placeholder immediately
        if (fallbackDrawableId != 0) {
            imageView.setImageResource(fallbackDrawableId);
        }

        // Try to load from local storage
        loadTeamLogoAsync(team, new ImageLoadCallback() {
            @Override
            public void onImageLoaded(Bitmap bitmap) {
                mainHandler.post(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }
                });
            }

            @Override
            public void onImageFailed() {
                // Keep the fallback image that was set initially
                Log.d(TAG, "Failed to load logo for team: " + team.getName());
            }
        });
    }

    /**
     * Load team logo into ImageView with custom placeholder
     */
    public void loadTeamLogo(Team team, ImageView imageView, Drawable placeholder) {
        if (team == null || imageView == null) {
            return;
        }

        // Set placeholder immediately
        if (placeholder != null) {
            imageView.setImageDrawable(placeholder);
        }

        // Try to load from local storage
        loadTeamLogoAsync(team, new ImageLoadCallback() {
            @Override
            public void onImageLoaded(Bitmap bitmap) {
                mainHandler.post(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }
                });
            }

            @Override
            public void onImageFailed() {
                // Keep the placeholder that was set initially
                Log.d(TAG, "Failed to load logo for team: " + team.getName());
            }
        });
    }

    /**
     * Load team logo asynchronously
     */
    public void loadTeamLogoAsync(Team team, ImageLoadCallback callback) {
        if (team == null || callback == null) {
            return;
        }

        executorService.execute(() -> {
            try {
                Bitmap bitmap = loadTeamLogoBitmap(team);
                if (bitmap != null) {
                    callback.onImageLoaded(bitmap);
                } else {
                    callback.onImageFailed();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading team logo bitmap", e);
                callback.onImageFailed();
            }
        });
    }

    /**
     * Load team logo bitmap from local storage
     */
    public Bitmap loadTeamLogoBitmap(Team team) {
        if (team == null) {
            return null;
        }

        // Check if team has a logo path set
        String logoPath = team.getLogoPath();
        if (logoPath != null && !logoPath.isEmpty()) {
            File logoFile = new File(context.getFilesDir(), logoPath);
            if (logoFile.exists()) {
                try {
                    return BitmapFactory.decodeFile(logoFile.getAbsolutePath());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decode logo file: " + logoFile.getAbsolutePath(), e);
                }
            }
        }

        // Fallback: try to find logo using team abbreviation
        return loadTeamLogoByAbbreviation(team.getAbreviatedName());
    }

    /**
     * Load team logo by abbreviation
     */
    private Bitmap loadTeamLogoByAbbreviation(String teamAbbrev) {
        if (teamAbbrev == null || teamAbbrev.isEmpty()) {
            return null;
        }

        String filename = teamAbbrev.toLowerCase() + "_logo.png";
        File logoFile = new File(new File(context.getFilesDir(), LOGOS_DIR), filename);

        if (logoFile.exists()) {
            try {
                return BitmapFactory.decodeFile(logoFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode logo file: " + logoFile.getAbsolutePath(), e);
            }
        }

        return null;
    }

    /**
     * Create a circular bitmap from any bitmap
     */
    public static Bitmap createCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(rectF.centerX(), rectF.centerY(), size / 2f, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // Center the original bitmap in the circle
        int left = (size - bitmap.getWidth()) / 2;
        int top = (size - bitmap.getHeight()) / 2;
        canvas.drawBitmap(bitmap, left, top, paint);

        return output;
    }

    /**
     * Create a rounded corner bitmap
     */
    public static Bitmap createRoundedBitmap(Bitmap bitmap, float radius) {
        if (bitmap == null) {
            return null;
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Get team logo file if it exists
     */
    public File getTeamLogoFile(Team team) {
        if (team == null) {
            return null;
        }

        String logoPath = team.getLogoPath();
        if (logoPath != null && !logoPath.isEmpty()) {
            File logoFile = new File(context.getFilesDir(), logoPath);
            if (logoFile.exists()) {
                return logoFile;
            }
        }

        // Fallback: check by abbreviation
        if (team.getAbreviatedName() != null) {
            String filename = team.getAbreviatedName().toLowerCase() + "_logo.png";
            File logoFile = new File(new File(context.getFilesDir(), LOGOS_DIR), filename);
            if (logoFile.exists()) {
                return logoFile;
            }
        }

        return null;
    }

    /**
     * Check if team has local logo
     */
    public boolean hasTeamLogo(Team team) {
        return getTeamLogoFile(team) != null;
    }

    /**
     * Create a placeholder drawable with team abbreviation
     */
    public static Drawable createTeamPlaceholder(Context context, Team team, int backgroundColor, int textColor) {
        if (team == null || team.getAbreviatedName() == null) {
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_gallery);
        }

        // Create a simple colored rectangle with team abbreviation
        // This would require custom drawing - simplified version returns default drawable
        return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_gallery);
    }

    /**
     * Get bitmap dimensions without loading full bitmap
     */
    public static BitmapFactory.Options getBitmapDimensions(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        return options;
    }

    /**
     * Load bitmap with size constraints
     */
    public static Bitmap loadBitmapWithConstraints(String filePath, int maxWidth, int maxHeight) {
        BitmapFactory.Options options = getBitmapDimensions(filePath);

        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        int scaleFactor = 1;
        if (imageWidth > maxWidth || imageHeight > maxHeight) {
            int widthRatio = Math.round((float) imageWidth / (float) maxWidth);
            int heightRatio = Math.round((float) imageHeight / (float) maxHeight);
            scaleFactor = Math.min(widthRatio, heightRatio);
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory

        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * Convert drawable to bitmap
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Shutdown the image helper
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
