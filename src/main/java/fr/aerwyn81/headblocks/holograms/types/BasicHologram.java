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
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class BasicHologram implements IHologram {
    TextDisplay hologram;

    private final HeadBlocks plugin = HeadBlocks.getInstance();

    @Override
    public void show(Player player) {
        if (!isAlive()) {
            return;
        }
        player.showEntity(plugin, hologram);
    }

    @Override
    public void hide(Player player) {
        if (!isAlive()) {
            return;
        }
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

        // Deliberately not the spawn(Location, Class, Consumer) overload: it took org.bukkit.util.Consumer
        // up to 1.20.4 and java.util.function.Consumer from 1.20.5 on, so compiling against either one
        // yields a NoSuchMethodError on the other. Configuring right after spawn is equivalent for the
        // client, since the entity tracker only broadcasts at the end of the tick.
        hologram = world.spawn(location, TextDisplay.class);
        hologram.setText(lines.stream().map(MessageUtils::colorize).collect(Collectors.joining("\n")));
        hologram.setVisibleByDefault(false);
        hologram.setPersistent(false);
        hologram.setBillboard(Display.Billboard.CENTER);

        return this;
    }

    @Override
    public boolean isVisible(Player player) {
        return isAlive() && player.canSee(hologram);
    }

    @Override
    public boolean isAlive() {
        return hologram != null && !hologram.isDead() && hologram.isValid();
    }

    @Override
    public void refresh(Player player) {
    }
}