package com.example.nhlapp.Activities;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.AppSettings;
import com.example.nhlapp.DataCallback;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.ImageDownloader;
import com.example.nhlapp.JsonHelper;
import com.example.nhlapp.NHLApiClient;
import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Player;
import com.example.nhlapp.Adapters.PlayersAdapter;
import com.example.nhlapp.R;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PlayersActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PlayersAdapter adapter;
    private EditText searchEditText;
    private Button btnNextPage, btnPrevPage;
    private Button savePlayersBtn;
    private List<NHLPlayer> allPlayers = new ArrayList<>();
    private List<NHLPlayer> filteredPlayers = new ArrayList<>();
    private List<NHLPlayer> currentPagePlayers = new ArrayList<>();
    private int currentPage = 0;
    private static final int PLAYERS_PER_PAGE = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_players);

        initViews();
        setupRecyclerView();
        setupSearch();
        setupPagination();
        loadPlayers();
    }

    private void initViews() {
        savePlayersBtn = findViewById(R.id.savePlayersBtn);
        recyclerView = findViewById(R.id.recyclerViewPlayers);
        searchEditText = findViewById(R.id.searchEditText);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnPrevPage = findViewById(R.id.btnPrevPage);
    }

    private void setupRecyclerView() {
        adapter = new PlayersAdapter(currentPagePlayers, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPlayers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupPagination() {
        Context context = this;
        savePlayersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataManager manager = DataManager.getInstance();
                if(!allPlayers.isEmpty()){
                    // Lets just save all of the players that are filtered
                    if(!filteredPlayers.isEmpty()){
                        manager.addPlayers(filteredPlayers);
                    } else{
                        manager.addPlayers(allPlayers);
                    }

                }
                manager.saveSpecificDataToJson(context, "Players");
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if ((currentPage + 1) * PLAYERS_PER_PAGE < filteredPlayers.size()) {
                currentPage++;
                updateCurrentPage();
            }
        });

        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                updateCurrentPage();
            }
        });
    }

    // Can I do searches like a shitty sql query?
    // So I could do Team:OTT or Team:Sunrise or Team:Wild or Team:Calgary Flames
    // I also want to do some sort of stats look up with this, but this might be another search on its own
    // For this stats I could have a few for the cayane expression but Im not entirely sure about that one yet
    // Stats can also be something I can get from internally looking at lists, but because it is not a DB, this might become more difficult
    // For now have it as an API call and later down the line refine for optimization
    private void filterPlayers(String query) {
        filteredPlayers.clear();
        if (query.isEmpty()) {
            filteredPlayers.addAll(allPlayers);
        } else {
            for (NHLPlayer player : allPlayers) {
                if (player.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredPlayers.add(player);
                }
            }
        }
        currentPage = 0;
        updateCurrentPage();
    }

    private void updateCurrentPage() {
        currentPagePlayers.clear();
        int start = currentPage * PLAYERS_PER_PAGE;
        int end = Math.min(start + PLAYERS_PER_PAGE, filteredPlayers.size());

        for (int i = start; i < end; i++) {
            currentPagePlayers.add(filteredPlayers.get(i));
        }

        adapter.notifyDataSetChanged();
        updatePaginationButtons();
    }

    private void updatePaginationButtons() {
        btnPrevPage.setVisibility(currentPage > 0 ? View.VISIBLE : View.INVISIBLE);
        btnNextPage.setVisibility((currentPage + 1) * PLAYERS_PER_PAGE < filteredPlayers.size() ? View.VISIBLE : View.INVISIBLE);
    }

    private void loadPlayers() {
        AppSettings settings = AppSettings.getInstance(this);

        if (settings.useSingleton()) {
            if (settings.isJsonSavingEnabled()) {
                loadPlayersFromJson();
                if(!allPlayers.isEmpty())
                    return;
            }
            DataManager.getInstance().getPlayers(new DataCallback<List<NHLPlayer>>() {
                @Override
                public void onSuccess(List<NHLPlayer> data) {
                    allPlayers.clear();
                    allPlayers.addAll(data);
                    filteredPlayers.clear();
                    filteredPlayers.addAll(allPlayers);
                    updateCurrentPage();
                }

                @Override
                public void onError(String error) {
                    // Handle error
                }
            });
        } else {
            if (settings.isJsonSavingEnabled()) {
                loadPlayersFromJson();
            }

            if (settings.isOnlineMode()) {
                new FetchPlayersTask().execute();
            }
        }
    }

    private void loadPlayersFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(this, "players.json");
        if (jsonData != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                List<NHLPlayer> loadedPlayers = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject playerJson = jsonArray.getJSONObject(i);
                    NHLPlayer player = new NHLPlayer();
                    player.setPlayerId(playerJson.getInt("id"));
                    player.setName(playerJson.getString("name"));
                    player.setTeamId(playerJson.getInt("teamId"));
                    player.setHeadshotPath(playerJson.optString("headshotPath", ""));
                    loadedPlayers.add(player);
                }

                allPlayers.clear();
                allPlayers.addAll(loadedPlayers);
                filteredPlayers.clear();
                filteredPlayers.addAll(allPlayers);
                updateCurrentPage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class FetchPlayersTask extends AsyncTask<Void, Void, List<NHLPlayer>> {
        @Override
        protected List<NHLPlayer> doInBackground(Void... voids) {
            return NHLApiClient.getPlayers();
        }

        @Override
        protected void onPostExecute(List<NHLPlayer> result) {
            if (result != null) {
                allPlayers.clear();
                allPlayers.addAll(result);
                filteredPlayers.clear();
                filteredPlayers.addAll(allPlayers);
                updateCurrentPage();

                // Download player headshots
                for (NHLPlayer player : allPlayers) {
                    new DownloadPlayerImageTask(player).execute();
                }
            }
        }
    }

    private class DownloadPlayerImageTask extends AsyncTask<Void, Void, String> {
        private NHLPlayer player;

        public DownloadPlayerImageTask(NHLPlayer player) {
            this.player = player;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return ImageDownloader.downloadPlayerHeadshot(PlayersActivity.this, player);
        }

        @Override
        protected void onPostExecute(String imagePath) {
            if (imagePath != null) {
                player.setHeadshotPath(imagePath);
                adapter.notifyDataSetChanged();
            }
        }
    }
}