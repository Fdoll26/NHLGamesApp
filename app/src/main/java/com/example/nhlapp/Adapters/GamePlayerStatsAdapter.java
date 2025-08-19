package com.example.nhlapp.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhlapp.Objects.NHLPlayer;
import com.example.nhlapp.Objects.Team;
import com.example.nhlapp.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GamePlayerStatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SKATER = 1;
    private static final int VIEW_TYPE_GOALIE = 2;
    private static final int VIEW_TYPE_SECTION = 3;

    private Context context;
    private List<Object> items; // Mixed list of headers, players, and section dividers
    private Team team;

    // Sort state tracking
    private String currentSortColumn = null;
    private boolean isAscending = true;

    public GamePlayerStatsAdapter(Context context, Team teamData, Team team) {
        this.context = context;
        this.team = team;
        this.items = new ArrayList<>();

        if (teamData != null) {
            setupItems(teamData);
        }
    }

    private void setupItems(Team teamData) {
        items.clear();
        List<NHLPlayer> forwards = new ArrayList<>();
        List<NHLPlayer> defense = new ArrayList<>();
        List<NHLPlayer> goalies = new ArrayList<>();
        // Add forwards section
        for (NHLPlayer player : teamData.getTeamRoster().values()) {
            if (player != null && player.getPosition() != null) {
                String position = player.getPosition().toUpperCase();
                if (position.equals("G")) {
                    goalies.add(player);
                } else if (position.equals("D")) {
                    defense.add(player);
                } else {
                    forwards.add(player);
                }
            }
        }
        if (!forwards.isEmpty()) {
            items.add(new SectionHeader("Forwards"));
            items.add(new SkaterHeader());
            items.addAll(forwards);
        }

        // Add defense section
        if (!defense.isEmpty()) {
            items.add(new SectionHeader("Defense"));
            items.add(new SkaterHeader());
            items.addAll(defense);
        }

        // Add goalies section
        if (!goalies.isEmpty()) {
            items.add(new SectionHeader("Goalies"));
            items.add(new GoalieHeader());
            items.addAll(goalies);
        }
    }

