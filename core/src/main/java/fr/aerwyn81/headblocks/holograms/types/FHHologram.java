package fr.aerwyn81.headblocks.holograms.types;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.data.property.visibility.Visibility;
import de.oliver.fancyholograms.api.hologram.Hologram;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.IHologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class FHHologram implements IHologram {
    Hologram hologram;
    final HologramManager manager;

    public FHHologram() {
        manager = FancyHologramsPlugin.get().getHologramManager();
    }

    @Override
    public void show(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(HeadBlocks.getInstance(), () -> {
            hologram.forceShowHologram(player);
            hologram.forceUpdateShownStateFor(player);
        });
    }

    @Override
    public void hide(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(HeadBlocks.getInstance(), () -> {
            hologram.hideHologram(player);
            hologram.forceHideHologram(player);
        });
    }

    @Override
    public void delete() {
        Bukkit.getScheduler().runTaskAsynchronously(HeadBlocks.getInstance(),
                () -> {
                    for (var uuid : hologram.getViewers()) {
                        var p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            hologram.forceHideHologram(p);
                        }
                    }
                    hologram.deleteHologram();
                });
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines, int displayRange) {
        var hologramData = new TextHologramData(UUID.randomUUID().toString(), location);

        hologramData.setBillboard(Display.Billboard.CENTER);

        hologramData.setText(lines)
                .setBackground(Hologram.TRANSPARENT)
                .setSeeThrough(false)
                .setTextShadow(false)
                .setVisibilityDistance(displayRange)
                .setVisibility(Visibility.ALL)
                .setPersistent(false);

        hologram = manager.create(hologramData);
        return this;
    }

    @Override
    public EnumTypeHologram getTypeHologram() {
        return EnumTypeHologram.FH;
    }

    @Override
    public boolean isVisible(Player player) {
        return hologram.isViewer(player);
    }
}
