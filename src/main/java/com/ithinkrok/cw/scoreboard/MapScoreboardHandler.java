package com.ithinkrok.cw.scoreboard;

import com.ithinkrok.minigames.User;
import com.ithinkrok.minigames.metadata.MapVote;
import com.ithinkrok.minigames.user.scoreboard.ScoreboardDisplay;
import com.ithinkrok.minigames.user.scoreboard.ScoreboardHandler;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Created by paul on 23/01/16.
 */
public class MapScoreboardHandler implements ScoreboardHandler {


    private String displayName;
    private List<String> votableMaps;

    public MapScoreboardHandler(User user) {
        ConfigurationSection config = user.getSharedObject("map_scoreboard");

        String displayNameLocale = config.getString("title");
        displayName = user.getLanguageLookup().getLocale(displayNameLocale);

        votableMaps = config.getStringList("votable_maps");
    }

    @Override
    public void updateScoreboard(User user, ScoreboardDisplay scoreboard) {
        for(String map : votableMaps) {
            int score = MapVote.getVotesForMap(user.getGameGroup().getUsers(), map);

            if(score == 0) scoreboard.removeScore(map);
            else scoreboard.setScore(map, score);
        }
    }

    @Override
    public void setupScoreboard(User user, ScoreboardDisplay scoreboard) {
        scoreboard.setDisplayName(displayName);
        scoreboard.resetAndDisplay();
    }
}
