package com.ithinkrok.mccw.util;

import com.avaje.ebean.Query;
import com.ithinkrok.mccw.WarsPlugin;
import com.ithinkrok.mccw.data.UserCategoryStats;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by paul on 28/11/15.
 *
 * Handles saving player stats to database
 */
public class Persistence extends Thread{

    private final WarsPlugin plugin;
    private final Object writeLock = new Object();

    private boolean stop = false;
    private ConcurrentLinkedQueue<Runnable> threadTasks = new ConcurrentLinkedQueue<>();


    public Persistence(WarsPlugin plugin) {
        this.plugin = plugin;
        checkDDL();

        start();
    }

    public void onPluginDisabled() {
        stop = true;

        try {
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(!stop) {
            Runnable runnable;

            while((runnable = threadTasks.poll()) != null) {
                runnable.run();
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                plugin.getLogger().warning("Persistence thread interrupted");
                e.printStackTrace();
                return;
            }
        }
    }

    private void checkDDL(){
        try{
            plugin.getDatabase().find(UserCategoryStats.class).findRowCount();
        } catch(PersistenceException e){
            plugin.installDDL();
        }
    }

    public UserCategoryStats getUserCategoryStats(UUID playerUUID, String category){
        return query(playerUUID, category).findUnique();
    }

    public UserCategoryStats getOrCreateUserCategoryStats(UUID playerUUID, String category){
        Query<UserCategoryStats> query = query(playerUUID, category);

        synchronized (writeLock) {
            UserCategoryStats result = query.findUnique();
            if (result != null) return result;

            result = plugin.getDatabase().createEntityBean(UserCategoryStats.class);

            result.setPlayerUUID(playerUUID);
            result.setCategory(category);

            plugin.getDatabase().save(result);

            //TODO prevent two being created at once

            return query.findUnique();
        }
    }

    public void saveUserCategoryStats(UserCategoryStats stats){
        try {
            plugin.getDatabase().save(stats);
        } catch(OptimisticLockException e){
            plugin.getLogger().warning("Failed to save stats");
            e.printStackTrace();
        }
    }

    private Query<UserCategoryStats> query(UUID playerUUID, String category){
        Query<UserCategoryStats> query = plugin.getDatabase().find(UserCategoryStats.class);

        query.where().eq("player_uuid", playerUUID.toString()).eq("category", category);

        return query;
    }
}
