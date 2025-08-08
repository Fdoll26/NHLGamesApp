package com.example.nhlapp.Objects;
//package code;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// This is to showcase a play by play due to the complexity of the play by play, need to expand on it and likely
// Have supporting constructors and callers that can be used
public class Play{
    private int eventID;
    //    public int[] periodDescriptor;
    public String periodDescriptor;
    public int[] timeInPeriod;
    public int timeRemaining;
    public int period;
    public String situationCode;
    public String homeTeamDefendingSide;
    public int typeCode; // The fick is this
    public String typeDescKey;
    public String sortOrder; // No clue about this either
    // Details can have a lot of information so later can deal with the different types of displaying info
    private Map<String, String> details; // Details can contain a lot of sub information so I might just make this a dict
    public String detailType;
    public LocalDateTime lastUpdated;



    public Play (){
//        this.periodDescriptor = new int[3]; // Assuming this is always three long, can be wrong
        this.details = new HashMap<>();
        this.timeInPeriod = new int[2];
    }


    public void addToDetails(String detailKey, String detailValue) {
        this.details.put(detailKey, detailValue);
    }

//    public void setDetailsFromJsonObject(JSONObject detailsJson) {
//        this.details = new HashMap<>();
//
//        if (detailsJson != null) {
//            for (String key : detailsJson.keySet()) {
//                Object value = detailsJson.get(key);
//                this.details.put(key, String.valueOf(value));
//            }
//        }
//    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    // Convenience methods to get typed values back out
    public String getDetailString(String key) {
        return details.get(key);
    }

    public Integer getDetailInt(String key) {
        String value = details.get(key);
        return value != null ? Integer.valueOf(value) : null;
    }

    public Double getDetailDouble(String key) {
        String value = details.get(key);
        return value != null ? Double.valueOf(value) : null;
    }

