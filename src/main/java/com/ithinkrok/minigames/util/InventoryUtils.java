package com.ithinkrok.minigames.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

import java.util.Arrays;

/**
 * Created by paul on 31/12/15.
 */
public class InventoryUtils {

    public static boolean isEmpty(ItemStack stack){
        return stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0;
    }

    public static boolean isMaterial(ItemStack stack, Material material) {
        return stack != null && stack.getType() == material;
    }

    public static ItemStack enchantItem(ItemStack item, Object... enchantments) {
        for (int i = 0; i < enchantments.length; i += 2) {
            int level = (int) enchantments[i + 1];
            if (level == 0) continue;

            item.addUnsafeEnchantment((Enchantment) enchantments[i], level);
        }

        return item;
    }

    public static ItemStack createPotion(PotionType type, int level, boolean splash, boolean extended, int amount){
        Potion pot = new Potion(type, level);
        pot.setSplash(splash);
        if(extended) pot.setHasExtendedDuration(true);

        return pot.toItemStack(amount);
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
        if (desc != null) stack = createItemWithNameAndLore(mat, amount, damage, name, desc);
        else stack = createItemWithNameAndLore(mat, amount, damage, name);

        return enchantItem(stack, enchantments);
    }

    public static ItemStack createItemWithNameAndLore(Material mat, int amount, int damage, String name,
                                                      String... lore) {
        ItemStack stack = new ItemStack(mat, amount, (short) damage);

        return setItemNameAndLore(stack, name, lore);
    }

    public static void replaceItem(Inventory inventory, ItemStack stack) {
        int first = inventory.first(stack.getType());

        if (first == -1) inventory.addItem(stack);
        else inventory.setItem(first, stack);
    }
}