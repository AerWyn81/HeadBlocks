package fr.aerwyn81.headblocks.holograms.types;

import com.Zrips.CMI.CMI;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.IHologram;
import net.Zrips.CMILib.Container.CMILocation;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class CMIHologram implements IHologram {
    com.Zrips.CMI.Modules.Holograms.CMIHologram hologram;

    @Override
    public void show(Player player) {
        throw new NotImplementedException();
    }

    @Override
    public void hide(Player player) {
        hologram.hide(player.getUniqueId());
    }

    @Override
    public void delete() {
        hologram.remove();
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines, int displayRange) {
        hologram = new com.Zrips.CMI.Modules.Holograms.CMIHologram(name, new CMILocation(location));
        hologram.setLines(lines);
        hologram.setShowRange(displayRange);
        hologram.setUpdateRange(-1);
        hologram.hide();
        CMI.getInstance().getHologramManager().addHologram(hologram);

        hologram.update();
        hologram.enable();
        return this;
    }

    @Override
    public EnumTypeHologram getTypeHologram() {
        //return EnumTypeHologram.CMI;
        throw new NotImplementedException();
    }

    @Override
    public boolean isVisible(Player player) {
        throw new NotImplementedException();
    }
}
