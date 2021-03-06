package com.ithinkrok.cw.item;

import com.ithinkrok.minigames.event.ListenerLoadedEvent;
import com.ithinkrok.minigames.event.MinigamesEventHandler;
import com.ithinkrok.minigames.event.user.world.UserInteractEvent;
import com.ithinkrok.minigames.util.math.Calculator;
import com.ithinkrok.minigames.util.math.ExpressionCalculator;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

/**
 * Created by paul on 21/01/16.
 */
public class ExplosionWand implements Listener {

    private int maxRange;
    private Calculator explosionPower;

    @MinigamesEventHandler
    public void onListenerLoaded(ListenerLoadedEvent event) {
        ConfigurationSection config = event.getConfig();

        maxRange = config.getInt("max_range", 100);
        explosionPower = new ExpressionCalculator(config.getString("explosion_power"));
    }

    @MinigamesEventHandler
    public void onInteract(UserInteractEvent event) {
        if (event.getBlockFace() == null) return;

        Block target = event.getUser().rayTraceBlocks(maxRange);
        if (target == null) return;

        BlockFace mod = event.getBlockFace();
        Location loc = target.getLocation().clone();
        loc.add(mod.getModX() + 0.5, mod.getModY() + 0.5, mod.getModZ() + 0.5);

        event.getUser()
                .createExplosion(loc, (float) explosionPower.calculate(event.getUser().getUpgradeLevels()), false, 0);

        event.setStartCooldownAfterAction(true);
    }
}
