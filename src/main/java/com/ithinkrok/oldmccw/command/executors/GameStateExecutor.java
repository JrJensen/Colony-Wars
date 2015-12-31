package com.ithinkrok.oldmccw.command.executors;

import com.ithinkrok.oldmccw.command.WarsCommandExecutor;
import com.ithinkrok.oldmccw.command.WarsCommandSender;
import com.ithinkrok.oldmccw.enumeration.GameState;
import org.bukkit.command.Command;

/**
 * Created by paul on 10/12/15.
 *
 * Handles the /gamestate command
 */
public class GameStateExecutor implements WarsCommandExecutor {

    @Override
    public boolean onCommand(WarsCommandSender sender, Command command, String label, String[] args) {
        try {
            GameState gameState = GameState.valueOf(args[0].toUpperCase());
            sender.getPlugin().changeGameState(gameState);
            sender.sendLocale("commands.gamestate.changed", gameState);
            return true;
        } catch (Exception e) {
            sender.sendLocale("commands.gamestate.invalid");
            return false;
        }
    }
}
