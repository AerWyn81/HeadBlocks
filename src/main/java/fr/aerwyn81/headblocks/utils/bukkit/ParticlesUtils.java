package fr.aerwyn81.headblocks.utils.bukkit;

import com.cryptomorin.xseries.particles.XParticle;
import fr.aerwyn81.headblocks.HeadBlocks;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class ParticlesUtils {

    /**
     * Particle constants were renamed wholesale in 1.20.5 (REDSTONE became DUST, VILLAGER_HAPPY
     * became HAPPY_VILLAGER, ...), so {@link Particle#valueOf} throws on any config written for the
     * other side of that line. XParticle resolves aliases both ways: checked against spigot-api
     * 1.20.1, "HAPPY_VILLAGER" yields VILLAGER_HAPPY and "DUST" yields REDSTONE.
     *
     * @throws IllegalArgumentException when the name matches no particle on this server — including
     *                                  a name XSeries knows but this server version does not ship.
     */
    public static Particle resolve(String name) {
        return XParticle.of(name)
                .map(XParticle::get)
                .orElseThrow(() -> new IllegalArgumentException("Unknown particle type: " + name));
    }

    public static void spawn(Location loc, Particle particle, int amount, ArrayList<String> colors, Player player) {
        double size = amount == 1 ? 0 : .25f;
        Location location = loc.clone().add(0, .75f, 0);

        ArrayList<Particle.DustOptions> dustOptions = new ArrayList<>();

        Particle redstoneParticle = XParticle.DUST.get();

        if (colors != null && particle == redstoneParticle && !colors.isEmpty()) {
            for (String color : colors) {
                String[] rgb = color.split(",");
                dustOptions.add(new Particle.DustOptions(Color.fromRGB(Integer.parseInt(rgb[0]),
                        Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2])), 1));
            }
        }

        HeadBlocks.getScheduler().runNow(player, () -> {
            if (!dustOptions.isEmpty()) {
                dustOptions.forEach(dustOpt ->
                        player.spawnParticle(particle, location, amount, size, size, size, dustOpt));
                return;
            }

            player.spawnParticle(particle, location, amount, size, size, size, 0);
        });
    }
}
