package com.example.nhlapp;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class PeriodicSaveService extends Service {
    private Handler handler;
    private Runnable saveRunnable;
    private static final long SAVE_INTERVAL = 300000; // 5 minutes
    private boolean isFirstRun = true;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        saveRunnable = new Runnable() {
            @Override
            public void run() {
                AppSettings settings = AppSettings.getInstance(PeriodicSaveService.this);

                // Skip saving on first run - only save periodically after that
                if (isFirstRun) {
                    Log.d("PeriodicSaveService", "Skipping save on first run");
                    isFirstRun = false;
                } else if (settings.isPeriodicSavingEnabled()) {
                    Log.d("PeriodicSaveService", "Performing periodic save");
                    DataManager.getInstance().saveAllDataToJson(PeriodicSaveService.this);
                    JsonHelper.saveImagePathsToJson(PeriodicSaveService.this);
                }

                // Schedule next save
                handler.postDelayed(this, SAVE_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the periodic saving cycle
        handler.post(saveRunnable);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && saveRunnable != null) {
            handler.removeCallbacks(saveRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}