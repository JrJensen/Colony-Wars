package com.ithinkrok.cw.command;

import com.ithinkrok.minigames.User;
import com.ithinkrok.minigames.command.Command;
import com.ithinkrok.minigames.command.CommandSender;
import com.ithinkrok.minigames.command.GameCommandExecutor;
import com.ithinkrok.minigames.team.TeamIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 22/01/16.
 */
public class ListCommand implements GameCommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command) {
        if(!command.requireGameGroup(sender)) return true;

        Map<TeamIdentifier, List<User>> teams = new LinkedHashMap<>();

        teams.put(null, new ArrayList<>());
        for(TeamIdentifier id : command.getGameGroup().getTeamIdentifiers()) {
            teams.put(id, new ArrayList<>());
        }

        int zombieCount = 0;

        for(User user : command.getGameGroup().getUsers()) {
            if(!user.isPlayer()) {
                ++zombieCount;
                continue;
            }
            teams.get(user.getTeamIdentifier()).add(user);
        }

        sender.sendLocale("command.list.title", command.getGameGroup().getUserCount() - zombieCount,
                Bukkit.getServer().getMaxPlayers());

        for (Map.Entry<TeamIdentifier, List<User>> entry : teams.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            StringBuilder names = new StringBuilder();

            for (User user : entry.getValue()) {
                if (names.length() != 0) names.append(ChatColor.GOLD).append(", ");

                names.append(user.getFormattedName());
            }

            String teamName;
            if (entry.getKey() == null) teamName = command.getGameGroup().getLocale("command.list.spectator");
            else teamName = entry.getKey().getFormattedName();

            //send a message directly to player to avoid Colony Wars prefix
            sender.sendLocaleNoPrefix("command.list.line", teamName, names);
        }

        if(zombieCount != 0) {
            sender.sendLocaleNoPrefix("command.list.zombies", zombieCount);
        }

        return true;
    }
}
