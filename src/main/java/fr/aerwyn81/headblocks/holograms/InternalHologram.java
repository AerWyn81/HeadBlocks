package fr.aerwyn81.headblocks.holograms;

import fr.aerwyn81.headblocks.holograms.types.AdvancedHologram;
import fr.aerwyn81.headblocks.holograms.types.BasicHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class InternalHologram {
    private UUID uuid;
    private Location location;
    private IHologram hologram;

    public InternalHologram(EnumTypeHologram enumTypeHologram) {
        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            hologram = new AdvancedHologram();
            return;
        }

        hologram = new BasicHologram();
    }

    public void show(Player player) {
        hologram.show(player);
    }

    public void hide(Player player) {
        hologram.hide(player);
    }

    public void createHologram(UUID uuid, Location loc, List<String> lines, double configHeightAbove) {
        this.uuid = uuid;
        location = loc;

        var headLocation = loc.clone();
        headLocation.add(0, 0.5 + configHeightAbove, 0);

        hologram = hologram.create(uuid.toString(), headLocation, lines);
    }

    public void deleteHologram() {
        hologram.delete();
    }

    public Location getLocation() {
        return location;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isHologramVisible(Player player) {
        return hologram.isVisible(player);
    }

    public void refresh(Player player) {
        hologram.refresh(player);
    }
}
