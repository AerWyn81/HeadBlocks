package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class OthersEvent implements Listener {

    private final ServiceRegistry registry;

    public OthersEvent(ServiceRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(BlockBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Block block = e.getBlock();

        // Check if block is a head
        if (!HeadUtils.isPlayerHead(block)) {
            return;
        }

        // Check if the head is a head of the plugin
        HeadLocation headLocation = registry.getHeadService().getHeadAt(block.getLocation());
        if (headLocation == null) {
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (!registry.getConfigService().preventPistonExtension()) {
            return;
        }

        if (e.getBlocks().stream().anyMatch(b -> registry.getHeadService().getChargedHeadLocations().stream()
                .anyMatch(p -> LocationUtils.areEquals(p.getLocation(), b.getLocation())))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        registry.getStorageService().loadPlayers(e.getPlayer());

        var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
        if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
            packetEventsHook.getHeadHidingListener().onPlayerJoin(e.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        registry.getStorageService().unloadPlayer(e.getPlayer());
        registry.getHeadService().getHeadMoves().remove(e.getPlayer().getUniqueId());
        registry.getHuntService().clearSelectedHunt(e.getPlayer().getUniqueId());
        TimedRunManager.leaveRun(e.getPlayer().getUniqueId());
        registry.getGuiService().getBehaviorSelectionManager().clearState(e.getPlayer().getUniqueId());
        registry.getGuiService().getTimedConfigManager().clearState(e.getPlayer().getUniqueId());
        registry.getGuiService().getRewardsManager().cancelPendingRewardInput(e.getPlayer());
        registry.getGuiService().getHintManager().clearCache(e.getPlayer().getUniqueId());

        var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
        if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
            packetEventsHook.getHeadHidingListener().invalidatePlayerCache(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onWorldLoaded(WorldLoadEvent e) {
        var headsInWorld = registry.getHeadService().getHeadLocations()
                .stream()
                .filter(h -> !h.isCharged() && e.getWorld().getName().equals(h.getConfigWorldName()))
                .toList();

        for (HeadLocation head : headsInWorld) {
            head.setLocation(new Location(e.getWorld(), head.getX(), head.getY(), head.getZ()));
            head.setCharged(true);

            Hunt primaryHunt = registry.getHuntService().getHighestPriorityHuntForHead(head.getUuid());
            HuntConfig huntConfig = primaryHunt != null ? primaryHunt.getConfig() : new HuntConfig(registry.getConfigService());
            registry.getHologramService().createHolograms(head.getLocation(), huntConfig);
        }
    }

    @EventHandler
    public void onBlockChange(BlockFromToEvent e) {
        if (!registry.getConfigService().preventLiquidFlow()) {
            return;
        }

        if (e.getBlock().isLiquid() && registry.getHeadService().getHeadAt(e.getToBlock().getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        if (!registry.getConfigService().preventExplosion()) {
            return;
        }

        e.blockList().removeIf(block -> {
            if (HeadUtils.isPlayerHead(block)) {
                var headLocation = registry.getHeadService().getHeadAt(block.getLocation());
                return headLocation != null;
            }
            return false;
        });
    }

}
