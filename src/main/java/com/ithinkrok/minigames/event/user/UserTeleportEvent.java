package com.ithinkrok.minigames.event.user;

import com.ithinkrok.minigames.User;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;

/**
 * Created by paul on 01/01/16.
 */
public class UserTeleportEvent<U extends User> extends UserEvent<U> implements Cancellable{

    private final Location from;
    private Location to;

    private boolean cancelled = false;

    public UserTeleportEvent(U user, Location from, Location to) {
        super(user);
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public void setTo(Location to) {
        this.to = to;
    }
}