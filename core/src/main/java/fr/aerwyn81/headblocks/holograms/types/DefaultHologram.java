package fr.aerwyn81.headblocks.holograms.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.IHologram;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.List;

public class DefaultHologram implements IHologram {
    TextDisplay hologram;

    private final HeadBlocks plugin = HeadBlocks.getInstance();

    @Override
    public void show(Player player) {
        player.showEntity(plugin, hologram);
    }

    @Override
    public void hide(Player player) {
        player.hideEntity(plugin, hologram);
    }

    @Override
    public void delete() {
        hologram.remove();
    }

    @Override
    public IHologram create(String name, Location location, List<String> lines, int displayRange) {
        var world = location.getWorld();
        if (world == null) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError creating internal hologram, world is null!"));
            return this;
        }

        hologram = world.spawn(location, TextDisplay.class, entity -> {
            lines.forEach(l -> entity.setText(MessageUtils.colorize(l)));
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
            entity.setBillboard(Display.Billboard.CENTER);
        });

        return this;
    }

    @Override
    public EnumTypeHologram getTypeHologram() {
        return EnumTypeHologram.DEFAULT;
    }

    @Override
    public boolean isVisible(Player player) {
        return player.canSee(hologram);
    }
}