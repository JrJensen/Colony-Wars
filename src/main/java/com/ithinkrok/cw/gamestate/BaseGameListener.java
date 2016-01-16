package com.ithinkrok.cw.gamestate;

import com.ithinkrok.cw.Building;
import com.ithinkrok.cw.event.ShopOpenEvent;
import com.ithinkrok.cw.metadata.BuildingController;
import com.ithinkrok.cw.metadata.CWTeamStats;
import com.ithinkrok.minigames.GameGroup;
import com.ithinkrok.minigames.User;
import com.ithinkrok.minigames.event.ListenerLoadedEvent;
import com.ithinkrok.minigames.event.map.MapBlockBreakNaturallyEvent;
import com.ithinkrok.minigames.event.map.MapItemSpawnEvent;
import com.ithinkrok.minigames.event.user.state.UserDeathEvent;
import com.ithinkrok.minigames.event.user.world.*;
import com.ithinkrok.minigames.inventory.ClickableInventory;
import com.ithinkrok.minigames.metadata.Money;
import com.ithinkrok.minigames.schematic.Facing;
import com.ithinkrok.minigames.team.Team;
import com.ithinkrok.minigames.util.ConfigUtils;
import com.ithinkrok.minigames.util.InventoryUtils;
import com.ithinkrok.minigames.util.SoundEffect;
import com.ithinkrok.minigames.util.TreeFeller;
import com.ithinkrok.minigames.util.math.ExpressionCalculator;
import com.ithinkrok.minigames.util.math.SingleValueVariables;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by paul on 16/01/16.
 */
public class BaseGameListener extends BaseGameStateListener {

    private String goldSharedConfig;
    private WeakHashMap<ConfigurationSection, GoldConfig> goldConfigMap = new WeakHashMap<>();

    private String unknownBuildingLocale;
    private String cannotBuildHereLocale;
    private String buildingNotFinishedLocale;
    private String buildingNotYoursLocale;

    private String cannotDestroyOwnBuildingLocale;
    private String cannotDestroyLocale;
    private String buildingDestroyedLocale;

    private String respawnLocale;
    private String noRespawnLocale;
    private int lostRespawnChance;

    private String teamLostPlayerLocale;
    private String teamPlayersLeftLocale;

    private int buildingDestroyWait;

    @EventHandler
    public void onListenerLoaded(ListenerLoadedEvent<?> event) {
        ConfigurationSection config = event.getConfig();

        goldSharedConfig = config.getString("gold_shared_object");

        unknownBuildingLocale = config.getString("buildings.unknown_locale", "building.unknown");
        cannotBuildHereLocale = config.getString("buildings.invalid_location_locale", "building.invalid_loc");
        buildingNotFinishedLocale = config.getString("buildings.not_finished_locale", "building.not_finished");
        buildingNotYoursLocale = config.getString("buildings.not_yours_locale", "building.not_yours");

        cannotDestroyOwnBuildingLocale =
                config.getString("buildings.destroy_own_team_locale", "building.destroy.own_team");
        cannotDestroyLocale = config.getString("buildings.destroy_protected_locale", "building.destroy.protected");
        buildingDestroyedLocale = config.getString("buildings.destroyed_locale", "building.destroy.success");

        respawnLocale = config.getString("respawn_locale", "respawn.success");
        noRespawnLocale = config.getString("no_respawn_locale", "respawn.fail");

        teamLostPlayerLocale = config.getString("team_lost_player_locale", "team.lost_player");
        teamPlayersLeftLocale = config.getString("team_players_left_locale", "team.players_left");

        buildingDestroyWait = (int) (config.getDouble("buildings.destroy_wait", 3.0d) * 20d);
        lostRespawnChance = config.getInt("lost_respawn_chance", 15);
    }

    @EventHandler
    public void onUserInteractWorld(UserInteractWorldEvent event) {
        if (event.getInteractType() != UserInteractEvent.InteractType.RIGHT_CLICK || !event.hasBlock()) return;

        if (event.getClickedBlock().getType() != Material.OBSIDIAN) return;

        BuildingController controller = BuildingController.getOrCreate(event.getUserGameGroup());

        Building building = controller.getBuilding(event.getClickedBlock().getLocation());
        if (building == null) return;

        if (!building.getTeamIdentifier().equals(event.getUser().getTeamIdentifier())) {
            event.getUser().sendLocale(buildingNotYoursLocale);
            return;
        }

        if (!building.isFinished()) {
            event.getUser().sendLocale(buildingNotFinishedLocale);
            return;
        }

        ClickableInventory shop = building.createShop();
        if (shop == null) shop = new ClickableInventory(building.getBuildingName());

        event.getUserGameGroup().userEvent(new ShopOpenEvent(event.getUser(), building, shop));

        event.getUser().showInventory(shop, event.getClickedBlock().getLocation());
    }

