package com.ithinkrok.minigames;

import com.ithinkrok.minigames.event.UserBreakBlockEvent;
import com.ithinkrok.minigames.event.UserJoinEvent;
import com.ithinkrok.minigames.event.UserPlaceBlockEvent;
import com.ithinkrok.minigames.lang.LanguageLookup;
import com.ithinkrok.minigames.lang.MultipleLanguageLookup;
import com.ithinkrok.minigames.map.GameMapInfo;
import com.ithinkrok.minigames.util.ResourceHandler;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by paul on 31/12/15.
 */
public abstract class Game<U extends User<U, T, G, M>, T extends Team<U, T, G>, G extends GameGroup<U, T, G, M>,
        M extends Game<U, T, G, M>> implements Listener, LanguageLookup {

    private ConcurrentMap<UUID, U> usersInServer = new ConcurrentHashMap<>();
    private List<G> gameGroups = new ArrayList<>();

    private Constructor<G> gameGroupConstructor;
    private Constructor<T> teamConstructor;
    private Constructor<U> userConstructor;

    private Plugin plugin;

    private G spawnGameGroup;

    private ConfigurationSection config;

    private MultipleLanguageLookup multipleLanguageLookup = new MultipleLanguageLookup();

    private Map<String, GameMapInfo> maps = new HashMap<>();
    private String startMapName;

    public Game(Plugin plugin, Class<G> gameGroupClass, Class<T> teamClass, Class<U> userClass) {
        this.plugin = plugin;

        try {
            teamConstructor = teamClass.getConstructor(TeamColor.class, gameGroupClass);
            userConstructor =
                    userClass.getConstructor(getClass(), gameGroupClass, teamClass, UUID.class, LivingEntity.class);
            gameGroupConstructor = gameGroupClass.getConstructor(getClass(), teamConstructor.getClass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to get constructors for data classes", e);
        }

    }

    public abstract List<GameState> getGameStates();

    private G createGameGroup() {
        try {
            return gameGroupConstructor.newInstance(this, teamConstructor);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to construct GameGroup", e);
        }
    }

    private U createUser(G gameGroup, T team, UUID uuid, LivingEntity entity) {
        try {
            U user = userConstructor.newInstance(this, gameGroup, team, uuid, entity);
            usersInServer.put(uuid, user);
            return user;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to construct User", e);
        }
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfig() {
        plugin.reloadConfig();

        config = plugin.getConfig();

        reloadMaps();
    }

    private void reloadMaps() {
        maps.clear();

        startMapName = config.getString("start_map");
        loadMapInfo(startMapName);
    }

    private void loadMapInfo(String mapName) {
        maps.put(mapName, new GameMapInfo(this, mapName));
    }

    public GameMapInfo getStartMapInfo(){
        return maps.get(startMapName);
    }

    public ConfigurationSection loadConfig(String path) {
        return ResourceHandler.getConfigResource(plugin, path);
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    @EventHandler
    public void eventPlayerJoined(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();

        U user = getUser(player.getUniqueId());
        G gameGroup;

        if(user != null) {
            gameGroup = user.getGameGroup();
        } else {
            if(spawnGameGroup == null) {
                spawnGameGroup = createGameGroup();
                gameGroups.add(spawnGameGroup);
            }

            gameGroup = spawnGameGroup;
            user = createUser(gameGroup, null, player.getUniqueId(), player);
        }

        gameGroup.eventUserJoinedAsPlayer(new UserJoinEvent<>(user, UserJoinEvent.JoinReason.JOINED_SERVER));
    }

    @EventHandler
    public void eventBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);

        U user = getUser(event.getPlayer().getUniqueId());
        user.getGameGroup().eventBlockBreak(new UserBreakBlockEvent<>(user, event));
    }

    @EventHandler
    public void eventBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);

        U user = getUser(event.getPlayer().getUniqueId());
        user.getGameGroup().eventBlockPlace(new UserPlaceBlockEvent<>(user, event));
    }

    private U getUser(UUID uuid) {
        return usersInServer.get(uuid);
    }

    public String getChatPrefix(){
        return ChatColor.GRAY + "[" + ChatColor.DARK_AQUA + "ColonyWars" + ChatColor.GRAY + "] " + ChatColor.YELLOW;
    }

    @Override
    public boolean hasLocale(String name) {
        return multipleLanguageLookup.hasLocale(name);
    }

    @Override
    public String getLocale(String name) {
        return multipleLanguageLookup.getLocale(name);
    }

    @Override
    public String getLocale(String name, Object... args) {
        return multipleLanguageLookup.getLocale(name, args);
    }

    public void unload() {
        gameGroups.forEach(GameGroup::unload);
    }
}
