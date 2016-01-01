package com.ithinkrok.minigames.map;

import com.ithinkrok.minigames.lang.LanguageLookup;
import org.bukkit.World;

/**
 * Created by paul on 01/01/16.
 */
public class GameMap implements LanguageLookup {

    private GameMapInfo gameMapInfo;
    private World world;
    private LanguageLookup languageLookup;

    public GameMap(GameMapInfo gameMapInfo) {
        this.gameMapInfo = gameMapInfo;
    }

    @Override
    public String getLocale(String name) {
        return languageLookup.getLocale(name);
    }

    @Override
    public String getLocale(String name, Object... args) {
        return languageLookup.getLocale(name, args);
    }

    @Override
    public boolean hasLocale(String name) {
        return languageLookup.hasLocale(name);
    }
}
