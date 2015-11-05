package com.ithinkrok.mccw.inventory;

import com.ithinkrok.mccw.data.PlayerInfo;
import com.ithinkrok.mccw.data.TeamInfo;
import com.ithinkrok.mccw.util.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paul on 04/11/15.
 * <p>
 * InventoryHandler for base shops
 */
public class BaseInventory implements InventoryHandler {

    private int farmCost = 3000;
    private int blacksmithCost = 4000;
    private int magetowerCost = 4000;

    @Override
    public void onInventoryClick(ItemStack item, PlayerInfo playerInfo, TeamInfo teamInfo) {
        PlayerInventory inv = playerInfo.getPlayer().getInventory();

        if (inv.firstEmpty() == -1) {
            playerInfo.getPlayer().sendMessage("Please ensure you have one free slot in your inventory");
            return;
        }

        ItemStack add = null;
        int cost = 0;

        switch (item.getItemMeta().getDisplayName()) {
            case "Farm":
                cost = farmCost;
                add = InventoryUtils
                        .createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, "Farm", "Builds a farm when placed!");
                break;
            case "Blacksmith":
                cost = blacksmithCost;
                add = InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, "Blacksmith",
                        "Builds a blacksmith when placed!");
                break;
            case "MageTower":
                cost = magetowerCost;
                add = InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, 1, 0, "MageTower",
                        "Builds a MageTower when placed!");
                break;
        }

        if (cost == 0 || add == null) return;

        if (!InventoryUtils.hasTeamCash(cost, teamInfo, playerInfo)) {
            playerInfo.getPlayer().sendMessage("You don't have that amount of money!");
            return;
        }

        inv.addItem(add);

        InventoryUtils.payWithTeamCash(cost, teamInfo, playerInfo);
        InventoryUtils.playBuySound(playerInfo.getPlayer());
    }

    @Override
    public List<ItemStack> getInventoryContents(PlayerInfo playerInfo, TeamInfo teamInfo) {
        ArrayList<ItemStack> result = new ArrayList<>();
        result.add(InventoryUtils.createShopItem(Material.LAPIS_ORE, 1, 0, "Farm", "Build a farm!", farmCost, true));

        if (teamInfo.getBuildingCount("Farm") > 0) {
            result.add(InventoryUtils
                    .createShopItem(Material.LAPIS_ORE, 1, 0, "Blacksmith", "Build a blacksmith!", blacksmithCost,
                            true));
            result.add(InventoryUtils
                    .createShopItem(Material.LAPIS_ORE, 1, 0, "MageTower", "Build a MageTower!", magetowerCost, true));
        }
        return result;
    }
}
