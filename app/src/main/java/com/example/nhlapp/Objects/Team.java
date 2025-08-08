package com.example.nhlapp.Objects;

import com.example.nhlapp.Objects.NHLPlayer;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//interface Mergeable<T> {
//    void mergeWith(T newTeam);
//    LocalDateTime getLastUpdated();
//    void setLastUpdated(LocalDateTime timestamp);
//}

public class Team {
    public HashMap<Integer, NHLPlayer> teamRoster;
    //    public ArrayList<Integer> playerIDs;
    public int gamesWon;
    public int gamesWonOT;
    public int gamesLost;
    public int gamesLostOT;
    public int goalsFor;
    public int goalsAgainst;
    public String name;
    public String abreviatedName;
    public int teamID;
    public ArrayList<String> gameRecord;
    public int[] summarizedGameRecord; // This one can be win-loss-OT win-OT loss
    public String triCode;
    public String fullName;
    public String logoUrl;
    public String logoPath;
    public int points;
    public int gamesPlayed;
    public double pointsPercentage;
    public int regulationWins;
    public int streakCode; // Current streak
    public String streakCount;
    private LocalDateTime lastUpdated;

    public Team (){
        this.teamRoster = new HashMap<>();
        this.gameRecord = new ArrayList<>();
        this.summarizedGameRecord = new int[4];
        this.gamesWon = -1;
        this.gamesWonOT = -1;
        this.gamesLost = -1;
        this.gamesLostOT = -1;
        this.goalsFor = -1;
        this.goalsAgainst = -1;
        this.teamID = -1;
        this.points = -1;
        this.gamesPlayed = -1;
        this.regulationWins = -1;
        this.streakCode = -1;
    }

    public HashMap<Integer, NHLPlayer> getTeamRoster() {
        return teamRoster;
    }

    public void setTeamRoster(HashMap<Integer, NHLPlayer> teamRoster) {
        this.teamRoster = teamRoster;
    }

    public void addToTeamRoster(NHLPlayer newPlayer) {
        if (this.teamRoster.containsKey(newPlayer.getPlayerId())) {
            this.teamRoster.get(newPlayer.getPlayerId()).combinePlayer(newPlayer);
        } else {
            this.teamRoster.put(newPlayer.getPlayerId(), newPlayer);
        }

    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        if(gamesWon >= 0 || this.gamesWon == -1)
            this.gamesWon = gamesWon;
    }

    public int getGamesWonOT() {
        return gamesWonOT;
    }

