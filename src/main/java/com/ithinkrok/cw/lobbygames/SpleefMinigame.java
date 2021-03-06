package com.ithinkrok.cw.lobbygames;

import com.ithinkrok.minigames.User;
import com.ithinkrok.minigames.event.ListenerLoadedEvent;
import com.ithinkrok.minigames.event.MinigamesEventHandler;
import com.ithinkrok.minigames.event.user.game.UserTeleportEvent;
import com.ithinkrok.minigames.event.user.state.UserDamagedEvent;
import com.ithinkrok.minigames.event.user.world.UserBreakBlockEvent;
import com.ithinkrok.minigames.event.user.world.UserInteractEvent;
import com.ithinkrok.minigames.event.user.world.UserInteractWorldEvent;
import com.ithinkrok.minigames.util.BoundingBox;
import com.ithinkrok.minigames.util.ConfigUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Created by paul on 01/01/16.
 */
public class SpleefMinigame implements Listener {

    private Material spadeMaterial;

    private Map<Vector, Arena> queueButtons = new HashMap<>();
    private Map<UUID, Arena> queueLookups = new HashMap<>();
    private Map<UUID, Arena> gameLookups = new HashMap<>();

    @MinigamesEventHandler
    public void configure(ListenerLoadedEvent event) {
        spadeMaterial = Material.matchMaterial(event.getConfig().getString("spade", "IRON_SPADE"));

        ConfigurationSection arenasConfig = event.getConfig().getConfigurationSection("arenas");
        for(String key : arenasConfig.getKeys(false)){
            ConfigurationSection arenaConfig = arenasConfig.getConfigurationSection(key);

            Arena arena = new Arena(arenaConfig);

            for(Vector button : arena.queueButtons){
                queueButtons.put(button, arena);
            }
        }
    }

    @MinigamesEventHandler
    public void onUserTeleport(UserTeleportEvent event) {
        Arena arena = gameLookups.get(event.getUser().getUuid());

        if(arena == null) return;

        double x = event.getUser().getLocation().getX();
        double z = event.getUser().getLocation().getZ();

        if(arena.checkUserInBounds(x, z)) return;

        arena.spleefUserKilled(event.getUser(), false);
    }

    @MinigamesEventHandler(priority = MinigamesEventHandler.HIGH)
    public void onUserBreakBlock(UserBreakBlockEvent event){
        if(event.getBlock().getType() == Material.SNOW_BLOCK) event.setCancelled(false);
    }

    @MinigamesEventHandler
    public void onUserDamaged(UserDamagedEvent event) {
        if(event.getDamageCause() != EntityDamageEvent.DamageCause.LAVA) return;

        Arena arena = gameLookups.get(event.getUser().getUuid());
        if(arena == null) return;

        event.setCancelled(true);

        arena.spleefUserKilled(event.getUser(), true);
    }

    @MinigamesEventHandler
    public void onUserInteractWorld(UserInteractWorldEvent event) {
        if(!event.hasBlock()) return;

        switch(event.getClickedBlock().getType()) {
            case SNOW_BLOCK:
                if(event.getInteractType() == UserInteractEvent.InteractType.LEFT_CLICK) event.setCancelled(false);
                return;
            case STONE_BUTTON:
                if(event.getInteractType() != UserInteractEvent.InteractType.RIGHT_CLICK) return;

                Arena arena = queueButtons.get(event.getClickedBlock().getLocation().toVector());

                arena.addUserToQueue(event.getUser());
        }
    }

    private class Arena {
        private List<Vector> queueButtons;
        private List<Vector> spawnLocations;
        private Vector exitLocation;
        private BoundingBox snowBounds;
        private int extraRadius;

        private List<UUID> usersInSpleef = new ArrayList<>();
        private LinkedHashSet<UUID> queue = new LinkedHashSet<>();

        public Arena(ConfigurationSection config) {
            queueButtons = ConfigUtils.getVectorList(config, "queue_buttons");
            spawnLocations = ConfigUtils.getVectorList(config, "spawn_locations");
            exitLocation = ConfigUtils.getVector(config, "exit_location");
            snowBounds = ConfigUtils.getBounds(config, "snow");
            extraRadius = config.getInt("extra_radius");
        }

        public boolean checkUserInBounds(double x, double z) {
            if(x + extraRadius < snowBounds.min.getX() || x - extraRadius > snowBounds.max.getX()) return false;

            return z + extraRadius >= snowBounds.min.getZ() && z - extraRadius <= snowBounds.max.getZ();
        }

        public void spleefUserKilled(User user, boolean teleport) {
            if(!usersInSpleef.remove(user.getUuid())) return;
            gameLookups.remove(user.getUuid());

            removeUserFromSpleef(user, teleport);
            user.getGameGroup().sendLocale("spleef.loser", user.getFormattedName());

            if(usersInSpleef.size() == 1) {
                User winner = user.getUser(usersInSpleef.remove(0));
                gameLookups.remove(winner.getUuid());

                removeUserFromSpleef(winner, true);
                user.getGameGroup().sendLocale("spleef.winner", winner.getFormattedName());

                tryStartGame(user);
            }

        }

        public void resetArena(User aUser) {
            World world = aUser.getLocation().getWorld();

            for(int x = snowBounds.min.getBlockX(); x <= snowBounds.max.getBlockX(); ++x) {
                for(int y = snowBounds.min.getBlockY(); y <= snowBounds.max.getBlockY(); ++y) {
                    for(int z = snowBounds.min.getBlockZ(); z <= snowBounds.max.getBlockZ(); ++z) {
                        world.getBlockAt(x, y, z).setType(Material.SNOW_BLOCK);
                    }
                }
            }
        }

        public void addUserToQueue(User user) {
            if(gameLookups.containsKey(user.getUuid())) return;

            boolean success = queue.add(user.getUuid());
            if(!success){
                user.sendLocale("spleef.queue.already_joined");
                return;
            }

            Arena old = queueLookups.get(user.getUuid());
            if(old != null){
                old.queue.remove(user.getUuid());
                user.sendLocale("spleef.queue.remove");
            }

            queueLookups.put(user.getUuid(), this);

            user.sendLocale("spleef.queue.join");

            tryStartGame(user);
        }

        public void tryStartGame(User aUser) {
            if(!usersInSpleef.isEmpty() || queue.size() < spawnLocations.size()) return;

            resetArena(aUser);

            Iterator<UUID> iterator = queue.iterator();
            for(Vector spawn : spawnLocations) {
                UUID joiningUUID = iterator.next();
                iterator.remove();

                queueLookups.remove(joiningUUID);

                User joining = aUser.getUser(joiningUUID);

                joining.teleport(spawn);
                joining.getInventory().addItem(new ItemStack(spadeMaterial));
                joining.setGameMode(GameMode.SURVIVAL);
                usersInSpleef.add(joining.getUuid());
                gameLookups.put(joining.getUuid(), this);
            }

            aUser.getGameGroup().sendLocale("spleef.begin");
        }

        @SuppressWarnings("unchecked")
        private void removeUserFromSpleef(User user, boolean teleport) {
            if(teleport) user.teleport(exitLocation);

            user.getInventory().remove(spadeMaterial);

            user.setGameMode(GameMode.ADVENTURE);

            user.setFireTicks(null, 0);
        }
    }
}
