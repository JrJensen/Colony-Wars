package com.ithinkrok.mccw.data;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.enumeration.TeamColor;
import com.ithinkrok.mccw.strings.Buildings;
import com.ithinkrok.mccw.util.building.CannonTowerHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by paul on 02/11/15.
 *
 * Handles Team data
 */
public class Team {

    private TeamColor teamColor;
    private ArrayList<User> users = new ArrayList<>();
    private ArrayList<StatsHolder> statsHolders = new ArrayList<>();
    private WarsPlugin plugin;
    private int teamCash;

    private int buildingsConstructingNow = 0;

    private HashMap<String, Integer> buildingCounts = new HashMap<>();
    private HashMap<String, Integer> buildingNowCounts = new HashMap<>();
    private HashMap<String, Boolean> hadBuildings = new HashMap<>();

    private List<Location> churchLocations = new ArrayList<>();
    private Location baseLocation;
    private int respawnChance;

    private HashMap<Location, Integer> cannonTowerTasks = new HashMap<>();

    private boolean disableRespawnNotification = false;

    public Location getBaseLocation() {
        return baseLocation;
    }

    public Team(WarsPlugin plugin, TeamColor teamColor) {
        this.plugin = plugin;
        this.teamColor = teamColor;
    }

    public void message(String message){
        for(User p : users){
            p.sendMessage(message);
        }
    }

    public TeamColor getTeamColor() {
        return teamColor;
    }

    public void addUser(User user){
        if(users.contains(user)) return;

        users.add(user);

        if(statsHolders.contains(user.getStatsHolder())) return;

        statsHolders.add(user.getStatsHolder());
    }

    public boolean isRespawnNotificationDisabled() {
        return disableRespawnNotification;
    }

    public void setDisableRespawnNotification(boolean disableRespawnNotification) {
        this.disableRespawnNotification = disableRespawnNotification;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public ArrayList<StatsHolder> getStatsHolders() {
        return statsHolders;
    }

    public void removeUser(User user){
        users.remove(user);

        if(plugin.isInGame()) return;

        statsHolders.remove(user.getStatsHolder());
    }

    public boolean hasUser(User user){
        return users.contains(user);
    }

    public int getTeamCash() {
        return teamCash;
    }

    public void addTeamCash(int cash){
        teamCash += cash;

        updatePlayerScoreboards();
    }

    public int getRespawnChance() {
        return respawnChance;
    }

    public void setRespawnChance(int respawnChance) {
        if(this.respawnChance == respawnChance) return;
        this.respawnChance = respawnChance;

        if(!isRespawnNotificationDisabled()) messageLocale("game.revival-chance", getRespawnChance());

        updatePlayerScoreboards();
    }

    public int getTotalBuildingNowCount() {
        return buildingsConstructingNow;
    }

    public boolean subtractTeamCash(int cash){
        if(cash > teamCash) return false;
        teamCash -= cash;

        updatePlayerScoreboards();

        message(ChatColor.RED + "$" + cash + ChatColor.YELLOW + " were deducted from your Team's Account!");
        message("Your Team's new Balance is: " + ChatColor.GREEN + "$" + teamCash + ChatColor.YELLOW + "!");

        return true;
    }

    public void updatePlayerScoreboards(){
        users.forEach(User::updateScoreboard);
    }

    public int getBuildingCount(String buildingType){
        Integer integer = buildingCounts.get(buildingType);

        return integer == null ? 0 : integer;
    }

    public int getBuildingNowCount(String buildingType){
        Integer integer = buildingNowCounts.get(buildingType);

        return integer == null ? 0 : integer;
    }

    public HashMap<String, Integer> getBuildingNowCounts() {
        return buildingNowCounts;
    }

    public void buildingStarted(String buildingType){
        buildingsConstructingNow += 1;

        buildingNowCounts.put(buildingType, getBuildingNowCount(buildingType) + 1);

        updatePlayerScoreboards();
    }

    public boolean everHadBuilding(String buildingName){
        Boolean bool = hadBuildings.get(buildingName);

        return bool == null ? false : bool;
    }

    public void buildingFinished(Building building){
        buildingsConstructingNow -= 1;

        int buildingNowOfType = getBuildingNowCount(building.getBuildingName()) - 1;
        if(buildingNowOfType > 0) buildingNowCounts.put(building.getBuildingName(), buildingNowOfType);
        else buildingNowCounts.remove(building.getBuildingName());

        buildingCounts.put(building.getBuildingName(), getBuildingCount(building.getBuildingName()) + 1);
        hadBuildings.put(building.getBuildingName(), true);

        switch(building.getBuildingName()){
            case Buildings.CHURCH:
                respawnChance = Math.max(respawnChance, 30);
                churchLocations.add(building.getCenterBlock());
                break;
            case Buildings.CATHEDRAL:
                respawnChance = Math.max(respawnChance, 75);
                churchLocations.add(building.getCenterBlock());
                break;
            case Buildings.BASE:
                baseLocation = building.getCenterBlock();
                break;
            case Buildings.CANNONTOWER:
            case Buildings.FORTRESS:
                int cannonTowerTask = CannonTowerHandler.startCannonTowerTask(plugin, building);
                cannonTowerTasks.put(building.getCenterBlock(), cannonTowerTask);
                break;
        }

        updatePlayerScoreboards();
    }

    public void removeBuilding(Building building){
        buildingCounts.put(building.getBuildingName(), Math.max(getBuildingCount(building.getBuildingName()) - 1, 0));

        switch(building.getBuildingName()){
            case Buildings.CHURCH:
            case Buildings.CATHEDRAL:
                churchLocations.remove(building.getCenterBlock());
                if(churchLocations.isEmpty()) setRespawnChance(0);
                break;
            case Buildings.CANNONTOWER:
            case Buildings.FORTRESS:
                int task = cannonTowerTasks.remove(building.getCenterBlock());

                plugin.getServer().getScheduler().cancelTask(task);
                break;
        }
    }

    public int getUserCount() {
        return users.size();
    }

    public void respawnPlayer(Player died) {
        Location loc;
        if(churchLocations.size() > 0) loc = churchLocations.get(plugin.getRandom().nextInt(churchLocations.size()));
        else loc = baseLocation;

        died.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);

        plugin.getUser(died).decloak();
    }

    public void eliminate() {
        plugin.messageAll(ChatColor.GOLD + "The " + getTeamColor().getFormattedName() + ChatColor.GOLD +
                " Team was eliminated!");

        if(baseLocation != null){
            plugin.getGameInstance().getBuildingInfo(baseLocation).explode();
            baseLocation = null;
        }

        for(StatsHolder statsHolder : statsHolders) {
            statsHolder.addGameLoss();
            statsHolder.saveStats();
        }

        statsHolders.clear();
    }

    public User getRandomUser() {
        if(users.isEmpty()) return null;
        return users.get(plugin.getRandom().nextInt(users.size()));
    }

    public void messageLocale(String locale, Object...args) {
        message(plugin.getLocale(locale, args));
    }
}
