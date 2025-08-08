package com.example.nhlapp.Objects;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

public class Game implements Serializable {
    public Team homeTeam;
    public Team awayTeam;
    private int [] Score;
    private int [] Shots;
    private String timeStatus;
    private int gameId;
    private String gameDate;
    // This one can be something of an amalgamation
    // I don't know or really care for this part for now but it does come up in the stats
    private String result;

    private String venue;
    private String startTime;
    public int period;
    public int [] timer; // minute seconds
    private String periodDescriptor;
    private ArrayList<NHLPlayer> homePlayerStats;
    private ArrayList<NHLPlayer> awayPlayerStats;

    private ArrayList<Play> playByPlay; // This one is a finicky one that I don't know how to deal with yet
    private ArrayList<Goal> goals;
    private ArrayList<Penalty> penalties;
    public LocalDateTime lastUpdated;


    public Game() {
        this.period = -1;
        this.homeTeam = null;
        this.awayTeam = null;
        this.Score = new int[2];
        this.Score[0] = -1;
        this.Score[1] = -1;
        this.Shots = new int[2];
        this.Shots[0] = -1;
        this.Shots[1] = -1;
        this.homePlayerStats = new ArrayList<>();
        this.awayPlayerStats = new ArrayList<>();
        this.playByPlay = new ArrayList<>();
        this.penalties = new ArrayList<>();
        this.goals = new ArrayList<>();
        this.timer = new int [2];
        this.timer[0] = -1;
        this.timer[1] = -1;
        this.gameId = -1;
        this.timeStatus = null;
        this.gameDate = null;
        this.result = null;
        this.venue = null;
        this.startTime = null;
    }

    public Game(Team awayTeam, Team homeTeam, String timeStatus) {
        this.awayTeam = awayTeam;
        this.homeTeam = homeTeam;
        this.Score = new int[2];
        this.Score[0] = -1;
        this.Score[1] = -1;
        this.Shots = new int[2];
        this.Shots[0] = -1;
        this.Shots[1] = -1;
        this.timeStatus = timeStatus;
        this.gameId = -1;
        this.timer = new int [2];
        this.timer[0] = -1;
        this.timer[1] = -1;
        this.period = -1;
        this.gameDate = null;
        this.result = null;
        this.venue = null;
        this.startTime = null;
    }

    // Method to check the home and away by ID, just for verification usage
    public ArrayList<NHLPlayer> getPlayerStatsByTeam(int teamID) {
        // I don't know a good other check to skirt around the type checking
        // revisit this later
        if (this.awayTeam != null) {
            // Need seperation because not sure how the compiler treats if statements
            if (OptionalInt.of(this.awayTeam.getTeamID()).isPresent()) {
                if (teamID == this.awayTeam.getTeamID()){
                    return this.getAwayPlayerStats();
                }
            }
        }

        if (this.homeTeam != null) {
            // Need seperation because not sure how the compiler treats if statements
            if (OptionalInt.of(this.homeTeam.getTeamID()).isPresent()) {
                if (teamID == this.homeTeam.getTeamID()){
                    return this.getHomePlayerStats();
                }
            }
        }
        return new ArrayList<>();
    }

    // Supportive call here, I might delete this later but this is useful as it is for getting
    // Relevant data for this
//    public GameStats getGameStats() {
//        return new GameStats(this.period, this.playByPlay);
//    }