    public void setGamesWonOT(int gamesWonOT) {
        if(gamesWonOT >= 0 || this.gamesWonOT == -1)
            this.gamesWonOT = gamesWonOT;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(int gamesLost) {
        if(gamesLost >= 0 || this.gamesLost == -1)
            this.gamesLost = gamesLost;
    }

    public int getGamesLostOT() {
        return gamesLostOT;
    }

    public void setGamesLostOT(int gamesLostOT) {
        if(gamesLostOT >= 0 || this.gamesLostOT == -1)
            this.gamesLostOT = gamesLostOT;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public void setGoalsFor(int goalsFor) {
        if(goalsFor >= 0 || this.goalsFor == -1)
            this.goalsFor = goalsFor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public void setGoalsAgainst(int goalsAgainst) {
        if(goalsAgainst >= 0 || this.goalsAgainst == -1)
            this.goalsAgainst = goalsAgainst;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
//    	if(goalsAgainst >= 0 || this.goalsAgainst == -1)
        this.name = name;
    }

    public void addPlayers(List<NHLPlayer> players){
        for(NHLPlayer player: players){
            if(!this.teamRoster.containsKey(player.getPlayerId())){
                this.teamRoster.put(player.getPlayerId(), player);
            } else{
                this.teamRoster.replace(player.getPlayerId(), player);
            }
        }
    }

    public void addPlayer(NHLPlayer player){
        if(!this.teamRoster.containsKey(player.getPlayerId())){
            this.teamRoster.put(player.getPlayerId(), player);
        } else{
            this.teamRoster.replace(player.getPlayerId(), player);
        }

    }

    public List<NHLPlayer> getDefense(){
        List<NHLPlayer> returnList = new ArrayList<>();
        for(NHLPlayer player: this.teamRoster.values()){
            if(Objects.equals(player.getPosition(), "D")){
                returnList.add(player);
            }
        }
        return returnList;
    }

//    public List<NHLPlayer> getGoalies(){
//        List<NHLPlayer> returnList = new ArrayList<>();
//        for(NHLPlayer player: this.teamRoster.values()){
//            if(Objects.equals(player.getPosition(), "G")){
//                returnList.add(player);
//            }
//        }
//        return returnList;
//    }

    public List<NHLPlayer> getForwards(){
        List<NHLPlayer> returnList = new ArrayList<>();
        for(NHLPlayer player: this.teamRoster.values()){
            if(Objects.equals(player.getPosition(), "C") || Objects.equals(player.getPosition(), "L") || Objects.equals(player.getPosition(), "R") ){
                returnList.add(player);
            }
        }
        return returnList;
    }


    public String getAbreviatedName() {
        return abreviatedName;
    }

    public void setAbreviatedName(String abreviatedName) {
        this.abreviatedName = abreviatedName;
    }

    public int getTeamID() {
        return teamID;
    }

    public void setTeamID(int teamID) {
        this.teamID = teamID;
    }

    public ArrayList<String> getGameRecord() {
        return gameRecord;
    }

    public void setGameRecord(ArrayList<String> gameRecord) {
        this.gameRecord = gameRecord;
    }

    public int[] getSummarizedGameRecord() {
        return summarizedGameRecord;
    }

    public void setSummarizedGameRecord(int[] summarizedGameRecord) {
        this.summarizedGameRecord = summarizedGameRecord;
    }

    public String getTriCode() {
        return triCode;
    }

    public void setTriCode(String triCode) {
        this.triCode = triCode;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStreakCount() {
        return streakCount;
    }

    public void setStreakCount(String streakCount) {
        this.streakCount = streakCount;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public double getPointsPercentage() {
        return pointsPercentage;
    }

    public void setPointsPercentage(double pointsPercentage) {
        this.pointsPercentage = pointsPercentage;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public int getRegulationWins() {
        return regulationWins;
    }

    public void setRegulationWins(int regulationWins) {
        this.regulationWins = regulationWins;
    }

    public int getStreakCode() {
        return streakCode;
    }

    public void setStreakCode(int streakCode) {
        this.streakCode = streakCode;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public ArrayList<Integer> getRosterPlayerIds(){
        if (this.teamRoster.isEmpty())
            return new ArrayList<Integer>();
        return (ArrayList<Integer>) this.teamRoster.keySet();
    }

    public void setRosterPlayerIds(List<Integer> playerIds){
        for(int playerID: playerIds){
            if (!this.teamRoster.containsKey(playerID)){
                this.teamRoster.put(playerID, new NHLPlayer());
            }
        }
    }

    public void combineTeam(Team newTeam) {
        if (newTeam == null) return;

        if(newTeam.getTeamID() != this.getTeamID()) return;

        if (newTeam.getLastUpdated() != null && (this.lastUpdated == null || newTeam.getLastUpdated().isAfter(this.lastUpdated))) {

            // Update primitive fields with newer values
            this.gamesWon = newTeam.gamesWon;
            this.gamesWonOT = newTeam.gamesWonOT;
            this.gamesLost = newTeam.gamesLost;
            this.gamesLostOT = newTeam.gamesLostOT;
            this.goalsFor = newTeam.goalsFor;
            this.goalsAgainst = newTeam.goalsAgainst;
            this.points = newTeam.points;
            this.gamesPlayed = newTeam.gamesPlayed;
            this.pointsPercentage = newTeam.pointsPercentage;
            this.regulationWins = newTeam.regulationWins;
            this.streakCode = newTeam.streakCode;

            // Update string fields if they're not null/empty in the newer version
            if (newTeam.name != null && !newTeam.name.trim().isEmpty()) {
                this.name = newTeam.name;
            }
            if (newTeam.abreviatedName != null && !newTeam.abreviatedName.trim().isEmpty()) {
                this.abreviatedName = newTeam.abreviatedName;
            }
            if (newTeam.triCode != null && !newTeam.triCode.trim().isEmpty()) {
                this.triCode = newTeam.triCode;
            }
            if (newTeam.fullName != null && !newTeam.fullName.trim().isEmpty()) {
                this.fullName = newTeam.fullName;
            }
            if (newTeam.logoUrl != null && !newTeam.logoUrl.trim().isEmpty()) {
                this.logoUrl = newTeam.logoUrl;
            }
            if (newTeam.streakCount != null && !newTeam.streakCount.trim().isEmpty()) {
                this.streakCount = newTeam.streakCount;
            }

            // Update teamID if it's set in the newer version
            if (newTeam.teamID != 0) {
                this.teamID = newTeam.teamID;
            }

            // Update array if it has meaningful data
            if (newTeam.summarizedGameRecord != null && newTeam.summarizedGameRecord.length > 0) {
                boolean hasData = false;
                for (int value : newTeam.summarizedGameRecord) {
                    if (value != 0) {
                        hasData = true;
                        break;
                    }
                }
                if (hasData) {
                    this.summarizedGameRecord = newTeam.summarizedGameRecord.clone();
                }
            }

            // Merge lists - append new items that don't already exist
            if (newTeam.gameRecord != null && !newTeam.gameRecord.isEmpty()) {
                for (String record : newTeam.gameRecord)
                {
                    if (!this.gameRecord.contains(record)) {
                        this.gameRecord.add(record);
                    }
                }
            }

            // Merge team roster
            if (newTeam.teamRoster != null && !newTeam.teamRoster.isEmpty()) {
                for (NHLPlayer player : newTeam.teamRoster.values()) {
                    this.addToTeamRoster(player);
                }
            }

            // Update timestamp
            this.lastUpdated = newTeam.getLastUpdated();

        } else if (newTeam.getLastUpdated() != null &&
                this.lastUpdated != null &&
                newTeam.getLastUpdated().equals(this.lastUpdated)) {

            // Same timestamp - merge non-null/non-default fields and append lists

            // Merge string fields - keep existing if newTeam is null/empty
            if (this.name == null || this.name.trim().isEmpty()) {
                if (newTeam.name != null && !newTeam.name.trim().isEmpty()) {
                    this.name = newTeam.name;
                }
            }
            if (this.abreviatedName == null || this.abreviatedName.trim().isEmpty()) {
                if (newTeam.abreviatedName != null && !newTeam.abreviatedName.trim().isEmpty()) {
                    this.abreviatedName = newTeam.abreviatedName;
                }
            }
            if (this.triCode == null || this.triCode.trim().isEmpty()) {
                if (newTeam.triCode != null && !newTeam.triCode.trim().isEmpty()) {
                    this.triCode = newTeam.triCode;
                }
            }
            if (this.fullName == null || this.fullName.trim().isEmpty()) {
                if (newTeam.fullName != null && !newTeam.fullName.trim().isEmpty()) {
                    this.fullName = newTeam.fullName;
                }
            }
            if (this.logoUrl == null || this.logoUrl.trim().isEmpty()) {
                if (newTeam.logoUrl != null && !newTeam.logoUrl.trim().isEmpty()) {
                    this.logoUrl = newTeam.logoUrl;
                }
            }
            if (this.streakCount == null || this.streakCount.trim().isEmpty()) {
                if (newTeam.streakCount != null && !newTeam.streakCount.trim().isEmpty()) {
                    this.streakCount = newTeam.streakCount;
                }
            }

            // Merge primitive fields - use newTeam's value if this one is default/zero
            if (this.gamesWon == 0 && newTeam.gamesWon != 0) this.gamesWon = newTeam.gamesWon;
            if (this.gamesWonOT == 0 && newTeam.gamesWonOT != 0) this.gamesWonOT = newTeam.gamesWonOT;
            if (this.gamesLost == 0 && newTeam.gamesLost != 0) this.gamesLost = newTeam.gamesLost;
            if (this.gamesLostOT == 0 && newTeam.gamesLostOT != 0) this.gamesLostOT = newTeam.gamesLostOT;
            if (this.goalsFor == 0 && newTeam.goalsFor != 0) this.goalsFor = newTeam.goalsFor;
            if (this.goalsAgainst == 0 && newTeam.goalsAgainst != 0) this.goalsAgainst = newTeam.goalsAgainst;
            if (this.points == 0 && newTeam.points != 0) this.points = newTeam.points;
            if (this.gamesPlayed == 0 && newTeam.gamesPlayed != 0) this.gamesPlayed = newTeam.gamesPlayed;
            if (this.pointsPercentage == 0.0 && newTeam.pointsPercentage != 0.0) this.pointsPercentage = newTeam.pointsPercentage;
            if (this.regulationWins == 0 && newTeam.regulationWins != 0) this.regulationWins = newTeam.regulationWins;
            if (this.streakCode == 0 && newTeam.streakCode != 0) this.streakCode = newTeam.streakCode;
            if (this.teamID == 0 && newTeam.teamID != 0) this.teamID = newTeam.teamID;

            // Merge arrays
            if (this.summarizedGameRecord == null || this.summarizedGameRecord.length == 0) {
                if (newTeam.summarizedGameRecord != null && newTeam.summarizedGameRecord.length > 0) {
                    this.summarizedGameRecord = newTeam.summarizedGameRecord.clone();
                }
            }

            // Append to lists
            if (newTeam.gameRecord != null && !newTeam.gameRecord.isEmpty()) {
                for (String record : newTeam.gameRecord) {
                    if (!this.gameRecord.contains(record)) {
                        this.gameRecord.add(record);
                    }
                }
            }

            // Merge team roster
            if (newTeam.teamRoster != null && !newTeam.teamRoster.isEmpty()) {
                for (NHLPlayer player : newTeam.teamRoster.values()) {
                    this.addToTeamRoster(player);
                }
            }
        }
        // If this object is newer, don't modify anything
    }

    public ArrayList<NHLPlayer> getGoalies(){
        ArrayList<NHLPlayer> returnArray = new ArrayList<>();
        for(NHLPlayer player : this.teamRoster.values()){
            if(player.isGoalie()){
                returnArray.add(player);
            }
        }
        return returnArray;
    }

    public ArrayList<NHLPlayer> getPlayers(){
        ArrayList<NHLPlayer> returnArray = new ArrayList<>();
        for(NHLPlayer player : this.teamRoster.values()){
            if(!player.isGoalie()){
                returnArray.add(player);
            }
        }
        return returnArray;
    }


    public Dictionary<String, Integer> getAllTeamIDs(){
        Dictionary<String, Integer> teamIDTable = new Hashtable<>();
        teamIDTable.put("Quebec Nordiques", 32);
        teamIDTable.put("Montréal Canadiens", 8);
        teamIDTable.put("Toronto St. Patricks", 58);
        teamIDTable.put("Buffalo Sabres", 7);
        teamIDTable.put("Oakland Seals", 46);
        teamIDTable.put("Kansas City Scouts", 48);
        teamIDTable.put("New York Islanders", 2);
        teamIDTable.put("Ottawa Senators (1917)", 36);
        teamIDTable.put("To be determined", 70);
        teamIDTable.put("Atlanta Thrashers", 11);
        teamIDTable.put("St. Louis Eagles", 45);
        teamIDTable.put("Winnipeg Jets (1979)", 33);
        teamIDTable.put("San Jose Sharks", 28);
        teamIDTable.put("Quebec Bulldogs", 42);
        teamIDTable.put("Columbus Blue Jackets", 29);
        teamIDTable.put("Arizona Coyotes", 53);
        teamIDTable.put("Florida Panthers", 13);
        teamIDTable.put("Carolina Hurricanes", 12);
        teamIDTable.put("Vegas Golden Knights", 54);
        teamIDTable.put("Winnipeg Jets", 52);
        teamIDTable.put("Tampa Bay Lightning", 14);
        teamIDTable.put("Nashville Predators", 18);
        teamIDTable.put("Phoenix Coyotes", 27);
        teamIDTable.put("Montreal Wanderers", 41);
        teamIDTable.put("Philadelphia Quakers", 39);
        teamIDTable.put("Hamilton Tigers", 37);
        teamIDTable.put("Detroit Cougars", 40);
        teamIDTable.put("Colorado Rockies", 35);
        teamIDTable.put("Pittsburgh Pirates", 38);
        teamIDTable.put("Hartford Whalers", 34);
        teamIDTable.put("New Jersey Devils", 1);
        teamIDTable.put("Ottawa Senators", 9);
        teamIDTable.put("Colorado Avalanche", 21);
        teamIDTable.put("New York Americans", 44);
        teamIDTable.put("Washington Capitals", 15);
        teamIDTable.put("Minnesota North Stars", 31);
        teamIDTable.put("Los Angeles Kings", 26);
        teamIDTable.put("NHL", 99);
        teamIDTable.put("Minnesota Wild", 30);
        teamIDTable.put("Montreal Maroons", 43);
        teamIDTable.put("Cleveland Barons", 49);
        teamIDTable.put("Brooklyn Americans", 51);
        teamIDTable.put("Detroit Falcons", 50);
        teamIDTable.put("California Golden Seals", 56);
        teamIDTable.put("Toronto Maple Leafs", 10);
        teamIDTable.put("Edmonton Oilers", 22);
        teamIDTable.put("Atlanta Flames", 47);
        teamIDTable.put("Toronto Arenas", 57);
        teamIDTable.put("Calgary Flames", 20);
        teamIDTable.put("St. Louis Blues", 19);
        teamIDTable.put("Detroit Red Wings", 17);
        teamIDTable.put("New York Rangers", 3);
        teamIDTable.put("Utah Hockey Club", 59);
        teamIDTable.put("Utah Mammoth", 68);
        teamIDTable.put("Anaheim Ducks", 24);
        teamIDTable.put("Vancouver Canucks", 23);
        teamIDTable.put("Philadelphia Flyers", 4);
        teamIDTable.put("Chicago Blackhawks", 16);
        teamIDTable.put("Seattle Kraken", 55);
        teamIDTable.put("Pittsburgh Penguins", 5);
        teamIDTable.put("Boston Bruins", 6);
        teamIDTable.put("Dallas Stars", 25);

        return teamIDTable;
    }

    public Dictionary<String, Integer> triCodeGetAllTeamIDs(){
        Dictionary<String, Integer> tricodeTeamTable = new Hashtable<>();
        tricodeTeamTable.put("QUE", 32);
        tricodeTeamTable.put("MTL", 8);
        tricodeTeamTable.put("TSP", 58);
        tricodeTeamTable.put("BUF", 7);
        tricodeTeamTable.put("OAK", 46);
        tricodeTeamTable.put("KCS", 48);
        tricodeTeamTable.put("NYI", 2);
        tricodeTeamTable.put("SEN", 36);
        tricodeTeamTable.put("TBD", 70);
        tricodeTeamTable.put("ATL", 11);
        tricodeTeamTable.put("SLE", 45);
        tricodeTeamTable.put("WIN", 33);
        tricodeTeamTable.put("SJS", 28);
        tricodeTeamTable.put("QBD", 42);
        tricodeTeamTable.put("CBJ", 29);
        tricodeTeamTable.put("ARI", 53);
        tricodeTeamTable.put("FLA", 13);
        tricodeTeamTable.put("CAR", 12);
        tricodeTeamTable.put("VGK", 54);
        tricodeTeamTable.put("WPG", 52);
        tricodeTeamTable.put("TBL", 14);
        tricodeTeamTable.put("NSH", 18);
        tricodeTeamTable.put("PHX", 27);
        tricodeTeamTable.put("MWN", 41);
        tricodeTeamTable.put("QUA", 39);
        tricodeTeamTable.put("HAM", 37);
        tricodeTeamTable.put("DCG", 40);
        tricodeTeamTable.put("CLR", 35);
        tricodeTeamTable.put("PIR", 38);
        tricodeTeamTable.put("HFD", 34);
        tricodeTeamTable.put("NJD", 1);
        tricodeTeamTable.put("OTT", 9);
        tricodeTeamTable.put("COL", 21);
        tricodeTeamTable.put("NYA", 44);
        tricodeTeamTable.put("WSH", 15);
        tricodeTeamTable.put("MNS", 31);
        tricodeTeamTable.put("LAK", 26);
        tricodeTeamTable.put("NHL", 99);
        tricodeTeamTable.put("MIN", 30);
        tricodeTeamTable.put("MMR", 43);
        tricodeTeamTable.put("CLE", 49);
        tricodeTeamTable.put("BRK", 51);
        tricodeTeamTable.put("DFL", 50);
        tricodeTeamTable.put("CGS", 56);
        tricodeTeamTable.put("TOR", 10);
        tricodeTeamTable.put("EDM", 22);
        tricodeTeamTable.put("AFM", 47);
        tricodeTeamTable.put("TAN", 57);
        tricodeTeamTable.put("CGY", 20);
        tricodeTeamTable.put("STL", 19);
        tricodeTeamTable.put("DET", 17);
        tricodeTeamTable.put("NYR", 3);
        tricodeTeamTable.put("UTA", 59);
        tricodeTeamTable.put("UTA", 68);
        tricodeTeamTable.put("ANA", 24);
        tricodeTeamTable.put("VAN", 23);
        tricodeTeamTable.put("PHI", 4);
        tricodeTeamTable.put("CHI", 16);
        tricodeTeamTable.put("SEA", 55);
        tricodeTeamTable.put("PIT", 5);
        tricodeTeamTable.put("BOS", 6);
        tricodeTeamTable.put("DAL", 25);
        return tricodeTeamTable;
    }

}
