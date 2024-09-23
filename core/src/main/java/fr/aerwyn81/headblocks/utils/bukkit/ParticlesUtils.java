package fr.aerwyn81.headblocks.utils.bukkit;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class ParticlesUtils {

    public static void spawn(Location loc, Particle particle, int amount, ArrayList<String> colors, Player player) {
        double size = amount == 1 ? 0 : .25f;
        Location location = loc.clone().add(.5f, .75f, .5f);

        ArrayList<Particle.DustOptions> dustOptions = new ArrayList<>();

        Particle redstoneParticle;

        if (VersionUtils.isNewerThan(VersionUtils.v1_20_R4)) {
            redstoneParticle = Particle.valueOf("DUST");
        } else {
            redstoneParticle = Particle.REDSTONE;
        }

        if (colors != null && particle == redstoneParticle && !colors.isEmpty()) {
            for (String color : colors) {
                String[] rgb = color.split(",");
                dustOptions.add(new Particle.DustOptions(Color.fromRGB(Integer.parseInt(rgb[0]),
                        Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2])), 1));
            }
        }

        if (!dustOptions.isEmpty()) {
            dustOptions.forEach(dustOpt ->
                    player.spawnParticle(particle, location, amount, size, size, size, dustOpt));
            return;
        }

        player.spawnParticle(particle, location, amount, size, size, size, 0);
    }
}
