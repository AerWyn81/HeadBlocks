package fr.aerwyn81.headblocks.holograms.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.holograms.IHologram;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.line.DisplayTextLine;
import org.holoeasy.line.Line;
import org.holoeasy.pool.IHologramPool;

import java.util.List;

public class AdvancedHologram implements IHologram {

    private final ServiceRegistry registry;
    Hologram hologram;

    public AdvancedHologram(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void show(Player player) {
        // Used only by Default hologram.
    }

    @Override
    public void hide(Player player) {
        // Used only by Default hologram.
    }

    @Override
    public void delete() {
        registry.getScheduler().runTaskAsync(() -> {
            var pool = getPool();
            if (pool != null) {
                hologram.hide(getPool());
            }
        });
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines) {
        hologram = new Hologram(registry.getHoloEasyLib(), location);

        registry.getScheduler().runTaskAsync(() -> {
            registry.getConfigService().hologramsAdvancedLines().forEach(l -> hologram.getLines().add(
                    new DisplayTextLine(hologram, player ->
                    {
                        if (!l.contains("%state%")) {
                            return registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(), l);
                        }

                        try {
                            var head = registry.getHeadService().getHeadAt(location);
                            if (head == null) {
                                return registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(), l);
                            }

                            var hasHeadFormatted = registry.getConfigService().hologramAdvancedNotFoundPlaceholder();
                            var hasHead = registry.getStorageService().hasHead(player.getUniqueId(), head.getUuid());
                            if (hasHead) {
                                hasHeadFormatted = registry.getConfigService().hologramAdvancedFoundPlaceholder();
                            }

                            return registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(), head, l
                                    .replace("%state%", hasHeadFormatted));
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
        // Used only by Default hologram.
        return false;
    }

    @Override
    public boolean isAlive() {
        return hologram != null;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void refresh(Player player) {
        registry.getScheduler().runTaskAsync(() -> {
            for (Line<?> line : hologram.getLines()) {
                line.update(player);
            }
        });
    }

    private IHologramPool<Hologram> getPool() {
        return registry.getHologramService().getHologramPool();
    }
}
