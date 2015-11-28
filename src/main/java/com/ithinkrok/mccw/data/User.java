package com.ithinkrok.mccw.data;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.enumeration.PlayerClass;
import com.ithinkrok.mccw.enumeration.TeamColor;
import com.ithinkrok.mccw.event.UserAbilityCooldownEvent;
import com.ithinkrok.mccw.event.UserUpgradeEvent;
import com.ithinkrok.mccw.inventory.InventoryHandler;
import com.ithinkrok.mccw.playerclass.PlayerClassHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by paul on 01/11/15.
 * <p>
 * Stores the player's info while they are online
 */
public class User {

    private static final HashSet<Material> SEE_THROUGH = new HashSet<>();

    static {
        SEE_THROUGH.add(Material.AIR);
        SEE_THROUGH.add(Material.WATER);
        SEE_THROUGH.add(Material.STATIONARY_WATER);
    }

    private UserCategoryStats statsChanges = new UserCategoryStats();
    private Player player;
    private TeamColor teamColor;
    private WarsPlugin plugin;
    private Location shopBlock;
    private Inventory shopInventory;
    private PlayerClass playerClass;
    private Map<String, Integer> upgradeLevels = new HashMap<>();
    private Map<String, Boolean> coolingDown = new HashMap<>();
    private Map<String, Metadata> metadata = new HashMap<>();
    private List<String> oldBuildingNows = new ArrayList<>();
    private int playerCash = 0;
    private boolean cloaked = false;
    private boolean inGame = false;
    private UUID fireAttacker;
    private UUID witherAttacker;
    private UUID lastAttacker;
    private String mapVote;
    private int trappedTicks;
    private PlayerClass lastPlayerClass;
    private TeamColor lastTeamColor;

