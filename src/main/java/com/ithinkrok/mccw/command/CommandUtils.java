package com.ithinkrok.mccw.command;

import com.ithinkrok.mccw.data.User;

/**
 * Created by paul on 10/12/15.
 * <p>
 * Utilities for commands
 */
public class CommandUtils {

    public static boolean checkUser(WarsCommandSender sender) {
        if (!(sender instanceof User)) {
            sender.sendLocale("command.no-console");
            return false;
        }

        return true;
    }
}
