package com.ithinkrok.cw.command;

import com.ithinkrok.minigames.command.Command;
import com.ithinkrok.minigames.command.CommandSender;
import com.ithinkrok.minigames.command.GameCommandExecutor;
import com.ithinkrok.minigames.metadata.Money;
import com.ithinkrok.minigames.util.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by paul on 13/01/16.
 */
public class CWCommand implements GameCommandExecutor {

    private Map<String, GameCommandExecutor> subExecutors = new HashMap<>();


    public CWCommand() {
        subExecutors.put("money", this::moneyCommand);
        subExecutors.put("building", this::buildingCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command) {
        if(!command.requireArgumentCount(sender, 1)) return false;

        Command subCommand = command.subCommand();

        if(!subExecutors.containsKey(subCommand.getCommand())) {
            sender.sendLocale("command.cw.unknown", subCommand.getCommand());
            return true;
        }

        if(!Command.requirePermission(sender, "command.cw." + subCommand.getCommand())) return true;

        return subExecutors.get(subCommand.getCommand()).onCommand(sender, subCommand);
    }

    private boolean moneyCommand(CommandSender sender, Command command) {
        if(!command.requireUser(sender)) return true;

        int amount = command.getIntArg(0, 10000);

        Money userMoney = Money.getOrCreate(command.getUser());
        userMoney.addMoney(amount, true);

        if(command.getUser().getTeam() == null) return true;
        int teamAmount = command.getIntArg(1, amount);

        Money teamMoney = Money.getOrCreate(command.getUser().getTeam());
        teamMoney.addMoney(teamAmount, true);

        return true;
    }

    private boolean buildingCommand(CommandSender sender, Command command) {
        if(!command.requireUser(sender)) return true;
        if(!command.requireArgumentCount(sender, 1)) return true;

        String buildingName = command.getStringArg(0, null);
        if(command.getGameGroup().getSchematic(buildingName) == null) {
            sender.sendLocale("command.cw.building.unknown", buildingName);
            return true;
        }

        int amount = command.getIntArg(1, 16);

        ItemStack item = InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, amount, 0, buildingName);

        command.getUser().getInventory().addItem(item);

        return true;
    }
}
