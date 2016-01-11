package com.ithinkrok.minigames;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.ithinkrok.minigames.event.game.CountdownFinishedEvent;
import com.ithinkrok.minigames.event.game.GameEvent;
import com.ithinkrok.minigames.event.game.GameStateChangedEvent;
import com.ithinkrok.minigames.event.game.MapChangedEvent;
import com.ithinkrok.minigames.event.team.TeamEvent;
import com.ithinkrok.minigames.event.user.UserEvent;
import com.ithinkrok.minigames.event.user.game.UserJoinEvent;
import com.ithinkrok.minigames.event.user.game.UserQuitEvent;
import com.ithinkrok.minigames.item.CustomItem;
import com.ithinkrok.minigames.lang.LangFile;
import com.ithinkrok.minigames.lang.LanguageLookup;
import com.ithinkrok.minigames.lang.Messagable;
import com.ithinkrok.minigames.map.GameMap;
import com.ithinkrok.minigames.map.GameMapInfo;
import com.ithinkrok.minigames.metadata.Metadata;
import com.ithinkrok.minigames.metadata.MetadataHolder;
import com.ithinkrok.minigames.schematic.Schematic;
import com.ithinkrok.minigames.schematic.SchematicResolver;
import com.ithinkrok.minigames.task.GameRunnable;
import com.ithinkrok.minigames.task.GameTask;
import com.ithinkrok.minigames.task.TaskList;
import com.ithinkrok.minigames.task.TaskScheduler;
import com.ithinkrok.minigames.user.UserResolver;
import com.ithinkrok.minigames.util.EventExecutor;
import com.ithinkrok.minigames.util.io.FileLoader;
import com.ithinkrok.oldmccw.data.TeamColor;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by paul on 31/12/15.
 */
