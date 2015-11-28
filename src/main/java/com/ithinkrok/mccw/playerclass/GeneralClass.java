package com.ithinkrok.mccw.playerclass;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.enumeration.PlayerClass;
import com.ithinkrok.mccw.playerclass.items.ClassItem;
import com.ithinkrok.mccw.playerclass.items.LinearCalculator;
import com.ithinkrok.mccw.strings.Buildings;
import com.ithinkrok.mccw.util.item.TeamCompass;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

/**
 * Created by paul on 05/11/15.
 * <p>
 * Handles the General class
 */
public class GeneralClass extends ClassItemClassHandler {

    public GeneralClass(WarsPlugin plugin, PlayerClass playerClass) {
        super(new ClassItem(plugin, playerClass.getName(), Material.DIAMOND_SWORD).withUpgradeBuildings(Buildings.BLACKSMITH)
                .withUnlockOnBuildingBuild(true).
                        withEnchantmentEffects(new ClassItem.EnchantmentEffect(Enchantment.DAMAGE_ALL, "sword",
                                        new LinearCalculator(0, 1)),
                                new ClassItem.EnchantmentEffect(Enchantment.KNOCKBACK, "sword",
                                        new LinearCalculator(3, 0))).withUpgradables(
                        new ClassItem.Upgradable("sword", "upgrades.diamond-sword.name", 2)),
                TeamCompass.createTeamCompass(plugin));
    }

}