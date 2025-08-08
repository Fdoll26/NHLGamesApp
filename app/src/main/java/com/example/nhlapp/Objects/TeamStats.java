package com.example.nhlapp.Objects;

public class TeamStats {
    private int goals;
    private int assists;
    private int points;
    private int shots;
    private int hits;
    private int blockedShots;
    private int penaltyMinutes;

    public TeamStats() {
        // Initialize all to 0
    }

    public void addGoals(int goals) { this.goals += goals; }
    public void addAssists(int assists) { this.assists += assists; }
    public void addPoints(int points) { this.points += points; }
    public void addShots(int shots) { this.shots += shots; }
    public void addHits(int hits) { this.hits += hits; }
    public void addBlockedShots(int blockedShots) { this.blockedShots += blockedShots; }
    public void addPenaltyMinutes(int penaltyMinutes) { this.penaltyMinutes += penaltyMinutes; }

    // Getters
    public int getGoals() { return goals; }
    public int getAssists() { return assists; }
    public int getPoints() { return points; }
    public int getShots() { return shots; }
    public int getHits() { return hits; }
    public int getBlockedShots() { return blockedShots; }
    public int getPenaltyMinutes() { return penaltyMinutes; }
}