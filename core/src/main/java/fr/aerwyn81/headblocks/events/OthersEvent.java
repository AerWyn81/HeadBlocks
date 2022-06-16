package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
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
        if (block.getType() != Material.PLAYER_WALL_HEAD || block.getType() != Material.PLAYER_HEAD) {
            return;
        }

        // Check if the head is a head of the plugin
        UUID headUuid = main.getHeadHandler().getHeadAt(block.getLocation());
        if (headUuid == null) {
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> main.getHeadHandler().getHeadLocations().entrySet().stream()
                .anyMatch(p -> HeadUtils.areEquals(p.getValue(), b.getLocation())))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        try {
            boolean hasRenamed = main.getStorageHandler().hasPlayerRenamed(p.getUniqueId(), p.getName());

            if (hasRenamed) {
                main.getStorageHandler().updatePlayerName(p.getUniqueId(), p.getName());
            }
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.translate("&cError while trying to update player name from the storage: " + ex.getMessage()));
        }
    }
}
