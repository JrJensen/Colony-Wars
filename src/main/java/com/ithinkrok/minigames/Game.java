package com.ithinkrok.minigames;

import com.ithinkrok.minigames.event.user.game.UserJoinEvent;
import com.ithinkrok.minigames.event.user.game.UserQuitEvent;
import com.ithinkrok.minigames.event.user.inventory.UserInventoryClickEvent;
import com.ithinkrok.minigames.event.user.inventory.UserInventoryCloseEvent;
import com.ithinkrok.minigames.event.user.state.UserAttackedEvent;
import com.ithinkrok.minigames.event.user.state.UserDamagedEvent;
import com.ithinkrok.minigames.event.user.state.UserFoodLevelChangeEvent;
import com.ithinkrok.minigames.event.user.world.*;
import com.ithinkrok.minigames.item.CustomItem;
import com.ithinkrok.minigames.item.IdentifierMap;
import com.ithinkrok.minigames.lang.LangFile;
import com.ithinkrok.minigames.lang.LanguageLookup;
import com.ithinkrok.minigames.lang.MultipleLanguageLookup;
import com.ithinkrok.minigames.map.GameMapInfo;
import com.ithinkrok.minigames.task.GameRunnable;
import com.ithinkrok.minigames.task.GameTask;
import com.ithinkrok.minigames.task.TaskScheduler;
import com.ithinkrok.minigames.user.UserResolver;
import com.ithinkrok.minigames.util.EntityUtils;
import com.ithinkrok.minigames.util.io.ConfigHolder;
import com.ithinkrok.minigames.util.io.ConfigParser;
import com.ithinkrok.minigames.util.io.FileLoader;
import com.ithinkrok.minigames.util.io.ResourceHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by paul on 31/12/15.
 */
