package com.example.nhlapp.Objects;

public class Player {
    private int id;
    private String name;
    private int teamId;
    private String headshotPath;

    // Constructors
    public Player() {}

    public Player(int id, String name, int teamId) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTeamId() { return teamId; }
    public void setTeamId(int teamId) { this.teamId = teamId; }

    public String getHeadshotPath() { return headshotPath; }
    public void setHeadshotPath(String headshotPath) { this.headshotPath = headshotPath; }
}