    public User(WarsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Map<String, Integer> getUpgradeLevels() {
        return upgradeLevels;
    }

    public void setMetadata(String name, Object data, boolean wipeOnGameEnd) {
        metadata.put(name, new Metadata(data, wipeOnGameEnd));
    }

    public Object getMetadata(String name) {
        Metadata data = metadata.get(name);
        if (data == null) return null;
        return data.data;
    }

    public PlayerInventory getPlayerInventory() {
        return player.getInventory();
    }

    public boolean isCloaked() {
        return cloaked;
    }

    private void setCloaked(boolean cloaked) {
        this.cloaked = cloaked;
    }

    public void cloak() {
        setCloaked(true);

        for (User u : plugin.getUsers()) {
            if (this == u) continue;

            u.getPlayer().hidePlayer(player);
        }
    }

    public UserCategoryStats getStats(String category) {
        return plugin.getUserCategoryStats(getUniqueId(), category);
    }

    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    public void saveStats() {
        if (!plugin.hasPersistence()) return;


        UserCategoryStats changes = statsChanges;
        statsChanges = new UserCategoryStats();

        updateStats(changes, plugin.getOrCreateUserCategoryStats(player.getUniqueId(), "total"));
        if (lastPlayerClass != null)
            updateStats(changes, plugin.getOrCreateUserCategoryStats(player.getUniqueId(), lastPlayerClass.getName()));
        if (lastTeamColor != null)
            updateStats(changes, plugin.getOrCreateUserCategoryStats(player.getUniqueId(), lastTeamColor.getName()));

    }

    private void updateStats(UserCategoryStats changes, UserCategoryStats target) {
        target.setScore(target.getScore() + changes.getScore());
        target.setKills(target.getKills() + changes.getKills());
        target.setDeaths(target.getDeaths() + changes.getDeaths());
        target.setGames(target.getGames() + changes.getGames());
        target.setGameWins(target.getGameWins() + changes.getGameWins());
        target.setGameLosses(target.getGameLosses() + changes.getGameLosses());
        target.setTotalMoney(target.getTotalMoney() + changes.getTotalMoney());

        plugin.saveUserCategoryStats(target);
    }

    public void addGameWin() {
        statsChanges.setGameWins(statsChanges.getGameWins() + 1);

        addScore(plugin.getWarsConfig().getWinScoreModifier());
    }

    private void addScore(int amount) {
        if (amount == 0) return;

        statsChanges.setScore(statsChanges.getScore() + amount);

        if (amount > 0) messageLocale("score.gain", amount);
        else messageLocale("score.loss", -amount);
    }

    public void messageLocale(String locale, Object... objects) {
        message(plugin.getLocale(locale, objects));
    }

    public void message(String message) {
        player.sendMessage(WarsPlugin.CHAT_PREFIX + message);
    }

    public void addGameLoss() {
        statsChanges.setGameLosses(statsChanges.getGameLosses() + 1);

        addScore(plugin.getWarsConfig().getLossScoreModifier());
    }

    public void addGame() {
        if (!plugin.hasPersistence()) return;

        statsChanges.setGames(statsChanges.getGames() + 1);
    }

    public void addKill() {
        if (!plugin.hasPersistence()) return;

        statsChanges.setKills(statsChanges.getKills() + 1);

        addScore(plugin.getWarsConfig().getKillScoreModifier());
    }

    public void addDeath() {
        if (!plugin.hasPersistence()) return;

        statsChanges.setDeaths(statsChanges.getDeaths() + 1);

        addScore(plugin.getWarsConfig().getDeathScoreModifier());
    }

    public User getFireAttacker() {
        return plugin.getUser(fireAttacker);
    }

    public void setFireAttacker(User fireAttacker) {
        this.fireAttacker = fireAttacker.getPlayer().getUniqueId();
    }

    public Player getPlayer() {
        return player;
    }

    public User getWitherAttacker() {
        return plugin.getUser(witherAttacker);
    }

    public void setWitherAttacker(User witherAttacker) {
        this.witherAttacker = witherAttacker.getPlayer().getUniqueId();
    }

    public User getLastAttacker() {
        return plugin.getUser(lastAttacker);
    }

    public void setLastAttacker(User lastAttacker) {
        this.lastAttacker = lastAttacker.getPlayer().getUniqueId();
    }

    public void setFireTicks(User attacker, int ticks) {
        fireAttacker = attacker.getPlayer().getUniqueId();
        player.setFireTicks(ticks);
    }

    public void setWitherTicks(User attacker, int ticks) {
        witherAttacker = attacker.getPlayer().getUniqueId();
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, ticks, 0, false, true), true);
    }

    public Block rayTraceBlocks(int distance) {
        return player.getTargetBlock(SEE_THROUGH, distance);
    }

    public void addPlayerCash(int cash) {
        playerCash += cash;
        updateScoreboard();
        addTotalMoney(cash);
    }

    public void updateScoreboard() {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }


        if (inGame) {
            removeScoreboardObjective(scoreboard, "map");
            updateInGameScoreboard(scoreboard);
        } else if (!plugin.isInGame()) {
            removeScoreboardObjective(scoreboard, "main");
            updateMapScoreboard(scoreboard);
        } else {
            removeScoreboardObjective(scoreboard, "main");
            removeScoreboardObjective(scoreboard, "map");
        }

    }

    public void addTotalMoney(int amount) {
        if (!plugin.hasPersistence()) return;

        statsChanges.setTotalMoney(statsChanges.getTotalMoney() + amount);
    }

    private void removeScoreboardObjective(Scoreboard scoreboard, String name) {
        Objective mainObjective = scoreboard.getObjective(name);
        if (mainObjective != null) {
            mainObjective.unregister();
        }
    }

    private void updateInGameScoreboard(Scoreboard scoreboard) {
        Objective mainObjective = scoreboard.getObjective("main");
        if (mainObjective == null) {
            mainObjective = scoreboard.registerNewObjective("main", "dummy");
            mainObjective.setDisplayName("Colony Wars");
            mainObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        mainObjective.getScore(ChatColor.YELLOW + "Balance:").setScore(getPlayerCash());

        Team team = getTeam();
        mainObjective.getScore(ChatColor.YELLOW + "Team Balance:").setScore(team.getTeamCash());

        mainObjective.getScore(ChatColor.GOLD + "Building Now:").setScore(team.getTotalBuildingNowCount());

        HashMap<String, Integer> buildingNow = team.getBuildingNowCounts();

        for (String s : oldBuildingNows) {
            if (buildingNow.containsKey(s)) continue;
            mainObjective.getScoreboard().resetScores(ChatColor.GREEN + s + ":");
        }

        oldBuildingNows.clear();

        for (Map.Entry<String, Integer> entry : buildingNow.entrySet()) {
            mainObjective.getScore(ChatColor.GREEN + entry.getKey() + ":").setScore(entry.getValue());
            oldBuildingNows.add(entry.getKey());
        }

        mainObjective.getScore(ChatColor.AQUA + "Revival Rate:").setScore(team.getRespawnChance());

    }

    private void updateMapScoreboard(Scoreboard scoreboard) {
        Objective mapObjective = scoreboard.getObjective("map");
        if (mapObjective == null) {
            mapObjective = scoreboard.registerNewObjective("map", "dummy");
            mapObjective.setDisplayName(ChatColor.DARK_AQUA + "Map Voting");
            mapObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (String map : plugin.getMapList()) {
            int votes = plugin.getMapVotes(map);

            if (votes == 0) mapObjective.getScoreboard().resetScores(map);
            else {
                mapObjective.getScore(map).setScore(votes);
            }
        }
    }

    public int getPlayerCash() {
        return playerCash;
    }

    public Team getTeam() {
        return plugin.getTeam(teamColor);
    }

    public void setUpgradeLevel(String upgrade, int level) {
        int oldLevel = getUpgradeLevel(upgrade);
        if (oldLevel == level && upgradeLevels.containsKey(upgrade)) return;

        upgradeLevels.put(upgrade, level);

        plugin.getGameInstance().onPlayerUpgrade(new UserUpgradeEvent(this, upgrade, level));
    }

    public int getUpgradeLevel(String upgrade) {
        Integer level = upgradeLevels.get(upgrade);

        return level == null ? 0 : level;
    }

    public boolean startCoolDown(String ability, int seconds, String coolDownMessage) {
        if (isCoolingDown(ability)) {
            message(ChatColor.RED + "Please wait for this ability to cool down!");
            return false;
        }

        coolingDown.put(ability, true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> stopCoolDown(ability, coolDownMessage), seconds * 20);

        return true;
    }

    public boolean isCoolingDown(String ability) {
        Boolean b = coolingDown.get(ability);

        if (b == null) return false;
        return b;
    }

    public void stopCoolDown(String ability, String message) {
        if (!isCoolingDown(ability)) return;

        coolingDown.put(ability, false);

        if (!isInGame()) return;
        getPlayerClassHandler().onAbilityCooldown(new UserAbilityCooldownEvent(this, ability));

        if (message == null) return;
        message(ChatColor.GREEN + message);
        player.playSound(player.getLocation(), Sound.ZOMBIE_UNFECT, 1.0f, 2.0f);
    }

    public boolean isInGame() {
        return inGame && teamColor != null;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;

        if (!inGame) {
            upgradeLevels.clear();
            coolingDown.clear();
            oldBuildingNows.clear();

            List<String> removeMetadata =
                    metadata.entrySet().stream().filter(entry -> entry.getValue().wipeOnGameEnd).map(Map.Entry::getKey)
                            .collect(Collectors.toList());

            for (String key : removeMetadata) {
                metadata.remove(key);
            }

            playerClass = null;
            shopInventory = null;
            shopBlock = null;
            playerCash = 0;
            oldBuildingNows.clear();
        }

        updateScoreboard();
    }

    public PlayerClassHandler getPlayerClassHandler() {
        return plugin.getPlayerClassHandler(playerClass);
    }

    public boolean subtractPlayerCash(int cash) {
        if (cash > playerCash) return false;
        playerCash -= cash;

        updateScoreboard();

        message(ChatColor.RED + "$" + cash + ChatColor.YELLOW + " were deducted from your Account!");
        message("Your new Balance is: " + ChatColor.GREEN + "$" + playerCash + "!");

        return true;
    }

    public void openShopInventory(Location shopBlock) {
        this.shopBlock = shopBlock;

        Building building = plugin.getGameInstance().getBuildingInfo(shopBlock);
        if (building == null || shopBlock.getBlock().getType() != Material.OBSIDIAN) {
            message(plugin.getLocale("building.shop.invalid"));
            return;
        }

        if (getTeamColor() != building.getTeamColor()) {
            message(plugin.getLocale("building.shop.not-yours"));
            return;
        }

        if (!building.isFinished()) {
            message(plugin.getLocale("building.shop.not-finished"));
            return;
        }

        redoShopInventory();

        player.openInventory(this.shopInventory);
    }

    public TeamColor getTeamColor() {
        return teamColor;
    }

    public void redoShopInventory() {
        if (shopBlock == null) return;

        Building building = plugin.getGameInstance().getBuildingInfo(shopBlock);
        if (building == null || shopBlock.getBlock().getType() != Material.OBSIDIAN) {
            if (shopInventory != null) player.closeInventory();
            return;
        }

        List<ItemStack> contents = calculateInventoryContents(building);

        int index = 0;
        if (shopInventory != null) shopInventory.clear();
        else {
            int slots = 9 * ((contents.size() + 9) / 9);
            shopInventory = Bukkit.createInventory(player, slots, building.getBuildingName());
        }

        for (ItemStack item : contents) {
            shopInventory.setItem(index++, item);
        }
    }

    private List<ItemStack> calculateInventoryContents(Building building) {
        Team team = plugin.getTeam(getTeamColor());

        InventoryHandler inventoryHandler = plugin.getBuildingInventoryHandler();
        List<ItemStack> contents = new ArrayList<>();

        if (inventoryHandler != null) inventoryHandler.addInventoryItems(contents, building, this, team);

        PlayerClassHandler classHandler = getPlayerClassHandler();
        classHandler.addInventoryItems(contents, building, this, team);
        return contents;
    }

    public void setTeamColor(TeamColor teamColor) {
        if (this.teamColor != null) {
            getTeam().removePlayer(player);
            if (teamColor == null) lastTeamColor = this.teamColor;
        }

        if (teamColor != null) lastTeamColor = teamColor;
        this.teamColor = teamColor;

        if (this.teamColor != null) {
            getTeam().addPlayer(player);
        }

        player.setPlayerListName(getFormattedName());
        player.setDisplayName(getFormattedName());

        if (teamColor != null) updateTeamArmor();
        else clearArmor();
    }

    public void removeFromGame() {
        if (!isInGame()) return;

        Team team = getTeam();
        setTeamColor(null);
        setInGame(false);

        plugin.messageAll(ChatColor.GOLD + "The " + team.getTeamColor().getFormattedName() + ChatColor.GOLD +
                " has lost a player!");
        plugin.messageAll(ChatColor.GOLD + "There are now " + ChatColor.DARK_AQUA + team.getPlayerCount() +
                ChatColor.GOLD + " players left on the " + team.getTeamColor().getFormattedName() + ChatColor.GOLD +
                " Team");

        if (team.getPlayerCount() == 0) {
            team.eliminate();
        }

        plugin.getGameInstance().checkVictory(true);

        plugin.getGameInstance().updateSpectatorInventories();

        setSpectator();
    }

    public void resetPlayerStats(boolean removePotionEffects) {
        player.setMaxHealth(plugin.getMaxHealth());
        player.setHealth(plugin.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(5);

        if (removePotionEffects) removePotionEffects();
    }

    public void createPlayerExplosion(Location loc, float power, boolean fire, int fuse) {
        TNTPrimed tnt = (TNTPrimed) loc.getWorld().spawnEntity(loc, EntityType.PRIMED_TNT);

        tnt.setMetadata("striker", new FixedMetadataValue(plugin, player.getUniqueId()));

        tnt.setIsIncendiary(fire);
        tnt.setYield(power);

        tnt.setFuseTicks(fuse);
    }

    public void removePotionEffects() {
        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());

        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
        }

        player.setFireTicks(0);
    }

    public void setSpectator() {
        if (isInGame()) throw new RuntimeException("You cannot be a spectator when you are already in a game");

        cloak();
        resetPlayerStats(true);

        player.setAllowFlight(true);
        player.spigot().setCollidesWithEntities(false);
        clearArmor();

        plugin.getGameInstance().setupSpectatorInventory(player);

        updateScoreboard();
    }

    public void unsetSpectator() {
        player.setAllowFlight(false);
        player.spigot().setCollidesWithEntities(true);

        decloak();
    }

    public void decloak() {
        setCloaked(false);

        for (User u : plugin.getUsers()) {
            if (this == u) continue;

            u.getPlayer().showPlayer(player);
        }
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PlayerClass playerClass) {
        if (this.playerClass != null && playerClass == null) lastPlayerClass = this.playerClass;
        else lastPlayerClass = playerClass;

        this.playerClass = playerClass;
    }

    public String getFormattedName() {
        if (teamColor == null) return player.getName();

        return teamColor.getChatColor() + player.getName();
    }

    public void clearArmor() {
        PlayerInventory inv = player.getInventory();

        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
    }

    public boolean teleport(Location location) {
        return player.teleport(location);
    }

    public void updateTeamArmor() {
        if (!plugin.isInGame()) return;
        if (teamColor == null) {
            clearArmor();
            return;
        }
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET, 1);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS, 1);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

        setArmorColor(helmet);
        setArmorColor(chestplate);
        setArmorColor(leggings);
        setArmorColor(boots);

        PlayerInventory inv = player.getInventory();

        inv.setHelmet(helmet);
        inv.setChestplate(chestplate);
        inv.setLeggings(leggings);
        inv.setBoots(boots);
    }

    private void setArmorColor(ItemStack armor) {
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();

        meta.setColor(teamColor.getArmorColor());
        armor.setItemMeta(meta);

        meta.spigot().setUnbreakable(true);
    }

    public boolean hasPlayerCash(int cash) {
        return cash <= playerCash;
    }

    public Location getShopBlock() {
        return shopBlock;
    }

    public Inventory getShopInventory() {
        return shopInventory;
    }

    public void shopInventoryClosed() {
        shopInventory = null;
        shopBlock = null;
    }

    public String getMapVote() {
        return mapVote;
    }

    public void setMapVote(String mapVote) {
        if (this.mapVote != null) plugin.setMapVotes(this.mapVote, plugin.getMapVotes(this.mapVote) - 1);

        this.mapVote = mapVote;

        if (this.mapVote != null) plugin.setMapVotes(this.mapVote, plugin.getMapVotes(this.mapVote) + 1);

        plugin.getUsers().forEach(User::updateScoreboard);
    }

    public int getTrappedTicks() {
        return trappedTicks;
    }

    public void setTrappedTicks(int trappedTicks) {
        this.trappedTicks = trappedTicks;
    }

    private static class Metadata {
        public Object data;
        public boolean wipeOnGameEnd;

        public Metadata(Object data, boolean wipeOnGameEnd) {
            this.data = data;
            this.wipeOnGameEnd = wipeOnGameEnd;
        }
    }
}
