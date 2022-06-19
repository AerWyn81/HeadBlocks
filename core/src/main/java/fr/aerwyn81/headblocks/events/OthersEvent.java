package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.LocationUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
                .anyMatch(p -> LocationUtils.areEquals(p.getValue(), b.getLocation())))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        main.getStorageHandler().loadPlayer(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        main.getStorageHandler().unloadPlayer(e.getPlayer());
        main.getHeadHandler().getHeadMoves().remove(e.getPlayer().getUniqueId());
    }
}