//    @Override
    public int getViewType(int position) {
        Object item = items.get(position);
        if (item instanceof SectionHeader) {
            return VIEW_TYPE_SECTION;
        } else if (item instanceof SkaterHeader || item instanceof GoalieHeader) {
            return VIEW_TYPE_HEADER;
        } else if (item instanceof NHLPlayer) {
            NHLPlayer player = (NHLPlayer) item;
            // Determine if goalie based on position or if they have goalie stats
            if ("G".equals(player.getPosition()) || player.getShotsAgainst() > 0) {
                return VIEW_TYPE_GOALIE;
            } else {
                return VIEW_TYPE_SKATER;
            }
        }
        return VIEW_TYPE_SKATER;
    }

    @Override
    public int getItemViewType(int position) {
        return getViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        switch (viewType) {
            case VIEW_TYPE_SECTION:
                return new SectionViewHolder(inflater.inflate(R.layout.item_section_header, parent, false));
            case VIEW_TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.item_player_stats_header, parent, false));
            case VIEW_TYPE_GOALIE:
                return new GoalieViewHolder(inflater.inflate(R.layout.item_goalie_stats, parent, false));
            case VIEW_TYPE_SKATER:
            default:
                return new SkaterViewHolder(inflater.inflate(R.layout.item_skater_stats, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_SECTION:
                SectionViewHolder sectionHolder = (SectionViewHolder) holder;
                SectionHeader sectionHeader = (SectionHeader) item;
                sectionHolder.sectionTitle.setText(sectionHeader.title);
                break;

            case VIEW_TYPE_HEADER:
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
                if (item instanceof SkaterHeader) {
                    setupSkaterHeader(headerHolder);
                } else if (item instanceof GoalieHeader) {
                    setupGoalieHeader(headerHolder);
                }
                break;

            case VIEW_TYPE_SKATER:
                SkaterViewHolder skaterHolder = (SkaterViewHolder) holder;
                bindSkaterStats(skaterHolder, (NHLPlayer) item);
                break;

            case VIEW_TYPE_GOALIE:
                GoalieViewHolder goalieHolder = (GoalieViewHolder) holder;
                bindGoalieStats(goalieHolder, (NHLPlayer) item);
                break;
        }
    }

    private void setupSkaterHeader(HeaderViewHolder holder) {
        holder.setupSkaterHeaders();
        holder.setClickListeners(new HeaderClickListener() {
            @Override
            public void onHeaderClick(String column) {
                sortByColumn(column);
            }
        });
    }

    private void setupGoalieHeader(HeaderViewHolder holder) {
        holder.setupGoalieHeaders();
        holder.setClickListeners(new HeaderClickListener() {
            @Override
            public void onHeaderClick(String column) {
                sortByColumn(column);
            }
        });
    }

    private void bindSkaterStats(SkaterViewHolder holder, NHLPlayer player) {
        // Load player photo (but don't save to storage as requested)
        loadPlayerPhoto(holder.playerPhoto, player.getPlayerId(), getTeamAbbreviation());

        holder.playerName.setText(player.getName());
        holder.jersey.setText(String.valueOf(player.getJerseyNumber()));
        holder.position.setText(player.getPosition());
        holder.toi.setText(String.format(Locale.US, "%.3f", player.getTimeOnIce()));
        holder.toi.setText(formatTimeOnIce(player.getTimeOnIce()));
        holder.goals.setText(String.valueOf(player.getGoals()));
        holder.assists.setText(String.valueOf(player.getAssists()));
        holder.points.setText(String.valueOf(player.getPoints()));
        holder.shots.setText(String.valueOf(player.getShotsOnGoal()));
        holder.hits.setText(String.valueOf(player.getHits()));
        holder.blockedShots.setText(String.valueOf(player.getBlocks()));
        holder.pim.setText(String.valueOf(player.getPenaltyMinutes()));
    }

    // Helper method to format seconds back to MM:SS for display
    @SuppressLint("DefaultLocale")
    public static String formatTimeOnIce(float totalSeconds) {
        if (totalSeconds <= 0) {
            return "0:00";
        }

        int hours = (int) (totalSeconds / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private void bindGoalieStats(GoalieViewHolder holder, NHLPlayer player) {
        // Load player photo (but don't save to storage as requested)
        loadPlayerPhoto(holder.playerPhoto, player.getPlayerId(), getTeamAbbreviation());

        holder.playerName.setText(player.getName());
        holder.jersey.setText(String.valueOf(player.getJerseyNumber()));
//        holder.toi.setText(String.format(Locale.US, "%.3f", player.getTimeOnIce()));
        holder.saves.setText(formatTimeOnIce(player.getTimeOnIce()));
        holder.saves.setText(String.valueOf(player.getSaves()));

        holder.shots.setText(String.valueOf(player.getTotalShots()));
        holder.savePctg.setText(String.format(Locale.US, "%.3f", player.getSavePercentage()));
        holder.goalsAgainst.setText(String.valueOf(player.getGoalsAgainst()));
//        holder.decision.setText(player.get());
    }

//    private void loadPlayerPhoto(ImageView imageView, String headshotPath, int playerId) {
//        // Generate NHL headshot URL (don't save to storage as requested)
//        String headshotUrl = String.format("https://assets.nhle.com/mugs/nhl/20232024/%d.png", playerId);
//
////        Picasso.get()
////                .load(headshotUrl)
////                .placeholder(R.drawable.ic_player_placeholder)
////                .error(R.drawable.ic_player_placeholder)
////                .into(imageView);
//        Log.d("GamePlayerStatsAdapter", "Trying to get player headshot of " + headshotPath);
//        try{
//            Picasso.get()
//                    .load(headshotPath)
//                    .placeholder(R.drawable.ic_player_placeholder)
//                    .error(R.drawable.ic_player_placeholder)
//                    .into(imageView);
//        } catch (Exception e) {
//            Picasso.get()
//                .load(headshotUrl)
//                .placeholder(R.drawable.ic_player_placeholder)
//                .error(R.drawable.ic_player_placeholder)
//                .into(imageView);
//        }
//
//    }

    /**
     * Improved player photo loading with multiple fallback URLs
     */
    private void loadPlayerPhoto(ImageView imageView, int playerId, String teamAbbrev) {
        if (playerId <= 0) {
            imageView.setImageResource(R.drawable.ic_player_placeholder);
            return;
        }

        // Try multiple URL patterns in order of preference
        String[] urlPatterns = {
                // Current season headshots (most likely to work)
                "https://assets.nhle.com/mugs/nhl/20242025/" + teamAbbrev + "/" + playerId + ".png",
                "https://cms.nhl.bamgrid.com/images/headshots/current/168x168/" + playerId + ".jpg",

                // Previous season as fallback
                "https://assets.nhle.com/mugs/nhl/20232024/" + playerId + ".png",

                // Alternative NHL API endpoints
                "https://cms.nhl.bamgrid.com/images/headshots/current/60x60/" + playerId + ".jpg",
                "https://www-league.nhlstatic.com/images/headshots/current/168x168/" + playerId + ".jpg",
        };

        tryLoadPlayerPhoto(imageView, urlPatterns, 0, playerId, teamAbbrev);
    }

    /**
     * Recursively try different URL patterns until one works
     */
    private void tryLoadPlayerPhoto(ImageView imageView, String[] urlPatterns, int urlIndex, int playerId, String teamAbbrev) {
        if (urlIndex >= urlPatterns.length) {
            // All URLs failed, show placeholder
            Log.d("GamePlayerStatsAdapter", "All headshot URLs failed for player " + playerId);
            imageView.setImageResource(R.drawable.ic_player_placeholder);
            return;
        }

        String currentUrl = urlPatterns[urlIndex];
        Log.d("GamePlayerStatsAdapter", "Trying headshot URL " + (urlIndex + 1) + "/" + urlPatterns.length + " for player " + playerId + ": " + currentUrl);

        Picasso.get()
                .load(currentUrl)
                .placeholder(R.drawable.ic_player_placeholder)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d("GamePlayerStatsAdapter", "Successfully loaded headshot for player " + playerId + " from URL: " + currentUrl);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.d("GamePlayerStatsAdapter", "Failed to load headshot from URL: " + currentUrl + ", trying next...");
                        // Try the next URL pattern
                        tryLoadPlayerPhoto(imageView, urlPatterns, urlIndex + 1, playerId, teamAbbrev);
                    }
                });
    }

    private void sortByColumn(String column) {
        // Toggle sort direction if same column
        if (column.equals(currentSortColumn)) {
            isAscending = !isAscending;
        } else {
            currentSortColumn = column;
            isAscending = true;
        }

        // Find sections and sort players within each section
        List<Object> newItems = new ArrayList<>();
        List<NHLPlayer> currentPlayers = new ArrayList<>();
        Object currentSection = null;
        Object currentHeader = null;

        for (Object item : items) {
            if (item instanceof SectionHeader || item instanceof SkaterHeader || item instanceof GoalieHeader) {
                // Sort and add previous section if it exists
                if (!currentPlayers.isEmpty()) {
                    sortPlayers(currentPlayers, column);
                    newItems.addAll(currentPlayers);
                    currentPlayers.clear();
                }

                newItems.add(item);
                if (item instanceof SectionHeader) {
                    currentSection = item;
                } else {
                    currentHeader = item;
                }
            } else if (item instanceof NHLPlayer) {
                currentPlayers.add((NHLPlayer) item);
            }
        }

        // Sort and add final section
        if (!currentPlayers.isEmpty()) {
            sortPlayers(currentPlayers, column);
            newItems.addAll(currentPlayers);
        }

        items = newItems;
        notifyDataSetChanged();
    }

    private void sortPlayers(List<NHLPlayer> players, String column) {
        Collections.sort(players, new Comparator<NHLPlayer>() {
            @Override
            public int compare(NHLPlayer p1, NHLPlayer p2) {
                int result = 0;

                switch (column) {
                    case "Name":
                        result = p1.getName().compareToIgnoreCase(p2.getName());
                        break;
                    case "Jersey":
                        result = Integer.compare(p1.getJerseyNumber(), p2.getJerseyNumber());
                        break;
                    case "Position":
                        result = p1.getPosition().compareToIgnoreCase(p2.getPosition());
                        break;
                    case "TOI":
                        result = Float.compare(p1.getTimeOnIce(), p2.getTimeOnIce());
                        break;
                    case "G":
                        result = Integer.compare(p1.getGoals(), p2.getGoals());
                        break;
                    case "A":
                        result = Integer.compare(p1.getAssists(), p2.getAssists());
                        break;
                    case "P":
                        result = Integer.compare(p1.getPoints(), p2.getPoints());
                        break;
                    case "S":
                        result = Integer.compare(p1.getShotsOnGoal(), p2.getShotsOnGoal());
                        break;
                    case "H":
                        result = Integer.compare(p1.getHits(), p2.getHits());
                        break;
                    case "BS":
                        result = Integer.compare(p1.getBlocks(), p2.getBlocks());
                        break;
                    case "PIM":
                        result = Integer.compare(p1.getPenaltyMinutes(), p2.getPenaltyMinutes());
                        break;
                    // Goalie stats
                    case "Saves":
                        result = Integer.compare(p1.getSaves(), p2.getSaves());
                        break;
                    case "Shots":
                        result = Integer.compare(p1.getShotsAgainst(), p2.getShotsAgainst());
                        break;
                    case "Sv%":
                        result = Double.compare(p1.getSavePercentage(), p2.getSavePercentage());
                        break;
                    case "GA":
                        result = Integer.compare(p1.getGoalsAgainst(), p2.getGoalsAgainst());
                        break;
//                    case "Decision":
//                        result = p1.getDecision().compareToIgnoreCase(p2.getDecision());
//                        break;
                    default:
                        result = p1.getName().compareToIgnoreCase(p2.getName());
                        break;
                }

                return isAscending ? result : -result;
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder classes
    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView sectionTitle;

        SectionViewHolder(View itemView) {
            super(itemView);
            sectionTitle = itemView.findViewById(R.id.sectionTitle);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        // Common headers
        TextView nameHeader, jerseyHeader;

        // Skater-specific headers
        TextView positionHeader, toiHeader, goalsHeader, assistsHeader, pointsHeader;
        TextView shotsHeader, hitsHeader, blockedShotsHeader, pimHeader;

        // Goalie-specific headers
        TextView savesHeader, shotsAgainstHeader, savePctgHeader, goalsAgainstHeader, decisionHeader;

        HeaderViewHolder(View itemView) {
            super(itemView);
            // Initialize common headers
            nameHeader = itemView.findViewById(R.id.nameHeader);
            jerseyHeader = itemView.findViewById(R.id.jerseyHeader);

            // Initialize skater headers
            positionHeader = itemView.findViewById(R.id.positionHeader);
            toiHeader = itemView.findViewById(R.id.toiHeader);
            goalsHeader = itemView.findViewById(R.id.goalsHeader);
            assistsHeader = itemView.findViewById(R.id.assistsHeader);
            pointsHeader = itemView.findViewById(R.id.pointsHeader);
            shotsHeader = itemView.findViewById(R.id.shotsHeader);
            hitsHeader = itemView.findViewById(R.id.hitsHeader);
            blockedShotsHeader = itemView.findViewById(R.id.blockedShotsHeader);
            pimHeader = itemView.findViewById(R.id.pimHeader);

            // Initialize goalie headers
            savesHeader = itemView.findViewById(R.id.savesHeader);
            shotsAgainstHeader = itemView.findViewById(R.id.shotsAgainstHeader);
            savePctgHeader = itemView.findViewById(R.id.savePctgHeader);
            goalsAgainstHeader = itemView.findViewById(R.id.goalsAgainstHeader);
            decisionHeader = itemView.findViewById(R.id.decisionHeader);
        }

        void setupSkaterHeaders() {
            // Show skater headers, hide goalie headers
            setVisibility(View.VISIBLE, positionHeader, toiHeader, goalsHeader, assistsHeader,
                    pointsHeader, shotsHeader, hitsHeader, blockedShotsHeader, pimHeader);
            setVisibility(View.GONE, savesHeader, shotsAgainstHeader, savePctgHeader,
                    goalsAgainstHeader, decisionHeader);
        }

        void setupGoalieHeaders() {
            // Show goalie headers, hide skater headers
            setVisibility(View.GONE, positionHeader, goalsHeader, assistsHeader,
                    pointsHeader, shotsHeader, hitsHeader, blockedShotsHeader);
            setVisibility(View.VISIBLE, toiHeader, savesHeader, shotsAgainstHeader,
                    savePctgHeader, goalsAgainstHeader, decisionHeader, pimHeader);
        }

        private void setVisibility(int visibility, TextView... views) {
            for (TextView view : views) {
                if (view != null) {
                    view.setVisibility(visibility);
                }
            }
        }

        void setClickListeners(HeaderClickListener listener) {
            setHeaderClickListener(nameHeader, "Name", listener);
            setHeaderClickListener(jerseyHeader, "Jersey", listener);
            setHeaderClickListener(positionHeader, "Position", listener);
            setHeaderClickListener(toiHeader, "TOI", listener);
            setHeaderClickListener(goalsHeader, "G", listener);
            setHeaderClickListener(assistsHeader, "A", listener);
            setHeaderClickListener(pointsHeader, "P", listener);
            setHeaderClickListener(shotsHeader, "S", listener);
            setHeaderClickListener(hitsHeader, "H", listener);
            setHeaderClickListener(blockedShotsHeader, "BS", listener);
            setHeaderClickListener(pimHeader, "PIM", listener);
            setHeaderClickListener(savesHeader, "Saves", listener);
            setHeaderClickListener(shotsAgainstHeader, "Shots", listener);
            setHeaderClickListener(savePctgHeader, "Sv%", listener);
            setHeaderClickListener(goalsAgainstHeader, "GA", listener);
            setHeaderClickListener(decisionHeader, "Decision", listener);
        }

        private void setHeaderClickListener(TextView header, String column, HeaderClickListener listener) {
            if (header != null) {
                header.setOnClickListener(v -> listener.onHeaderClick(column));
            }
        }
    }

    private String getTeamAbbreviation() {
        if (team != null && team.getAbreviatedName() != null) {
            return team.getAbreviatedName();
        }
        return "NHL";
    }

    static class SkaterViewHolder extends RecyclerView.ViewHolder {
        ImageView playerPhoto;
        TextView playerName, jersey, position, toi, goals, assists, points, shots, hits, blockedShots, pim;

        SkaterViewHolder(View itemView) {
            super(itemView);
            playerPhoto = itemView.findViewById(R.id.playerPhoto);
            playerName = itemView.findViewById(R.id.playerName);
            jersey = itemView.findViewById(R.id.jersey);
            position = itemView.findViewById(R.id.position);
            toi = itemView.findViewById(R.id.toi);
            goals = itemView.findViewById(R.id.goals);
            assists = itemView.findViewById(R.id.assists);
            points = itemView.findViewById(R.id.points);
            shots = itemView.findViewById(R.id.shots);
            hits = itemView.findViewById(R.id.hits);
            blockedShots = itemView.findViewById(R.id.blockedShots);
            pim = itemView.findViewById(R.id.pim);
        }
    }

    static class GoalieViewHolder extends RecyclerView.ViewHolder {
        ImageView playerPhoto;
        TextView playerName, jersey, toi, saves, shots, savePctg, goalsAgainst, decision;

        GoalieViewHolder(View itemView) {
            super(itemView);
            playerPhoto = itemView.findViewById(R.id.playerPhoto);
            playerName = itemView.findViewById(R.id.playerName);
            jersey = itemView.findViewById(R.id.jersey);
            toi = itemView.findViewById(R.id.toi);
            saves = itemView.findViewById(R.id.saves);
            shots = itemView.findViewById(R.id.shots);
            savePctg = itemView.findViewById(R.id.savePctg);
            goalsAgainst = itemView.findViewById(R.id.goalsAgainst);
            decision = itemView.findViewById(R.id.decision);
        }
    }

    // Helper classes
    static class SectionHeader {
        String title;
        SectionHeader(String title) { this.title = title; }
    }

    static class SkaterHeader { }
    static class GoalieHeader { }

    interface HeaderClickListener {
        void onHeaderClick(String column);
    }
}