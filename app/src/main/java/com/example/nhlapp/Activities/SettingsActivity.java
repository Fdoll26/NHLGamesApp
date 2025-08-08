package com.example.nhlapp.Activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nhlapp.AppSettings;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.ImageDownloader;
import com.example.nhlapp.JsonHelper;
import com.example.nhlapp.R;

public class SettingsActivity extends AppCompatActivity {
    private Switch switchJsonSaving, switchOnlineMode, switchUseSingleton, switchPeriodicSaving;
    private Button btnSaveJson, btnDeleteJson, btnDeleteImages;
    private AppSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = AppSettings.getInstance(this);
        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchJsonSaving = findViewById(R.id.switchJsonSaving);
        switchOnlineMode = findViewById(R.id.switchOnlineMode);
        switchUseSingleton = findViewById(R.id.switchUseSingleton);
        switchPeriodicSaving = findViewById(R.id.switchPeriodicSaving);
        btnSaveJson = findViewById(R.id.btnSaveJson);
        btnDeleteJson = findViewById(R.id.btnDeleteJson);
        btnDeleteImages = findViewById(R.id.btnDeleteImages);
    }

    private void loadSettings() {
        switchJsonSaving.setChecked(settings.isJsonSavingEnabled());
        switchOnlineMode.setChecked(settings.isOnlineMode());
        switchUseSingleton.setChecked(settings.useSingleton());
        switchPeriodicSaving.setChecked(settings.isPeriodicSavingEnabled());
    }

    private void setupListeners() {
        switchJsonSaving.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setJsonSavingEnabled(isChecked);
        });

        switchOnlineMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setOnlineMode(isChecked);
        });

        switchUseSingleton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setUseSingleton(isChecked);
        });

        switchPeriodicSaving.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setPeriodicSavingEnabled(isChecked);
        });

        btnSaveJson.setOnClickListener(v -> {
            DataManager.getInstance().saveAllDataToJson(this);
        });

        btnDeleteJson.setOnClickListener(v -> {
            JsonHelper.deleteAllJsonFiles(this);
        });

        btnDeleteImages.setOnClickListener(v -> {
            ImageDownloader.deleteAllImages(this);
        });
    }
}