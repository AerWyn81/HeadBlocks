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

    public static void launchFirework(Location loc, boolean isFlickering, boolean isColorsRandom, List<Color> colors, boolean isFadeColorsRandom, List<Color> fadeColors, int power, boolean isWalled) {
        if (loc.getWorld() == null) {
            return;
        }

        Firework firework = (Firework) loc.getWorld().spawnEntity(loc.add(0.5, isWalled ? 0.5 : 0, 0.5), EntityType.FIREWORK_ROCKET);
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
