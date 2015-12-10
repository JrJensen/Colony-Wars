package com.ithinkrok.mccw.lobby;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.data.User;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

/**
 * Created by paul on 18/11/15.
 * <p>
 * Handles the parcour minigame
 */
public class ParcourMinigame implements LobbyMinigame {

    WarsPlugin plugin;
    private int basicParkourMoney, advancedParkourMoney, maxParkourMoney;

    public ParcourMinigame(WarsPlugin plugin) {
        this.plugin = plugin;

        basicParkourMoney = plugin.getWarsConfig().getParkourMoney("basic");
        advancedParkourMoney = plugin.getWarsConfig().getParkourMoney("advanced");
        maxParkourMoney = plugin.getWarsConfig().getParkourMoney("cap");
    }

    @Override
    public void resetMinigame() {

    }

    @Override
    public void onUserJoinLobby(User user) {

    }

    @Override
    public void onUserQuitLobby(User user) {

    }

    @Override
    public boolean onUserInteractEntity(User user, Entity entity) {
        return false;
    }

    @Override
    public boolean onUserInteractWorld(User user, Block block) {
        switch (block.getType()) {
            case ENDER_CHEST:
                addParkourMoney(user, basicParkourMoney, false);
                return true;
            case REDSTONE_LAMP_OFF:
                addParkourMoney(user, advancedParkourMoney, true);
                return true;
            default:
                return false;
        }


    }

    private void addParkourMoney(User user, int amount, boolean advanced) {
        amount = Math.min(amount, maxParkourMoney - user.getPlayerCash());

        if (amount <= 0) user.sendLocale("minigames.parcour.capped");
        else {
            user.addPlayerCash(amount);
            if (advanced) plugin.messageAllLocale("minigames.parcour.advanced", user.getFormattedName(), amount);
            else plugin.messageAllLocale("minigames.parcour.winner", user.getFormattedName(), amount);
        }

        user.teleport(plugin.getLobbySpawn());
    }
}
