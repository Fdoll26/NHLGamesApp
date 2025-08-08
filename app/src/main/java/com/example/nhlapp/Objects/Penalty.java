package com.example.nhlapp.Objects;
import java.time.LocalDateTime;

public class Penalty {
    public String committedPlayer;
    public String drawnOnPlayer;
    public String teamAbbrev;
    public int[] timeOccured;
    public int duration;
    public int periodOccured;
    public String typeOfPenalty;
    public String penaltyDescription;
    public LocalDateTime lastUpdated;

    public Penalty (String committedPlayer, String drawnOnPlayer, String teamAbbrev, String typeOfPenalty,
                    String penaltyDescription, int duration, int periodOccured, int[] timeOccured){
        this.committedPlayer = committedPlayer;
        this.drawnOnPlayer = drawnOnPlayer;
        this.teamAbbrev = teamAbbrev;
        this.typeOfPenalty = typeOfPenalty;
        this.penaltyDescription = penaltyDescription;
        this.duration = duration;
        this.periodOccured = periodOccured;
        this.timeOccured = timeOccured;
    }


    public String getCommittedPlayer() {
        return committedPlayer;
    }

    public void setCommittedPlayer(String committedPlayer) {
        this.committedPlayer = committedPlayer;
    }

    public String getDrawnOnPlayer() {
        return drawnOnPlayer;
    }

    public void setDrawnOnPlayer(String drawnOnPlayer) {
        this.drawnOnPlayer = drawnOnPlayer;
    }

    public String getTeamAbbrev() {
        return teamAbbrev;
    }

    public void setTeamAbbrev(String teamAbbrev) {
        this.teamAbbrev = teamAbbrev;
    }

    public int[] getTimeOccured() {
        return timeOccured;
    }

    public void setTimeOccured(int[] timeOccured) {
        this.timeOccured = timeOccured;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getTypeOfPenalty() {
        return typeOfPenalty;
    }

    public void setTypeOfPenalty(String typeOfPenalty) {
        this.typeOfPenalty = typeOfPenalty;
    }

    public int getPeriodOccured() {
        return periodOccured;
    }

    public void setPeriodOccured(int periodOccured) {
        this.periodOccured = periodOccured;
    }

    public String getPenaltyDescription() {
        return penaltyDescription;
    }

    public void setPenaltyDescription(String penaltyDescription) {
        this.penaltyDescription = penaltyDescription;
    }


    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }


    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void combinePenalty(Penalty newPenalty) {
        // Only combine if it's the same game (same ID) or if IDs are not set
        if (newPenalty == null) return;

        if (newPenalty.getLastUpdated() != null && (this.lastUpdated == null || newPenalty.getLastUpdated().isAfter(this.lastUpdated))) {

            // Update string fields if they're not null/empty in the newer version
            if (this.committedPlayer == null || this.committedPlayer.isEmpty()) {
                this.committedPlayer = newPenalty.committedPlayer;
            }
            if (this.drawnOnPlayer == null || this.drawnOnPlayer.isEmpty()) {
                this.drawnOnPlayer = newPenalty.drawnOnPlayer;
            }
            if (this.teamAbbrev == null || this.teamAbbrev.isEmpty()) {
                this.teamAbbrev = newPenalty.teamAbbrev;
            }
            if (this.typeOfPenalty == null || this.typeOfPenalty.isEmpty()) {
                this.typeOfPenalty = newPenalty.typeOfPenalty;
            }
            if (this.penaltyDescription == null || this.penaltyDescription.isEmpty()) {
                this.penaltyDescription = newPenalty.penaltyDescription;
            }

            if (newPenalty.getDuration() != 0) {
                this.duration = newPenalty.getDuration();
            }
            if (newPenalty.getPeriodOccured() != 0) {
                this.duration = newPenalty.getPeriodOccured();
            }

            // Update array if it has meaningful data
            if (newPenalty.timeOccured != null && newPenalty.timeOccured.length > 0) {
                boolean hasData = false;
                for (int value : newPenalty.timeOccured) {
                    if (value != 0) {
                        hasData = true;
                        break;
                    }
                }
                if (hasData) {
                    this.timeOccured = newPenalty.timeOccured.clone();
                }
            }

        } else if (newPenalty.getLastUpdated() != null && this.lastUpdated != null && newPenalty.getLastUpdated().equals(this.lastUpdated)) {


            if (this.committedPlayer == null || this.committedPlayer.trim().isEmpty()) {
                if (newPenalty.committedPlayer != null && !newPenalty.committedPlayer.trim().isEmpty()) {
                    this.committedPlayer = newPenalty.committedPlayer;
                }
            }
            if (this.drawnOnPlayer == null || this.drawnOnPlayer.trim().isEmpty()) {
                if (newPenalty.drawnOnPlayer != null && !newPenalty.drawnOnPlayer.trim().isEmpty()) {
                    this.drawnOnPlayer = newPenalty.drawnOnPlayer;
                }
            }
            if (this.teamAbbrev == null || this.teamAbbrev.trim().isEmpty()) {
                if (newPenalty.teamAbbrev != null && !newPenalty.teamAbbrev.trim().isEmpty()) {
                    this.teamAbbrev = newPenalty.teamAbbrev;
                }
            }
            if (this.typeOfPenalty == null || this.typeOfPenalty.trim().isEmpty()) {
                if (newPenalty.typeOfPenalty != null && !newPenalty.typeOfPenalty.trim().isEmpty()) {
                    this.typeOfPenalty = newPenalty.typeOfPenalty;
                }
            }
            if (this.penaltyDescription == null || this.penaltyDescription.trim().isEmpty()) {
                if (newPenalty.penaltyDescription != null && !newPenalty.penaltyDescription.trim().isEmpty()) {
                    this.penaltyDescription = newPenalty.penaltyDescription;
                }
            }

            // Merging ints
            if (this.duration < 0 && newPenalty.duration > 0) {
                this.duration = newPenalty.duration;
            }
            if (this.periodOccured < 0 && newPenalty.periodOccured > 0) {
                this.periodOccured = newPenalty.periodOccured;
            }
            // Just going to check all of the int[] values, because less code
            for(int i=0; i<this.timeOccured.length; i++) {
                if(this.timeOccured[i] < 0 && newPenalty.timeOccured[i] >= 0) {
                    this.timeOccured[i] = newPenalty.timeOccured[i];
                }
            }


            if(newPenalty.lastUpdated.isAfter(this.lastUpdated)) {
                this.lastUpdated = newPenalty.lastUpdated;
            }


        }
    }


}
