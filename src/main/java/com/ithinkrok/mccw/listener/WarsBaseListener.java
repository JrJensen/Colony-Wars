package com.ithinkrok.mccw.listener;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.data.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * Created by paul on 08/11/15.
 * <p>
 * Base listener for ColonyWars. Listens both during the game and in the lobby
 */
public class WarsBaseListener implements Listener {

    protected final WarsPlugin plugin;

    public WarsBaseListener(WarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        User user = new User(plugin, event.getPlayer());
        plugin.setUser(event.getPlayer(), user);

        user.messageLocale("server.welcome");

        user.getPlayer().getInventory().clear();

        for(User other : plugin.getUsers()){
            if(other == user) continue;
            if(!other.isCloaked()) continue;

            user.getPlayer().hidePlayer(other.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getUser(event.getPlayer()).saveStats();
        plugin.setUser(event.getPlayer(), null);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) event.setCancelled(true);
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        if (event.toThunderState()) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().substring(1).split(" ")[0].toLowerCase();

        switch (cmd) {
            case "kill":
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void onPlayerAchievementAwarded(PlayerAchievementAwardedEvent event){
        event.setCancelled(true);
    }

}
