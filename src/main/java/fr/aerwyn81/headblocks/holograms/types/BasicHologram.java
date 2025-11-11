package fr.aerwyn81.headblocks.holograms.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.holograms.IHologram;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.List;

public class BasicHologram implements IHologram {
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
    public IHologram create(String name, Location location, List<String> lines) {
        var world = location.getWorld();
        if (world == null) {
            LogUtil.error("Error creating internal hologram, world is null!");
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
    public boolean isVisible(Player player) {
        return player.canSee(hologram);
    }

    @Override
    public void refresh(Player player) {
    }
}