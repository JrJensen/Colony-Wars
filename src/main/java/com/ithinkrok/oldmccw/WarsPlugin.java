package com.ithinkrok.oldmccw;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.google.common.collect.MapMaker;
import com.ithinkrok.oldmccw.command.CommandListener;
import com.ithinkrok.oldmccw.data.*;
import com.ithinkrok.oldmccw.enumeration.GameState;
import com.ithinkrok.oldmccw.enumeration.PlayerClass;
import com.ithinkrok.minigames.TeamColor;
import com.ithinkrok.oldmccw.event.UserJoinLobbyEvent;
import com.ithinkrok.oldmccw.handler.CountdownHandler;
import com.ithinkrok.oldmccw.handler.GameInstance;
import com.ithinkrok.oldmccw.inventory.InventoryHandler;
import com.ithinkrok.oldmccw.inventory.OmniInventory;
import com.ithinkrok.oldmccw.inventory.SpectatorInventory;
import com.ithinkrok.oldmccw.listener.WarsBaseListener;
import com.ithinkrok.oldmccw.listener.WarsLobbyListener;
import com.ithinkrok.oldmccw.lobby.LobbyMinigame;
import com.ithinkrok.oldmccw.lobby.ParcourMinigame;
import com.ithinkrok.oldmccw.lobby.SpleefMinigame;
import com.ithinkrok.oldmccw.lobby.WoolHeadMinigame;
import com.ithinkrok.oldmccw.playerclass.PlayerClassHandler;
import com.ithinkrok.oldmccw.util.EntityUtils;
import com.ithinkrok.oldmccw.util.disguisecraft.Disguises;
import com.ithinkrok.oldmccw.util.InvisiblePlayerAttacker;
import com.ithinkrok.oldmccw.util.Persistence;
import com.ithinkrok.oldmccw.util.io.LangFile;
import com.ithinkrok.oldmccw.util.io.ResourceHandler;
import com.ithinkrok.oldmccw.util.io.WarsConfig;
import com.ithinkrok.oldmccw.util.item.Handbook;
import com.ithinkrok.oldmccw.util.item.InventoryUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Created by paul on 01/11/15.
 * <p>
 * The main plugin class for Colony Wars
 */
public class WarsPlugin extends JavaPlugin {

    public static final String CHAT_PREFIX =
            ChatColor.GRAY + "[" + ChatColor.DARK_AQUA + "ColonyWars" + ChatColor.GRAY + "] " + ChatColor.YELLOW;

    private ConcurrentMap<UUID, User> playerInfoHashMap = new ConcurrentHashMap<>();
    private Map<UUID, StatsHolder> statsHolderMap = new MapMaker().weakValues().makeMap();
    private Map<TeamColor, Team> teamInfoEnumMap = new HashMap<>();


    /**
     * Is there a game currently in progress
     */
    private boolean inGame = false;

    private OmniInventory buildingInventoryHandler;
    private SpectatorInventory spectatorInventoryHandler;

    private Map<PlayerClass, PlayerClassHandler> classHandlerMap = new HashMap<>();
    private Random random = new Random();

    private Listener currentListener;
    private CommandListener commandListener;

    private CountdownHandler countdownHandler;
    private GameInstance gameInstance;

    private List<String> mapList;
    private ConcurrentHashMap<String, Integer> mapVotes = new ConcurrentHashMap<>();

    private List<LobbyMinigame> lobbyMinigames = new ArrayList<>();

    private String handbookMeta;
    private ItemStack handbook;

    private LangFile langFile;

    private WarsConfig warsConfig;

    private Persistence persistence;

