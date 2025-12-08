package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;

public class FireworkUtils {

    /**
     * Launch a firework at a location.
     * IMPORTANT: This method must be called from a region-aware context (e.g., from runAtLocation).
     * Entity spawning must be on the region thread, not asynchronously.
     * 
     * @param loc Location to launch firework at
     * @param isFlickering Whether the firework should flicker
     * @param isColorsRandom Whether to use random colors
     * @param colors List of colors (if not random)
     * @param isFadeColorsRandom Whether to use random fade colors
     * @param fadeColors List of fade colors (if not random)
     * @param power Firework power
     * @param isWalled Whether this is a wall head
     */
    public static void launchFirework(Location loc, boolean isFlickering, boolean isColorsRandom, List<Color> colors, boolean isFadeColorsRandom, List<Color> fadeColors, int power, boolean isWalled) {
        if (loc.getWorld() == null) {
            return;
        }

        EntityType entityType;
        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            entityType = EntityType.valueOf("FIREWORK_ROCKET");
        } else {
            entityType = EntityType.FIREWORK;
        }

        Firework firework = (Firework) loc.getWorld().spawnEntity(loc.add(0.5, isWalled ? 0.5 : 0, 0.5), entityType);
        FireworkMeta fMeta = firework.getFireworkMeta();

        FireworkEffect.Builder fBuilder = FireworkEffect.builder()
                .flicker(isFlickering)
                .with(FireworkEffect.Type.BALL)
                .trail(false);

        if (isColorsRandom) {
            fBuilder.withColor(MessageUtils.getRandomColors());
        } else {
            fBuilder.withColor(colors);
        }

        if (isFadeColorsRandom) {
            fBuilder.withFade(MessageUtils.getRandomColors());
        } else {
            fBuilder.withFade(fadeColors);
        }

        fMeta.addEffects(fBuilder.build());

        fMeta.setPower(power);
        firework.setFireworkMeta(fMeta);

        if (power == 0) {
            firework.detonate();
        }
    }
}
