package com.ithinkrok.minigames.map;

import com.ithinkrok.minigames.Game;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Created by paul on 01/01/16.
 */
public class GameMapInfo {

    private String name;
    private ConfigurationSection config;

    public GameMapInfo(Game game, String name) {
        this.name = name;
        this.config = game.loadConfig(getConfigName());
    }

    public String getName() {
        return name;
    }

    public String getConfigName() {
        return "maps/" + name + ".yml";
    }

    public String getDescription() {
        return config.getString("description");
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public String getMapFolder() {
        return config.getString("folder");
    }

    public World.Environment getEnvironment() {
        String envName = config.getString("environment", "normal").toUpperCase();

        return World.Environment.valueOf(envName);
    }

}
