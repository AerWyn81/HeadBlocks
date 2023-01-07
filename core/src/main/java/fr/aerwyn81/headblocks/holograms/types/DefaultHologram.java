package fr.aerwyn81.headblocks.holograms.types;

import com.github.unldenis.hologram.Hologram;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.IHologram;
import fr.aerwyn81.headblocks.utils.internal.HoloLibSingleton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DefaultHologram implements IHologram {
    Hologram hologram;

    @Override
    public void show(Player player) {
        hologram.removeExcludedPlayer(player);
    }

    @Override
    public void hide(Player player) {
        hologram.addExcludedPlayer(player);
    }

    @Override
    public void delete() {
        HoloLibSingleton.getHologramPool().remove(hologram);
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines, int displayRange) {
        var hologramBuilder = Hologram.builder()
                .location(location.subtract(0, 1.3, 0));

        for (String line : lines) {
            hologramBuilder.addLine(line);
        }

        hologram = hologramBuilder.build(HoloLibSingleton.getHologramPool());

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
