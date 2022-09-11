package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadDeletedEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import fr.aerwyn81.headblocks.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class OnPlayerBreakBlockEvent implements Listener {
    private final HeadBlocks main;
    private final ConfigHandler configHandler;
    private final LanguageHandler languageHandler;

    public OnPlayerBreakBlockEvent(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();
        this.languageHandler = main.getLanguageHandler();
    }

    @EventHandler
    public void OnBlockBreakEvent(BlockBreakEvent e) {
        var player = e.getPlayer();
        var block = e.getBlock();

        // Check if block destroyed is a head
        if (block.getType() != Material.PLAYER_WALL_HEAD && block.getType() != Material.PLAYER_HEAD) {
            return;
        }

        Location blockLocation = block.getLocation();

        // Check if the head is a head of the plugin
        HeadLocation headLocation = main.getHeadHandler().getHeadAt(blockLocation);
        if (headLocation == null) {
            return;
        }

        if (HeadBlocks.isReloadInProgress) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.PluginReloading"));
            return;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.NoPermissionBlock"));
            return;
        }

        // Destroying HeadBlock require creative gamemode and sneaking
        if (!player.isSneaking() || player.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.CreativeSneakRemoveHead"));
            return;
        }

        // Check if there is a storage issue
        if (main.getStorageHandler().hasStorageError()) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
            return;
        }

        // Remove the head
        try {
            main.getHeadHandler().removeHeadLocation(headLocation, configHandler.shouldResetPlayerData());
        } catch (InternalException ex) {
            player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove a head (" + headLocation.getUuid() + ") from the storage: " + ex.getMessage()));
        }

        // Send player success message
        player.sendMessage(MessageUtils.parseLocationPlaceholders(languageHandler.getMessage("Messages.HeadRemoved"), blockLocation));

        // Trigger the event HeadDeleted
        Bukkit.getPluginManager().callEvent(new HeadDeletedEvent(headLocation.getUuid(), blockLocation));
    }
}
