package fr.aerwyn81.headblocks.holograms.types;

import com.Zrips.CMI.CMI;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.IHologram;
import net.Zrips.CMILib.Container.CMILocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class CMIHologram implements IHologram {
    com.Zrips.CMI.Modules.Holograms.CMIHologram hologram;

    @Override
    public void show(Player player) {
        hologram.update(player);
    }

    @Override
    public void hide(Player player) {
        hologram.hide(player.getUniqueId());
    }

    @Override
    public void delete() {
        hologram.remove();
        hologram.update();
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines, int displayRange) {
        hologram = new com.Zrips.CMI.Modules.Holograms.CMIHologram(name, new CMILocation(location));

        hologram.setShowRange(displayRange);
        hologram.setUpdateIntervalSec(-1.0);
        hologram.setLines(lines);

        CMI.getInstance().getHologramManager().addHologram(hologram, false);

        hologram.hide();
        return this;
    }

    @Override
    public EnumTypeHologram getTypeHologram() {
        return EnumTypeHologram.CMI;
    }

    @Override
    public boolean isVisible(Player player) {
        return false; // Not used
    }

}
