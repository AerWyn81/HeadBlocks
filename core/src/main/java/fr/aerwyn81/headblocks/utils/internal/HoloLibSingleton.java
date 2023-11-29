package fr.aerwyn81.headblocks.utils.internal;

import com.github.unldenis.hologram.HologramPool;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.ConfigService;

public class HoloLibSingleton {
    private static HologramPool hologramPool;

    public static HologramPool getHologramPool() {
        if (hologramPool == null) {
            hologramPool = new HologramPool(HeadBlocks.getInstance(), ConfigService.getHologramParticlePlayerViewDistance());
        }

        return hologramPool;
    }

    public static void updateViewDistance() {
        if (hologramPool == null) {
            return;
        }

        hologramPool.updateSpawnDistance(ConfigService.getHologramParticlePlayerViewDistance());
    }
}
