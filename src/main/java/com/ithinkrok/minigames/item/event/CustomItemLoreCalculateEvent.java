package com.ithinkrok.minigames.item.event;

import com.ithinkrok.minigames.User;
import com.ithinkrok.minigames.item.CustomItem;
import com.ithinkrok.minigames.lang.LanguageLookup;
import com.ithinkrok.minigames.util.math.Variables;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * Created by paul on 03/01/16.
 */
public class CustomItemLoreCalculateEvent<U extends User> extends Event{

    private final CustomItem<U> customItem;
    private final List<String> lore;
    private final LanguageLookup languageLookup;
    private final Variables variables;

    public CustomItemLoreCalculateEvent(CustomItem<U> customItem, List<String> lore, LanguageLookup languageLookup,
                                        Variables variables) {
        this.customItem = customItem;
        this.lore = lore;
        this.languageLookup = languageLookup;
        this.variables = variables;
    }

    public CustomItem<U> getCustomItem() {
        return customItem;
    }

    public List<String> getLore() {
        return lore;
    }

    public LanguageLookup getLanguageLookup() {
        return languageLookup;
    }

    public Variables getVariables() {
        return variables;
    }

    @Override
    public HandlerList getHandlers() {
        return null;
    }
}
