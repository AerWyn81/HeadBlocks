package fr.aerwyn81.headblocks.holograms;

import fr.aerwyn81.headblocks.holograms.types.DecentHologram;
import fr.aerwyn81.headblocks.holograms.types.DefaultHologram;
import fr.aerwyn81.headblocks.holograms.types.HolographicDisplaysHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class InternalHologram {
    private UUID uuid;
    private Location location;
    private IHologram hologram;

    public InternalHologram(EnumTypeHologram enumTypeHologram) {
        switch (enumTypeHologram) {
            case DECENT:
                hologram = new DecentHologram();
                break;
            case HD:
                hologram = new HolographicDisplaysHologram();
                break;
            case DEFAULT:
                hologram = new DefaultHologram();
                break;
            default:
                hologram = null;
                break;
        }
    }

    public void show(Player player) {
        hologram.show(player);
    }

    public void hide(Player player) {
        hologram.hide(player);
    }

    public void createHologram(UUID uuid, Location loc, List<String> lines, double configHeightAbove, int displayRange) {
        this.uuid = uuid;
        location = loc;

        var headLocation = loc.clone();
        headLocation.add(0.5, 0.5 + configHeightAbove, 0.5);

        hologram = hologram.create(uuid.toString(), headLocation, lines, displayRange);
    }

    public void deleteHologram() {
        hologram.delete();
    }

    public Location getLocation() {
        return location;
    }

    public IHologram getHologram() {
        return hologram;
    }

    public EnumTypeHologram getTypeHologram() {
        return hologram.getTypeHologram();
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isHologramVisible(Player player) {
        return hologram.isVisible(player);
    }
}
