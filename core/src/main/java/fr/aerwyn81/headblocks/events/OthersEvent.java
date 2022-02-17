package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import fr.aerwyn81.headblocks.utils.Version;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;

import java.util.UUID;

public class OthersEvent implements Listener {
    private final HeadBlocks main;

    public OthersEvent(HeadBlocks main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerInteract(BlockBreakEvent e) {
        Block block = e.getBlock();

        // Check if block is a head
        if (!isBlockIsHeadBlock(block)) {
            return;
        }

        Location clickedLocation = block.getLocation();
        UUID headUuid = main.getHeadHandler().getHeadAt(clickedLocation);

        // Check if the head is a head of the plugin
        if (headUuid == null) {
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> main.getHeadHandler().getHeadLocations().stream()
                .anyMatch(p -> HeadUtils.areEquals(p.getValue1(), b.getLocation())))) {
            e.setCancelled(true);
        }
    }

    private boolean isBlockIsHeadBlock(Block block) {
        // Specific case where we only check if the block type if a skull
        if (Version.getCurrent().isOlderOrSameThan(Version.v1_12)) {
            return block.getType().name().equals("SKULL");
        }

        return block.getType() == Material.PLAYER_WALL_HEAD || block.getType() == Material.PLAYER_HEAD;
    }
}
