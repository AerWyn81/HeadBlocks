package fr.aerwyn81.headblocks.holograms.types;

import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.IHologram;
import fr.aerwyn81.headblocks.utils.internal.HoloLibSingleton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.holoeasy.builder.HologramBuilder;
import org.holoeasy.hologram.Hologram;

import java.util.Collections;
import java.util.List;

import static org.holoeasy.builder.HologramBuilder.hologram;

public class DefaultHologram implements IHologram {
    Hologram hologram;

    @Override
    public void show(Player player) {
        hologram.show(player);
    }

    @Override
    public void hide(Player player) {
        hologram.hide(player);
    }

    @Override
    public void delete() {
        HoloLibSingleton.getHologramPool().remove(hologram.getId());
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines, int displayRange) {
        HoloLibSingleton.getHologramPool().registerHolograms(() -> hologram = hologram(location.subtract(0, 2.3, 0),
                () -> lines.forEach(HologramBuilder::textline)));

        for (Player pl : Collections.synchronizedCollection(Bukkit.getOnlinePlayers())) {
            hide(pl);
        }

        return this;
    }

    @Override
    public EnumTypeHologram getTypeHologram() {
        return EnumTypeHologram.DEFAULT;
    }

    @Override
    public boolean isVisible(Player player) {
        return hologram.isShownFor(player);
    }

}