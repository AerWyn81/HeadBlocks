package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HeadType;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import me.arcaniax.hdb.api.HeadDatabaseAPI;

public class HeadDatabaseHook extends HeadDatabaseAPI {
    private final HeadDatabaseAPI headDatabaseAPI;
    private final HeadBlocks main;

    public HeadDatabaseHook(HeadBlocks main) {
        this.main = main;
        this.headDatabaseAPI = new HeadDatabaseAPI();
    }

    public void loadHeadsHDB() {
        main.getHeadHandler().getHeads().stream()
                .filter(h -> !h.isLoaded() && h.getHeadType() == HeadType.HDB)
                .forEach(h -> {
                    h.setTexture(headDatabaseAPI.getBase64(h.getId()));
                    HeadUtils.applyTexture(h);
                });
    }
}