    // Getters and setters
    public Team getHomeTeam() { return homeTeam; }
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }

    public Team getAwayTeam() { return awayTeam; }
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }

    public int getHomeScore() { return Score[0]; }
    public void setHomeScore(int homeScore) {
        if(homeScore >= 0 || this.Score[0] == -1)
            this.Score[0] = homeScore; }

    public int getAwayScore() { return Score[1]; }
    public void setAwayScore(int awayScore) {
        if(awayScore >= 0 || this.Score[1] == -1)
            this.Score[1] = awayScore; }

    public String getTimeStatus() { return timeStatus; }
    public void setTimeStatus(String timeStatus) { this.timeStatus = timeStatus; }

    public int getGameId() { return gameId; }

    public int getHomeShots() { return Shots[0]; }
    public void setHomeShots(int homeScore) {
        if(homeScore >= 0 || this.Shots[0] == -1)
            this.Shots[0] = homeScore; }

    public int getAwayShots() { return Shots[1]; }
    public void setAwayShots(int awayScore) {
        if(awayScore >= 0 || this.Shots[1] == -1)
            this.Shots[1] = awayScore; }

    public int[] getScore() {
        return Score;
    }

    public void setScore(int[] score) {
        Score = score;
    }

    public ArrayList<NHLPlayer> getAwayPlayerStats() {
        return awayPlayerStats;
    }

    public void setAwayPlayerStats(ArrayList<NHLPlayer> awayPlayerStats) {
        this.awayPlayerStats = awayPlayerStats;
    }

    public int[] getShots() {
        return Shots;
    }

    public void setShots(int[] shots) {
        Shots = shots;
    }

    public ArrayList<NHLPlayer> getHomePlayerStats() {
        return homePlayerStats;
    }

    public void setHomePlayerStats(ArrayList<NHLPlayer> homePlayerStats) {
        this.homePlayerStats = homePlayerStats;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getGameDate() {
        return gameDate;
    }

    public void setGameDate(String gameDate) {
        this.gameDate = gameDate;
    }

    public void setGameId(int gameId) {
        if(gameId >= 0 || this.gameId == -1)
            this.gameId = gameId;
    }

    public int getHomeTeamId(){
        if(this.homeTeam == null)
            this.homeTeam = new Team();
        return this.homeTeam.getTeamID();
    }

    public void setHomeTeamId(int teamId){
        if(this.homeTeam == null)
            this.homeTeam = new Team();
        this.homeTeam.setTeamID(teamId);
    }

    public int getAwayTeamId(){
        if(this.awayTeam == null)
            this.awayTeam = new Team();
        return this.awayTeam.getTeamID();
    }

    public void setAwayTeamId(int teamId){
        if(this.awayTeam == null)
            this.awayTeam = new Team();
        this.awayTeam.setTeamID(teamId);
    }

    public void setHomeTeamName(String teamName){
        if(this.homeTeam == null)
            this.homeTeam = new Team();
        this.homeTeam.setName(teamName);
    }

    public String getHomeTeamName(){
        if(this.homeTeam == null)
            this.homeTeam = new Team();
        return this.homeTeam.getName();
    }

    public String getAwayTeamName(){
        if(this.awayTeam == null)
            this.awayTeam = new Team();
        return this.awayTeam.getName();
    }

    public void setAwayTeamName(String teamName){
        if(this.awayTeam == null)
            this.awayTeam = new Team();
        this.awayTeam.setName(teamName);
    }

    public String getPeriodDescriptor() {
        return periodDescriptor;
    }

    public void setPeriodDescriptor(String periodDescriptor) {
        this.periodDescriptor = periodDescriptor;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        if(period >= 0 || this.period == -1)
            this.period = period;
    }

    public ArrayList<Play> getPlayByPlay() {
        return playByPlay;
    }

    public void setPlayByPlay(ArrayList<Play> playByPlay) {
        this.playByPlay = playByPlay;
    }

    public void addToPlayByPlay(Play entryPlay){
        this.playByPlay.add(entryPlay);
    }

    public ArrayList<Goal> getGoals() {
        return goals;
    }

    public void setGoals(ArrayList<Goal> goals) {
        this.goals = goals;
    }

    public void addToGoals(Goal entryGoal){
        this.goals.add(entryGoal);
    }

    public ArrayList<Penalty> getPenalties() {
        return penalties;
    }

    public void setPenalties(ArrayList<Penalty> penalties) {
        this.penalties = penalties;
    }

    public void addToPenalties(Penalty entryPenalty){
        this.penalties.add(entryPenalty);
    }



    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void combineGame(Game newGame) {
        // Only combine if it's the same game (same ID) or if IDs are not set
        if (newGame == null) return;

        if(newGame.getGameId() != this.getGameId()) return;

        if (newGame.getLastUpdated() != null && (this.lastUpdated == null || newGame.getLastUpdated().isAfter(this.lastUpdated))) {

            // Update primitive fields with newer values
            this.gameDate = newGame.gameDate;
            this.startTime = newGame.startTime;
            this.venue = newGame.venue;
            this.periodDescriptor = newGame.periodDescriptor;
            this.result = newGame.result;
            this.timeStatus = newGame.timeStatus;
            this.period = newGame.period;

            // Update string fields if they're not null/empty in the newer version

            // Update teamID if it's set in the newer version
            if (newGame.getGameId() != 0) {
                this.gameId = newGame.getGameId();
            }

            // Update array if it has meaningful data
            if (newGame.Score != null && newGame.Score.length > 0) {
                boolean hasData = false;
                for (int value : newGame.Score) {
                    if (value != 0) {
                        hasData = true;
                        break;
                    }
                }
                if (hasData) {
                    this.Score = newGame.Score.clone();
                }
            }

            if (newGame.Shots != null && newGame.Shots.length > 0) {
                boolean hasData = false;
                for (int value : newGame.Shots) {
                    if (value != 0) {
                        hasData = true;
                        break;
                    }
                }
                if (hasData) {
                    this.Shots = newGame.Shots.clone();
                }
            }

            if (newGame.timer != null && newGame.timer.length > 0) {
                boolean hasData = false;
                for (int value : newGame.timer) {
                    if (value != 0) {
                        hasData = true;
                        break;
                    }
                }
                if (hasData) {
                    this.timer = newGame.timer.clone();
                }
            }

            // Merge lists - append new items that don't already exist
            if (newGame.homePlayerStats != null && !newGame.homePlayerStats.isEmpty()) {
                this.homePlayerStats = new ArrayList<>();
                for (NHLPlayer record : newGame.homePlayerStats) {
                    this.homePlayerStats.add(record);
                }
            }

            if (newGame.awayPlayerStats != null && !newGame.awayPlayerStats.isEmpty()) {
                this.awayPlayerStats = new ArrayList<>();
                for (NHLPlayer record : newGame.awayPlayerStats) {
                    this.awayPlayerStats.add(record);
                }
            }

            if (newGame.playByPlay != null && !newGame.playByPlay.isEmpty()) {
                this.playByPlay = new ArrayList<>();
                for (Play record : newGame.playByPlay) {
                    this.playByPlay.add(record);
                }
            }

            if (newGame.goals != null && !newGame.goals.isEmpty()) {
                this.goals = new ArrayList<>();
                for (Goal record : newGame.goals) {
                    this.goals.add(record);
                }
            }

            if (newGame.penalties != null && !newGame.penalties.isEmpty()) {
                this.penalties = new ArrayList<>();
                for (Penalty record : newGame.penalties) {
                    this.penalties.add(record);
                }
            }
//            if(newGame.homeTeam != null && newGame.homeTeam.getLastUpdated().isAfter(this.homeTeam.getLastUpdated())) {
//            	this.homeTeam.combineTeam(newGame.homeTeam);
//            }
            if(newGame.homeTeam != null ) {

                if(this.homeTeam == null) {
                    this.homeTeam = newGame.homeTeam;
                }
                else if (newGame.homeTeam.getLastUpdated().isAfter(this.homeTeam.getLastUpdated())) {
                    this.homeTeam.combineTeam(newGame.homeTeam);
                }
            }

            if(newGame.awayTeam != null ) {
                if(this.awayTeam == null) {
                    this.awayTeam = newGame.awayTeam;
                }
                else if (newGame.awayTeam.getLastUpdated().isAfter(this.awayTeam.getLastUpdated())) {
                    this.awayTeam.combineTeam(newGame.awayTeam);
                }
            }


            // Update timestamp
            this.lastUpdated = newGame.getLastUpdated();

        } else if (newGame.getLastUpdated() != null && this.lastUpdated != null && newGame.getLastUpdated().equals(this.lastUpdated)) {


            if (this.timeStatus == null || this.timeStatus.trim().isEmpty()) {
                if (newGame.timeStatus != null && !newGame.timeStatus.trim().isEmpty()) {
                    this.timeStatus = newGame.timeStatus;
                }
            }
            if (this.gameDate == null || this.gameDate.trim().isEmpty()) {
                if (newGame.gameDate != null && !newGame.gameDate.trim().isEmpty()) {
                    this.gameDate = newGame.gameDate;
                }
            }
            if (this.result == null || this.result.trim().isEmpty()) {
                if (newGame.result != null && !newGame.result.trim().isEmpty()) {
                    this.result = newGame.result;
                }
            }
            if (this.venue == null || this.venue.trim().isEmpty()) {
                if (newGame.venue != null && !newGame.venue.trim().isEmpty()) {
                    this.venue = newGame.venue;
                }
            }
            if (this.startTime == null || this.startTime.trim().isEmpty()) {
                if (newGame.startTime != null && !newGame.startTime.trim().isEmpty()) {
                    this.startTime = newGame.startTime;
                }
            }
            if (this.periodDescriptor == null || this.periodDescriptor.trim().isEmpty()) {
                if (newGame.periodDescriptor != null && !newGame.periodDescriptor.trim().isEmpty()) {
                    this.periodDescriptor = newGame.periodDescriptor;
                }
            }

            // Merging ints
            if (this.period < 0 && newGame.period > 0) {
                this.period = newGame.period;
            }
            // Just going to check all of the int[] values, because less code
            for(int i=0; i<this.Score.length; i++) {
                if(this.Score[i] < 0 && newGame.Score[i] >= 0) {
                    this.Score[i] = newGame.Score[i];
                }
                if(this.Shots[i] < 0 && newGame.Shots[i] >= 0) {
                    this.Shots[i] = newGame.Shots[i];
                }
                if(this.timer[i] < 0 && newGame.timer[i] >= 0) {
                    this.timer[i] = newGame.timer[i];
                }
            }

            // For teams got to let them individually deal with it

            if(newGame.homeTeam != null ) {
                if(this.homeTeam == null) {
                    System.out.println("Game Home team null other " + newGame.homeTeam.teamID);
                    this.homeTeam = newGame.homeTeam;
                }
                else if (newGame.homeTeam.getLastUpdated().isAfter(this.homeTeam.getLastUpdated())) {
                    System.out.println("Game Home team" + this.homeTeam.teamID + " other " + newGame.homeTeam.teamID);
                    this.homeTeam.combineTeam(newGame.homeTeam);
                }
            }

            if(newGame.awayTeam != null ) {
                if(this.awayTeam == null) {
                    this.awayTeam = newGame.awayTeam;
                }
                else if (newGame.awayTeam.getLastUpdated().isAfter(this.awayTeam.getLastUpdated())) {
                    this.awayTeam.combineTeam(newGame.awayTeam);
                }
            }

            // Dealing with the ArrayList's here

            // For player stats I want to avoid duplicates for playerID
            Set<Integer> homeMap = new HashSet<>();
            Set<Integer> awayMap = new HashSet<>();
            for(int i = 0; i<this.homePlayerStats.size(); i++) {
                homeMap.add(this.homePlayerStats.get(i).getPlayerId());
            }

            for(int i = 0; i<this.awayPlayerStats.size(); i++) {
                awayMap.add(this.awayPlayerStats.get(i).getPlayerId());
            }
            if (newGame.homePlayerStats != null && !newGame.homePlayerStats.isEmpty()) {
                for (NHLPlayer record : newGame.homePlayerStats) {
                    if (!this.homePlayerStats.contains(record)) {
                        this.homePlayerStats.add(record);
                    }
                }
            }
            if (newGame.homePlayerStats != null && !newGame.homePlayerStats.isEmpty()) {
                for (NHLPlayer record : newGame.homePlayerStats) {
                    if (!this.homePlayerStats.contains(record)) {
                        this.homePlayerStats.add(record);
                    }
                }
            }

            // ArrayLists:
            Collections.sort(this.playByPlay,
                    Comparator.comparing((Play obj) -> obj.getPeriod())
                            .thenComparing(obj -> obj.getTimeInPeriod()[0])
                            .thenComparing(obj -> obj.getTimeInPeriod()[1])
                            .thenComparing(obj -> obj.getSortOrder())
            );

            Collections.sort(newGame.playByPlay,
                    Comparator.comparing((Play obj) -> obj.getPeriod())
                            .thenComparing(obj -> obj.getTimeInPeriod()[0])
                            .thenComparing(obj -> obj.getTimeInPeriod()[1])
                            .thenComparing(obj -> obj.getSortOrder())
            );

            Collections.sort(this.goals,
                    Comparator.comparing((Goal obj) -> obj.getPeriodScored())
                            .thenComparing(obj -> obj.getTimeOccured()[0])
                            .thenComparing(obj -> obj.getTimeOccured()[1])
                            .thenComparing(obj -> obj.getGoalScorer().getPlayerId())
            );

            Collections.sort(newGame.goals,
                    Comparator.comparing((Goal obj) -> obj.getPeriodScored())
                            .thenComparing(obj -> obj.getTimeOccured()[0])
                            .thenComparing(obj -> obj.getTimeOccured()[1])
                            .thenComparing(obj -> obj.getGoalScorer().getPlayerId())
            );

            Collections.sort(this.penalties,
                    Comparator.comparing((Penalty obj) -> obj.getPeriodOccured())
                            .thenComparing(obj -> obj.getTimeOccured()[0])
                            .thenComparing(obj -> obj.getTimeOccured()[1])
                            .thenComparing(obj -> obj.getCommittedPlayer())
                            .thenComparing(obj -> obj.getDrawnOnPlayer())
            );

            Collections.sort(newGame.penalties,
                    Comparator.comparing((Penalty obj) -> obj.getPeriodOccured())
                            .thenComparing(obj -> obj.getTimeOccured()[0])
                            .thenComparing(obj -> obj.getTimeOccured()[1])
                            .thenComparing(obj -> obj.getCommittedPlayer())
                            .thenComparing(obj -> obj.getDrawnOnPlayer())
            );

            // Merge playByPlay ArrayList
            for (Play newPlay : newGame.playByPlay) {
                boolean found = false;
                for (Play existingPlay : this.playByPlay) {
                    if (existingPlay.getPeriod() == newPlay.getPeriod() &&
                            existingPlay.getTimeInPeriod()[0] == newPlay.getTimeInPeriod()[0] &&
                            existingPlay.getTimeInPeriod()[1] == newPlay.getTimeInPeriod()[1] &&
                            existingPlay.getSortOrder() == newPlay.getSortOrder()) {
                        // Elements match, combine them
                        existingPlay.combinePlay(newPlay);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Element doesn't exist in this.playByPlay, add it
                    this.playByPlay.add(newPlay);
                }
            }

            // Merge goals ArrayList
            for (Goal newGoal : newGame.goals) {
                boolean found = false;
                for (Goal existingGoal : this.goals) {
                    if (existingGoal.getPeriodScored() == newGoal.getPeriodScored() &&
                            existingGoal.getTimeOccured()[0] == newGoal.getTimeOccured()[0] &&
                            existingGoal.getTimeOccured()[1] == newGoal.getTimeOccured()[1] &&
                            existingGoal.getGoalScorer().getPlayerId() == newGoal.getGoalScorer().getPlayerId()) {
                        // Elements match, combine them
                        existingGoal.combineGoal(newGoal);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Element doesn't exist in this.goals, add it
                    this.goals.add(newGoal);
                }
            }

            // Merge penalties ArrayList
            for (Penalty newPenalty : newGame.penalties) {
                boolean found = false;
                for (Penalty existingPenalty : this.penalties) {
                    if (existingPenalty.getPeriodOccured() == newPenalty.getPeriodOccured() &&
                            existingPenalty.getTimeOccured()[0] == newPenalty.getTimeOccured()[0] &&
                            existingPenalty.getTimeOccured()[1] == newPenalty.getTimeOccured()[1] &&
                            existingPenalty.getCommittedPlayer() == newPenalty.getCommittedPlayer() &&
                            existingPenalty.getDrawnOnPlayer() == newPenalty.getDrawnOnPlayer()) {
                        // Elements match, combine them
                        existingPenalty.combinePenalty(newPenalty);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Element doesn't exist in this.penalties, add it
                    this.penalties.add(newPenalty);
                }
            }

            // Re-sort the ArrayLists after merging to maintain order
            Collections.sort(this.playByPlay,
                    Comparator.comparing((Play obj) -> obj.getPeriod())
                            .thenComparing(obj -> obj.getTimeInPeriod()[0])
                            .thenComparing(obj -> obj.getTimeInPeriod()[1])
                            .thenComparing(obj -> obj.getSortOrder())
            );

            Collections.sort(this.goals,
                    Comparator.comparing((Goal obj) -> obj.getPeriodScored())
                            .thenComparing(obj -> obj.getTimeOccured()[0])
                            .thenComparing(obj -> obj.getTimeOccured()[1])
                            .thenComparing(obj -> obj.getGoalScorer().getPlayerId())
            );

            Collections.sort(this.penalties,
                    Comparator.comparing((Penalty obj) -> obj.getPeriodOccured())
                            .thenComparing(obj -> obj.getTimeOccured()[0])
                            .thenComparing(obj -> obj.getTimeOccured()[1])
                            .thenComparing(obj -> obj.getCommittedPlayer())
                            .thenComparing(obj -> obj.getDrawnOnPlayer())
            );

//            private ArrayList<NHLPlayer> homePlayerStats;
//            private ArrayList<NHLPlayer> awayPlayerStats;

            Collections.sort(this.homePlayerStats,
                    Comparator.comparing((NHLPlayer obj) -> obj.getPlayerId())
                            .thenComparing(obj -> obj.isGoalie())
                            .thenComparing(obj -> obj.getJerseyNumber())
                            .thenComparing(obj -> obj.getTeam())
            );

            Collections.sort(newGame.homePlayerStats,
                    Comparator.comparing((NHLPlayer obj) -> obj.getPlayerId())
                            .thenComparing(obj -> obj.isGoalie())
                            .thenComparing(obj -> obj.getJerseyNumber())
                            .thenComparing(obj -> obj.getTeam())
            );

            Collections.sort(this.awayPlayerStats,
                    Comparator.comparing((NHLPlayer obj) -> obj.getPlayerId())
                            .thenComparing(obj -> obj.isGoalie())
                            .thenComparing(obj -> obj.getJerseyNumber())
                            .thenComparing(obj -> obj.getTeam())
            );

            Collections.sort(newGame.awayPlayerStats,
                    Comparator.comparing((NHLPlayer obj) -> obj.getPlayerId())
                            .thenComparing(obj -> obj.isGoalie())
                            .thenComparing(obj -> obj.getJerseyNumber())
                            .thenComparing(obj -> obj.getTeam())
            );


            // Merge playByPlay ArrayList
            for (NHLPlayer newPlayerStat : newGame.homePlayerStats) {
                boolean found = false;
                for (NHLPlayer existingPlayer : this.homePlayerStats) {
                    if (existingPlayer.getPlayerId() == newPlayerStat.getPlayerId() &&
                            existingPlayer.isGoalie() == newPlayerStat.isGoalie() &&
                            existingPlayer.getJerseyNumber() == newPlayerStat.getJerseyNumber() &&
                            existingPlayer.getTeam() == newPlayerStat.getTeam()) {
                        // Elements match, combine them
                        existingPlayer.combinePlayer(newPlayerStat);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Element doesn't exist in this.playByPlay, add it
                    this.homePlayerStats.add(newPlayerStat);
                }
            }

            for (NHLPlayer newPlayerStat : newGame.awayPlayerStats) {
                boolean found = false;
                for (NHLPlayer existingPlayer : this.awayPlayerStats) {
                    if (existingPlayer.getPlayerId() == newPlayerStat.getPlayerId() &&
                            existingPlayer.isGoalie() == newPlayerStat.isGoalie() &&
                            existingPlayer.getJerseyNumber() == newPlayerStat.getJerseyNumber() &&
                            existingPlayer.getTeam() == newPlayerStat.getTeam()) {
                        // Elements match, combine them
                        existingPlayer.combinePlayer(newPlayerStat);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Element doesn't exist in this.playByPlay, add it
                    this.awayPlayerStats.add(newPlayerStat);
                }
            }
//            this.homePlayerStats.combinePlayer(newGame.homePlayerStats);
//            this.awayPlayerStats.combinePlayer(newGame.awayPlayerStats);


        }


    }
}