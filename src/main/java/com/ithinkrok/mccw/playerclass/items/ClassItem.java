package com.ithinkrok.mccw.playerclass.items;

import com.ithinkrok.mccw.data.User;
import com.ithinkrok.mccw.event.*;
import com.ithinkrok.mccw.inventory.Buyable;
import com.ithinkrok.mccw.inventory.UpgradeBuyable;
import com.ithinkrok.mccw.util.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Created by paul on 18/11/15.
 * <p>
 * An item available for a class
 */
public class ClassItem {

    private static final double HEALTH_PER_HEART = 2;
    private static final double TICKS_PER_SECOND = 20;
    private String[] upgradeBuildings;
    private boolean unlockOnBuildingBuild;
    private boolean unlockOnGameStart;
    private InteractAction rightClickAction;
    private InteractAction leftClickAction;
    private WeaponModifier weaponModifier;
    private String itemDisplayName;
    private Material itemMaterial;
    private String rightClickCooldownUpgrade;
    private String rightClickCooldownFinished;
    private Calculator rightClickCooldown;
    private Upgradable[] upgradables;
    private EnchantmentEffect[] enchantmentEffects;
    private String signature;

    public ClassItem(Material itemMaterial, String itemDisplayName) {
        this.itemMaterial = itemMaterial;
        this.itemDisplayName = itemDisplayName;

        signature = UUID.randomUUID().toString();
    }

    public ClassItem withUpgradeBuildings(String... upgradeBuildings) {
        this.upgradeBuildings = upgradeBuildings;
        Arrays.sort(upgradeBuildings);
        return this;
    }

    public ClassItem withUnlockOnBuildingBuild(boolean unlockOnBuildingBuild) {
        this.unlockOnBuildingBuild = unlockOnBuildingBuild;
        return this;
    }

    public ClassItem withUnlockOnGameStart(boolean unlockOnGameStart){
        this.unlockOnGameStart = unlockOnGameStart;
        return this;
    }

    public ClassItem withRightClickAction(InteractAction rightClickAction) {
        this.rightClickAction = rightClickAction;
        return this;
    }

    public ClassItem withLeftClickAction(InteractAction leftClickAction) {
        this.leftClickAction = leftClickAction;
        return this;
    }

    public ClassItem withWeaponModifier(WeaponModifier weaponModifier) {
        this.weaponModifier = weaponModifier;
        return this;
    }

    public ClassItem withRightClickCooldown(String upgradeName, Calculator rightClickCooldown, String
            cooldownFinished) {
        this.rightClickCooldownUpgrade = upgradeName;
        this.rightClickCooldown = rightClickCooldown;
        this.rightClickCooldownFinished = cooldownFinished;
        return this;
    }

    public ClassItem withUpgradables(Upgradable... upgradables) {
        this.upgradables = upgradables;
        return this;
    }

    public ClassItem withEnchantmentEffects(EnchantmentEffect... enchantmentEffects) {
        this.enchantmentEffects = enchantmentEffects;
        return this;
    }

    public void addBuyablesToList(List<Buyable> buyables) {
        for (Upgradable upgradable : upgradables) {
            for (int level = 1; level <= upgradable.maxLevel; ++level) {
                Map<String, Integer> upgradeLevels = new HashMap<>();
                upgradeLevels.put(upgradable.upgradeName, level);

                ItemStack display = createItemFromUpgradeLevels(upgradeLevels);

                ItemMeta displayMeta = display.getItemMeta();
                displayMeta.setDisplayName(String.format(upgradable.upgradeDisplayName, level));
                display.setItemMeta(displayMeta);

                Buyable buyable =
                        new ClassItemBuyable(display, upgradeBuildings[0], (int) upgradable.upgradeCost.calculate(level),
                                upgradable.upgradeName, level);

                for (int buildingIndex = 1; buildingIndex < upgradeBuildings.length; ++buildingIndex) {
                    buyable.withAdditionalBuildings(upgradeBuildings[buildingIndex]);
                }

                buyables.add(buyable);
            }
        }
    }

