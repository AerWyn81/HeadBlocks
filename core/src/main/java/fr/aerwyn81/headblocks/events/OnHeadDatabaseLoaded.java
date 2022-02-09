package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class OnHeadDatabaseLoaded implements Listener {
    private final HeadBlocks main;

    public OnHeadDatabaseLoaded(HeadBlocks main) {
        this.main = main;
    }

    @EventHandler
    public void onDatabaseLoad(DatabaseLoadEvent e) {
        main.getHeadHandler().loadConfiguration();
    }
}
