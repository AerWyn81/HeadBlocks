package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HeadDeletedEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class OnPlayerBreakBlockEvent implements Listener {

    private final ServiceRegistry registry;

    public OnPlayerBreakBlockEvent(ServiceRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void OnBlockBreakEvent(BlockBreakEvent e) {
        var player = e.getPlayer();
        var block = e.getBlock();

        // Check if block destroyed is a head
        if (!HeadUtils.isPlayerHead(block)) {
            return;
        }

        Location blockLocation = block.getLocation();

        // Check if the head is a head of the plugin
        HeadLocation headLocation = registry.getHeadService().getHeadAt(blockLocation);
        if (headLocation == null) {
            return;
        }

        if (HeadBlocks.isReloadInProgress) {
            e.setCancelled(true);
            player.sendMessage(registry.getLanguageService().message("Messages.PluginReloading"));
            return;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            e.setCancelled(true);

            var message = registry.getLanguageService().message("Messages.NoPermissionBlock");
            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }
            return;
        }

        // Destroying HeadBlock require creative gamemode and sneaking
        if (!player.isSneaking() || player.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            player.sendMessage(registry.getLanguageService().message("Messages.CreativeSneakRemoveHead"));
            return;
        }

        // Check if there is a storage issue
        if (registry.getStorageService().isStorageError()) {
            e.setCancelled(true);
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return;
        }

        // Remove the head
        try {
            registry.getHeadService().removeHeadLocation(headLocation, registry.getConfigService().resetPlayerData());
        } catch (InternalException ex) {
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error while trying to remove a head \"{0}\" from the storage: {1}", headLocation.getNameOrUuid(), ex.getMessage());
        }

        // Send player success message
        player.sendMessage(LocationUtils.parseLocationPlaceholders(registry.getLanguageService().message("Messages.HeadRemoved"), blockLocation));

        // Trigger the event HeadDeleted
        Hunt primaryHunt = registry.getHuntService().getHighestPriorityHuntForHead(headLocation.getUuid());
        String huntId = primaryHunt != null ? primaryHunt.getId() : null;
        Bukkit.getPluginManager().callEvent(new HeadDeletedEvent(headLocation.getUuid(), blockLocation, huntId));
    }
}