public class GameGroup
        implements LanguageLookup, Messagable, TaskScheduler, UserResolver, FileLoader, SharedObjectAccessor,
        MetadataHolder<Metadata>, SchematicResolver {

    private ConcurrentMap<UUID, User> usersInGroup = new ConcurrentHashMap<>();

    private Map<String, TeamIdentifier> teamIdentifiers = new HashMap<>();
    private Map<String, Kit> kits = new HashMap<>();

    private Map<TeamIdentifier, Team> teamsInGroup = new HashMap<>();
    private Game game;

    private Map<String, GameState> gameStates = new HashMap<>();
    private GameState gameState;

    private GameMap currentMap;

    private TaskList gameGroupTaskList = new TaskList();
    private TaskList gameStateTaskList = new TaskList();
    private HashMap<String, Listener> defaultListeners = new HashMap<>();

    private Listener gameGroupListener;
    private List<Listener> defaultAndMapListeners = new ArrayList<>();

    private ClassToInstanceMap<Metadata> metadataMap = MutableClassToInstanceMap.create();

    private Countdown countdown;

    public GameGroup(Game game) {
        this.game = game;

        gameGroupListener = new GameGroupListener();
        defaultAndMapListeners = createDefaultAndMapListeners();
    }

    public void start() {
        createDefaultAndMapListeners();

        boolean hasDefault = false;
        for (GameState gs : game.getGameStates()) {
            if (!hasDefault) {
                changeGameState(gs);
                hasDefault = true;
            }

            this.gameStates.put(gs.getName(), gs);
        }

        changeMap(game.getStartMapInfo());
    }

    @SuppressWarnings("unchecked")
    public void changeGameState(GameState gameState) {
        if (gameState.equals(this.gameState)) return;
        GameState oldState = this.gameState;
        GameState newState = this.gameState = gameState;

        gameStateTaskList.cancelAllTasks();

        Event event = new GameStateChangedEvent(this, oldState, newState);
        EventExecutor.executeEvent(event,
                getListeners(getAllUserListeners(), getAllTeamListeners(), gameState.getListeners()));
    }

    public void changeMap(GameMapInfo mapInfo) {
        GameMap oldMap = currentMap;
        GameMap newMap = new GameMap(this, mapInfo);

        usersInGroup.values().forEach(newMap::teleportUser);

        currentMap = newMap;

        game.setGameGroupForMap(this, newMap.getWorld().getName());

        Event event = new MapChangedEvent(this, oldMap, newMap);

        EventExecutor
                .executeEvent(event, getListeners(getAllUserListeners(), getAllTeamListeners(), newMap.getListeners()));

        defaultAndMapListeners = createDefaultAndMapListeners(newMap.getListenerMap());

        if (oldMap != null) oldMap.unloadMap();
    }

    @SafeVarargs
    private final Collection<Collection<Listener>> getListeners(Collection<Listener>... extras) {
        Collection<Collection<Listener>> listeners = new ArrayList<>(4);
        if (gameState != null) listeners.add(gameState.getListeners());
        listeners.add(defaultAndMapListeners);
        Collections.addAll(listeners, extras);

        return listeners;
    }

    private Collection<Listener> getAllUserListeners() {
        ArrayList<Listener> result = new ArrayList<>(usersInGroup.size());

        for (User user : usersInGroup.values()) {
            result.addAll(user.getListeners());
        }

        return result;
    }

    private Collection<Listener> getAllUsersInTeamListeners(Team team) {
        ArrayList<Listener> result = new ArrayList<>(team.getUsers().size());

        for (User user : team.getUsers()) {
            result.addAll(user.getListeners());
        }

        return result;
    }

    private Collection<Listener> getAllTeamListeners() {
        ArrayList<Listener> result = new ArrayList<>(teamsInGroup.size());

        for (Team team : teamsInGroup.values()) {
            result.addAll(team.getListeners());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private final List<Listener> createDefaultAndMapListeners(Map<String, Listener>... extra) {
        HashMap<String, Listener> clone = (HashMap<String, Listener>) defaultListeners.clone();

        for (Map<String, Listener> map : extra) {
            clone.putAll(map);
        }

        List<Listener> result = new ArrayList<>(clone.values());
        result.add(gameGroupListener);

        return result;
    }

    public void changeMap(String mapName) {
        GameMapInfo mapInfo = game.getMapInfo(mapName);
        Validate.notNull(mapInfo, "The map " + mapName + " does not exist");

        changeMap(mapInfo);
    }

    public GameMap getCurrentMap() {
        return currentMap;
    }

    @Override
    public ConfigurationSection getSharedObject(String name) {
        ConfigurationSection result = null;
        if (currentMap != null) result = currentMap.getSharedObject(name);
        return result != null ? result : game.getSharedObject(name);
    }

    @Override
    public LanguageLookup getLanguageLookup() {
        return this;
    }

    public Team getTeam(String name) {
        return getTeam(getTeamIdentifier(name));
    }

    public Team getTeam(TeamIdentifier identifier) {
        return teamsInGroup.get(identifier);
    }

    public TeamIdentifier getTeamIdentifier(String name) {
        return teamIdentifiers.get(name);
    }

    public Countdown getCountdown() {
        return countdown;
    }

    @Override
    public User getUser(UUID uuid) {
        return usersInGroup.get(uuid);
    }

    public void userEvent(UserEvent event) {
        if (event.getUser().getTeam() != null) {
            EventExecutor.executeEvent(event,
                    getListeners(event.getUser().getListeners(), event.getUser().getTeam().getListeners()));
        } else {
            EventExecutor.executeEvent(event, getListeners(event.getUser().getListeners()));
        }
    }

    public void teamEvent(TeamEvent event) {
        EventExecutor.executeEvent(event, event.getTeam().getListeners(), getAllUsersInTeamListeners(event.getTeam()));
    }

    @Override
    public ConfigurationSection loadConfig(String name) {
        return game.loadConfig(name);
    }

    @Override
    public LangFile loadLangFile(String path) {
        return game.loadLangFile(path);
    }

    @Override
    public File getDataFolder() {
        return game.getDataFolder();
    }

    public void changeGameState(String gameStateName) {
        GameState gameState = gameStates.get(gameStateName);
        if (gameState == null) throw new IllegalArgumentException("Unknown game state name: " + gameStateName);

        changeGameState(gameState);
    }

    public void startCountdown(String name, String localeStub, int seconds) {
        if (countdown != null) countdown.cancel();

        countdown = new Countdown(name, localeStub, seconds);
        countdown.start(this);
    }

    public void stopCountdown() {
        countdown.cancel();
    }

    public CustomItem getCustomItem(String name) {
        CustomItem item = null;
        if (currentMap != null) item = currentMap.getCustomItem(name);
        return item != null ? item : game.getCustomItem(name);
    }

    public CustomItem getCustomItem(int identifier) {
        CustomItem item = null;
        if (currentMap != null) item = currentMap.getCustomItem(identifier);
        return item != null ? item : game.getCustomItem(identifier);
    }

    @Override
    public Schematic getSchematic(String name) {
        Schematic schem = null;
        if (currentMap != null) schem = currentMap.getSchematic(name);
        return schem != null ? schem : game.getSchematic(name);
    }

    public void gameEvent(GameEvent event) {
        EventExecutor.executeEvent(event, getListeners(getAllUserListeners(), getAllTeamListeners()));
    }

    public void unload() {
        currentMap.unloadMap();
    }

    public void bindTaskToCurrentGameState(GameTask task) {
        gameStateTaskList.addTask(task);
    }

    public void bindTaskToCurrentMap(GameTask task) {
        if (currentMap == null) throw new RuntimeException("No GameMap to bind task to");
        currentMap.bindTaskToMap(task);
    }

    @Override
    public String getLocale(String name) {
        if (currentMap != null && currentMap.hasLocale(name)) return currentMap.getLocale(name);
        else return game.getLocale(name);
    }

    @Override
    public boolean hasLocale(String name) {
        return (currentMap != null && currentMap.hasLocale(name) || game.hasLocale(name));
    }

    public Game getGame() {
        return game;
    }

    @Override
    public void sendLocale(String locale, Object... args) {
        sendMessage(getLocale(locale, args));
    }

    @Override
    public void sendMessage(String message) {
        sendMessageNoPrefix(getChatPrefix() + message);
    }

    @Override
    public String getLocale(String name, Object... args) {
        if (currentMap != null && currentMap.hasLocale(name)) return currentMap.getLocale(name, args);
        else return game.getLocale(name, args);
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        for (User user : usersInGroup.values()) {
            user.sendMessageNoPrefix(message);
        }
    }

    public String getChatPrefix() {
        return game.getChatPrefix();
    }

    @Override
    public void sendLocaleNoPrefix(String locale, Object... args) {
        sendMessageNoPrefix(getLocale(locale, args));
    }

    @Override
    public GameTask doInFuture(GameRunnable task) {
        GameTask gameTask = game.doInFuture(task);

        gameGroupTaskList.addTask(gameTask);
        return gameTask;
    }

    @Override
    public GameTask doInFuture(GameRunnable task, int delay) {
        GameTask gameTask = game.doInFuture(task, delay);

        gameGroupTaskList.addTask(gameTask);
        return gameTask;
    }

    @Override
    public GameTask repeatInFuture(GameRunnable task, int delay, int period) {
        GameTask gameTask = game.repeatInFuture(task, delay, period);

        gameGroupTaskList.addTask(gameTask);
        return gameTask;
    }

    @Override
    public void cancelAllTasks() {
        gameGroupTaskList.cancelAllTasks();
    }

    public void setDefaultListeners(HashMap<String, Listener> defaultListeners) {
        this.defaultListeners = defaultListeners;

        if (currentMap != null) defaultAndMapListeners = createDefaultAndMapListeners(currentMap.getListenerMap());
        else defaultAndMapListeners = createDefaultAndMapListeners();
    }

    public boolean hasActiveCountdown() {
        return countdown != null;
    }

    public boolean hasActiveCountdown(String name) {
        return countdown != null && countdown.getName().equals(name);
    }

    public int getUserCount() {
        return getUsers().size();
    }

    public Collection<User> getUsers() {
        return usersInGroup.values();
    }

    public void setTeamIdentifiers(List<TeamIdentifier> identifiers) {
        for (TeamIdentifier identifier : identifiers) {
            teamIdentifiers.put(identifier.getName(), identifier);
        }
        recreateTeamObjects();
    }

    public void recreateTeamObjects() {
        for (User user : getUsers()) {
            user.setTeam(null);
        }

        teamsInGroup.clear();
        for (TeamIdentifier teamIdentifier : teamIdentifiers.values()) {
            teamsInGroup.put(teamIdentifier, createTeam(teamIdentifier));
        }
    }

    public Kit getKit(String name) {
        return kits.get(name);
    }

    private Team createTeam(TeamIdentifier teamIdentifier) {
        return new Team(teamIdentifier, this);
    }

    @Override
    public <B extends Metadata> B getMetadata(Class<? extends B> clazz) {
        return metadataMap.getInstance(clazz);
    }

    @Override
    public <B extends Metadata> void setMetadata(B metadata) {
        metadataMap.put(metadata.getMetadataClass(), metadata);
    }

    @Override
    public boolean hasMetadata(Class<? extends Metadata> clazz) {
        return metadataMap.containsKey(clazz);
    }

    public void setKits(List<Kit> kits) {
        this.kits.clear();

        for(Kit kit : kits) {
            this.kits.put(kit.getName(), kit);
        }
    }

    private class GameGroupListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void eventUserJoin(UserJoinEvent event) {
            if(event.getReason() != UserJoinEvent.JoinReason.JOINED_SERVER) return;

            usersInGroup.put(event.getUser().getUuid(), event.getUser());

            currentMap.teleportUser(event.getUser());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void eventUserQuit(UserQuitEvent event) {
            if (event.getRemoveUser()) {
                event.getUser().setTeam(null);
                usersInGroup.remove(event.getUser().getUuid());
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void eventCountdownFinished(CountdownFinishedEvent event) {
            if (event.getCountdown().getSecondsRemaining() > 0) return;
            if (event.getCountdown() != countdown) return;

            countdown = null;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void eventGameStateChange(GameStateChangedEvent event) {
            Iterator<Metadata> iterator = metadataMap.values().iterator();

            while (iterator.hasNext()) {
                Metadata metadata = iterator.next();

                if (metadata.removeOnGameStateChange(event)) iterator.remove();
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void eventMapChange(MapChangedEvent event) {
            Iterator<Metadata> iterator = metadataMap.values().iterator();

            while (iterator.hasNext()) {
                Metadata metadata = iterator.next();

                if (metadata.removeOnMapChange(event)) iterator.remove();
            }
        }
    }
}
