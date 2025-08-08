package com.example.nhlapp.Activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Adapters.TeamsAdapter;
import com.example.nhlapp.AppSettings;
import com.example.nhlapp.DataManager;
import com.example.nhlapp.DataCallback;
import com.example.nhlapp.ImageDownloader;
import com.example.nhlapp.JsonHelper;
import com.example.nhlapp.NHLApiClient;
import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.R;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TeamsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TeamsAdapter adapter;
    private EditText searchEditText;
    private List<Team> teams = new ArrayList<>();
    private List<Team> filteredTeams = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams);

        initViews();
        setupRecyclerView();
        setupSearch();
        loadTeams();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewTeams);
        searchEditText = findViewById(R.id.searchEditText);
    }

    private void setupRecyclerView() {
        adapter = new TeamsAdapter(filteredTeams, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTeams(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterTeams(String query) {
        filteredTeams.clear();
        if (query.isEmpty()) {
            filteredTeams.addAll(teams);
        } else {
            for (Team team : teams) {
                if (team.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredTeams.add(team);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadTeams() {
        AppSettings settings = AppSettings.getInstance(this);

        if (settings.useSingleton()) {
            ArrayList<Team> recievedTeams = new ArrayList<>(DataManager.getInstance().getCachedTeams());
            if(!recievedTeams.isEmpty()){
                teams.clear();
                teams.addAll(recievedTeams);
                filteredTeams.clear();
                filteredTeams.addAll(teams);
                adapter.notifyDataSetChanged();
            }

        } else {
            // Load from JSON first if enabled
            if (settings.isJsonSavingEnabled()) {
                loadTeamsFromJson();
            }

            // Then fetch from API if online
            if (settings.isOnlineMode()) {
                new FetchTeamsTask().execute();
            }
        }
    }

    private void loadTeamsFromJson() {
        String jsonData = JsonHelper.loadJsonFromFile(this, "teams.json");
        if (jsonData != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                List<Team> loadedTeams = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject teamJson = jsonArray.getJSONObject(i);
                    Team team = new Team();
                    team.setTeamID(teamJson.getInt("id"));
                    team.setName(teamJson.getString("name"));
                    team.setLogoPath(teamJson.optString("logoPath", ""));
                    loadedTeams.add(team);
                }

                teams.clear();
                teams.addAll(loadedTeams);
                filteredTeams.clear();
                filteredTeams.addAll(teams);
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class FetchTeamsTask extends AsyncTask<Void, Void, List<Team>> {
        @Override
        protected List<Team> doInBackground(Void... voids) {
            return NHLApiClient.getTeams();
        }

        @Override
        protected void onPostExecute(List<Team> result) {
            if (result != null) {
                teams.clear();
                teams.addAll(result);
                filteredTeams.clear();
                filteredTeams.addAll(teams);
                adapter.notifyDataSetChanged();

                // Download team logos
                for (Team team : teams) {
                    new DownloadImageTask(team).execute();
                }
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<Void, Void, String> {
        private Team team;

        public DownloadImageTask(Team team) {
            this.team = team;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return ImageDownloader.downloadTeamLogo(TeamsActivity.this, team);
        }

        @Override
        protected void onPostExecute(String imagePath) {
            if (imagePath != null) {
                team.setLogoPath(imagePath);
                adapter.notifyDataSetChanged();
            }
        }
    }
}