@SuppressWarnings("unchecked")
public abstract class Game<U extends User<U, T, G, M>, T extends Team<U, T, G>, G extends GameGroup<U, T, G, M>, M extends Game<U, T, G, M>>
        implements LanguageLookup, TaskScheduler, UserResolver<U>, FileLoader, ConfigHolder {

    private ConcurrentMap<UUID, U> usersInServer = new ConcurrentHashMap<>();
    private List<G> gameGroups = new ArrayList<>();

    private Constructor<G> gameGroupConstructor;
    private Constructor<T> teamConstructor;
    private Constructor<U> userConstructor;

    private Plugin plugin;

    private G spawnGameGroup;

    private ConfigurationSection config;

    private MultipleLanguageLookup multipleLanguageLookup = new MultipleLanguageLookup();

    private IdentifierMap<CustomItem> customItemIdentifierMap = new IdentifierMap<>();

    private HashMap<String, Listener> defaultListeners = new HashMap<>();
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

        unloadDefaultWorlds();
    }

    private void unloadDefaultWorlds() {
        if (Bukkit.getWorlds().size() != 1) System.out.println("You should disable the nether/end worlds to save RAM!");

        for (World world : Bukkit.getWorlds()) {
            world.setKeepSpawnInMemory(false);

            for (Chunk chunk : world.getLoadedChunks()) {
                chunk.unload(false, true);
            }
        }
    }

    @Override
    public void addListener(String name, Listener listener) {
        defaultListeners.put(name, listener);
    }

    @Override
    public void addLanguageLookup(LanguageLookup languageLookup) {
        multipleLanguageLookup.addLanguageLookup(languageLookup);
    }

    @Override
    public void addCustomItem(CustomItem item) {
        customItemIdentifierMap.put(item.getName(), item);
    }

    public CustomItem getCustomItem(String name) {
        return customItemIdentifierMap.get(name);
    }

    public CustomItem getCustomItem(int identifier) {
        return customItemIdentifierMap.get(identifier);
    }

    public abstract List<GameState> getGameStates();

    public void registerListeners() {
        Listener listener = new GameListener();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public void reloadConfig() {
        plugin.reloadConfig();

        config = plugin.getConfig();

        reloadMaps();

        defaultListeners.clear();
        multipleLanguageLookup = new MultipleLanguageLookup();
        customItemIdentifierMap.clear();

        ConfigParser.parseConfig(this, this, this, "config.yml", config);
    }

    private void reloadMaps() {
        maps.clear();

        startMapName = config.getString("start_map");
        loadMapInfo(startMapName);
    }


    private void loadMapInfo(String mapName) {
        maps.put(mapName, new GameMapInfo(this, mapName));
    }

    @Override
    public LangFile loadLangFile(String path) {
        return new LangFile(ResourceHandler.getPropertiesResource(plugin, path));
    }

    public GameMapInfo getStartMapInfo() {
        return maps.get(startMapName);
    }

    @Override
    public ConfigurationSection loadConfig(String path) {
        return ResourceHandler.getConfigResource(plugin, path);
    }

    public ConfigurationSection getConfig() {
        return config;
    }


    @Override
    public U getUser(UUID uuid) {
        return usersInServer.get(uuid);
    }

    private G createGameGroup() {
        try {
            G gameGroup = gameGroupConstructor.newInstance(this, teamConstructor);
            gameGroup.setDefaultListeners((HashMap<String, Listener>) defaultListeners.clone());
            return gameGroup;
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

    public String getChatPrefix() {
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

    @Override
    public GameTask doInFuture(GameRunnable task) {
        return doInFuture(task, 1);
    }

    @Override
    public GameTask doInFuture(GameRunnable task, int delay) {
        GameTask gameTask = new GameTask(task);

        gameTask.schedule(plugin, delay);
        return gameTask;
    }

    @Override
    public GameTask repeatInFuture(GameRunnable task, int delay, int period) {
        GameTask gameTask = new GameTask(task);

        gameTask.schedule(plugin, delay, period);
        return gameTask;
    }

    @Override
    public void cancelAllTasks() {
        throw new RuntimeException("You cannot cancel all game tasks");
    }

    public void makeEntityRepresentUser(User user, Entity entity) {
        entity.setMetadata("rep", new FixedMetadataValue(plugin, user.getUuid()));
    }

    private class GameListener implements Listener {

        @EventHandler
        public void eventPlayerJoined(PlayerJoinEvent event) {
            event.setJoinMessage(null);

            Player player = event.getPlayer();

            U user = getUser(player.getUniqueId());
            G gameGroup;

            if (user != null) {
                gameGroup = user.getGameGroup();
            } else {
                if (spawnGameGroup == null) {
                    spawnGameGroup = createGameGroup();
                    gameGroups.add(spawnGameGroup);
                }

                gameGroup = spawnGameGroup;
                user = createUser(gameGroup, null, player.getUniqueId(), player);
            }

            gameGroup.eventUserJoinedAsPlayer(new UserJoinEvent<>(user, UserJoinEvent.JoinReason.JOINED_SERVER));
        }

        @EventHandler
        public void eventPlayerQuit(PlayerQuitEvent event) {
            event.setQuitMessage(null);

            U user = getUser(event.getPlayer().getUniqueId());

            UserQuitEvent<U> userEvent = new UserQuitEvent<>(user, UserQuitEvent.QuitReason.QUIT_SERVER);
            user.getGameGroup().userQuitEvent(userEvent);

            if (userEvent.getRemoveUser()) {
                usersInServer.remove(event.getPlayer().getUniqueId());
                user.cancelAllTasks();
            }
        }

        @EventHandler
        public void eventPlayerDropItem(PlayerDropItemEvent event) {
            U user = getUser(event.getPlayer().getUniqueId());
            user.getGameGroup().userEvent(new UserDropItemEvent<>(user, event));
        }

        @EventHandler
        public void eventPlayerPickupItem(PlayerPickupItemEvent event) {
            U user = getUser(event.getPlayer().getUniqueId());
            user.getGameGroup().userEvent(new UserPickupItemEvent<>(user, event));
        }

        @EventHandler
        public void eventPlayerInteractWorld(PlayerInteractEvent event) {
            U user = getUser(event.getPlayer().getUniqueId());
            user.getGameGroup().userEvent(new UserInteractWorldEvent<>(user, event));
        }

        @EventHandler
        public void eventBlockBreak(BlockBreakEvent event) {
            U user = getUser(event.getPlayer().getUniqueId());
            user.getGameGroup().userEvent(new UserBreakBlockEvent<>(user, event));
        }

        @EventHandler
        public void eventBlockPlace(BlockPlaceEvent event) {
            U user = getUser(event.getPlayer().getUniqueId());
            user.getGameGroup().userEvent(new UserPlaceBlockEvent<>(user, event));
        }

        @EventHandler(priority = EventPriority.LOW)
        public void eventEntityDamagedByEntity(EntityDamageByEntityEvent event) {
            U attacker = EntityUtils.getRepresentingUser(Game.this, event.getDamager());
            if (attacker == null) return;

            U attacked = EntityUtils.getRepresentingUser(attacker, event.getEntity());
            boolean representing = !attacker.equals(EntityUtils.getActualUser(Game.this, event.getDamager()));

            attacker.getGameGroup().userEvent(new UserAttackEvent<>(attacker, event, attacked, representing));
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void eventEntityDamaged(EntityDamageEvent event) {
            U attacked = EntityUtils.getActualUser(Game.this, event.getEntity());
            if (attacked == null) return;

            if (event instanceof EntityDamageByEntityEvent) {
                U attacker = EntityUtils.getRepresentingUser(attacked, event.getEntity());
                attacked.getGameGroup()
                        .userEvent(new UserAttackedEvent<>(attacked, (EntityDamageByEntityEvent) event, attacker));
            } else {
                attacked.getGameGroup().userEvent(new UserDamagedEvent<>(attacked, event));
            }


        }

        @EventHandler
        public void eventPlayerFoodLevelChange(FoodLevelChangeEvent event) {
            U user = getUser(event.getEntity().getUniqueId());
            user.getGameGroup().userEvent(new UserFoodLevelChangeEvent<>(user, event));
        }

        @EventHandler
        public void eventPlayerInteractEntity(PlayerInteractEntityEvent event) {
            U user = getUser(event.getPlayer().getUniqueId());
            user.getGameGroup().userEvent(new UserRightClickEntityEvent<>(user, event));
        }

        @EventHandler
        public void eventPlayerInventoryClick(InventoryClickEvent event) {
            U user = getUser(event.getWhoClicked().getUniqueId());
            user.getGameGroup().userEvent(new UserInventoryClickEvent<>(user, event));
        }

        @EventHandler
        public void eventPlayerInventoryClose(InventoryCloseEvent event) {
            U user = getUser(event.getPlayer().getUniqueId());
            user.getGameGroup().userEvent(new UserInventoryCloseEvent<>(user, event));
        }
    }
}
