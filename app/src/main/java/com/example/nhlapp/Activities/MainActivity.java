package com.example.nhlapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nhlapp.AppSettings;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.PeriodicSaveService;
import com.example.nhlapp.R;
import com.example.nhlapp.Activities.TeamsActivity;

public class MainActivity extends AppCompatActivity {
    private Button btnTeams, btnPlayers, btnGames, btnDates, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();

        // Initialize data manager based on settings
        DataManager.getInstance().initialize(this);
    }

    private void initViews() {
        btnTeams = findViewById(R.id.btnTeams);
        btnPlayers = findViewById(R.id.btnPlayers);
        btnGames = findViewById(R.id.btnGames);
        btnDates = findViewById(R.id.btnDates);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void setupClickListeners() {
        btnTeams.setOnClickListener(v -> startActivity(new Intent(this, TeamsActivity.class)));
        btnPlayers.setOnClickListener(v -> startActivity(new Intent(this, PlayersActivity.class)));
        btnGames.setOnClickListener(v -> startActivity(new Intent(this, GamesActivity.class)));
        btnDates.setOnClickListener(v -> startActivity(new Intent(this, DatesActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void startPeriodicSaveService() {
        AppSettings settings = AppSettings.getInstance(this);
        if (settings.isPeriodicSavingEnabled()) {
            Intent serviceIntent = new Intent(this, PeriodicSaveService.class);
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPeriodicSaveService();
    }
}