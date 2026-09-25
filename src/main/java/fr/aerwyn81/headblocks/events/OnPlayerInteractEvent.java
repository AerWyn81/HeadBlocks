package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadClaimService;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class OnPlayerInteractEvent implements Listener {
    private final ServiceRegistry registry;
    private final HeadClaimService claimService;

    public OnPlayerInteractEvent(ServiceRegistry registry) {
        this.registry = registry;
        this.claimService = new HeadClaimService(registry);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAreaCapture(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK || e.getClickedBlock() == null) {
            return;
        }

        Player player = e.getPlayer();
        var areaEditor = registry.getGuiService().getRequirementsGui().getAreaEditor();
        if (!areaEditor.isAwaitingBlockClick(player.getUniqueId())) {
            return;
        }

        e.setCancelled(true);
        areaEditor.handleBlockClick(player, e.getClickedBlock().getLocation());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();

        if (block == null || e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        boolean isPlayerHead = HeadUtils.isPlayerHead(block);
        HeadLocation headLocation = null;
        if (!isPlayerHead) {
            headLocation = registry.getHeadService().getHeadAt(block.getLocation());
            if (headLocation == null || !isBlockRendered(headLocation)) {
                return;
            }

            e.setUseInteractedBlock(Event.Result.DENY);
        }

        Player player = e.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE && e.getAction() == Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (HeadBlocks.isReloadInProgress) {
            e.setCancelled(true);
            player.sendMessage(registry.getLanguageService().message("Messages.PluginReloading"));
            return;
        }

        Location clickedLocation = block.getLocation();

        if (headLocation == null) {
            headLocation = registry.getHeadService().getHeadAt(clickedLocation);
            if (headLocation == null || !isBlockRendered(headLocation)) {
                return;
            }
        }

        var outcome = claimService.click(player, headLocation, clickedLocation, block.getType() == Material.PLAYER_WALL_HEAD);
        if (outcome == HeadClaimService.Outcome.STORAGE_ERROR) {
            e.setCancelled(true);
        }
    }

    private boolean isBlockRendered(HeadLocation headLocation) {
        return registry.getVisualService().isBlockRendered(headLocation);
    }
}
