package fr.aerwyn81.headblocks.holograms.types;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.IHologram;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class DecentHologram implements IHologram {
    Hologram hologram;

    @Override
    public void show(Player player) {
        hologram.setShowPlayer(player);
    }

    @Override
    public void hide(Player player) {
        hologram.setHidePlayer(player);
    }

    @Override
    public void delete() {
        hologram.delete();
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines, int displayRange) {
        hologram = DHAPI.createHologram(name, location);
        for (String line : lines) {
            getFirstPage().addLine(new HologramLine(getFirstPage(), hologram.getLocation(), MessageUtils.colorize(line)));
        }

        hologram.setDefaultVisibleState(false);
        hologram.setDisplayRange(displayRange);
        return this;
    }

    @Override
    public EnumTypeHologram getTypeHologram() {
        return EnumTypeHologram.DECENT;
    }

    @Override
    public boolean isVisible(Player player) {
        return hologram.isVisible(player);
    }

    public Hologram getHologram() {
        return hologram;
    }

    private HologramPage getFirstPage() {
        return hologram.getPage(0);
    }
}
