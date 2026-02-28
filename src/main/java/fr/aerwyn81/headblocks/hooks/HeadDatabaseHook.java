package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.events.OnHeadDatabaseLoaded;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.arcaniax.hdb.enums.CategoryEnum;
import me.arcaniax.hdb.object.head.Head;
import org.bukkit.Bukkit;

import java.util.List;

public class HeadDatabaseHook {
    private HeadDatabaseAPI headDatabaseAPI;

    public boolean init() {
        var plugin = HeadBlocks.getInstance();

        try {
            headDatabaseAPI = new HeadDatabaseAPI();
        } catch (NoClassDefFoundError ex) {
            LogUtil.error("Error loading HeadDatabaseAPI support: {0}. Please try to update HeadDatabase plugin or report the error on HeadBlocks discord.", ex.getMessage());
            return false;
        }

        try {
            var fields = headDatabaseAPI.getClass().getDeclaredFields();
            if (fields.length == 0) {
                throw new RuntimeException("Too old version, API not compatible.");
            }
        } catch (Exception ex) {
            LogUtil.error("Error loading HeadDatabaseAPI support: {0}. Please try to update HeadDatabase plugin or report the error on HeadBlocks discord.", ex.getMessage());
            return false;
        }

        Bukkit.getPluginManager().registerEvents(new OnHeadDatabaseLoaded(plugin), plugin);

        // Plugman/HeadDatabase issue
        // OnHeadDatabaseLoaded event is not fired on plugman reload command
        try {
            // If the list is not empty, then the database is already loaded
            List<Head> heads = headDatabaseAPI.getHeads(CategoryEnum.ALPHABET);
            if (heads != null && !heads.isEmpty()) {
                this.loadHeadsHDB();
            }
        } catch (Exception ignored) {
        }

        LogUtil.info("HeadDatabase successfully hooked!");
        return true;
    }

    public void loadHeadsHDB() {
        HeadService.getHeads().stream()
                .filter(HBHeadHDB.class::isInstance)
                .map(HBHeadHDB.class::cast)
                .filter(h -> !h.isLoaded())
                .forEach(h -> {
                    var texture = headDatabaseAPI.getBase64(h.getId());
                    if (texture == null || texture.isEmpty()) {
                        LogUtil.error("HeadDatabase head id {0} is not found. Please check if the head id exists.", h.getId());
                        return;
                    }

                    HeadUtils.createHead(h, texture);
                    h.setLoaded(true);
                    LogUtil.info("Loaded HeadDatabase head id {0}.", h.getId());
                });
    }
}