    public LangFile getLangFile() {
        return langFile;
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (persistence != null) persistence.onPluginDisabled();
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public void changeListener(Listener newListener) {
        HandlerList.unregisterAll(currentListener);
        currentListener = newListener;
        getServer().getPluginManager().registerEvents(currentListener, this);
    }

    public boolean hasPersistence() {
        return persistence != null;
    }

    @Override
    public void installDDL() {
        super.installDDL();
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> classes = new ArrayList<>();

        classes.add(UserCategoryStats.class);

        return classes;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void getOrCreateUserCategoryStats(UUID playerUUID, String category, Persistence.PersistenceTask task) {
        persistence.getOrCreateUserCategoryStats(playerUUID, category, task);
    }

    public void saveUserCategoryStats(UserCategoryStats stats) {
        persistence.saveUserCategoryStats(stats);
    }

    @Override
    public void onEnable() {
        configureWorlds();

        warsConfig = new WarsConfig(this::getMapConfig);

        saveDefaultConfig();

        if (warsConfig.hasPersistence()) persistence = new Persistence(this);
        commandListener = new CommandListener(this);

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        InvisiblePlayerAttacker.enablePlayerAttacker(this, protocolManager);

        currentListener = new WarsLobbyListener(this);
        getServer().getPluginManager().registerEvents(new WarsBaseListener(this), this);
        getServer().getPluginManager().registerEvents(currentListener, this);

        spectatorInventoryHandler = new SpectatorInventory(this);

        lobbyMinigames.add(new WoolHeadMinigame(this));
        lobbyMinigames.add(new ParcourMinigame(this));
        lobbyMinigames.add(new SpleefMinigame(this));

        getLobbyMinigames().forEach(LobbyMinigame::resetMinigame);

        countdownHandler = new CountdownHandler(this);

        reload();
    }

    private void configureWorlds() {
        for (World world : Bukkit.getWorlds()) {
            world.setAutoSave(false);
            world.setKeepSpawnInMemory(false);
        }
    }

    public List<LobbyMinigame> getLobbyMinigames() {
        return lobbyMinigames;
    }

    public void reload() {
        reloadConfig();

        String languageFile = getWarsConfig().getLanguageName() + ".lang";
        langFile = new LangFile(ResourceHandler.getPropertiesResource(this, languageFile));

        handbookMeta = Handbook.loadHandbookMeta(this);
        handbook = null;

        buildingInventoryHandler = new OmniInventory(this, getWarsConfig());

        changeTeamCount(warsConfig.getTeamCount());
    }

    /**
     * @return The {@code WarsConfig} to access known configuration values
     */
    public WarsConfig getWarsConfig() {
        return warsConfig;
    }

    public void changeTeamCount(int teamCount) {
        TeamColor.initialise(teamCount);

        mapList = new ArrayList<>();

        for (String mapName : getWarsConfig().getMapList()) {
            WarsMap warsMap = new WarsMap(this, mapName);

            if (!warsMap.supportsTeamCount() && !mapName.equals(getWarsConfig().getRandomMapName())) continue;
            mapList.add(warsMap.getName());
        }


        changeGameState(GameState.LOBBY);
    }

    public void changeGameState(GameState state) {
        countdownHandler.stopCountdown();

        if (state == GameState.GAME && gameInstance == null) {
            setInGame(true);
            gameInstance = new GameInstance(this, assignGameMap());
        }

        if (gameInstance != null) gameInstance.changeGameState(state);

        if (state == GameState.LOBBY) {
            gameInstance = null;
            setInGame(false);

            getLobbyMinigames().forEach(LobbyMinigame::resetMinigame);

            getUsers().forEach(this::userJoinLobby);

            resetTeams();

            countdownHandler.startLobbyCountdown();
        }

        for (User user : getUsers()) {
            user.getStatsHolder().saveStats();
        }
    }

    public String assignGameMap() {
        int highestVotes = Integer.MIN_VALUE;

        List<String> possible = new ArrayList<>();

        for (String map : mapList) {
            int votes = getMapVotes(map);

            if (votes > highestVotes) {
                highestVotes = votes;
                possible.clear();
            }
            if (votes == highestVotes) possible.add(map);
        }

        String bestMap = possible.get(random.nextInt(possible.size()));

        while (bestMap.equals(getWarsConfig().getRandomMapName())) {
            bestMap = mapList.get(random.nextInt(mapList.size()));
        }

        messageAll(getLocale("voting.maps.map-winner", bestMap));

        return bestMap;
    }

    public Collection<User> getUsers() {
        return playerInfoHashMap.values();
    }

    public void userJoinLobby(User user) {
        Disguises.unDisguise(user);

        user.setInGame(false);
        if (!user.isPlayer()) return;

        user.setGameMode(GameMode.ADVENTURE);
        user.unsetSpectator();
        user.resetPlayerStats(true);
        user.clearArmor();

        user.setTeamColor(null);
        user.setMapVote(null);

        PlayerInventory inv = user.getPlayerInventory();

        inv.clear();

        inv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.LEATHER_HELMET, 1, 0, getLocale("lobby.chooser.team.name"),
                        getLocale("lobby.chooser.team.desc")));

        inv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.WOOD_SWORD, 1, 0, getLocale("lobby.chooser.class.name"),
                        getLocale("lobby.chooser.class.desc")));

        inv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.EMPTY_MAP, 1, 0, getLocale("lobby.chooser.map.name"),
                        getLocale("lobby.chooser.map.desc")));

        user.sendMessage(getLocale("lobby.info.choose-class"));

        user.sendMessage(getLocale("lobby.info.map-info"));

        user.teleport(getLobbySpawn());

        giveUserHandbook(user);

        UserJoinLobbyEvent userJoinLobbyEvent = new UserJoinLobbyEvent(user);

        for (LobbyMinigame minigame : getLobbyMinigames()) {
            minigame.onUserJoinLobby(userJoinLobbyEvent);
        }
    }

    public void resetTeams() {
        for (TeamColor c : TeamColor.values()) {
            teamInfoEnumMap.put(c, new Team(this, c));
        }
    }

    public int getMapVotes(String map) {
        Integer result = mapVotes.get(map);

        if (result == null) return 0;
        return result;
    }

    public void messageAll(String message) {
        getServer().getConsoleSender().sendMessage(CHAT_PREFIX + message);
        for (User p : playerInfoHashMap.values()) {
            p.sendMessage(message);
        }
    }

    public String getLocale(String name, Object... params) {
        return langFile.getLocale(name, params);
    }

    public Location getLobbySpawn() {
        return Bukkit.getWorld(getLobbyWorldName()).getSpawnLocation();
    }

    public void giveUserHandbook(User player) {
        PlayerInventory inv = player.getPlayerInventory();
        if (getHandbook() == null) {
            String meta = getHandbookMeta();

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "minecraft:give " + player.getName() + " written_book 1 0 " + meta);

            int index = inv.first(Material.WRITTEN_BOOK);
            ItemStack book = inv.getItem(index);
            book.setAmount(1);
            inv.setItem(index, null);
            setHandbook(book.clone());
        }

        if (inv.getItem(8) == null || inv.getItem(8).getType() == Material.AIR) {
            inv.setItem(8, getHandbook().clone());
        } else inv.addItem(getHandbook().clone());
    }

    public String getLobbyWorldName() {
        return getWarsConfig().getLobbyMapFolder();
    }

    public ItemStack getHandbook() {
        return handbook;
    }

    public String getHandbookMeta() {
        return handbookMeta;
    }

    public void setHandbook(ItemStack handbook) {
        this.handbook = handbook;
    }

    /**
     * @return The configuration for the current map
     */
    public ConfigurationSection getMapConfig() {
        if (gameInstance != null) return gameInstance.getMapConfig();
        else return getBaseConfig();
    }

    /**
     * @return The base configuration, ignoring the current map configuration
     */
    public FileConfiguration getBaseConfig() {
        return super.getConfig();
    }

    public List<String> getMapList() {
        return mapList;
    }

    public void setMapVotes(String map, int votes) {
        mapVotes.put(map, votes);
    }

    public void playerTeleportLobby(User user) {
        user.teleport(getLobbySpawn());
    }

    public String getPlayingWorldName() {
        return getWarsConfig().getGameMapFolder();
    }

    public Location getMapSpawn(TeamColor teamColorOrNull) {
        if (gameInstance == null) return getLobbySpawn();
        return gameInstance.getMapSpawn(teamColorOrNull);
    }

    public Building getBuildingInfo(Location centerBlock) {
        return gameInstance.getBuildingInfo(centerBlock);
    }

    public boolean isInBuilding(Location blockLocation) {
        return gameInstance.isInBuilding(blockLocation);
    }

    public void handleSpectatorInventory(InventoryClickEvent event) {
        gameInstance.handleSpectatorInventory(event);
    }

    public ShowdownArena getShowdownArena() {
        return gameInstance.getShowdownArena();
    }

    public void onUserAttacked() {
        if (gameInstance == null) return;
        gameInstance.onUserAttacked();
    }

    public InventoryHandler getBuildingInventoryHandler() {
        return buildingInventoryHandler;
    }

    public User createUser(Player player) {
        StatsHolder statsHolder = statsHolderMap.get(player.getUniqueId());

        User user = new User(this, player, statsHolder);

        statsHolderMap.put(user.getUniqueId(), user.getStatsHolder());


        setUser(player, user);

        return user;
    }

    public void setUser(Player player, User user) {
        setUser(player.getUniqueId(), user);
    }

    public void setUser(UUID uniqueId, User user) {
        if (user == null) playerInfoHashMap.remove(uniqueId);
        else playerInfoHashMap.put(uniqueId, user);
    }

    public Random getRandom() {
        return random;
    }

    public Team getTeam(TeamColor teamColor) {
        return teamInfoEnumMap.get(teamColor);
    }

    public PlayerClassHandler getPlayerClassHandler(PlayerClass playerClass) {
        PlayerClassHandler classHandler = classHandlerMap.get(playerClass);
        if (classHandler == null) {
            classHandler = playerClass.createPlayerClassHandler(this);

            classHandlerMap.put(playerClass, classHandler);
        }

        return classHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandListener.onCommand(sender, command, label, args);
    }

    public CountdownHandler getCountdownHandler() {
        return countdownHandler;
    }

    public GameInstance getGameInstance() {
        return gameInstance;
    }

    public void messageAllLocale(String locale, Object... args) {
        messageAll(getLocale(locale, args));
    }

    public double getMaxHealth() {
        return (double) 40;
    }

    public Schematic getSchematicData(String buildingName) {
        return gameInstance.getSchematicData(buildingName);
    }

    public User getUser(UUID uniqueId) {
        if (uniqueId == null) return null;
        return playerInfoHashMap.get(uniqueId);
    }

    public int getPlayerCount() {
        return playerInfoHashMap.size();
    }

    public int getPlayersInGame() {
        int count = 0;

        for (Team team : teamInfoEnumMap.values()) {
            count += team.getUserCount();
        }

        return count;
    }

    public int getNonZombiePlayersInGame() {
        int count = 0;

        for (User user : getUsers()) {
            if (!user.isInGame() || !user.isPlayer()) continue;
            ++count;
        }

        return count;
    }

    public SpectatorInventory getSpectatorInventoryHandler() {
        return spectatorInventoryHandler;
    }

    public User getUser(LivingEntity livingEntity) {
        if (livingEntity instanceof Player) return getUser((Player) livingEntity);
        else if (!(livingEntity instanceof Zombie)) return null;

        return EntityUtils.getUserFromEntity(this, livingEntity);
    }

    public User getUser(Player player) {
        if (player == null) return null;
        return playerInfoHashMap.get(player.getUniqueId());
    }

    public GameState getGameState() {
        if (gameInstance != null) return gameInstance.getGameState();
        else return GameState.LOBBY;
    }


    public void getUserCategoryStats(UUID uniqueId, String category, Persistence.PersistenceTask task) {
        persistence.getUserCategoryStats(uniqueId, category, task);
    }
}