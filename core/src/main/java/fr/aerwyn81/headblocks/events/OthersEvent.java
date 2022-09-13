package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.handlers.ConfigService;
import fr.aerwyn81.headblocks.handlers.HeadService;
import fr.aerwyn81.headblocks.handlers.HologramHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.stream.Collectors;

public class OthersEvent implements Listener {
    private final HeadBlocks main;
    private final StorageHandler storageHandler;
    private final HologramHandler hologramHandler;

    public OthersEvent(HeadBlocks main) {
        this.main = main;
        this.storageHandler = main.getStorageHandler();
        this.hologramHandler = main.getHologramHandler();
    }

    @EventHandler
    public void onPlayerInteract(BlockBreakEvent e) {
        Block block = e.getBlock();

        // Check if block is a head
        if (block.getType() != Material.PLAYER_WALL_HEAD || block.getType() != Material.PLAYER_HEAD) {
            return;
        }

        // Check if the head is a head of the plugin
        HeadLocation headLocation = HeadService.getHeadAt(block.getLocation());
        if (headLocation == null) {
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> HeadService.getChargedHeadLocations().stream()
                .anyMatch(p -> LocationUtils.areEquals(p.getLocation(), b.getLocation())))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        storageHandler.loadPlayer(e.getPlayer());

        if (ConfigService.isHologramsEnabled()) {
            hologramHandler.addExcludedPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        storageHandler.unloadPlayer(e.getPlayer());
        HeadService.getHeadMoves().remove(e.getPlayer().getUniqueId());

        if (ConfigService.isHologramsEnabled()) {
            hologramHandler.removeExcludedPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onWorldLoaded(WorldLoadEvent e) {
        var headsInWorld = HeadService.getHeadLocations()
                .stream()
                .filter(h -> !h.isCharged() && e.getWorld().getName().equals(h.getConfigWorldName()))
                .collect(Collectors.toList());

        for (HeadLocation head : headsInWorld) {
            if (ConfigService.isHologramsEnabled()) {
                hologramHandler.createHolograms(head.getLocation());
            }

            head.setLocation(new Location(e.getWorld(), head.getX(), head.getY(), head.getZ()));
            head.setCharged(true);
        }
    }

    @EventHandler
    public void onBlockChange(BlockFromToEvent e) {
        if (e.getBlock().isLiquid() && HeadService.getHeadAt(e.getToBlock().getLocation()) != null) {
            e.setCancelled(true);
        }
    }
}
