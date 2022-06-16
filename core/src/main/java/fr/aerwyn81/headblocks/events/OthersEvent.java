package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import fr.aerwyn81.headblocks.utils.Version;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        try {
            main.getStorageHandler().updatePlayerName(p.getUniqueId(), p.getName());
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.translate("Error while trying to update player name from the storage: " + ex.getMessage()));
        }
    }
}
