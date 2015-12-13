package com.ithinkrok.mccw.playerclass.items;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.data.User;
import com.ithinkrok.mccw.event.*;
import com.ithinkrok.mccw.inventory.Buyable;
import com.ithinkrok.mccw.inventory.UpgradeBuyable;
import com.ithinkrok.mccw.util.io.WarsConfig;
import com.ithinkrok.mccw.util.item.InventoryUtils;
import com.ithinkrok.mccw.util.io.LangFile;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
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
    private AttackAction attackAction;
    private WeaponModifier weaponModifier;
    private String itemDisplayName;
    private Material itemMaterial;
    private String rightClickCooldownUpgrade;
    private String rightClickCooldownFinished;
    private Calculator rightClickCooldown;
    private Upgradable[] upgradables;
    private EnchantmentEffect[] enchantmentEffects;
    private String signature;
    private LangFile langFile;
    private TimeoutAction timeoutAction;
    private String timeoutUpgrade;
    private String timeoutAbility;
    private String timeoutDescriptionLocale;
    private String timeoutFinished;
    private Calculator timeoutCalculator;
    private String rightClickCooldownAbility;
    private WarsConfig warsConfig;
    private String upgradeGroup;
    private String descriptionLocale;

    public ClassItem(WarsPlugin plugin, String upgradeGroup, Material itemMaterial) {
        this(plugin, upgradeGroup, itemMaterial, null);
    }

    public ClassItem(WarsPlugin plugin, String upgradeGroup, Material itemMaterial, String itemDisplayLang) {
        this.upgradeGroup = upgradeGroup;
        this.langFile = plugin.getLangFile();
        this.warsConfig = plugin.getWarsConfig();

        this.itemMaterial = itemMaterial;
        if (itemDisplayLang != null) this.itemDisplayName = langFile.getLocale(itemDisplayLang);

        signature = UUID.randomUUID().toString();
    }

    /**
     * Sets the buildings that the item can be upgraded in.
     * If unlockOnBuildingBuild is true, the item will be given to the player when one of these buildings is built.
     *
     * @param upgradeBuildings The buildings that the user can upgrade the item in.
     * @return The updated ClassItem object
     */
    public ClassItem withUpgradeBuildings(String... upgradeBuildings) {
        this.upgradeBuildings = upgradeBuildings;
        Arrays.sort(upgradeBuildings);
        return this;
    }

    public ClassItem withDescriptionLocale(String descriptionLocale) {
        this.descriptionLocale = descriptionLocale;
        return this;
    }

    public ClassItem withUnlockOnBuildingBuild(boolean unlockOnBuildingBuild) {
        this.unlockOnBuildingBuild = unlockOnBuildingBuild;
        return this;
    }

    public ClassItem withUnlockOnGameStart(boolean unlockOnGameStart) {
        this.unlockOnGameStart = unlockOnGameStart;
        return this;
    }

    public ClassItem withRightClickAction(InteractAction rightClickAction) {
        this.rightClickAction = rightClickAction;
        return this;
    }

    public ClassItem withRightClickTimeout(TimeoutAction timeoutAction, String timeoutUpgrade, String timeoutAbility,
                                           String timeoutDescriptionLocale, String timeoutEndedLocale,
                                           Calculator timeoutCalculator) {

        this.timeoutAction = timeoutAction;
        this.timeoutUpgrade = timeoutUpgrade;
        this.timeoutAbility = timeoutAbility;
        this.timeoutDescriptionLocale = timeoutDescriptionLocale;
        this.timeoutFinished = langFile.getLocale(timeoutEndedLocale);
        this.timeoutCalculator = timeoutCalculator;
        return this;
    }

    public ClassItem withLeftClickAction(InteractAction leftClickAction) {
        this.leftClickAction = leftClickAction;
        return this;
    }

    public ClassItem withAttackAction(AttackAction attackAction) {
        this.attackAction = attackAction;
        return this;
    }

    public ClassItem withWeaponModifier(WeaponModifier weaponModifier) {
        this.weaponModifier = weaponModifier;
        return this;
    }

    public ClassItem withRightClickCooldown(String upgradeName, String abilityName, Calculator rightClickCooldown,
                                            String cooldownFinishedLang) {
        this.rightClickCooldownUpgrade = upgradeName;
        this.rightClickCooldownAbility = abilityName;
        this.rightClickCooldown = rightClickCooldown;
        this.rightClickCooldownFinished = langFile.getLocale(cooldownFinishedLang);
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
        if (upgradables == null) return;

        for (Upgradable upgradable : upgradables) {
            for (int level = 1; level <= upgradable.maxLevel; ++level) {
                Map<String, Integer> upgradeLevels = new HashMap<>();
                upgradeLevels.put(upgradable.upgradeName, level);

                ItemStack display = createItemFromUpgradeLevels(upgradeLevels);

                ItemMeta displayMeta = display.getItemMeta();
                displayMeta.setDisplayName(langFile.getLocale(upgradable.upgradeDisplayLang, level));
                display.setItemMeta(displayMeta);

                Buyable buyable = new ClassItemBuyable(display, upgradeBuildings[0],
                        warsConfig.getCost(upgradeGroup, upgradable.upgradeName + level), upgradable.upgradeName,
                        level);

                for (int buildingIndex = 1; buildingIndex < upgradeBuildings.length; ++buildingIndex) {
                    buyable.withAdditionalBuildings(upgradeBuildings[buildingIndex]);
                }

                buyables.add(buyable);
            }
        }
    }

    private ItemStack createItemFromUpgradeLevels(Map<String, Integer> upgradeLevels) {
        List<String> lore = new ArrayList<>();

        if (descriptionLocale != null) {
            lore.add(langFile.getLocale(descriptionLocale));
        }

        if (weaponModifier != null) {
            weaponModifier.addLoreItems(langFile, lore, upgradeLevels);
        }

        if (rightClickCooldown != null) {
            double seconds = rightClickCooldown.calculate(getUpgradeLevel(upgradeLevels, rightClickCooldownUpgrade));
            lore.add(langFile.getLocale("lore.cooldown", seconds));
        }

        if (timeoutCalculator != null) {
            double seconds = timeoutCalculator.calculate(getUpgradeLevel(upgradeLevels, timeoutUpgrade));
            lore.add(langFile.getLocale(timeoutDescriptionLocale, seconds));
        }

        String[] loreArray = new String[lore.size()];
        lore.toArray(loreArray);

        ItemStack item = InventoryUtils.createItemWithNameAndLore(itemMaterial, 1, 0, itemDisplayName, loreArray);

        if (enchantmentEffects != null) {
            for (EnchantmentEffect enchantmentEffect : enchantmentEffects) {
                int level = getUpgradeLevel(upgradeLevels, enchantmentEffect.upgradeName);
                int enchantmentLevel = (int) enchantmentEffect.levelCalculator.calculate(level);
                if (enchantmentLevel <= 0) continue;

                item.addUnsafeEnchantment(enchantmentEffect.enchantment, enchantmentLevel);
            }
        }

        item.getItemMeta().spigot().setUnbreakable(true);

        return item;
    }

    private static int getUpgradeLevel(Map<String, Integer> upgradeLevels, String upgrade) {
        Integer level = upgradeLevels.get(upgrade);
        if (level == null) return 0;
        else return level;
    }

    public void onUserBeginGame(UserBeginGameEvent event) {
        if (!unlockOnGameStart) return;

        giveUserItem(event.getUser());
    }

    private void giveUserItem(User user) {
        setUserHasItem(user);
        InventoryUtils.replaceItem(user.getPlayerInventory(), createItemForUser(user));
    }

    private void setUserHasItem(User user) {
        user.setUpgradeLevel(signature, 1);
    }

    public ItemStack createItemForUser(User user) {
        return createItemFromUpgradeLevels(user.getUpgradeLevels());
    }

    public void onBuildingBuilt(UserTeamBuildingBuiltEvent event) {
        if (!unlockOnBuildingBuild) return;
        if (userHasItem(event.getUser())) return;
        if (Arrays.binarySearch(upgradeBuildings, event.getBuilding().getBuildingName()) < 0) return;

        setUserHasItem(event.getUser());
        InventoryUtils.replaceItem(event.getUserInventory(), createItemForUser(event.getUser()));
    }

    private boolean userHasItem(User user) {
        return user.getUpgradeLevel(signature) != 0;
    }

    public void onUserUpgrade(UserUpgradeEvent event) {
        if (!userHasItem(event.getUser())) return;

        InventoryUtils.replaceItem(event.getUserInventory(), createItemForUser(event.getUser()));
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public void onUserAttack(UserAttackEvent event) {
        if (event.getDamageCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && weaponModifier != null)
            attackWithWeaponModifier(event);

        if (attackAction == null) return;

        attackAction.onUserAttack(event);
    }

    private void attackWithWeaponModifier(UserAttackEvent event) {
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
        } else {
            if (rightClickAction == null) return false;
            if (isTimingOut(event.getUser())) {
                event.getUser().sendLocale("timeouts.default.wait");
                return true;
            }
            if (isCoolingDown(event.getUser())) {
                event.getUser().sendLocale("cooldowns.default.wait");
                return true;
            }
            boolean done = rightClickAction.onInteractWorld(event);
            if (!done) return false;

            if (timeoutCalculator != null) {
                int upgradeLevel = event.getUser().getUpgradeLevel(timeoutUpgrade);
                int timeout = (int) timeoutCalculator.calculate(upgradeLevel);
                event.getUser().startCoolDown(timeoutAbility, timeout, timeoutFinished);
            } else if (rightClickCooldown != null) {
                startRightClickCooldown(event.getUser());
            }

            return true;
        }

    }

    private boolean isTimingOut(User user) {
        return timeoutCalculator != null && user.isCoolingDown(timeoutAbility);
    }

    private boolean isCoolingDown(User user) {
        return rightClickCooldown != null && user.isCoolingDown(rightClickCooldownAbility);

    }

    private void startRightClickCooldown(User user) {
        int upgradeLevel = user.getUpgradeLevel(rightClickCooldownUpgrade);
        int cooldown = (int) rightClickCooldown.calculate(upgradeLevel);
        user.startCoolDown(rightClickCooldownAbility, cooldown, rightClickCooldownFinished);
    }

    public void onAbilityCooldown(UserAbilityCooldownEvent event) {
        if (!event.getAbility().equals(timeoutAbility)) return;
        if (!timeoutAction.onAbilityTimeout(event)) return;

        startRightClickCooldown(event.getUser());

    }

    public interface InteractAction {
        boolean onInteractWorld(UserInteractEvent event);
    }

    public interface TimeoutAction {
        boolean onAbilityTimeout(UserAbilityCooldownEvent event);
    }

    public interface AttackAction {
        void onUserAttack(UserAttackEvent event);
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

        public void addLoreItems(LangFile lang, List<String> lore, Map<String, Integer> upgradeLevels) {
            int upgradeLevel = getUpgradeLevel(upgradeLevels, upgradeName);

            if (damageCalculator != null)
                lore.add(lang.getLocale("lore.damage", damageCalculator.calculate(upgradeLevel)));
            if (fireCalculator != null) lore.add(lang.getLocale("lore.fire", fireCalculator.calculate(upgradeLevel)));
            if (nauseaCalculator != null)
                lore.add(lang.getLocale("lore.nausea", nauseaCalculator.calculate(upgradeLevel)));
            if (witherCalculator != null)
                lore.add(lang.getLocale("lore.wither", witherCalculator.calculate(upgradeLevel)));
        }

    }

    public static class Upgradable {
        private String upgradeName;
        private String upgradeDisplayLang;
        private int maxLevel;

        public Upgradable(String upgradeName, String upgradeDisplayLang, int maxLevel) {
            this.upgradeName = upgradeName;
            this.upgradeDisplayLang = upgradeDisplayLang;
            this.maxLevel = maxLevel;
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
}
