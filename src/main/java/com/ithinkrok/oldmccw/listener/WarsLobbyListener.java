package com.ithinkrok.oldmccw.listener;

import com.ithinkrok.oldmccw.WarsPlugin;
import com.ithinkrok.oldmccw.data.User;
import com.ithinkrok.oldmccw.enumeration.PlayerClass;
import com.ithinkrok.oldmccw.data.TeamColor;
import com.ithinkrok.oldmccw.event.*;
import com.ithinkrok.oldmccw.lobby.LobbyMinigame;
import com.ithinkrok.oldmccw.util.item.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.userJoinLobby(plugin.getUser(event.getPlayer()));

        String name = ChatColor.YELLOW + event.getPlayer().getName();
        String online = Integer.toString(plugin.getPlayerCount());
        String max = Integer.toString(plugin.getServer().getMaxPlayers());
        event.setJoinMessage(plugin.getLocale("server.players.join", name, online, max));

        if (!plugin.getCountdownHandler().isCountingDown()) plugin.getCountdownHandler().startLobbyCountdown();
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickupItem(PlayerPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.WRITTEN_BOOK) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);

        User user = plugin.getUser(event.getPlayer());
        UserBreakBlockEvent userBreakBlockEvent = new UserBreakBlockEvent(user, event);

        for (LobbyMinigame minigame : plugin.getLobbyMinigames()) {
            minigame.onUserBreakBlock(userBreakBlockEvent);
        }

        if (!event.isCancelled()) event.getBlock().setType(Material.AIR);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        User user = plugin.getUser(event.getPlayer());
        UserMoveEvent userMoveEvent = new UserMoveEvent(user, event);

        for (LobbyMinigame minigame : plugin.getLobbyMinigames()) {
            minigame.onUserMove(userMoveEvent);
        }
    }

    @EventHandler
    public void onEntityDamaged(EntityDamageEvent event) {
        event.setCancelled(true);

        if (!(event.getEntity() instanceof Player)) return;

        User user = plugin.getUser((Player) event.getEntity());
        UserDamagedEvent userDamagedEvent = new UserDamagedEvent(user, event);

        for (LobbyMinigame minigame : plugin.getLobbyMinigames()) {
            minigame.onUserDamaged(userDamagedEvent);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.WRITTEN_BOOK) return;

        if ((event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) ||
                !isRedstoneControl(event.getClickedBlock().getType())) {
            event.setCancelled(true);
        }

        User user = plugin.getUser(event.getPlayer());

        UserInteractEvent userInteractEvent = new UserInteractWorldEvent(user, event);

        for (LobbyMinigame minigame : plugin.getLobbyMinigames()) {
            if (minigame.onUserInteract(userInteractEvent)) break;
        }

        if (event.getItem() == null || !event.getItem().hasItemMeta() ||
                !event.getItem().getItemMeta().hasDisplayName()) return;

        if (plugin.getLocale("lobby.chooser.team.name").equals(event.getItem().getItemMeta().getDisplayName())) {
            showTeamChooser(event.getPlayer());
        } else if (plugin.getLocale("lobby.chooser.class.name")
                .equals(event.getItem().getItemMeta().getDisplayName())) {
            showClassChooser(event.getPlayer());
        } else if (plugin.getLocale("lobby.chooser.map.name").equals(event.getItem().getItemMeta().getDisplayName())) {
            showMapChooser(event.getPlayer());
        }
    }

    private static boolean isRedstoneControl(Material type) {
        switch (type) {
            case LEVER:
            case STONE_BUTTON:
            case WOOD_BUTTON:
            case STONE_PLATE:
            case WOOD_PLATE:
            case GOLD_PLATE:
            case IRON_PLATE:
            case WOOD_DOOR:
            case TRAP_DOOR:
                return true;
            default:
                return false;
        }
    }

    private void showTeamChooser(Player player) {
        int size = 9 * ((TeamColor.values().size() + 9) / 9);
        Inventory shopInv = Bukkit.createInventory(player, size, plugin.getLocale("lobby.chooser.team.name"));

        for (TeamColor team : TeamColor.values()) {
            shopInv.addItem(InventoryUtils.createItemWithNameAndLore(Material.WOOL, 1, team.getDyeColor().getWoolData(),
                    plugin.getLocale("team.name", team.getFormattedName())));
        }

        player.openInventory(shopInv);
    }

    private void showClassChooser(Player player) {
        Inventory shopInv = Bukkit.createInventory(player, 18, plugin.getLocale("lobby.chooser.class.name"));

        for (PlayerClass playerClass : PlayerClass.values()) {
            shopInv.addItem(InventoryUtils
                    .createItemWithNameAndLore(playerClass.getChooser(), 1, 0, playerClass.getFormattedName(),
                            plugin.getLocale("classes." + playerClass.getName() + ".desc")));
        }

        player.openInventory(shopInv);
    }

    private void showMapChooser(Player player) {
        int slots = ((plugin.getMapList().size() + 9) / 9) * 9;

        Inventory shopInv = Bukkit.createInventory(player, slots, plugin.getLocale("lobby.chooser.map.name"));

        for (String map : plugin.getMapList()) {
            shopInv.addItem(InventoryUtils
                    .createItemWithNameAndLore(Material.MAP, 1, 0, map, plugin.getLocale("maps." + map + ".desc")));
        }

        player.openInventory(shopInv);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        UserInteractEvent userInteractEvent = new UserRightClickEntityEvent(plugin.getUser(event.getPlayer()), event);

        for (LobbyMinigame minigame : plugin.getLobbyMinigames()) {
            if (minigame.onUserInteract(userInteractEvent)) break;
        }
    }

    @EventHandler
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        User attacker = plugin.getUser((Player) event.getDamager());
        User target = null;
        if (event.getEntity() instanceof Player) target = plugin.getUser((Player) event.getEntity());

        UserInteractEvent userInteractEvent = new UserAttackEvent(attacker, target, event);

        for (LobbyMinigame minigame : plugin.getLobbyMinigames()) {
            if (minigame.onUserInteract(userInteractEvent)) break;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getInventory().getTitle() == null) return;
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta() ||
                !event.getCurrentItem().getItemMeta().hasDisplayName()) return;

        User user = plugin.getUser((Player) event.getWhoClicked());

        try {

            if (plugin.getLocale("lobby.chooser.team.name").equals(event.getInventory().getTitle())) {
                TeamColor teamColor = TeamColor.fromWoolColor(event.getCurrentItem().getDurability());
                if (teamColor == null) {
                    user.sendMessage("Null team. This is impossible error");
                    return;
                }

                if (teamColor == user.getTeamColor()) {
                    user.sendMessage(plugin.getLocale("team.join.already-member", teamColor.getFormattedName()));
                    return;
                }

                if (!checkTeamJoinAllowed(teamColor, user.getTeamColor())) {
                    user.sendMessage(plugin.getLocale("team.join.full", teamColor.getFormattedName()));
                    return;
                }

                user.setTeamColor(teamColor);

                user.sendMessage(plugin.getLocale("team.join.success", teamColor.getFormattedName()));

                user.closeInventory();
            } else if (plugin.getLocale("lobby.chooser.class.name").equals(event.getInventory().getTitle())) {
                PlayerClass playerClass = PlayerClass.fromChooserMaterial(event.getCurrentItem().getType());
                if (playerClass == null) {
                    user.sendMessage("Null class. This is impossible error");
                    return;
                }

                user.setPlayerClass(playerClass);

                user.sendMessage(plugin.getLocale("class.join.success", playerClass.getFormattedName()));

                user.closeInventory();
            } else if (plugin.getLocale("lobby.chooser.map.name").equals(event.getInventory().getTitle())) {
                String mapName = event.getCurrentItem().getItemMeta().getDisplayName();

                String oldVote = user.getMapVote();

                if (mapName.equals(oldVote)) {
                    user.sendMessage(plugin.getLocale("voting.maps.already-voted", mapName));
                    return;
                }

                user.setMapVote(mapName);
                user.sendMessage(plugin.getLocale("voting.maps.success", mapName));

                String playerName = ChatColor.YELLOW + user.getFormattedName();

                if (oldVote == null) {
                    plugin.messageAll(plugin.getLocale("voting.maps.player-voted", playerName, mapName));
                } else {
                    plugin.messageAll(plugin.getLocale("voting.maps.player-transfer", playerName, oldVote, mapName));
                }

                user.closeInventory();
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    public boolean checkTeamJoinAllowed(TeamColor newTeam, TeamColor oldTeam) {
        int players = plugin.getTeam(newTeam).getUserCount();

        if (players < (plugin.getPlayerCount() / TeamColor.values().size())) return true;

        for (TeamColor teamColor : TeamColor.values()) {
            int count = plugin.getTeam(teamColor).getUserCount();
            if (teamColor.equals(oldTeam)) --count;

            if (count < players) return false;
        }

        return true;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        User user = plugin.getUser(event.getPlayer());
        user.setTeamColor(null);
        user.setMapVote(null);

        UserQuitLobbyEvent userQuitLobbyEvent = new UserQuitLobbyEvent(user);

        for (LobbyMinigame minigame : plugin.getLobbyMinigames()) {
            minigame.onUserQuitLobby(userQuitLobbyEvent);
        }

        String name = ChatColor.YELLOW + event.getPlayer().getName();
        String online = Integer.toString(plugin.getPlayerCount() - 1);
        String max = Integer.toString(plugin.getServer().getMaxPlayers());
        event.setQuitMessage(plugin.getLocale("server.players.quit", name, online, max));

        //Old code from WBL, now moved to this class
        user.getStatsHolder().saveStats();
        user.onDisconnect();
        plugin.setUser(event.getPlayer(), null);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);


        ((Player) event.getEntity()).setSaturation(20);
    }
}