    @EventHandler
    public void onUserDeath(UserDeathEvent event) {
        User died = event.getUser();

        if(!died.isInGame()) {
            died.resetUserStats(true);

            died.teleport(died.getGameGroup().getCurrentMap().getSpawn());
            return;
        }

        //TODO remove entity targets on the dead player
        //TODO kill stats

        Team team = died.getTeam();
        CWTeamStats teamStats = CWTeamStats.getOrCreate(team);

        boolean respawn = shouldRespawnUser(died, teamStats);

        if(respawn) {
            died.getGameGroup().sendLocale(respawnLocale, died.getFormattedName());

            teamStats.setRespawnChance(teamStats.getRespawnChance() - lostRespawnChance, true);

            teamStats.respawnUser(died);
            died.resetUserStats(false);
        } else {
            died.getGameGroup().sendLocale(noRespawnLocale, died.getFormattedName());

            removeUserFromGame(died);

            died.setSpectator(true);
        }
    }

    private void removeUserFromGame(User died) {
        Team team = died.getTeam();

        died.getGameGroup().sendLocale(teamLostPlayerLocale, team.getFormattedName());
        died.getGameGroup().sendLocale(teamPlayersLeftLocale, team.getUserCount(), team.getFormattedName());

        if(team.getUserCount() == 0) {
            CWTeamStats.getOrCreate(team).eliminate();
        }

        //TODO check victory
        //TODO update spectator inventories

        died.setInGame(false);
    }

    public boolean shouldRespawnUser(User user, CWTeamStats teamStats) {
        return user.isPlayer() && random.nextFloat() < (teamStats.getRespawnChance() / 100f);
    }

    @EventHandler
    public void onUserBreakBlock(UserBreakBlockEvent event) {
        if (event.getBlock().getType() != Material.OBSIDIAN){
            ConfigurationSection goldShared = event.getUserGameGroup().getSharedObject(goldSharedConfig);
            GoldConfig gold = getGoldConfig(goldShared);

            gold.onBlockBreak(event.getBlock(), event.getUserGameGroup());
            return;
        }

        BuildingController controller = BuildingController.getOrCreate(event.getUserGameGroup());
        Building building = controller.getBuilding(event.getBlock().getLocation());

        if (building == null) return;

        if (building.getTeamIdentifier().equals(event.getUser().getTeamIdentifier())) {
            event.getUser().sendLocale(cannotDestroyOwnBuildingLocale);
            event.setCancelled(true);
        } else if(building.isProtected()) {
            event.getUser().sendLocale(cannotDestroyLocale, building.getBuildingName());
            event.setCancelled(true);
        } else {
            event.getUserGameGroup().sendLocale(buildingDestroyedLocale, event.getUser().getFormattedName(), building
                    .getBuildingName(), building.getTeamIdentifier().getFormattedName());
            event.getUserGameGroup().doInFuture(task -> building.explode(), buildingDestroyWait);
        }
    }

    private GoldConfig getGoldConfig(ConfigurationSection config) {
        GoldConfig gold = goldConfigMap.get(config);

        if (gold == null) {
            gold = new GoldConfig(config);
            goldConfigMap.put(config, gold);
        }

        return gold;
    }

    @EventHandler
    public void onUserPickupItem(UserPickupItemEvent event) {
        GoldConfig goldConfig = getGoldConfig(event.getUserGameGroup().getSharedObject(goldSharedConfig));
        Material material = event.getItem().getItemStack().getType();

        event.setCancelled(true);

        if (goldConfig.allowItemPickup(material)) {
            int amount = event.getItem().getItemStack().getAmount();

            int userGold = goldConfig.getUserGold(material) * amount;
            Money userMoney = Money.getOrCreate(event.getUser());
            userMoney.addMoney(userGold, false);

            int teamGold = goldConfig.getTeamGold(material) * amount;
            Money teamMoney = Money.getOrCreate(event.getUser().getTeam());
            teamMoney.addMoney(teamGold, false);

            SoundEffect sound = new SoundEffect(goldConfig.getPickupSound(), 1.0f, 0.8f + (random.nextFloat()) * 0.4f);
            event.getUser().playSound(event.getUser().getLocation(), sound);
        }

        event.getItem().remove();
    }

