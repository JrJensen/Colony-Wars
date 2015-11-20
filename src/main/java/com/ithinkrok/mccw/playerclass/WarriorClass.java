package com.ithinkrok.mccw.playerclass;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.event.UserInteractEvent;
import com.ithinkrok.mccw.playerclass.items.ClassItem;
import com.ithinkrok.mccw.playerclass.items.LinearCalculator;
import com.ithinkrok.mccw.strings.Buildings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wolf;

/**
 * Created by paul on 15/11/15.
 * <p>
 * Handles the warrior class
 */
public class WarriorClass extends ClassItemClassHandler {

    public WarriorClass(WarsPlugin plugin, FileConfiguration config) {
        super(new ClassItem(Material.IRON_SWORD).withUpgradeBuildings(Buildings.BLACKSMITH)
                        .withUnlockOnBuildingBuild(true).withEnchantmentEffects(
                        new ClassItem.EnchantmentEffect(Enchantment.DAMAGE_ALL, "sharpness",
                                new LinearCalculator(0, 1)),
                        new ClassItem.EnchantmentEffect(Enchantment.KNOCKBACK, "knockback", new LinearCalculator(0, 1)))
                        .withUpgradables(new ClassItem.Upgradable("sharpness", plugin.getLocale("upgrades.sharpness.name"), 2,
                                        configArrayCalculator(config, "costs.warrior.sharpness", 2)),
                                new ClassItem.Upgradable("knockback", plugin.getLocale("upgrades.knockback.name"), 2,
                                        configArrayCalculator(config, "costs.warrior.knockback", 2))),
                new ClassItem(Material.GOLD_HELMET, plugin.getLocale("items.wolf-wand.name"))
                        .withUpgradeBuildings(Buildings.BLACKSMITH).withUnlockOnBuildingBuild(true)
                        .withRightClickAction(new WolfWand())
                        .withRightClickCooldown("wolf", new LinearCalculator(120, -30),
                                plugin.getLocale("cooldowns.wolf.finished")).withUpgradables(
                        new ClassItem.Upgradable("wolf", plugin.getLocale("upgrades.wolf-wand.name"), 2,
                                configArrayCalculator(config, "costs.warrior.wolf", 2))));
    }

    private static class WolfWand implements ClassItem.InteractAction {

        @Override
        public boolean onInteractWorld(UserInteractEvent event) {
            if (!event.hasBlock()) return false;

            Location target = event.getClickedBlock().getLocation();
            BlockFace face = event.getBlockFace();
            target = target.clone().add(face.getModX(), face.getModY(), face.getModZ());

            Wolf wolf = (Wolf) target.getWorld().spawnEntity(target, EntityType.WOLF);
            wolf.setCollarColor(event.getUser().getTeamColor().dyeColor);
            wolf.setOwner(event.getPlayer());

            return true;
        }
    }

}
