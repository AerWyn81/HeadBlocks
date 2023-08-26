package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.events.OnHeadDatabaseLoaded;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.arcaniax.hdb.enums.CategoryEnum;
import me.arcaniax.hdb.object.head.Head;
import org.bukkit.Bukkit;

import java.util.List;

public class HeadDatabaseHook {
    private final HeadDatabaseAPI headDatabaseAPI;

    public HeadDatabaseHook() {
        var plugin = HeadBlocks.getInstance();

        headDatabaseAPI = new HeadDatabaseAPI();
        Bukkit.getPluginManager().registerEvents(new OnHeadDatabaseLoaded(plugin), plugin);

        // Plugman/HeadDatabase issue
        // OnHeadDatabaseLoaded event is not fired on plugman reload command
        try {
            // If the list is not empty, then the database is already loaded
            List<Head> heads = headDatabaseAPI.getHeads(CategoryEnum.ALPHABET);
            if (heads != null && !heads.isEmpty()) {
                this.loadHeadsHDB();
            }
        } catch (Exception ignored) { }
    }

    public void loadHeadsHDB() {
        HeadService.getHeads().stream()
                .filter(h -> h instanceof HBHeadHDB)
                .map(h -> (HBHeadHDB) h)
                .filter(h -> !h.isLoaded())
                .forEach(h -> {
                    HeadUtils.createHead(h, headDatabaseAPI.getBase64(h.getId()));
                    h.setLoaded(true);
                });
    }
}
