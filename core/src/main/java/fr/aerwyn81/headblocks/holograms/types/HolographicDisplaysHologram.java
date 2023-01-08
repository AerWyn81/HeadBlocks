package fr.aerwyn81.headblocks.holograms.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.IHologram;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.VisibilitySettings;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class HolographicDisplaysHologram implements IHologram {
    Hologram hologram;

    @Override
    public void show(Player player) {
        hologram.getVisibilitySettings().setIndividualVisibility(player, VisibilitySettings.Visibility.VISIBLE);
    }

    @Override
    public void hide(Player player) {
        hologram.getVisibilitySettings().setIndividualVisibility(player, VisibilitySettings.Visibility.HIDDEN);
    }

    @Override
    public void delete() {
        hologram.delete();
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines, int displayRange) {
        HolographicDisplaysAPI api = HolographicDisplaysAPI.get(HeadBlocks.getInstance());
        for (String line : lines) {
            hologram.getLines().appendText(line);
        }
        hologram = api.createHologram(location);
        hologram.getVisibilitySettings().setGlobalVisibility(VisibilitySettings.Visibility.HIDDEN);
        return this;
    }

    @Override
    public EnumTypeHologram getTypeHologram() {
        return EnumTypeHologram.HD;
    }

    @Override
    public boolean isVisible(Player player) {
        return hologram.getVisibilitySettings().isVisibleTo(player);
    }
}
