package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class ParticlesUtils {

    /**
     * Spawn particles at a location for a player.
     * IMPORTANT: This method must be called from a region-aware context (e.g., from runAtLocation).
     * Particles must be spawned on the region thread, not asynchronously.
     * 
     * @param loc Location to spawn particles at
     * @param particle Particle type
     * @param amount Amount of particles
     * @param colors Colors for REDSTONE particles (can be null)
     * @param player Player to show particles to
     */
    public static void spawn(Location loc, Particle particle, int amount, ArrayList<String> colors, Player player) {
        double size = amount == 1 ? 0 : .25f;
        Location location = loc.clone().add(0, .75f, 0);

        ArrayList<Particle.DustOptions> dustOptions = new ArrayList<>();

        Particle redstoneParticle;

        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
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

        // Particles must be spawned on the region thread (not async)
        // Caller should ensure this is called from runAtLocation context
        if (!dustOptions.isEmpty()) {
            dustOptions.forEach(dustOpt ->
                    player.spawnParticle(particle, location, amount, size, size, size, dustOpt));
        } else {
            player.spawnParticle(particle, location, amount, size, size, size, 0);
        }
    }
}
