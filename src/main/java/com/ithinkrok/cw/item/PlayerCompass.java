package com.ithinkrok.cw.item;

import com.ithinkrok.minigames.User;
import com.ithinkrok.minigames.event.ListenerLoadedEvent;
import com.ithinkrok.minigames.event.MinigamesEventHandler;
import com.ithinkrok.minigames.event.user.world.UserInteractEvent;
import com.ithinkrok.minigames.lang.LanguageLookup;
import com.ithinkrok.minigames.util.InventoryUtils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.Listener;

import java.util.Objects;

/**
 * Created by paul on 15/01/16.
 */
public class PlayerCompass implements Listener {

    private String nameLocale, locatingLocale, noPlayerLocale, orientedLocale;

    private int locatingTime;

    @MinigamesEventHandler
    public void onListenerLoaded(ListenerLoadedEvent event) {
        ConfigurationSection config;
        if (event.hasConfig()) config = event.getConfig();
        else config = new MemoryConfiguration();

        nameLocale = config.getString("display_name_locale", "player_compass.name");
        locatingLocale = config.getString("locating_locale", "player_compass.locating");
        noPlayerLocale = config.getString("no_player_locale", "player_compass.no_player");
        orientedLocale = config.getString("oriented_locale", "player_compass.oriented");

        locatingTime = config.getInt("locating_time", 3) * 20;
    }


    @MinigamesEventHandler
    public void onInteract(UserInteractEvent event) {
        InventoryUtils.setItemName(event.getItem(), event.getUser().getLanguageLookup().getLocale(locatingLocale));

        event.getUser().doInFuture(task -> {
            Location closest = null;
            double minDist = 9999999999999d;
            String closestName = null;

            for (User user : event.getUserGameGroup().getUsers()) {
                if (!user.isInGame() || Objects.equals(event.getUser().getTeamIdentifier(), user.getTeamIdentifier()))
                    continue;

                double dist = event.getUser().getLocation().distanceSquared(user.getLocation());

                if (dist > minDist) continue;
                minDist = dist;
                closest = user.getLocation();
                closestName = user.getName();
            }

            LanguageLookup lookup = event.getUser().getLanguageLookup();

            if (closest != null) event.getUser().setCompassTarget(closest);
            else closestName = lookup.getLocale(noPlayerLocale);

            InventoryUtils.setItemNameAndLore(event.getItem(), lookup.getLocale(nameLocale),
                    lookup.getLocale(orientedLocale, closestName));
        }, locatingTime);

        event.setStartCooldownAfterAction(true);
    }
}