    @EventHandler
    public void onUserPlaceBlock(UserPlaceBlockEvent event) {
        if (event.getBlock().getType() != Material.LAPIS_ORE) return;

        event.getBlock().setType(Material.AIR);

        String buildingType = InventoryUtils.getDisplayName(event.getItemPlaced());

        if (buildingType == null || event.getUserGameGroup().getSchematic(buildingType) == null) {
            event.getUser().sendLocale(unknownBuildingLocale);
            return;
        }

        int rotation = Facing.getFacing(event.getUser().getLocation().getYaw());

        BuildingController controller = BuildingController.getOrCreate(event.getUserGameGroup());

        if (!controller.buildBuilding(buildingType, event.getUser().getTeamIdentifier(), event.getBlock().getLocation(),
                rotation, false)) {
            event.getUser().sendLocale(cannotBuildHereLocale);
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onItemSpawn(MapItemSpawnEvent event) {
        ConfigurationSection goldShared = event.getGameGroup().getSharedObject(goldSharedConfig);
        GoldConfig gold = getGoldConfig(goldShared);

        if (!gold.allowItemPickup(event.getItem().getItemStack().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreakNaturally(MapBlockBreakNaturallyEvent event) {
        ConfigurationSection goldShared = event.getGameGroup().getSharedObject(goldSharedConfig);
        GoldConfig gold = getGoldConfig(goldShared);

        gold.onBlockBreak(event.getBlock(), event.getGameGroup());
    }

    protected static class GoldConfig {
        HashMap<Material, ItemStack> oreBlocks = new HashMap<>();

        HashMap<Material, Integer> userGold = new HashMap<>();
        HashMap<Material, Integer> teamGold = new HashMap<>();

        boolean treesEnabled;
        Material treeItemMaterial;
        ExpressionCalculator treeItemAmount;
        Set<Material> logMaterials = new HashSet<>();

        Sound pickupSound;

        public GoldConfig(ConfigurationSection config) {
            ConfigurationSection ores = config.getConfigurationSection("ore_blocks");
            if (ores != null) {
                for (String matName : ores.getKeys(false)) {
                    Material material = Material.matchMaterial(matName);
                    ItemStack item = ConfigUtils.getItemStack(ores, matName);
                    oreBlocks.put(material, item);
                }
            }

            ConfigurationSection items = config.getConfigurationSection("items");
            for (String matName : items.getKeys(false)) {
                Material material = Material.matchMaterial(matName);
                ConfigurationSection matConfig = items.getConfigurationSection(matName);

                userGold.put(material, matConfig.getInt("user"));
                teamGold.put(material, matConfig.getInt("team"));
            }

            pickupSound = Sound.valueOf(config.getString("pickup_sound").toUpperCase());

            ConfigurationSection trees = config.getConfigurationSection("trees");
            treesEnabled = trees != null && trees.getBoolean("enabled");

            if (!treesEnabled) return;

            treeItemMaterial = Material.matchMaterial(trees.getString("item_material"));

            treeItemAmount = new ExpressionCalculator(trees.getString("item_amount"));

            List<String> logMaterialNames = trees.getStringList("log_materials");
            logMaterials.addAll(logMaterialNames.stream().map(Material::matchMaterial).collect(Collectors.toList()));
        }

        public void onBlockBreak(Block block, GameGroup gameGroup) {
            ItemStack drop = null;
            if (oreBlocks.containsKey(block.getType())) {
                drop = oreBlocks.get(block.getType()).clone();
            } else if (treesEnabled && logMaterials.contains(block.getType())) {
                int count = TreeFeller.fellTree(block.getLocation(), BuildingController.getOrCreate(gameGroup));
                count = (int) treeItemAmount.calculate(new SingleValueVariables(count));
                if(count > 0) drop = new ItemStack(treeItemMaterial, count);
            }

            block.setType(Material.AIR);

            if (drop == null) return;
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5d, 0.1d, 0.5d), drop);
        }

        public Sound getPickupSound() {
            return pickupSound;
        }

        public boolean allowItemPickup(Material material) {
            return userGold.containsKey(material);
        }

        public int getUserGold(Material material) {
            Integer result = userGold.get(material);
            return result == null ? 0 : result;
        }

        public int getTeamGold(Material material) {
            Integer result = teamGold.get(material);
            return result == null ? 0 : result;
        }
    }
}
