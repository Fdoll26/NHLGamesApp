package com.example.nhlapp.Objects;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class NHLPlayer implements Serializable {
    private String name;
    private String team;
    private int jerseyNumber;
    private String position;
    private int penaltyMinutes;

    private int goals, assists, shotsOnGoal, powerplayGoals;
    private int hits, blocks, faceoffsWon, faceoffsLost;
    private int plusMinus;
    private float timeOnIce, powerPlayTimeOnIce, penaltyKillTimeOnIce;
    private double faceoffWinPercentage;
    private int giveaways, takeaways, shifts;


    private boolean isGoalie;
    private int saves, totalShots, shotsAgainst;
    private double savePercentage;
    private int []evenStrengthShotsAgainst, powerPlayShotsAgainst, shorthandedShotsAgainst, saveShotsAgainst;
    private int evenStrengthGoalsAgainst, powerPlayGoalsAgainst, shorthandedGoalsAgainst, goalsAgainst;
    private boolean starter;


    private String firstName;
    private String lastName;
    private int age;
    private String birthDate;
    private String birthCity;
    private String birthCountry;
    private String shootsCatches; // L/R for skaters, L/R for goalies
    private int points; // goals + assists
    private String logoURL; // Yeah idk about this one, iffy on it and how it plays out in the overall aspect
    private String headshotPath;

    // Higher level stats that might not be available

    private int playerId;
    private int teamId;
    public LocalDateTime lastUpdated;

    public NHLPlayer() {}

    public NHLPlayer(String name, String team, String position) {
        this.name = name;
        this.team = team;
        this.position = position;
        this.goals = 0;
        this.assists = 0;
        this.timeOnIce = 0;
        this.powerPlayTimeOnIce = 0;
        this.penaltyMinutes = 0;
        this.penaltyKillTimeOnIce = 0;
        this.shotsOnGoal = 0;
        this.logoURL = "";
        this.firstName = "";
        this.lastName = "";
        this.birthDate = "";
        this.birthCity = "";
        this.birthCountry = "";

        if (Objects.equals(position, "G")){
            this.evenStrengthShotsAgainst = new int[2];
            this.powerPlayShotsAgainst = new int[2];
            this.shorthandedShotsAgainst = new int[2];
            this.saveShotsAgainst = new int[2];
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public int getGoals() {
        return goals;
    }

    public void setGoals(int goals) {
        this.goals = goals;
    }

    public int getPenaltyMinutes() {
        return penaltyMinutes;
    }

    public void setPenaltyMinutes(int penaltyMinutes) {
        this.penaltyMinutes = penaltyMinutes;
    }

    public int getShotsOnGoal() {
        return shotsOnGoal;
    }

    public void setShotsOnGoal(int shotsOnGoal) {
        this.shotsOnGoal = shotsOnGoal;
    }

    public int getHits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public int getBlocks() {
        return blocks;
    }

    public void setBlocks(int blocks) {
        this.blocks = blocks;
    }

    public int getFaceoffsWon() {
        return faceoffsWon;
    }

    public void setFaceoffsWon(int faceoffsWon) {
        this.faceoffsWon = faceoffsWon;
    }

    public int getPlusMinus() {
        return plusMinus;
    }

    public void setPlusMinus(int plusMinus) {
        this.plusMinus = plusMinus;
    }

    public int getFaceoffsLost() {
        return faceoffsLost;
    }

    public void setFaceoffsLost(int faceoffsLost) {
        this.faceoffsLost = faceoffsLost;
    }

    public float getTimeOnIce() {
        return timeOnIce;
    }

    public void setTimeOnIce(float timeOnIce) {
        this.timeOnIce = timeOnIce;
    }

    public float getPowerPlayTimeOnIce() {
        return powerPlayTimeOnIce;
    }

    public String getHeadshotPath() {
        return headshotPath;
    }

    public void setHeadshotPath(String headshotPath) {
        this.headshotPath = headshotPath;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public void setPowerPlayTimeOnIce(float powerPlayTimeOnIce) {
        this.powerPlayTimeOnIce = powerPlayTimeOnIce;
    }

    public float getPenaltyKillTimeOnIce() {
        return penaltyKillTimeOnIce;
    }

    public void setPenaltyKillTimeOnIce(float penaltyKillTimeOnIce) {
        this.penaltyKillTimeOnIce = penaltyKillTimeOnIce;
    }

    public boolean isGoalie() {
        return isGoalie;
    }

    public void setGoalie(boolean goalie) {
        isGoalie = goalie;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public int getSaves() {
        return saves;
    }

    public void setSaves(int saves) {
        this.saves = saves;
    }

    public int getTotalShots() {
        return totalShots;
    }

    public void setTotalShots(int totalShots) {
        this.totalShots = totalShots;
    }

    public double getSavePercentage() {
        return savePercentage;
    }

    public void setSavePercentage(double savePercentage) {
        this.savePercentage = savePercentage;
    }

    public int getJerseyNumber() {
        return jerseyNumber;
    }

    public void setJerseyNumber(int jerseyNumber) {
        this.jerseyNumber = jerseyNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthCity() {
        return birthCity;
    }

    public void setBirthCity(String birthCity) {
        this.birthCity = birthCity;
    }

    public String getBirthCountry() {
        return birthCountry;
    }

    public void setBirthCountry(String birthCountry) {
        this.birthCountry = birthCountry;
    }

    public String getShootsCatches() {
        return shootsCatches;
    }

    public void setShootsCatches(String shootsCatches) {
        this.shootsCatches = shootsCatches;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public double getFaceoffWinPercentage() {
        return faceoffWinPercentage;
    }

    public void setFaceoffWinPercentage(double faceoffWinPercentage) {
        this.faceoffWinPercentage = faceoffWinPercentage;
    }

    public int getPowerplayGoals() {
        return powerplayGoals;
    }

    public void setPowerplayGoals(int powerplayGoals) {
        this.powerplayGoals = powerplayGoals;
    }

    public int getGiveaways() {
        return giveaways;
    }

    public void setGiveaways(int giveaways) {
        this.giveaways = giveaways;
    }

    public int getTakeaways() {
        return takeaways;
    }

    public void setTakeaways(int takeaways) {
        this.takeaways = takeaways;
    }

    public int getShifts() {
        return shifts;
    }

    public void setShifts(int shifts) {
        this.shifts = shifts;
    }

    public int getShotsAgainst() {
        return shotsAgainst;
    }

    public void setShotsAgainst(int shotsAgainst) {
        this.shotsAgainst = shotsAgainst;
    }

    public String getLogoURL() {
        return logoURL;
    }

    public void setLogoURL(String logoURL) {
        this.logoURL = logoURL;
    }

    public int[] getEvenStrengthShotsAgainst() {
        return evenStrengthShotsAgainst;
    }

    public void setEvenStrengthShotsAgainst(int[] evenStrengthShotsAgainst) {
        this.evenStrengthShotsAgainst = evenStrengthShotsAgainst;
    }

    public int[] getPowerPlayShotsAgainst() {
        return powerPlayShotsAgainst;
    }

    public void setPowerPlayShotsAgainst(int[] powerPlayShotsAgainst) {
        this.powerPlayShotsAgainst = powerPlayShotsAgainst;
    }

    public int[] getShorthandedShotsAgainst() {
        return shorthandedShotsAgainst;
    }

    public void setShorthandedShotsAgainst(int[] shorthandedShotsAgainst) {
        this.shorthandedShotsAgainst = shorthandedShotsAgainst;
    }

    public int[] getSaveShotsAgainst() {
        return saveShotsAgainst;
    }

    public void setSaveShotsAgainst(int[] saveShotsAgainst) {
        this.saveShotsAgainst = saveShotsAgainst;
    }

    public int getEvenStrengthGoalsAgainst() {
        return evenStrengthGoalsAgainst;
    }

    public void setEvenStrengthGoalsAgainst(int evenStrengthGoalsAgainst) {
        this.evenStrengthGoalsAgainst = evenStrengthGoalsAgainst;
    }

    public int getShorthandedGoalsAgainst() {
        return shorthandedGoalsAgainst;
    }

    public void setShorthandedGoalsAgainst(int shorthandedGoalsAgainst) {
        this.shorthandedGoalsAgainst = shorthandedGoalsAgainst;
    }

    public int getPowerPlayGoalsAgainst() {
        return powerPlayGoalsAgainst;
    }

    public void setPowerPlayGoalsAgainst(int powerPlayGoalsAgainst) {
        this.powerPlayGoalsAgainst = powerPlayGoalsAgainst;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public void setGoalsAgainst(int goalsAgainst) {
        this.goalsAgainst = goalsAgainst;
    }

    public boolean isStarter() {
        return starter;
    }

    public void setStarter(boolean starter) {
        this.starter = starter;
    }


    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // This is to take in another player that has the same playerID and compare as much as
    // possible and make one better overall player
    // This is to take in another player that has the same playerID and compare as much as
    // possible and make one better overall player
    public void combinePlayer(NHLPlayer newPlayer) {
        // Only combine if it's the same player (same ID) or if IDs are not set
        if (this.playerId != 0 && newPlayer.playerId != 0 && this.playerId != newPlayer.playerId) {
            return; // Don't combine different players
        }

        // Update basic info if not already set
        if (this.name == null || this.name.isEmpty()) {
            this.name = newPlayer.name;
        }
        if (this.firstName == null || this.firstName.isEmpty()) {
            this.firstName = newPlayer.firstName;
        }
        if (this.lastName == null || this.lastName.isEmpty()) {
            this.lastName = newPlayer.lastName;
        }
        if (this.team == null || this.team.isEmpty()) {
            this.team = newPlayer.team;
        }
        if (this.position == null || this.position.isEmpty()) {
            this.position = newPlayer.position;
        }
        if (this.jerseyNumber == 0) {
            this.jerseyNumber = newPlayer.jerseyNumber;
        }
        if (this.birthDate == null || this.birthDate.isEmpty()) {
            this.birthDate = newPlayer.birthDate;
        }
        if (this.birthCity == null || this.birthCity.isEmpty()) {
            this.birthCity = newPlayer.birthCity;
        }
        if (this.birthCountry == null || this.birthCountry.isEmpty()) {
            this.birthCountry = newPlayer.birthCountry;
        }
        if (this.shootsCatches == null || this.shootsCatches.isEmpty()) {
            this.shootsCatches = newPlayer.shootsCatches;
        }
        if (this.age == 0) {
            this.age = newPlayer.age;
        }
        if (this.playerId == 0) {
            this.playerId = newPlayer.playerId;
        }

        // Combine cumulative stats (add them together)
        this.goals = Math.max(this.goals, newPlayer.goals);
        this.assists = Math.max(this.assists ,newPlayer.assists);
        this.penaltyMinutes = Math.max(this.penaltyMinutes, newPlayer.penaltyMinutes);
        this.shotsOnGoal = Math.max(this.shotsOnGoal, newPlayer.shotsOnGoal);
        this.powerplayGoals = Math.max(this.powerplayGoals, newPlayer.powerplayGoals);
        this.hits = Math.max(this.hits, newPlayer.hits);
        this.blocks = Math.max(this.blocks, newPlayer.blocks);
        this.faceoffsWon = Math.max(this.faceoffsWon, newPlayer.faceoffsWon);
        this.faceoffsLost = Math.max(this.faceoffsLost, newPlayer.faceoffsLost);
        this.timeOnIce = Math.max(this.timeOnIce, newPlayer.timeOnIce);
        this.powerPlayTimeOnIce = Math.max(this.powerPlayTimeOnIce, newPlayer.powerPlayTimeOnIce);
        this.penaltyKillTimeOnIce = Math.max(this.penaltyKillTimeOnIce, newPlayer.penaltyKillTimeOnIce);
        this.giveaways = Math.max(this.giveaways, newPlayer.giveaways);
        this.takeaways = Math.max(this.takeaways, newPlayer.takeaways);
        this.shifts = Math.max(this.shifts, newPlayer.shifts);

        // Plus/minus force to be most recent so bleh
        // Most recent update section
        this.plusMinus = newPlayer.plusMinus;
        this.faceoffWinPercentage = newPlayer.faceoffWinPercentage;

        // Recalculate points (goals + assists)
        this.points = this.goals + this.assists;




        // Combine goalie stats if this is a goalie
        if (this.isGoalie || newPlayer.isGoalie) {
            this.isGoalie = true;
            this.saves = Math.max(newPlayer.saves, this.saves);
            this.totalShots = Math.max(newPlayer.totalShots, this.totalShots);
            this.shotsAgainst = Math.max(newPlayer.shotsAgainst, this.shotsAgainst);
            this.evenStrengthGoalsAgainst = Math.max(newPlayer.evenStrengthGoalsAgainst, this.evenStrengthGoalsAgainst);
            this.powerPlayGoalsAgainst = Math.max(newPlayer.powerPlayGoalsAgainst, this.powerPlayGoalsAgainst);
            this.shorthandedGoalsAgainst = Math.max(newPlayer.shorthandedGoalsAgainst, this.shorthandedGoalsAgainst);
            this.goalsAgainst = Math.max(newPlayer.goalsAgainst, this.goalsAgainst);

            // Initialize arrays if they don't exist
            if (this.evenStrengthShotsAgainst == null) {
                this.evenStrengthShotsAgainst = new int[2];
            }
            if (this.powerPlayShotsAgainst == null) {
                this.powerPlayShotsAgainst = new int[2];
            }
            if (this.shorthandedShotsAgainst == null) {
                this.shorthandedShotsAgainst = new int[2];
            }
            if (this.saveShotsAgainst == null) {
                this.saveShotsAgainst = new int[2];
            }

            // Combine array stats if both players have them
            if (newPlayer.evenStrengthShotsAgainst != null) {
                for (int i = 0; i < Math.min(this.evenStrengthShotsAgainst.length, newPlayer.evenStrengthShotsAgainst.length); i++) {
                    this.evenStrengthShotsAgainst[i] += newPlayer.evenStrengthShotsAgainst[i];
                }
            }
            if (newPlayer.powerPlayShotsAgainst != null) {
                for (int i = 0; i < Math.min(this.powerPlayShotsAgainst.length, newPlayer.powerPlayShotsAgainst.length); i++) {
                    this.powerPlayShotsAgainst[i] += newPlayer.powerPlayShotsAgainst[i];
                }
            }
            if (newPlayer.shorthandedShotsAgainst != null) {
                for (int i = 0; i < Math.min(this.shorthandedShotsAgainst.length, newPlayer.shorthandedShotsAgainst.length); i++) {
                    this.shorthandedShotsAgainst[i] += newPlayer.shorthandedShotsAgainst[i];
                }
            }
            if (newPlayer.saveShotsAgainst != null) {
                for (int i = 0; i < Math.min(this.saveShotsAgainst.length, newPlayer.saveShotsAgainst.length); i++) {
                    this.saveShotsAgainst[i] += newPlayer.saveShotsAgainst[i];
                }
            }

            // Recalculate save percentage
            if (this.shotsAgainst > 0) {
                this.savePercentage = (double) this.saves / this.shotsAgainst * 100.0;
            }

            // Keep starter status if either player was a starter
            this.starter = this.starter || newPlayer.starter;
        }
    }
}


