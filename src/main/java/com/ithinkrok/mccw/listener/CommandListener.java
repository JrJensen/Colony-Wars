package com.ithinkrok.mccw.listener;

import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.data.Team;
import com.ithinkrok.mccw.data.User;
import com.ithinkrok.mccw.data.UserCategoryStats;
import com.ithinkrok.mccw.enumeration.GameState;
import com.ithinkrok.mccw.enumeration.PlayerClass;
import com.ithinkrok.mccw.enumeration.TeamColor;
import com.ithinkrok.mccw.handler.CountdownHandler;
import com.ithinkrok.mccw.util.item.InventoryUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Created by paul on 13/11/15.
 * <p>
 * Handles commands for Colony Wars
 */
public class CommandListener implements CommandExecutor {

    private WarsPlugin plugin;

    public CommandListener(WarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLocale("command.no-console"));
            return true;
        }

        Player player = (Player) sender;
        User user = plugin.getUser(player);

        switch (command.getName().toLowerCase()) {
            case "transfer":
                return onTransferCommand(user, args);
            case "test":
                return args.length >= 1 && onTestCommand(user, args);
            case "gamestate":
                return onGameStateCommand(user, args);
            case "members":
                return onMembersCommand(user);
            case "spawn":
                return onSpawnCommand(user);
            case "fix":
                return onFixCommand(user);
            case "countdown":
                return onCountdownCommand(user, args);
            case "stats":
                return onStatsCommand(user, args);
            case "list":
                return onListCommand(user);
            default:
                return false;
        }

    }

    private boolean onTransferCommand(User user, String[] args) {
        if (args.length < 1) return false;
        if (user.getTeam() == null) {
            user.messageLocale("commands.transfer.not-in-game");
            return true;
        }

        User target = null;
        if (args.length > 1) {
            String targetName = args[1];

            for (Player player : user.getTeam().getPlayers()) {
                if (!player.getName().equals(targetName)) continue;
                target = plugin.getUser(player);
            }

            if (target == null) {
                user.messageLocale("commands.transfer.no-player", targetName);
                return true;
            }
        }

        try {
            int amount = Integer.parseInt(args[0]);

            if (!user.subtractPlayerCash(amount)) {
                user.messageLocale("money.exchange.too-expensive");
                return true;
            }

            if (target == null) {
                Team team = user.getTeam();
                team.addTeamCash(amount);

                team.messageLocale("money.exchange.team-transfer", user.getFormattedName(), amount);
                team.messageLocale("money.balance.team.new", team.getTeamCash());
            } else {
                target.addPlayerCash(amount);

                user.getTeam().messageLocale("money.exchange.user-transfer", user.getFormattedName(), amount,
                        target.getFormattedName());

                target.messageLocale("money.balance.user.new", target.getPlayerCash());
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean onTestCommand(User user, String[] args) {

        switch (args[0]) {
            case "team":
                if (args.length < 2) return false;

                TeamColor teamColor = TeamColor.fromName(args[1]);
                if (teamColor == null) {
                    user.messageLocale("commands.test.invalid-team", args[1]);
                    return true;
                }
                user.setTeamColor(teamColor);

                user.messageLocale("commands.test.team-change", teamColor.getFormattedName());

                break;
            case "class":
                if (args.length < 2) return false;

                PlayerClass playerClass = PlayerClass.fromName(args[1]);
                if (playerClass == null) {
                    user.messageLocale("commands.test.invalid-class", args[1]);
                    return true;
                }
                user.setPlayerClass(playerClass);

                user.messageLocale("commands.test.class-change", playerClass.getFormattedName());

                break;
            case "money":

                user.addPlayerCash(10000);
                user.getTeam().addTeamCash(10000);

                user.messageLocale("commands.test.money", 10000);
                break;
            case "build":
                if (args.length < 2) return false;

                user.getPlayerInventory()
                        .addItem(InventoryUtils.createItemWithNameAndLore(Material.LAPIS_ORE, 16, 0, args[1]));

                user.messageLocale("commands.test.build-blocks", 16, args[1]);
                break;
            case "base_location":
                Team team = user.getTeam();
                if (team == null) {
                    user.messageLocale("commands.test.base.no-team");
                    break;
                }

                user.messageLocale("commands.test.base.loc", team.getBaseLocation());
                break;
            case "shrink":
                if (plugin.getShowdownArena() == null) return true;
                if (args.length < 2) return false;

                try {
                    int amount = Integer.parseInt(args[1]);

                    if (amount < 1 || amount > 30) {
                        user.messageLocale("commands.shrink.bad-size", args[1]);
                        return true;
                    }

                    for (int count = 0; count < amount; ++count) {
                        plugin.getShowdownArena().shrinkArena(plugin);
                    }

                    return true;
                } catch (NumberFormatException e) {
                    user.messageLocale("commands.shrink.bad-size", args[1]);
                    return true;
                }

        }

        return true;
    }

    private boolean onGameStateCommand(User user, String[] args) {
        try {
            GameState gameState = GameState.valueOf(args[0].toUpperCase());
            plugin.changeGameState(gameState);
            user.messageLocale("commands.gamestate.changed", gameState);
            return true;
        } catch (Exception e) {
            user.messageLocale("commands.gamestate.invalid");
            return false;
        }
    }

    private boolean onMembersCommand(User user) {
        if (user.getTeamColor() == null) {
            user.messageLocale("commands.members.no-team");
            return true;
        }

        user.messageLocale("commands.members.title");

        Team team = user.getTeam();

        for (Player player : team.getPlayers()) {
            User other = plugin.getUser(player);

            String name = other.getFormattedName();
            String playerClass = other.getPlayerClass() == null ? plugin.getLocale("team.none") :
                    other.getPlayerClass().getFormattedName();

            String nearestBase = null;
            if (plugin.isInGame()) {
                double smallestDistSquared = 99999999;

                for (TeamColor teamColor : TeamColor.values()) {
                    Location loc = plugin.getMapSpawn(teamColor);
                    double distSquared = loc.distanceSquared(player.getLocation());

                    if (distSquared < smallestDistSquared) {
                        smallestDistSquared = distSquared;
                        nearestBase = teamColor.getFormattedName();
                    }
                }

                if (plugin.getMapSpawn(null).distanceSquared(player.getLocation()) < smallestDistSquared) {
                    nearestBase = plugin.getLocale("commands.members.near-showdown", nearestBase);
                }
            } else {
                nearestBase = "Not in game";
            }

            //Send the player a message directly to avoid the chat prefix
            user.getPlayer()
                    .sendMessage(plugin.getLocale("commands.members.player-info", name, playerClass, nearestBase));
        }

        return true;
    }

    private boolean onSpawnCommand(User user) {
        if (user.isInGame()) {
            user.messageLocale("commands.spawn.not-in-game");
            return true;
        }

        user.teleport(plugin.getMapSpawn(null));
        return true;
    }

    private boolean onFixCommand(User user) {
        if (!plugin.isInGame()) return true;
        if (!user.startCoolDown("fix", 1, plugin.getLocale("cooldowns.fix.finished"))) return true;
        user.teleport(user.getPlayer().getLocation().add(0, 0.4d, 0));
        user.getPlayer().setVelocity(new Vector(0, -1, 0));
        return true;
    }

    private boolean onCountdownCommand(User user, String[] args) {
        if (args.length < 1) return false;

        CountdownHandler handler = plugin.getCountdownHandler();
        if (!handler.isCountingDown()) {
            user.messageLocale("commands.countdown.none");
            return true;
        }

        try {
            int amount = Integer.parseInt(args[0]);

            int newTime = Math.max(handler.getCountDownTime() + amount, 1);
            handler.setCountDownTime(newTime);

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean onStatsCommand(User user, String[] args) {
        if (!plugin.hasPersistence()) {
            user.messageLocale("commands.stats.disabled");
            return true;
        }

        String category = "total";
        if (args.length > 0) category = args[0];

        UserCategoryStats stats = user.getStats(category);
        if (stats == null) {
            user.messageLocale("commands.stats.none", category);
            return true;
        }

        user.messageLocale("commands.stats.category", category);

        String kd = "NA";
        if (stats.getDeaths() > 0) kd = Double.toString(stats.getKills() / (double) stats.getDeaths());

        user.getPlayer().sendMessage(plugin.getLocale("commands.stats.kills", stats.getKills()));
        user.getPlayer().sendMessage(plugin.getLocale("commands.stats.deaths", stats.getDeaths()));
        user.getPlayer().sendMessage(plugin.getLocale("commands.stats.kd", kd));
        user.getPlayer().sendMessage(plugin.getLocale("commands.stats.wins", stats.getGameWins()));
        user.getPlayer().sendMessage(plugin.getLocale("commands.stats.losses", stats.getGameLosses()));
        user.getPlayer().sendMessage(plugin.getLocale("commands.stats.games", stats.getGames()));
        user.getPlayer().sendMessage(plugin.getLocale("commands.stats.totalmoney", stats.getTotalMoney()));
        user.getPlayer().sendMessage(plugin.getLocale("commands.stats.score", stats.getScore()));

        return true;
    }

    private boolean onListCommand(User sender) {
        Map<TeamColor, List<User>> teams = new LinkedHashMap<>();

        teams.put(null, new ArrayList<>());
        for (TeamColor teamColor : TeamColor.values()) {
            teams.put(teamColor, new ArrayList<>());
        }

        for (User user : plugin.getUsers()) {
            if (!user.isInGame()) teams.get(null).add(user);
            else teams.get(user.getTeamColor()).add(user);
        }

        sender.messageLocale("commands.list.title");

        for (Map.Entry<TeamColor, List<User>> entry : teams.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            StringBuilder names = new StringBuilder();

            for (User user : entry.getValue()) {
                if (names.length() != 0) names.append(ChatColor.GOLD).append(", ");

                names.append(user.getFormattedName());
            }

            String teamName;
            if(entry.getKey() == null) teamName = plugin.getLocale("commands.list.spectator");
            else teamName = entry.getKey().getFormattedName();

            //send a message directly to player to avoid Colony Wars prefix
            sender.getPlayer().sendMessage(plugin.getLocale("commands.list.line", teamName, names));
        }
        return true;
    }
}
