package com.ithinkrok.mccw.inventory;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.event.ItemPurchaseEvent;
import com.ithinkrok.mccw.strings.Buildings;
import com.ithinkrok.mccw.util.item.InventoryUtils;
import com.ithinkrok.mccw.util.io.LangFile;
import com.ithinkrok.mccw.util.building.SchematicBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paul on 07/11/15.
 * <p>
 * An inventory handler for every building inventory
 */
public class OmniInventory extends BuyableInventory {

    public OmniInventory(WarsPlugin plugin, FileConfiguration config) {
        super(getBuyables(plugin, config));
    }

    private static List<Buyable> getBuyables(WarsPlugin plugin, FileConfiguration config) {
        List<Buyable> result = new ArrayList<>();

        addBaseItems(result, plugin.getLangFile(), config);
        addFarmItems(result, config);
        addChurchItems(result, plugin, config);
        addCathedralItems(result, config);
        addGreenhouseItems(result, plugin, config);
        addBlacksmithItems(result, plugin.getLangFile(), config);
        addScoutTowerItems(result, plugin, config);

        return result;
    }

    private static void addBaseItems(List<Buyable> result, LangFile lang, FileConfiguration config) {
        int farmCost = config.getInt("costs.buildings." + Buildings.FARM);
        int lumbermillCost = config.getInt("costs.buildings." + Buildings.LUMBERMILL);
        int blacksmithCost = config.getInt("costs.buildings." + Buildings.BLACKSMITH);
        int magetowerCost = config.getInt("costs.buildings." + Buildings.MAGETOWER);
        int churchCost = config.getInt("costs.buildings." + Buildings.CHURCH);
        int greenhouseCost = config.getInt("costs.buildings." + Buildings.GREENHOUSE);

        result.add(new BuildingBuyable(lang, Buildings.FARM, Buildings.BASE, farmCost));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.LUMBERMILL, Buildings.BASE, lumbermillCost));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.BLACKSMITH, Buildings.BASE, blacksmithCost));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.MAGETOWER, Buildings.BASE, magetowerCost));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.CHURCH, Buildings.BASE, churchCost));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.GREENHOUSE, Buildings.BASE, greenhouseCost));

    }

    private static void addFarmItems(List<Buyable> result, FileConfiguration config) {
        int rawPotatoAmount = config.getInt("amounts.farm.rawPotato");
        int cookieAmount = config.getInt("amounts.farm.cookie");
        int rawBeefAmount = config.getInt("amounts.farm.rawBeef");
        int bakedPotatoAmount = config.getInt("amounts.farm.bakedPotato");
        int cookedBeefAmount = config.getInt("amounts.farm.cookedBeef");
        int goldenAppleAmount = config.getInt("amounts.farm.goldenApple");

        result.add(new ItemBuyable(new ItemStack(Material.POTATO_ITEM, rawPotatoAmount), Buildings.FARM,
                config.getInt("costs.farm.rawPotato") * rawPotatoAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.COOKIE, cookieAmount), Buildings.FARM,
                config.getInt("costs.farm.cookie") * cookieAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.RAW_BEEF, rawBeefAmount), Buildings.FARM,
                config.getInt("costs.farm.rawBeef") * rawBeefAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.BAKED_POTATO, bakedPotatoAmount), Buildings.FARM,
                config.getInt("costs.farm.bakedPotato") * bakedPotatoAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.COOKED_BEEF, cookedBeefAmount), Buildings.FARM,
                config.getInt("costs.farm.cookedBeef") * cookedBeefAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.GOLDEN_APPLE, goldenAppleAmount), Buildings.FARM,
                config.getInt("costs.farm.goldenApple") * goldenAppleAmount, true));
    }

    private static void addChurchItems(List<Buyable> result, WarsPlugin plugin, FileConfiguration config) {
        result.add(new Buyable(InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, Buildings.CATHEDRAL,
                plugin.getLocale("building.upgrade.replace", Buildings.CHURCH, Buildings.CATHEDRAL)), Buildings.CHURCH,
                config.getInt("costs.buildings." + Buildings.CATHEDRAL), true, 1) {


            @Override
            public void onPurchase(ItemPurchaseEvent event) {
                int oldRespawnChance = event.getTeam().getRespawnChance();
                event.getTeam().setDisableRespawnNotification(true);

                event.getBuilding().remove();
                event.getTeam().setRespawnChance(oldRespawnChance);

                event.getTeam().setDisableRespawnNotification(false);

                if (!SchematicBuilder.buildSchematic(plugin, plugin.getSchematicData(Buildings.CATHEDRAL),
                        event.getBuilding().getCenterBlock(), event.getBuilding().getRotation(),
                        event.getBuilding().getTeamColor())) {
                    event.getUser().messageLocale("building.upgrade.failed", Buildings.CATHEDRAL);

                    event.getUserInventory().addItem(InventoryUtils
                            .createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, Buildings.PLAYERCATHEDRAL,
                                    plugin.getLocale("building.item.desc", Buildings.CATHEDRAL)));
                }
            }

            @Override
            public boolean canBuy(ItemPurchaseEvent event) {
                return true;
            }
        });

        result.add(new ItemBuyable(InventoryUtils.createPotion(PotionType.INSTANT_HEAL, 1, true, false, 32),
                Buildings.CHURCH, config.getInt("costs.church.healingPotion32"), true));
    }

    private static void addCathedralItems(List<Buyable> result, FileConfiguration config) {
        result.add(new ItemBuyable(InventoryUtils.createPotion(PotionType.INSTANT_HEAL, 1, true, false, 32),
                Buildings.CATHEDRAL, config.getInt("costs.cathedral.healingPotion32"), true));
    }

    private static void addGreenhouseItems(List<Buyable> result, WarsPlugin plugin, FileConfiguration config) {
        int axeCost = config.getInt("costs.greenhouse.axe");

        result.add(new ItemBuyable(InventoryUtils
                .createItemWithNameAndLore(Material.STONE_AXE, 1, 0, plugin.getLocale("items.mighty-axe.name")),
                Buildings.GREENHOUSE, axeCost, false));
    }

    private static void addBlacksmithItems(List<Buyable> result, LangFile lang, FileConfiguration config) {
        int scoutTowerCost = config.getInt("costs.buildings." + Buildings.SCOUTTOWER);
        int wallCost = config.getInt("costs.buildings." + Buildings.WALL);
        int mineCost = config.getInt("costs.buildings." + Buildings.LANDMINE);

        result.add(new BuildingBuyable(lang, Buildings.SCOUTTOWER, Buildings.BLACKSMITH, scoutTowerCost));
        result.add(new BuildingBuyable(lang, Buildings.WALL, Buildings.BLACKSMITH, wallCost, 16, true));
        result.add(new BuildingBuyable(lang, Buildings.LANDMINE, Buildings.BLACKSMITH, mineCost));
    }

    private static void addScoutTowerItems(List<Buyable> result, WarsPlugin plugin, FileConfiguration config) {
        int cannonTowerCost = config.getInt("costs.buildings." + Buildings.CANNONTOWER);

        result.add(new Buyable(InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, Buildings.CANNONTOWER,
                plugin.getLocale("building.upgrade.replace", Buildings.SCOUTTOWER, Buildings.CANNONTOWER)),
                Buildings.SCOUTTOWER, cannonTowerCost, true, 1) {


            @Override
            public void onPurchase(ItemPurchaseEvent event) {
                event.getBuilding().remove();

                if (!SchematicBuilder.buildSchematic(plugin, plugin.getSchematicData(Buildings.CANNONTOWER),
                        event.getBuilding().getCenterBlock(), event.getBuilding().getRotation(),
                        event.getBuilding().getTeamColor())) {
                    event.getUser().messageLocale("building.upgrade.failed", Buildings.CANNONTOWER);

                    event.getUserInventory().addItem(InventoryUtils
                            .createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, Buildings.CANNONTOWER,
                                    plugin.getLocale("building.item.desc", Buildings.CANNONTOWER)));
                }
            }

            @Override
            public boolean canBuy(ItemPurchaseEvent event) {
                return true;
            }
        });
    }

    private static class BuildingBuyableWithFarm extends BuildingBuyable {

        public BuildingBuyableWithFarm(LangFile lang, String buildingName, String purchaseFromBuilding, int cost) {
            super(lang, buildingName, purchaseFromBuilding, cost);
        }

        @Override
        public boolean canBuy(ItemPurchaseEvent event) {
            return super.canBuy(event) && event.getTeam().getBuildingCount(Buildings.FARM) > 0;
        }
    }
}
