package com.ithinkrok.mccw.command.executors;

import com.ithinkrok.mccw.command.WarsCommandExecutor;
import com.ithinkrok.mccw.command.WarsCommandSender;
import com.ithinkrok.mccw.data.User;
import com.ithinkrok.mccw.enumeration.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 10/12/15.
 * <p>
 * Handles the /list command
 */
public class ListExecutor implements WarsCommandExecutor {

    @Override
    public boolean onCommand(WarsCommandSender sender, Command command, String label, String[] args) {

        Map<TeamColor, List<User>> teams = new LinkedHashMap<>();

        teams.put(null, new ArrayList<>());
        for (TeamColor teamColor : TeamColor.values()) {
            teams.put(teamColor, new ArrayList<>());
        }

        for (User user : sender.getPlugin().getUsers()) {
            if (!user.isInGame()) teams.get(null).add(user);
            else teams.get(user.getTeamColor()).add(user);
        }

        sender.sendLocale("commands.list.title", sender.getPlugin().getUsers().size(),
                Bukkit.getServer().getMaxPlayers());

        for (Map.Entry<TeamColor, List<User>> entry : teams.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            StringBuilder names = new StringBuilder();

            for (User user : entry.getValue()) {
                if(!user.isPlayer()) continue;
                if (names.length() != 0) names.append(ChatColor.GOLD).append(", ");

                names.append(user.getFormattedName());
            }

            String teamName;
            if (entry.getKey() == null) teamName = sender.getPlugin().getLocale("commands.list.spectator");
            else teamName = entry.getKey().getFormattedName();

            //send a message directly to player to avoid Colony Wars prefix
            sender.sendLocaleDirect("commands.list.line", teamName, names);
        }
        return true;
    }
}
