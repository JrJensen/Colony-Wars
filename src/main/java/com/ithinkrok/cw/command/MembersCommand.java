package com.ithinkrok.cw.command;

import com.ithinkrok.cw.metadata.CWTeamStats;
import com.ithinkrok.minigames.User;
import com.ithinkrok.minigames.command.Command;
import com.ithinkrok.minigames.command.CommandSender;
import com.ithinkrok.minigames.command.GameCommandExecutor;
import com.ithinkrok.minigames.team.Team;
import com.ithinkrok.minigames.team.TeamIdentifier;
import org.bukkit.Location;

/**
 * Created by paul on 22/01/16.
 */
public class MembersCommand implements GameCommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command) {
        if(!command.requireGameGroup(sender) || !command.requireTeamIdentifier(sender)) return true;
        if(!command.requireOthersPermission(sender, "mccw.members.others")) return true;

        Team team = command.getGameGroup().getTeam(command.getTeamIdentifier());

        sender.sendLocale("command.members.title");

        for (User other : team.getUsers()) {

            String name = other.getFormattedName();
            String kit = other.getKit() == null ? command.getGameGroup().getLocale("command.members.no_team") :
                    other.getKit().getFormattedName();

            String nearestBase = null;
            if (!command.getGameGroup().getCurrentGameState().getName().equals("lobby")) {
                double smallestDistSquared = 99999999;

                for (TeamIdentifier id : command.getGameGroup().getTeamIdentifiers()) {
                    Location loc = CWTeamStats.getOrCreate(command.getGameGroup().getTeam(id)).getBaseLocation();
                    double distSquared = loc.distanceSquared(other.getLocation());

                    if (distSquared < smallestDistSquared) {
                        smallestDistSquared = distSquared;
                        nearestBase = id.getFormattedName();
                    }
                }

                if (command.getGameGroup().getCurrentMap().getSpawn().distanceSquared(other.getLocation()) <
                        smallestDistSquared) {
                    nearestBase = command.getGameGroup().getLocale("command.members.near_showdown", nearestBase);
                }
            } else {
                nearestBase = command.getGameGroup().getLocale("command.members.not_in_game");
            }

            //Send the player a message directly to avoid the chat prefix
            sender.sendLocaleNoPrefix("command.members.player_info", name, kit, nearestBase);
        }

        return true;
    }
}
