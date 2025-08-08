package com.example.nhlapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Adapters.DateAdapter;
import com.example.nhlapp.Adapters.GameAdapter;
import com.example.nhlapp.Adapters.GamesAdapter;
import com.example.nhlapp.AppSettings;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.ImageDownloader;
import com.example.nhlapp.JsonHelper;
import com.example.nhlapp.Objects.Game;
import com.example.nhlapp.R;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatesActivity extends AppCompatActivity {
    private static final String TAG = "DatesActivity";
    private static final long LIVE_UPDATE_INTERVAL = 15000; // 15 seconds

    private RecyclerView dateRecyclerView;
    private RecyclerView gamesRecyclerView;
    private DateAdapter dateAdapter;
    private GameAdapter gameAdapter;

    private ArrayList<String> dates;
    private ArrayList<Game> games;
    private DataManager dataManager;
    private String selectedDate;
    private Map<String, List<Game>> gamesByDateCache;

    private Handler liveUpdateHandler;
    private Runnable liveUpdateRunnable;
    private boolean isLiveUpdatesEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dates);
        dataManager = DataManager.getInstance();
        gamesByDateCache = new HashMap<>();

//        settings = AppSettings.getInstance(this);
        initViews();
    }

    private void initViews() {
        dateRecyclerView = findViewById(R.id.recycler_dates);
        LinearLayoutManager dateLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        dateRecyclerView.setLayoutManager(dateLayoutManager);

        dates = new ArrayList<>();
        dateAdapter = new DateAdapter(dates, this::selectDate);
        dateRecyclerView.setAdapter(dateAdapter);

        // Games RecyclerView
        gamesRecyclerView = findViewById(R.id.recycler_games);
        LinearLayoutManager gamesLayoutManager = new LinearLayoutManager(this);
        gamesRecyclerView.setLayoutManager(gamesLayoutManager);

        games = new ArrayList<>();
        gameAdapter = new GameAdapter(games, this::onGameClicked);
        gamesRecyclerView.setAdapter(gameAdapter);
    }

    private String getTodayDateString() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(cal.getTime());
    }

    private void stopLiveUpdates() {
        if (isLiveUpdatesEnabled) {
            Log.d(TAG, "Stopping live updates");
            isLiveUpdatesEnabled = false;
            if (liveUpdateRunnable != null) {
                liveUpdateHandler.removeCallbacks(liveUpdateRunnable);
            }
        }
    }

    private void updateGamesDisplay(List<Game> gamesToShow) {
        games.clear();
        games.addAll(gamesToShow);
        gameAdapter.notifyDataSetChanged();

        Log.d(TAG, "Updated games display with " + gamesToShow.size() + " games");
    }

    private void startLiveUpdates() {
        if (!isLiveUpdatesEnabled && getTodayDateString().equals(selectedDate)) {
            Log.d(TAG, "Starting live updates for today's games");
            isLiveUpdatesEnabled = true;

            liveUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isLiveUpdatesEnabled && getTodayDateString().equals(selectedDate)) {
                        updateLiveScores();
                        liveUpdateHandler.postDelayed(this, LIVE_UPDATE_INTERVAL);
                    }
                }
            };

            // Start after initial delay
            liveUpdateHandler.postDelayed(liveUpdateRunnable, LIVE_UPDATE_INTERVAL);
        }
    }
    /// TODO: Finish the update live scores because forgot lol
    private void updateLiveScores(){

    }

    private void selectDate(String date) {
        Log.d(TAG, "Selecting date: " + date);

        // Stop live updates when switching away from today
        if (!getTodayDateString().equals(date)) {
            stopLiveUpdates();
        }

        selectedDate = date;

        // Check cache first
        if (gamesByDateCache.containsKey(date)) {
            List<Game> cachedGames = gamesByDateCache.get(date);
            updateGamesDisplay(cachedGames);
            Log.d(TAG, "Using cached games for " + date + ": " + cachedGames.size() + " games");

            // Start live updates if this is today
            if (getTodayDateString().equals(date)) {
                startLiveUpdates();
            }
            return;
        }

        // Try to get games from DataManager
        ArrayList<Game> gamesForDate = dataManager.getGamesForDate(date);
        if (gamesForDate != null && !gamesForDate.isEmpty()) {
            gamesByDateCache.put(date, gamesForDate);
            updateGamesDisplay(gamesForDate);
            Log.d(TAG, "Using DataManager games for " + date + ": " + gamesForDate.size() + " games");

            // Start live updates if this is today
            if (getTodayDateString().equals(date)) {
                startLiveUpdates();
            }
        } else {
            // No games found for this date
            updateGamesDisplay(new ArrayList<>());
            showMessage("No games found for " + date);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    public void onGameClicked(Game game) {
        Intent intent = new Intent(this, SpecificGameActivity.class);
        intent.putExtra("gameId", game.getId());
        startActivity(intent);
    }
}
