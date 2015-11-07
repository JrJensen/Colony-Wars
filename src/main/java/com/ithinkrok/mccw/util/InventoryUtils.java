package com.ithinkrok.mccw.util;

import com.ithinkrok.mccw.data.PlayerInfo;
import com.ithinkrok.mccw.data.TeamInfo;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by paul on 03/11/15.
 *
 * Utility class for inventories
 */
public class InventoryUtils {

    public static ItemStack addPrice(ItemStack item, int cost, boolean team) {
        ItemMeta im = item.getItemMeta();

        String teamText = team ? " (Team Money)" : " (Player Money)";

        List<String> lore;
        if (im.hasLore()) lore = im.getLore();
        else lore = new ArrayList<>();

        lore.add("Cost: " + cost + teamText);
        im.setLore(lore);

        item.setItemMeta(im);

        return item;
    }

    public static ItemStack createShopItemWithEnchantments(Material mat, int amount, int damage, String name,
                                                           String desc, int cost, boolean team,
                                                           Object... enchantments) {
        ItemStack stack = createShopItem(mat, amount, damage, name, desc, cost, team);

        return enchantItem(stack, enchantments);
    }

    public static ItemStack createShopItem(Material mat, int amount, int damage, String name, String desc, int cost,
                                           boolean team) {
        ItemStack stack = new ItemStack(mat, amount, (short) damage);

        String teamText = team ? " (Team Money)" : " (Player Money)";

        if (desc != null) return setItemNameAndLore(stack, name, desc, "Cost: " + cost + teamText);
        else return setItemNameAndLore(stack, name, "Cost: " + cost + teamText);
    }

    public static ItemStack enchantItem(ItemStack item, Object... enchantments) {
        for (int i = 0; i < enchantments.length; i += 2) {
            int level = (int) enchantments[i + 1];
            if (level == 0) continue;

            item.addUnsafeEnchantment((Enchantment) enchantments[i], level);
        }

        return item;
    }

    public static ItemStack setItemNameAndLore(ItemStack item, String name, String... lore) {
        ItemMeta im = item.getItemMeta();
        if (name != null) im.setDisplayName(name);
        im.setLore(Arrays.asList(lore));
        item.setItemMeta(im);
        return item;
    }

    public static ItemStack createItemWithEnchantments(Material mat, int amount, int damage, String name, String desc,
                                                       Object... enchantments) {
        ItemStack stack;
        if(desc != null) stack = createItemWithNameAndLore(mat, amount, damage, name, desc);
        else stack = createItemWithNameAndLore(mat, amount, damage, name);

        return enchantItem(stack, enchantments);
    }

    public static ItemStack createItemWithNameAndLore(Material mat, int amount, int damage, String name,
                                                      String... lore) {
        ItemStack stack = new ItemStack(mat, amount, (short) damage);

        return setItemNameAndLore(stack, name, lore);
    }

    public static boolean checkUpgradeAndTryCharge(PlayerInfo playerInfo, int cost, String upgradeName, int level) {
        if (playerInfo.getUpgradeLevel(upgradeName) >= level) {
            playerInfo.getPlayer().sendMessage("You already have that upgrade");
            return false;
        } else if (!playerInfo.subtractPlayerCash(cost)) {
            playerInfo.getPlayer().sendMessage("You don't have that amount of money!");
            return false;
        }

        return true;
    }

    public static boolean payWithTeamCash(int amount, TeamInfo teamInfo, PlayerInfo playerInfo) {
        int teamAmount = Math.min(teamInfo.getTeamCash(), amount);
        int playerAmount = amount - teamAmount;

        if (playerAmount > 0 && !playerInfo.subtractPlayerCash(playerAmount)) return false;

        teamInfo.subtractTeamCash(teamAmount);

        if (playerAmount > 0) playerInfo.getPlayer().sendMessage("Payed " + playerAmount + " using your own money.");

        return true;
    }

    public static boolean hasTeamCash(int amount, TeamInfo teamInfo, PlayerInfo playerInfo) {
        int teamAmount = Math.min(teamInfo.getTeamCash(), amount);
        int playerAmount = amount - teamAmount;

        return !(playerAmount > 0 && !playerInfo.hasPlayerCash(playerAmount));

    }

    public static void playBuySound(Player player) {
        player.playSound(player.getLocation(), Sound.BLAZE_HIT, 1.0f, 1.0f);
    }

    public static void replaceItem(Inventory inventory, ItemStack stack) {
        int first = inventory.first(stack.getType());

        if (first == -1) inventory.addItem(stack);
        else inventory.setItem(first, stack);
    }
}
