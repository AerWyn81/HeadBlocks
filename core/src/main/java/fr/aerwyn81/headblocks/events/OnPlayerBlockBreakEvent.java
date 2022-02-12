package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.Version;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.UUID;

public class OnPlayerBlockBreakEvent implements Listener {
    private final HeadBlocks main;

    public OnPlayerBlockBreakEvent(HeadBlocks main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerInteract(BlockBreakEvent e) {
        Block block = e.getBlock();

        // Check if clickedBlock is a head
        if (!isClickedBlockIsHeadBlocks(block)) {
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

    private boolean isClickedBlockIsHeadBlocks(Block block) {
        // Specific case where we only check if the block type if a skull
        if (Version.getCurrent().isOlderOrSameThan(Version.v1_12)) {
            return block.getType().name().equals("SKULL");
        }

        return block.getType() == Material.PLAYER_WALL_HEAD || block.getType() == Material.PLAYER_HEAD;
    }
}
