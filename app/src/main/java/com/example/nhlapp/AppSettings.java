package com.example.nhlapp;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    private static AppSettings instance;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "nhl_app_settings";
    private static final String KEY_JSON_SAVING = "json_saving_enabled";
    private static final String KEY_ONLINE_MODE = "online_mode";
    private static final String KEY_USE_SINGLETON = "use_singleton";
    private static final String KEY_PERIODIC_SAVING = "periodic_saving";

    private AppSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static AppSettings getInstance(Context context) {
        if (instance == null) {
            instance = new AppSettings(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isJsonSavingEnabled() {
        return prefs.getBoolean(KEY_JSON_SAVING, true);
    }

    public void setJsonSavingEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_JSON_SAVING, enabled).apply();
    }

    public boolean isOnlineMode() {
        return prefs.getBoolean(KEY_ONLINE_MODE, true);
    }

    public void setOnlineMode(boolean online) {
        prefs.edit().putBoolean(KEY_ONLINE_MODE, online).apply();
    }

    public boolean useSingleton() {
        return prefs.getBoolean(KEY_USE_SINGLETON, false);
    }

    public void setUseSingleton(boolean useSingleton) {
        prefs.edit().putBoolean(KEY_USE_SINGLETON, useSingleton).apply();
    }

    public boolean isPeriodicSavingEnabled() {
        return prefs.getBoolean(KEY_PERIODIC_SAVING, false);
    }

    public void setPeriodicSavingEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PERIODIC_SAVING, enabled).apply();
    }
}