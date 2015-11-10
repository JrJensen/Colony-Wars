package com.ithinkrok.mccw;

import com.ithinkrok.mccw.data.PlayerInfo;
import com.ithinkrok.mccw.data.TeamInfo;
import com.ithinkrok.mccw.enumeration.PlayerClass;
import com.ithinkrok.mccw.enumeration.TeamColor;
import com.ithinkrok.mccw.util.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;

/**
 * Created by paul on 08/11/15.
 * <p>
 * Handles bukkit events in the lobby
 */
public class WarsLobbyListener implements Listener {

    private final WarsPlugin plugin;

    public WarsLobbyListener(WarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.playerJoinLobby(event.getPlayer());
    }

    @EventHandler
    public void onPickupItem(PlayerPickupItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamaged(EntityDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        event.setCancelled(true);

        if (event.getItem() == null || !event.getItem().hasItemMeta() ||
                !event.getItem().getItemMeta().hasDisplayName()) return;

        switch (event.getItem().getItemMeta().getDisplayName()) {
            case "Team Chooser":
                showTeamChooser(event.getPlayer());
                break;
            case "Class Chooser":
                showClassChooser(event.getPlayer());
                break;
        }
    }

    private void showTeamChooser(Player player) {
        Inventory shopInv = Bukkit.createInventory(player, 9, "Team Chooser");

        shopInv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.WOOL, 1, TeamColor.RED.dyeColor.getWoolData(), "Red Team"));

        shopInv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.WOOL, 1, TeamColor.GREEN.dyeColor.getWoolData(), "Green Team"));

        shopInv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.WOOL, 1, TeamColor.YELLOW.dyeColor.getWoolData(), "Yellow Team"));

        shopInv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.WOOL, 1, TeamColor.BLUE.dyeColor.getWoolData(), "Blue Team"));

        player.openInventory(shopInv);
    }

    private void showClassChooser(Player player) {
        Inventory shopInv = Bukkit.createInventory(player, 9, "Class Chooser");

        shopInv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.DIAMOND_SWORD, 1, 0, "General", "Fights with a diamond sword"));

        shopInv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.COMPASS, 1, 0, "Scout", "Runs fast and has a player locator"));

        shopInv.addItem(InventoryUtils
                .createItemWithNameAndLore(Material.BOW, 1, 0, "Archer", "Fights with a bow!"));

        shopInv.addItem(InventoryUtils.createItemWithNameAndLore(Material.IRON_LEGGINGS, 1, 0, "Cloaker",
                "Can go invisible for short periods of time"));

        player.openInventory(shopInv);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event){
        event.setFormat(ChatColor.DARK_GRAY + "<" + ChatColor.WHITE + "%s" + ChatColor.DARK_GRAY + "> " + ChatColor
                .WHITE + "%s");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getInventory().getTitle() == null) return;
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta() ||
                !event.getCurrentItem().getItemMeta().hasDisplayName()) return;

        String item = event.getCurrentItem().getItemMeta().getDisplayName();

        PlayerInfo playerInfo = plugin.getPlayerInfo((Player) event.getWhoClicked());

        try {

            switch (event.getInventory().getTitle()) {
                case "Team Chooser":
                    String teamName = item.substring(0, item.length() - 5).toUpperCase();
                    TeamColor teamColor = TeamColor.valueOf(teamName);

                    String chatTeamName = teamColor.chatColor + item.substring(0, item.length() - 5);

                    if (teamColor == playerInfo.getTeamColor()) {
                        playerInfo.message(ChatColor.RED + "You are already in the " + chatTeamName + ChatColor.RED +
                                " Team");
                        break;
                    }

                    int playerCount = plugin.getPlayerCount();
                    int teamSize = plugin.getTeamInfo(teamColor).getPlayerCount();

                    if (teamSize >= (playerCount + 3) / 4) {
                        playerInfo.message(ChatColor.RED + "The " + chatTeamName + ChatColor.RED + " Team" +
                                " is full. Please try another team.");
                        break;
                    }

                    plugin.setPlayerTeam(playerInfo.getPlayer(), teamColor);

                    playerInfo.message(ChatColor.GOLD + "You will be in the " +
                            chatTeamName + ChatColor.GOLD + " Team in the next game!");

                    playerInfo.getPlayer().closeInventory();
                    break;
                case "Class Chooser":
                    String className = item.toUpperCase();
                    PlayerClass playerClass = PlayerClass.valueOf(className);

                    playerInfo.setPlayerClass(playerClass);

                    playerInfo.message(ChatColor.GOLD + "You will be a " + ChatColor.DARK_AQUA + item +
                            ChatColor.GOLD + " in the next game!");

                    playerInfo.getPlayer().closeInventory();
                    break;
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        plugin.setPlayerTeam(event.getPlayer(), null);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event){
        event.setFoodLevel(20);


        ((Player)event.getEntity()).setSaturation(20);
    }
}