    public void onUserBeginGame(UserBeginGameEvent event) {
        if(!unlockOnGameStart) return;

        giveUserItem(event.getUser());
    }

    private class ClassItemBuyable extends UpgradeBuyable {

        public ClassItemBuyable(ItemStack display, String buildingName, int cost, String upgradeName,
                                int upgradeLevel) {
            super(display, buildingName, cost, upgradeName, upgradeLevel);
        }

        @Override
        public void onPurchase(ItemPurchaseEvent event) {
            giveUserItem(event.getUser());
            super.onPurchase(event);
        }
    }

    private void giveUserItem(User user){
        setUserHasItem(user);
        InventoryUtils.replaceItem(user.getPlayerInventory(), createItemForUser(user));
    }

    private ItemStack createItemFromUpgradeLevels(Map<String, Integer> upgradeLevels) {
        List<String> lore = new ArrayList<>();

        if (weaponModifier != null) {
            weaponModifier.addLoreItems(lore, upgradeLevels);
        }

        if (rightClickCooldown != null) {
            lore.add("Cooldown: " +
                    rightClickCooldown.calculate(getUpgradeLevel(upgradeLevels, rightClickCooldownUpgrade)) +
                    " seconds");
        }

        String[] loreArray = new String[lore.size()];
        lore.toArray(loreArray);

        ItemStack item = InventoryUtils.createItemWithNameAndLore(itemMaterial, 1, 0, itemDisplayName, loreArray);

        for (EnchantmentEffect enchantmentEffect : enchantmentEffects) {
            int level = getUpgradeLevel(upgradeLevels, enchantmentEffect.upgradeName);
            int enchantmentLevel = (int) enchantmentEffect.levelCalculator.calculate(level);
            if (enchantmentLevel <= 0) continue;

            item.addUnsafeEnchantment(enchantmentEffect.enchantment, enchantmentLevel);
        }

        return item;
    }

    private static int getUpgradeLevel(Map<String, Integer> upgradeLevels, String upgrade) {
        Integer level = upgradeLevels.get(upgrade);
        if (level == null) return 0;
        else return level;
    }

    public void onBuildingBuilt(UserTeamBuildingBuiltEvent event) {
        if (!unlockOnBuildingBuild) return;
        if (userHasItem(event.getUser())) return;
        if (Arrays.binarySearch(upgradeBuildings, event.getBuilding().getBuildingName()) == -1) return;

        setUserHasItem(event.getUser());
        InventoryUtils.replaceItem(event.getUserInventory(), createItemForUser(event.getUser()));
    }

    private boolean userHasItem(User user) {
        return user.getUpgradeLevel(signature) != 0;
    }

    private void setUserHasItem(User user) {
        user.setUpgradeLevel(signature, 1);
    }

    public ItemStack createItemForUser(User user) {
        return createItemFromUpgradeLevels(user.getUpgradeLevels());
    }

