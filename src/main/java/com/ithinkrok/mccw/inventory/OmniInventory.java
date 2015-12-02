package com.ithinkrok.mccw.inventory;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.event.ItemPurchaseEvent;
import com.ithinkrok.mccw.strings.Buildings;
import com.ithinkrok.mccw.util.io.WarsConfig;
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

    public OmniInventory(WarsPlugin plugin, WarsConfig config) {
        super(getBuyables(plugin, config));
    }

    private static List<Buyable> getBuyables(WarsPlugin plugin, WarsConfig config) {
        List<Buyable> result = new ArrayList<>();

        addBaseItems(result, plugin, config);
        addFarmItems(result, config);
        addChurchItems(result, plugin, config);
        addCathedralItems(result, config);
        addGreenhouseItems(result, plugin, config);
        addBlacksmithItems(result, plugin.getLangFile(), config);
        addScoutTowerItems(result, plugin, config);
        addFortressItems(result, plugin.getLangFile(), config);

        return result;
    }

    private static void addBaseItems(List<Buyable> result, WarsPlugin plugin, WarsConfig config) {
        LangFile lang = plugin.getLangFile();

        int farmCost = config.getBuildingCost(Buildings.FARM);
        int lumbermillCost = config.getBuildingCost(Buildings.LUMBERMILL);
        int blacksmithCost = config.getBuildingCost(Buildings.BLACKSMITH);
        int magetowerCost = config.getBuildingCost(Buildings.MAGETOWER);
        int churchCost = config.getBuildingCost(Buildings.CHURCH);
        int greenhouseCost = config.getBuildingCost(Buildings.GREENHOUSE);

        result.add(new BuildingBuyable(lang, Buildings.FARM, Buildings.BASE, farmCost)
                .withAdditionalBuildings(Buildings.FORTRESS));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.LUMBERMILL, Buildings.BASE, lumbermillCost)
                .withAdditionalBuildings(Buildings.FORTRESS));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.BLACKSMITH, Buildings.BASE, blacksmithCost)
                .withAdditionalBuildings(Buildings.FORTRESS));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.MAGETOWER, Buildings.BASE, magetowerCost)
                .withAdditionalBuildings(Buildings.FORTRESS));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.CHURCH, Buildings.BASE, churchCost)
                .withAdditionalBuildings(Buildings.FORTRESS));
        result.add(new BuildingBuyableWithFarm(lang, Buildings.GREENHOUSE, Buildings.BASE, greenhouseCost)
                .withAdditionalBuildings(Buildings.FORTRESS));

        result.add(new Buyable(InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, Buildings.FORTRESS,
                plugin.getLocale("building.upgrade.replace", Buildings.BASE, Buildings.FORTRESS)), Buildings.BASE,
                config.getBuildingCost(Buildings.FORTRESS), true, 1) {


            @Override
            public void onPurchase(ItemPurchaseEvent event) {
                event.getBuilding().remove();

                if (!SchematicBuilder.buildSchematic(plugin, plugin.getSchematicData(Buildings.FORTRESS),
                        event.getBuilding().getCenterBlock(), event.getBuilding().getRotation(),
                        event.getBuilding().getTeamColor())) {
                    plugin.getLogger().warning("Fortress should be marked as upgradable from base in the config.");
                }
            }

            @Override
            public boolean canBuy(ItemPurchaseEvent event) {
                return (event.getTeam().getBuildingCount(Buildings.FARM) > 0) &&
                        (event.getTeam().getBuildingCount(Buildings.LUMBERMILL) > 0) &&
                        (event.getTeam().getBuildingCount(Buildings.BLACKSMITH) > 0) &&
                        (event.getTeam().getBuildingCount(Buildings.MAGETOWER) > 0) &&
                        (event.getTeam().getBuildingCount(Buildings.GREENHOUSE) > 0) &&
                        (event.getTeam().getBuildingCount(Buildings.CATHEDRAL) > 0);
            }
        });

    }

    private static void addFarmItems(List<Buyable> result, WarsConfig config) {
        String buildingName = Buildings.FARM;

        int rawPotatoAmount = config.getBuildingItemAmount(buildingName, "rawPotato");
        int cookieAmount = config.getBuildingItemAmount(buildingName, "cookie");
        int rawBeefAmount = config.getBuildingItemAmount(buildingName, "rawBeef");
        int bakedPotatoAmount = config.getBuildingItemAmount(buildingName, "bakedPotato");
        int cookedBeefAmount = config.getBuildingItemAmount(buildingName, "cookedBeef");
        int goldenAppleAmount = config.getBuildingItemAmount(buildingName, "goldenApple");

        result.add(new ItemBuyable(new ItemStack(Material.POTATO_ITEM, rawPotatoAmount), Buildings.FARM,
                config.getBuildingItemCost(buildingName, "rawPotato") * rawPotatoAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.COOKIE, cookieAmount), Buildings.FARM,
                config.getBuildingItemCost(buildingName, "cookie") * cookieAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.RAW_BEEF, rawBeefAmount), Buildings.FARM,
                config.getBuildingItemCost(buildingName, "rawBeef") * rawBeefAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.BAKED_POTATO, bakedPotatoAmount), Buildings.FARM,
                config.getBuildingItemCost(buildingName, "bakedPotato") * bakedPotatoAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.COOKED_BEEF, cookedBeefAmount), Buildings.FARM,
                config.getBuildingItemCost(buildingName, "cookedBeef") * cookedBeefAmount, true));

        result.add(new ItemBuyable(new ItemStack(Material.GOLDEN_APPLE, goldenAppleAmount), Buildings.FARM,
                config.getBuildingItemCost(buildingName, "goldenApple") * goldenAppleAmount, true));
    }

    private static void addChurchItems(List<Buyable> result, WarsPlugin plugin, WarsConfig config) {
        result.add(new Buyable(InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, Buildings.CATHEDRAL,
                plugin.getLocale("building.upgrade.replace", Buildings.CHURCH, Buildings.CATHEDRAL)), Buildings.CHURCH,
                config.getBuildingCost(Buildings.CATHEDRAL), true, 1) {


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
                    plugin.getLogger().warning("Cathedral should be marked as upgradable from church in the config.");
                }
            }

            @Override
            public boolean canBuy(ItemPurchaseEvent event) {
                return true;
            }
        });

        result.add(new ItemBuyable(InventoryUtils.createPotion(PotionType.INSTANT_HEAL, 1, true, false, 32),
                Buildings.CHURCH, config.getBuildingItemCost(Buildings.CHURCH, "healingPotion32"), true));
    }

    private static void addCathedralItems(List<Buyable> result, WarsConfig config) {
        result.add(new ItemBuyable(InventoryUtils.createPotion(PotionType.INSTANT_HEAL, 1, true, false, 32),
                Buildings.CATHEDRAL, config.getBuildingItemCost(Buildings.CATHEDRAL, "healingPotion32"), true));
    }

    private static void addGreenhouseItems(List<Buyable> result, WarsPlugin plugin, WarsConfig config) {
        int axeCost = config.getBuildingItemCost(Buildings.GREENHOUSE, "axe");

        result.add(new ItemBuyable(InventoryUtils
                .createItemWithNameAndLore(Material.STONE_AXE, 1, 0, plugin.getLocale("items.mighty-axe.name")),
                Buildings.GREENHOUSE, axeCost, false));
    }

    private static void addBlacksmithItems(List<Buyable> result, LangFile lang, WarsConfig config) {
        int scoutTowerCost = config.getBuildingCost(Buildings.SCOUTTOWER);
        int wallCost = config.getBuildingCost(Buildings.WALL);
        int mineCost = config.getBuildingCost(Buildings.LANDMINE);

        result.add(new BuildingBuyable(lang, Buildings.SCOUTTOWER, Buildings.BLACKSMITH, scoutTowerCost));
        result.add(new BuildingBuyable(lang, Buildings.WALL, Buildings.BLACKSMITH, wallCost, 16, true));
        result.add(new BuildingBuyable(lang, Buildings.LANDMINE, Buildings.BLACKSMITH, mineCost));
    }

    private static void addScoutTowerItems(List<Buyable> result, WarsPlugin plugin, WarsConfig config) {
        int cannonTowerCost = config.getBuildingCost(Buildings.CANNONTOWER);

        result.add(new Buyable(InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, Buildings.CANNONTOWER,
                plugin.getLocale("building.upgrade.replace", Buildings.SCOUTTOWER, Buildings.CANNONTOWER)),
                Buildings.SCOUTTOWER, cannonTowerCost, true, 1) {


            @Override
            public void onPurchase(ItemPurchaseEvent event) {
                event.getBuilding().remove();

                if (!SchematicBuilder.buildSchematic(plugin, plugin.getSchematicData(Buildings.CANNONTOWER),
                        event.getBuilding().getCenterBlock(), event.getBuilding().getRotation(),
                        event.getBuilding().getTeamColor())) {
                    plugin.getLogger().warning("CT should be marked as upgradable from ST in the config.");
                }
            }

            @Override
            public boolean canBuy(ItemPurchaseEvent event) {
                return true;
            }
        });
    }

    private static void addFortressItems(List<Buyable> result, LangFile lang, WarsConfig config) {
        int cannonTowerCost =
                config.getBuildingCost(Buildings.CANNONTOWER) + config.getBuildingCost(Buildings.SCOUTTOWER);

        result.add(new BuildingBuyable(lang, Buildings.CANNONTOWER, Buildings.FORTRESS, cannonTowerCost));
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
