package fr.aerwyn81.headblocks.utils;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;

public class FireworkUtils {
    public static void launchFirework(Location loc, boolean isFlickering, boolean isColorsRandom, List<Color> colors, boolean isFadeColorsRandom, List<Color> fadeColors, int power) {
        Firework firework = (Firework) loc.getWorld().spawnEntity(loc.add(0.5, 0, 0.5), EntityType.FIREWORK);
        FireworkMeta fMeta = firework.getFireworkMeta();

        FireworkEffect.Builder fBuilder = FireworkEffect.builder()
                .flicker(isFlickering)
                .with(FireworkEffect.Type.BALL)
                .trail(false);

        if (isColorsRandom) {
            fBuilder.withColor(FormatUtils.getRandomColors());
        } else {
            fBuilder.withColor(colors);
        }

        if (isFadeColorsRandom) {
            fBuilder.withFade(FormatUtils.getRandomColors());
        } else {
            fBuilder.withFade(fadeColors);
        }

        fMeta.addEffects(fBuilder.build());

        fMeta.setPower(power);
        firework.setFireworkMeta(fMeta);
    }
}
