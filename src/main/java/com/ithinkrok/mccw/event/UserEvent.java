package com.ithinkrok.mccw.event;

import com.ithinkrok.mccw.data.Team;
import com.ithinkrok.mccw.data.User;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

/**
 * Created by paul on 18/11/15.
 * <p>
 * An event involving a user
 */
public abstract class UserEvent {

    protected User user;

    public UserEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }


    public Team getTeam() {
        return user.getTeam();
    }

    public PlayerInventory getUserInventory() {
        return user.getPlayerInventory();
    }

    public Entity getEntity() {
        return user.getEntity();
    }
}
