package com.ithinkrok.mccw.data;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.enumeration.PlayerClass;
import com.ithinkrok.mccw.enumeration.TeamColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Created by paul on 01/11/15.
 *
 * Stores the player's info while they are online
 */
public class PlayerInfo {

    private Player player;
    private TeamColor teamColor;
    private WarsPlugin plugin;
    private Location shopBlock;
    private Inventory shopInventory;
    private PlayerClass playerClass;

    private int playerCash = 0;

    public PlayerInfo(WarsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public TeamColor getTeamColor() {
        return teamColor;
    }

    public int getPlayerCash() {
        return playerCash;
    }

    public void addPlayerCash(int cash){
        playerCash += cash;
        updateScoreboard();
    }

    public boolean subtractPlayerCash(int cash){
        if(cash > playerCash) return false;
        playerCash -= cash;

        updateScoreboard();

        return true;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
    }

    public boolean hasPlayerCash(int cash){
        return cash <= playerCash;
    }

    public void setTeamColor(TeamColor teamColor) {
        this.teamColor = teamColor;

        updateTeamArmor();
    }

    public Location getShopBlock() {
        return shopBlock;
    }

    public void setShopBlock(Location shopBlock) {
        this.shopBlock = shopBlock;
    }

    public Inventory getShopInventory() {
        return shopInventory;
    }

    public void setShopInventory(Inventory shopInventory) {
        this.shopInventory = shopInventory;
    }

    private void updateTeamArmor() {
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET, 1);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS, 1);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

        setArmorColor(helmet);
        setArmorColor(chestplate);
        setArmorColor(leggings);
        setArmorColor(boots);

        PlayerInventory inv = player.getInventory();

        inv.setHelmet(helmet);
        inv.setChestplate(chestplate);
        inv.setLeggings(leggings);
        inv.setBoots(boots);
    }

    private void setArmorColor(ItemStack armor){
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();

        meta.setColor(teamColor.armorColor);
        armor.setItemMeta(meta);
    }

    public void setupScoreboard(){
        Scoreboard scoreboard = player.getScoreboard();

        Objective mainObjective = scoreboard.getObjective("main");
        if(mainObjective == null) mainObjective = scoreboard.registerNewObjective("main", "dummy");
        mainObjective.setDisplayName("Stats:");
        mainObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        mainObjective.getScore("Player Money:").setScore(0);
        mainObjective.getScore("Team Money:").setScore(0);
    }

    public void updateScoreboard(){
        Scoreboard scoreboard = player.getScoreboard();

        Objective mainObjective = scoreboard.getObjective("main");
        mainObjective.getScore("Player Money:").setScore(getPlayerCash());

        TeamInfo teamInfo = plugin.getTeamInfo(teamColor);
        mainObjective.getScore("Team Money:").setScore(teamInfo.getTeamCash());
    }
}
