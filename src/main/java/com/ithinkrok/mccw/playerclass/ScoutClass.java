package com.ithinkrok.mccw.playerclass;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.data.User;
import com.ithinkrok.mccw.enumeration.PlayerClass;
import com.ithinkrok.minigames.TeamColor;
import com.ithinkrok.mccw.event.UserBeginGameEvent;
import com.ithinkrok.mccw.event.UserInteractEvent;
import com.ithinkrok.mccw.playerclass.items.ClassItem;
import com.ithinkrok.mccw.playerclass.items.LinearCalculator;
import com.ithinkrok.mccw.strings.Buildings;
import com.ithinkrok.mccw.util.item.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Created by paul on 05/11/15.
 * <p>
 * Handles the Scout class
 */
public class ScoutClass extends ClassItemClassHandler {

    public ScoutClass(WarsPlugin plugin, PlayerClass playerClass) {
        super(new ClassItem(plugin, playerClass.getName(), Material.WOOD_SWORD)
                        .withUpgradeBuildings(Buildings.LUMBERMILL).withUnlockOnBuildingBuild(true).withEnchantmentEffects(
                        new ClassItem.EnchantmentEffect(Enchantment.DAMAGE_ALL, "sharpness",
                                new LinearCalculator(0, 1)),
                        new ClassItem.EnchantmentEffect(Enchantment.KNOCKBACK, "knockback", new LinearCalculator(0, 1)))
                        .withUpgradables(new ClassItem.Upgradable("sharpness", "upgrades.sharpness.name", 2),
                                new ClassItem.Upgradable("knockback", "upgrades.knockback.name", 2)),
                new ClassItem(plugin, playerClass.getName(), Material.COMPASS, "items.player-compass.name")
                        .withUpgradeBuildings(Buildings.CHURCH, Buildings.CATHEDRAL)
                        .withRightClickAction(new ScoutCompass(plugin))
                        .withUpgradables(new ClassItem.Upgradable("compass", "items.player-compass.name", 1)),
                new ClassItem(plugin, playerClass.getName(), Material.CHAINMAIL_HELMET, "items.regen-ability.name")
                        .withUpgradeBuildings(Buildings.MAGETOWER).withUnlockOnBuildingBuild(true)
                        .withRightClickAction(new RegenAbility())
                        .withRightClickCooldown("regen", "regen", new LinearCalculator(35, 10),
                                "cooldowns.regen.finished")
                        .withUpgradables(new ClassItem.Upgradable("regen", "upgrades.regen-ability.name", 1)));
    }


    @Override
    public void onUserBeginGame(UserBeginGameEvent event) {
        super.onUserBeginGame(event);
        event.getUser().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1), true);
    }


    private static class ScoutCompass implements ClassItem.InteractAction {

        private WarsPlugin plugin;

        public ScoutCompass(WarsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onInteractWorld(UserInteractEvent event) {
            TeamColor exclude = event.getUser().getTeamColor();

            InventoryUtils.setItemNameAndLore(event.getItem(), plugin.getLocale("items.player-compass.locating"));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location closest = null;
                double minDist = 99999999999d;
                String closestName = null;

                for (User info : plugin.getUsers()) {
                    if (!info.isInGame() || info.getTeamColor() == exclude) continue;

                    double dist = event.getUser().getLocation().distanceSquared(info.getLocation());

                    if (dist < minDist) {
                        minDist = dist;
                        closest = info.getLocation();
                        closestName = info.getName();
                    }
                }

                if (closest != null) event.getUser().setCompassTarget(closest);
                else closestName = plugin.getLocale("items.player-compass.no-player");

                int compassIndex = event.getUserInventory().first(Material.COMPASS);
                ItemStack newCompass = event.getUserInventory().getItem(compassIndex);

                InventoryUtils.setItemNameAndLore(newCompass, plugin.getLocale("items.player-compass.name"),
                        plugin.getLocale("items.player-compass.oriented", closestName));

                event.getUserInventory().setItem(compassIndex, newCompass);
            }, 60);

            return true;
        }
    }

    private static class RegenAbility implements ClassItem.InteractAction {

        @Override
        public boolean onInteractWorld(UserInteractEvent event) {
            event.getUser().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200,
                    event.getUser().getUpgradeLevel("regen") * 2 + 1));

            return true;
        }
    }
}
