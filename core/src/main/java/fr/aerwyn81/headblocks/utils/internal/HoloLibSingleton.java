package fr.aerwyn81.headblocks.utils.internal;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.ConfigService;
import org.holoeasy.HoloEasy;
import org.holoeasy.pool.IHologramPool;

public class HoloLibSingleton {
    private static IHologramPool hologramPool;

    public static IHologramPool getHologramPool() {
        if (hologramPool == null) {
            hologramPool = HoloEasy.startPool(HeadBlocks.getInstance(), ConfigService.getHologramParticlePlayerViewDistance());
        }

        return hologramPool;
    }

    public static void updateViewDistance() {
        if (hologramPool == null) {
            return;
        }

        hologramPool = HoloEasy.startPool(HeadBlocks.getInstance(), ConfigService.getHologramParticlePlayerViewDistance());
    }
}