    public void onUserUpgrade(UserUpgradeEvent event) {
        if (!userHasItem(event.getUser())) return;

        InventoryUtils.replaceItem(event.getUserInventory(), createItemForUser(event.getUser()));
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public void onUserAttack(UserAttackEvent event) {
        if (weaponModifier == null) return;

        int upgradeLevel = event.getUser().getUpgradeLevel(weaponModifier.upgradeName);

        if (weaponModifier.damageCalculator != null) {
            event.setDamage(weaponModifier.damageCalculator.calculate(upgradeLevel) * HEALTH_PER_HEART);
        }
        if (weaponModifier.witherCalculator != null) {
            int wither = (int) (weaponModifier.witherCalculator.calculate(upgradeLevel) * TICKS_PER_SECOND);

            if (wither > 1) {
                if (event.isAttackingUser()) event.getTargetUser().setWitherTicks(event.getUser(), wither);
                else if (event.getClickedEntity() instanceof LivingEntity) ((LivingEntity) event.getClickedEntity())
                        .addPotionEffect(new PotionEffect(PotionEffectType.WITHER, wither, 0, false, true));
            }
        }
        if (weaponModifier.nauseaCalculator != null) {
            int nausea = (int) (weaponModifier.nauseaCalculator.calculate(upgradeLevel) * TICKS_PER_SECOND);

            if (nausea > 1 && event.getClickedEntity() instanceof LivingEntity) {
                ((LivingEntity) event.getClickedEntity())
                        .addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, nausea, 0, false, true));
            }
        }
        if (weaponModifier.fireCalculator != null) {
            int fire = (int) (weaponModifier.fireCalculator.calculate(upgradeLevel) * TICKS_PER_SECOND);

            if (fire > 1) {
                if (event.isAttackingUser()) event.getTargetUser().setFireTicks(event.getUser(), fire);
                else event.getClickedEntity().setFireTicks(fire);
            }
        }
    }

    public boolean onInteract(UserInteractEvent event) {
        if (!event.isRightClick()) {
            return leftClickAction != null && leftClickAction.onInteractWorld(event);
        } else{
            if(rightClickAction == null) return false;
            if(rightClickCooldown != null){
                int cooldown =
                        (int) rightClickCooldown.calculate(event.getUser().getUpgradeLevel(rightClickCooldownUpgrade));
                if(!event.getUser().startCoolDown(rightClickCooldownUpgrade, cooldown, rightClickCooldownFinished))
                    return true;
            }
            return rightClickAction.onInteractWorld(event);
        }

    }

    public interface InteractAction {
        boolean onInteractWorld(UserInteractEvent event);
    }

    public static class WeaponModifier {
        private String upgradeName;

        /**
         * Calculates damage in hearts
         */
        private Calculator damageCalculator;

        /**
         * Calculates wither effect duration in seconds
         */
        private Calculator witherCalculator;
        private Calculator fireCalculator;
        private Calculator nauseaCalculator;

        public WeaponModifier(String upgradeName) {
            this.upgradeName = upgradeName;
        }

        public WeaponModifier withDamageCalculator(Calculator damageCalculator) {
            this.damageCalculator = damageCalculator;
            return this;
        }

        public WeaponModifier withWitherCalculator(Calculator witherCalculator) {
            this.witherCalculator = witherCalculator;
            return this;
        }

        public WeaponModifier withFireCalculator(Calculator fireCalculator) {
            this.fireCalculator = fireCalculator;
            return this;
        }

        public WeaponModifier withNauseaCalculator(Calculator nauseaCalculator) {
            this.nauseaCalculator = nauseaCalculator;
            return this;
        }

        public void addLoreItems(List<String> lore, Map<String, Integer> upgradeLevels) {
            int upgradeLevel = getUpgradeLevel(upgradeLevels, upgradeName);

            if (damageCalculator != null) lore.add("Damage: " + damageCalculator.calculate(upgradeLevel) + " Hearts");
            if (fireCalculator != null)
                lore.add("Fire Duration: " + fireCalculator.calculate(upgradeLevel) + " seconds");
            if (nauseaCalculator != null)
                lore.add("Nausea Duration: " + nauseaCalculator.calculate(upgradeLevel) + " seconds");
            if (witherCalculator != null)
                lore.add("Wither Duration: " + witherCalculator.calculate(upgradeLevel) + " seconds");
        }

    }

    public static class Upgradable {
        private String upgradeName;
        private String upgradeDisplayName;
        private int maxLevel;
        private Calculator upgradeCost;

        public Upgradable(String upgradeName, String upgradeDisplayName, int maxLevel, Calculator upgradeCost) {
            this.upgradeName = upgradeName;
            this.upgradeDisplayName = upgradeDisplayName;
            this.maxLevel = maxLevel;
            this.upgradeCost = upgradeCost;
        }
    }

    public static class EnchantmentEffect {
        private Enchantment enchantment;
        private String upgradeName;
        private Calculator levelCalculator;

        public EnchantmentEffect(Enchantment enchantment, String upgradeName, Calculator levelCalculator) {
            this.enchantment = enchantment;
            this.upgradeName = upgradeName;
            this.levelCalculator = levelCalculator;
        }
    }
}
