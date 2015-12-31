package com.ithinkrok.cw;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by paul on 31/12/15.
 */
public class CWPlugin extends JavaPlugin {

    CWMinigame minigame;


    @Override
    public void onEnable() {
        super.onEnable();

        minigame = new CWMinigame(this);

        minigame.registerListeners();
    }
}
