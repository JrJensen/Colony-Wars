package com.ithinkrok.mccw.playerclass;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.event.UserInteractEvent;
import com.ithinkrok.mccw.inventory.ItemBuyable;
import com.ithinkrok.mccw.playerclass.items.ClassItem;
import com.ithinkrok.mccw.playerclass.items.LinearCalculator;
import com.ithinkrok.mccw.strings.Buildings;
import com.ithinkrok.mccw.util.TeamCompass;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * Created by paul on 14/11/15.
 * <p>
 * Handles the inferno class
 */
public class InfernoClass extends ClassItemClassHandler {

    public InfernoClass(WarsPlugin plugin, FileConfiguration config) {
        super(new ClassItem(plugin.getLangFile(), Material.IRON_CHESTPLATE, "items.explosion-wand.name")
                        .withUpgradeBuildings(Buildings.MAGETOWER).withUnlockOnBuildingBuild(true)
                        .withRightClickAction(new ExplosionWand())
                        .withRightClickCooldown("wand", "wand", new LinearCalculator(25, -10), "cooldowns.explosion.finished")
                        .withUpgradables(new ClassItem.Upgradable("wand", "upgrades.explosion-wand.name", 2,
                                configArrayCalculator(config, "costs.inferno.wand", 2))),
                new ClassItem(plugin.getLangFile(), Material.DIAMOND_HELMET, "items.flame-sword.name")
                        .withUpgradeBuildings(Buildings.BLACKSMITH).withUnlockOnBuildingBuild(true).withWeaponModifier(
                        new ClassItem.WeaponModifier("flame").withDamageCalculator(new LinearCalculator(1, 1.5))
                                .withFireCalculator(new LinearCalculator(4, 0))).withUpgradables(
                        new ClassItem.Upgradable("flame", "upgrades.flame-sword.name", 2,
                                configArrayCalculator(config, "costs.inferno.flame", 2))),
                TeamCompass.createTeamCompass(plugin, config));

        addExtraBuyables(new ItemBuyable(new ItemStack(Material.TNT, 16), Buildings.BLACKSMITH,
                config.getInt("costs.inferno.tnt") * 16, true));
    }

    @Override
    public boolean onInteract(UserInteractEvent event) {
        if (super.onInteract(event)) return true;

        if (!event.isRightClick()) return false;
        ItemStack item = event.getItem();
        if (item == null) return false;

        switch (item.getType()) {
            case TNT:
                if (!event.hasBlock()) return true;
                BlockFace mod = event.getBlockFace();

                event.getUser().createPlayerExplosion(event.getClickedBlock().getLocation().clone()
                        .add(mod.getModX() + 0.5, mod.getModY() + 0.5, mod.getModZ() + 0.5), 4F, false, 80);

                ItemStack oneLess = event.getItem().clone();
                if (oneLess.getAmount() > 1) oneLess.setAmount(oneLess.getAmount() - 1);
                else oneLess = null;
                event.getUserInventory().setItemInHand(oneLess);

                return true;
            default:
                return false;
        }
    }

    private static class ExplosionWand implements ClassItem.InteractAction {

        @Override
        public boolean onInteractWorld(UserInteractEvent event) {
            if(!event.isRightClick() || event.getBlockFace() == null) return false;
            Block target = event.getUser().rayTraceBlocks(200);
            if (target == null) return true;

            BlockFace mod = event.getBlockFace();
            event.getUser().createPlayerExplosion(
                    target.getLocation().clone().add(mod.getModX() + 0.5, mod.getModY() + 0.5, mod.getModZ() + 0.5), 2F,
                    false, 0);

            return true;
        }
    }
}
