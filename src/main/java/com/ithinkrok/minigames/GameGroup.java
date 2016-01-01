package com.ithinkrok.minigames;

import com.ithinkrok.minigames.event.UserBreakBlockEvent;
import com.ithinkrok.minigames.event.UserInGameChangeEvent;
import com.ithinkrok.minigames.event.UserJoinEvent;
import com.ithinkrok.minigames.event.UserPlaceBlockEvent;
import com.ithinkrok.minigames.util.EventExecutor;
import org.bukkit.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by paul on 31/12/15.
 */
public abstract class GameGroup<U extends User<U, T, G, M>, T extends Team<U, T, G>, G extends GameGroup<U, T, G, M>, M extends Game<U, T, G, M>> {

    private ConcurrentMap<UUID, U> usersInGroup = new ConcurrentHashMap<>();
    private Map<TeamColor, T> teamsInGroup = new HashMap<>();
    private M minigame;

    private Map<String, GameState> gameStates = new HashMap<>();
    private GameState gameState;

    private Constructor<T> teamConstructor;

    private World currentWorld;

    public GameGroup(M minigame, Constructor<T> teamConstructor) {
        this.minigame = minigame;
        this.teamConstructor = teamConstructor;

        boolean hasDefault = false;
        for (GameState gs : minigame.getGameStates()) {
            if(!hasDefault){
                this.gameState = gs;
                hasDefault = true;
            }

            this.gameStates.put(gs.getName(), gs);
        }
    }

    private T createTeam(TeamColor teamColor) {
        try {
            return teamConstructor.newInstance(teamColor, this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to construct Team", e);
        }
    }

    public void eventUserJoinedAsPlayer(UserJoinEvent<U> event) {
        usersInGroup.put(event.getUser().getUuid(), event.getUser());

        EventExecutor.executeEvent(event, gameState.getListener());
    }

    public void eventBlockBreak(UserBreakBlockEvent<U> event) {
        EventExecutor.executeEvent(event, gameState.getListener());
    }

    public void eventBlockPlace(UserPlaceBlockEvent<U> event) {
        EventExecutor.executeEvent(event, gameState.getListener());
    }

    public void eventUserInGameChanged(UserInGameChangeEvent<U> event) {
        EventExecutor.executeEvent(event, gameState.getListener());
    }
}
