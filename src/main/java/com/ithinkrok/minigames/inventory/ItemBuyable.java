package com.ithinkrok.minigames.inventory;

import com.ithinkrok.minigames.User;
import com.ithinkrok.minigames.inventory.event.BuyablePurchaseEvent;
import com.ithinkrok.minigames.util.ConfigUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * Created by paul on 10/01/16.
 */
public class ItemBuyable extends Buyable{

    protected ItemStack purchase;

    private String noSpaceLocale;

    public ItemBuyable(ItemStack baseDisplay) {
        super(baseDisplay);
    }

    @Override
    public void configure(ConfigurationSection config) {
        super.configure(config);

        ItemStack purchase = ConfigUtils.getItemStack(config, "item");
        if(purchase != null){
            this.purchase = purchase;
            if(baseDisplay != null) baseDisplay = purchase;
        }

        noSpaceLocale = config.getString("no_inventory_space_locale", "item_buyable.no_space");
    }

    @Override
    public boolean onPurchase(BuyablePurchaseEvent event) {
        return giveUserItem(event.getUser(), purchase);
    }

    protected boolean giveUserItem(User user, ItemStack purchase) {
        PlayerInventory inv = user.getInventory();

        Map<Integer, ItemStack> failedItems = inv.addItem(purchase);
        if(failedItems.isEmpty()) return true;

        int removeAmount = purchase.getAmount() - failedItems.get(0).getAmount();

        if(removeAmount != 0) {
            ItemStack remove = purchase.clone();
            remove.setAmount(removeAmount);

            inv.removeItem(remove);
        }

        user.sendLocale(noSpaceLocale);
        return false;
    }
}
