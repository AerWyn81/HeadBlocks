package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.AreaRunManager;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
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

        HeadLocation headLocation = registry.getHeadService().getBlockHeadAt(block);
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

        if (e.getBlocks().stream().anyMatch(this::isHeadBlock)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (!registry.getConfigService().preventPistonExtension()) {
            return;
        }

        if (e.getBlocks().stream().anyMatch(this::isHeadBlock)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent e) {
        if (isHeadBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (isHeadBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (isHeadBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent e) {
        if (isHeadBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (isHeadBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    private boolean isHeadBlock(Block block) {
        return registry.getHeadService().getBlockHeadAt(block) != null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        registry.getStorageService().loadPlayers(e.getPlayer());

        registry.getVisibilityService().onJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        registry.getStorageService().unloadPlayer(e.getPlayer());
        registry.getHeadService().getHeadMoves().remove(e.getPlayer().getUniqueId());
        registry.getHuntService().clearSelectedHunt(e.getPlayer().getUniqueId());
        TimedRunManager.leaveRun(e.getPlayer().getUniqueId());
        AreaRunManager.clear(e.getPlayer().getUniqueId());
        registry.getGuiService().getBehaviorSelectionManager().clearState(e.getPlayer().getUniqueId());
        registry.getGuiService().getTimedConfigManager().clearState(e.getPlayer().getUniqueId());
        registry.getGuiService().getScheduledConfigManager().clearState(e.getPlayer().getUniqueId());
        registry.getGuiService().getRequirementsGui().clearState(e.getPlayer().getUniqueId());
        registry.getChatPromptService().cancel(e.getPlayer().getUniqueId());
        registry.getGuiService().getRewardsManager().cancelPendingRewardInput(e.getPlayer());
        registry.getGuiService().getHintManager().clearCache(e.getPlayer().getUniqueId());
        registry.getGuiService().getCatalogGui().clearState(e.getPlayer().getUniqueId());

        registry.getVisibilityService().onQuit(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return;
        }

        var areaEditor = registry.getGuiService().getRequirementsGui().getAreaEditor();
        if (areaEditor.isAwaitingSneak(e.getPlayer().getUniqueId())) {
            areaEditor.handleReturnSneak(e.getPlayer());
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

            registry.getHologramService().createHolograms(head.getLocation(), registry.getHuntService().configOf(head.getHuntId()));
            registry.getVisualService().ensureSpawned(head);
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

        e.blockList().removeIf(this::isHeadBlock);
    }

}
