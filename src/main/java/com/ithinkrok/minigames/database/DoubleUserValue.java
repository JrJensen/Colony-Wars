package com.ithinkrok.minigames.database;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by paul on 17/01/16.
 */
@Entity
@Table(name = "mg_user_doubles")
public class DoubleUserValue {

    @Id
    private int id;

    @Column
    private String playerUUID;

    @Column
    private String property;

    @Column
    private double value;

    @Version
    private Date version;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Date getVersion() {
        return version;
    }

    public void setVersion(Date version) {
        this.version = version;
    }
}
