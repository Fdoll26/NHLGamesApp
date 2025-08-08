package com.example.nhlapp.Objects;

import com.example.nhlapp.Objects.NHLPlayer;

import java.time.LocalDateTime;

public class Goal {
    public NHLPlayer goalScorer;
    public NHLPlayer primaryAssist;
    public NHLPlayer secondaryAssist;
    public int advantage; // this can be +/- depending on short handed or not
    public String sharingLink;
    public int[] timeOccured;
    public int periodScored;
    public LocalDateTime lastUpdated;

    public Goal(int periodScored, int[] timeOccured, String sharingLink) {
        this.periodScored = periodScored;
        this.timeOccured = timeOccured;
        this.sharingLink = sharingLink;
    }

    public void addGoalScorer(String firstName, String lastName, int playerID, int goalsToDate){
        NHLPlayer newPlayer = new NHLPlayer();
        newPlayer.setFirstName(firstName);
        newPlayer.setLastName(lastName);
        newPlayer.setName(firstName + " " + lastName);
        newPlayer.setPlayerId(playerID);
        newPlayer.setGoals(goalsToDate);
        this.goalScorer = newPlayer;
    }

    public void addAssist(String firstName, String lastName, int playerID, int assistsToDate, boolean isPrimary){
        NHLPlayer newPlayer = new NHLPlayer();
        newPlayer.setFirstName(firstName);
        newPlayer.setLastName(lastName);
        newPlayer.setName(firstName + " " + lastName);
        newPlayer.setPlayerId(playerID);
        newPlayer.setAssists(assistsToDate);
        if (isPrimary){
            this.primaryAssist = newPlayer;
        } else{
            this.secondaryAssist = newPlayer;
        }
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public NHLPlayer getGoalScorer() {
        return goalScorer;
    }

    public void setGoalScorer(NHLPlayer goalScorer) {
        this.goalScorer = goalScorer;
    }

    public NHLPlayer getSecondaryAssist() {
        return secondaryAssist;
    }

    public void setSecondaryAssist(NHLPlayer secondaryAssist) {
        this.secondaryAssist = secondaryAssist;
    }

    public int[] getTimeOccured() {
        return timeOccured;
    }

    public void setTimeOccured(int[] timeOccured) {
        this.timeOccured = timeOccured;
    }

    public int getPeriodScored() {
        return periodScored;
    }

    public void setPeriodScored(int periodScored) {
        this.periodScored = periodScored;
    }


    public int getAdvantage() {
        return advantage;
    }

    public void setAdvantage(int advantage) {
        this.advantage = advantage;
    }

    public void combineGoal(Goal newGoal) {
        // Only combine if it's the same game (same ID) or if IDs are not set
        if (newGoal == null) return;

        if (newGoal.getLastUpdated() != null && (this.lastUpdated == null || newGoal.getLastUpdated().isAfter(this.lastUpdated))) {
//		    public NHLPlayer goalScorer;
//		    public NHLPlayer primaryAssist;
//		    public NHLPlayer secondaryAssist;
//		    public LocalDateTime lastUpdated;
            if (this.sharingLink == null || this.sharingLink.isEmpty()) {
                this.sharingLink = newGoal.sharingLink;
            }

            this.advantage = newGoal.getAdvantage();
            this.periodScored = newGoal.getPeriodScored();

            // Update array if it has meaningful data
            boolean hasData = false;
            for (int value : newGoal.timeOccured) {
                if (value != 0) {
                    hasData = true;
                    break;
                }
            }
            if (hasData) {
                this.timeOccured = newGoal.timeOccured.clone();
            }

            this.goalScorer = newGoal.goalScorer;
            this.primaryAssist = newGoal.primaryAssist;
            this.secondaryAssist = newGoal.secondaryAssist;

            this.lastUpdated = newGoal.lastUpdated;

        } else if (newGoal.getLastUpdated() != null && this.lastUpdated != null && newGoal.getLastUpdated().equals(this.lastUpdated)) {

            if (this.sharingLink == null || this.sharingLink.trim().isEmpty()) {
                if (newGoal.sharingLink != null && !newGoal.sharingLink.trim().isEmpty()) {
                    this.sharingLink = newGoal.sharingLink;
                }
            }

            // Merging ints
            if (this.periodScored <= 0 && newGoal.periodScored > 0) {
                this.periodScored = newGoal.periodScored;
            }

            for(int i=0; i<this.timeOccured.length; i++) {
                if(this.timeOccured[i] < 0 && newGoal.timeOccured[i] >= 0) {
                    this.timeOccured[i] = newGoal.timeOccured[i];
                }
            }

            this.goalScorer.combinePlayer(newGoal.goalScorer);
            this.primaryAssist.combinePlayer(newGoal.primaryAssist);
            this.secondaryAssist.combinePlayer(newGoal.secondaryAssist);


        }
    }




}