    public Boolean getDetailBoolean(String key) {
        String value = details.get(key);
        return value != null ? Boolean.valueOf(value) : null;
    }


    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }


    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }


    public int getEventID() {
        return eventID;
    }


    public void setEventID(int eventID) {
        this.eventID = eventID;
    }


    public String getPeriodDescriptor() {
        return periodDescriptor;
    }


    public void setPeriodDescriptor(String periodDescriptor) {
        this.periodDescriptor = periodDescriptor;
    }


    public int[] getTimeInPeriod() {
        return timeInPeriod;
    }


    public void setTimeInPeriod(int[] timeInPeriod) {
        this.timeInPeriod = timeInPeriod;
    }


    public int getTimeRemaining() {
        return timeRemaining;
    }


    public void setTimeRemaining(int timeRemaining) {
        this.timeRemaining = timeRemaining;
    }


    public int getPeriod() {
        return period;
    }


    public void setPeriod(int period) {
        this.period = period;
    }


    public String getSituationCode() {
        return situationCode;
    }


    public void setSituationCode(String situationCode) {
        this.situationCode = situationCode;
    }


    public String getHomeTeamDefendingSide() {
        return homeTeamDefendingSide;
    }


    public void setHomeTeamDefendingSide(String homeTeamDefendingSide) {
        this.homeTeamDefendingSide = homeTeamDefendingSide;
    }


    public int getTypeCode() {
        return typeCode;
    }


    public void setTypeCode(int typeCode) {
        this.typeCode = typeCode;
    }


    public String getTypeDescKey() {
        return typeDescKey;
    }


    public void setTypeDescKey(String typeDescKey) {
        this.typeDescKey = typeDescKey;
    }


    public String getSortOrder() {
        return sortOrder;
    }


    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }


    public String getDetailType() {
        return detailType;
    }


    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }


    public void combinePlay(Play newPlay) {
        // Only combine if it's the same game (same ID) or if IDs are not set
        if (newPlay == null) return;

        if (newPlay.getLastUpdated() != null && (this.lastUpdated == null || newPlay.getLastUpdated().isAfter(this.lastUpdated))) {

            // Update string fields if they're not null/empty in the newer version
            if (this.situationCode == null || this.situationCode.isEmpty()) {
                this.situationCode = newPlay.situationCode;
            }
            if (this.periodDescriptor == null || this.periodDescriptor.isEmpty()) {
                this.periodDescriptor = newPlay.periodDescriptor;
            }
            if (this.homeTeamDefendingSide == null || this.homeTeamDefendingSide.isEmpty()) {
                this.homeTeamDefendingSide = newPlay.homeTeamDefendingSide;
            }
            if (this.typeDescKey == null || this.typeDescKey.isEmpty()) {
                this.typeDescKey = newPlay.typeDescKey;
            }
            if (this.detailType == null || this.detailType.isEmpty()) {
                this.detailType = newPlay.detailType;
            }

            if (newPlay.getPeriod() > this.period) {
                this.period = newPlay.getPeriod();
            }
            if (newPlay.getTimeRemaining() > this.timeRemaining) {
                this.timeRemaining = newPlay.getTimeRemaining();
            }
            if (newPlay.getTypeCode() > this.typeCode) {
                this.typeCode = newPlay.getTypeCode();
            }
            if (newPlay.getEventID() > this.eventID) {
                this.eventID = newPlay.getEventID();
            }

            // Update array if it has meaningful data
            boolean hasData = false;
            for (int value : newPlay.timeInPeriod) {
                if (value != 0) {
                    hasData = true;
                    break;
                }
            }
            if (hasData) {
                this.timeInPeriod = newPlay.timeInPeriod.clone();
            }

            this.details = newPlay.details;

            this.lastUpdated = newPlay.lastUpdated;

        } else if (newPlay.getLastUpdated() != null && this.lastUpdated != null && newPlay.getLastUpdated().equals(this.lastUpdated)) {
            // Details can have a lot of information so later can deal with the different types of displaying info
//		    private Map<String, String> details; // Details can contain a lot of sub information so I might just make this a dict
//		    public LocalDateTime lastUpdated;

            if (this.periodDescriptor == null || this.periodDescriptor.trim().isEmpty()) {
                if (newPlay.periodDescriptor != null && !newPlay.periodDescriptor.trim().isEmpty()) {
                    this.periodDescriptor = newPlay.periodDescriptor;
                }
            }
            if (this.situationCode == null || this.situationCode.trim().isEmpty()) {
                if (newPlay.situationCode != null && !newPlay.situationCode.trim().isEmpty()) {
                    this.situationCode = newPlay.situationCode;
                }
            }
            if (this.homeTeamDefendingSide == null || this.homeTeamDefendingSide.trim().isEmpty()) {
                if (newPlay.homeTeamDefendingSide != null && !newPlay.homeTeamDefendingSide.trim().isEmpty()) {
                    this.homeTeamDefendingSide = newPlay.homeTeamDefendingSide;
                }
            }
            if (this.typeDescKey == null || this.typeDescKey.trim().isEmpty()) {
                if (newPlay.typeDescKey != null && !newPlay.typeDescKey.trim().isEmpty()) {
                    this.typeDescKey = newPlay.typeDescKey;
                }
            }
            if (this.sortOrder == null || this.sortOrder.trim().isEmpty()) {
                if (newPlay.sortOrder != null && !newPlay.sortOrder.trim().isEmpty()) {
                    this.sortOrder = newPlay.sortOrder;
                }
            }
            if (this.detailType == null || this.detailType.trim().isEmpty()) {
                if (newPlay.detailType != null && !newPlay.detailType.trim().isEmpty()) {
                    this.detailType = newPlay.detailType;
                }
            }

            // Merging ints
            if (this.eventID <= 0 && newPlay.eventID > 0) {
                this.eventID = newPlay.eventID;
            }
            if (this.timeRemaining <= 0 && newPlay.timeRemaining > 0) {
                this.timeRemaining = newPlay.timeRemaining;
            }
            if (this.period <= 0 && newPlay.period > 0) {
                this.period = newPlay.period;
            }
            if (this.typeCode <= 0 && newPlay.typeCode > 0) {
                this.typeCode = newPlay.typeCode;
            }

            for(int i=0; i<this.timeInPeriod.length; i++) {
                if(this.timeInPeriod[i] < 0 && newPlay.timeInPeriod[i] >= 0) {
                    this.timeInPeriod[i] = newPlay.timeInPeriod[i];
                }
            }

            if(newPlay.lastUpdated.isAfter(this.lastUpdated)) {
                this.lastUpdated = newPlay.lastUpdated;
            }

            if (newPlay.details != null) {
                if (this.details == null) {
                    this.details = new HashMap<>();
                }
                for (Map.Entry<String, String> entry : newPlay.details.entrySet()) {
                    if (!this.details.containsKey(entry.getKey()) ||
                            this.details.get(entry.getKey()) == null ||
                            this.details.get(entry.getKey()).trim().isEmpty()) {
                        this.details.put(entry.getKey(), entry.getValue());
                    }
                }
            }


        }
    }



}
