package fr.aerwyn81.headblocks.holograms.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.holograms.IHologram;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.line.DisplayTextLine;
import org.holoeasy.line.Line;
import org.holoeasy.pool.IHologramPool;

import java.util.List;

public class AdvancedHologram implements IHologram {

    Hologram hologram;

    @Override
    public void show(Player player) {
        // Used only by Default hologram.
    }

    @Override
    public void hide(Player player) {
        hologram.hide(player);
    }

    @Override
    public void delete() {
        Bukkit.getScheduler().runTaskAsynchronously(HeadBlocks.getInstance(), () -> {
            var pool = getPool();
            if (pool != null) {
                hologram.hide(getPool());
            }
        });
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines) {
        hologram = new Hologram(HeadBlocks.getInstance().getHoloEasyLib(), location);

        Bukkit.getScheduler().runTaskAsynchronously(HeadBlocks.getInstance(), () -> {
            ConfigService.getHologramsAdvancedLines().forEach(l -> hologram.getLines().add(
                    new DisplayTextLine(hologram, player ->
                    {
                        if (!l.contains("%state%")) {
                            return PlaceholdersService.parse(player.getName(), player.getUniqueId(), l);
                        }

                        try {
                            var head = HeadService.getHeadAt(location);
                            if (head == null) {
                                return PlaceholdersService.parse(player.getName(), player.getUniqueId(), l);
                            }

                            var hasHeadFormatted = ConfigService.getHologramAdvancedNotFoundPlaceholder();
                            var hasHead = StorageService.hasHead(player.getUniqueId(), head.getUuid());
                            if (hasHead) {
                                hasHeadFormatted = ConfigService.getHologramAdvancedFoundPlaceholder();
                            }

                            return PlaceholdersService.parse(player.getName(), player.getUniqueId(), head, l
                                    .replaceAll("%state%", hasHeadFormatted));
                        } catch (InternalException e) {
                            throw new RuntimeException(e);
                        }
                    }).backgroundColor(0).billboard((byte) 3)));
            hologram.show(getPool());
        });

        return this;
    }

    @Override
    public boolean isVisible(Player player) {
        return hologram.isShownFor(player);
    }

    public void refresh(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(HeadBlocks.getInstance(), () -> {
            for (Line<?> line : hologram.getLines()) {
                line.update(player);
            }
        });
    }

    private IHologramPool<Hologram> getPool() {
        return HologramService.hologramPool;
    }
}
