package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class OnPlayerPlaceBlockEvent implements Listener {

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block headBlock = e.getBlockPlaced();

        if (!hasHeadBlocksItemInHand(player)) {
            return;
        }

        if (HeadBlocks.isReloadInProgress) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.PluginReloading"));
            return;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.NoPermissionBlock"));
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.CreativeSneakAddHead"));
            return;
        }

        Location headLocation = headBlock.getLocation();

        if (TrackService.getHeadAt(headLocation).isPresent()) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.HeadAlreadyExistHere"));
            return;
        }

        // Check if there is a storage issue
        if (StorageService.hasStorageError()) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            return;
        }

        var headTexture = HeadUtils.getHeadTexture(e.getItemInHand());

        var trackPlayerChoose = TrackService.getPlayersTrackChoice().get(player.getUniqueId());

        if (player.isSneaking() && trackPlayerChoose != null) {
            try {
                TrackService.addHead(player, trackPlayerChoose, headLocation, headTexture);
            } catch (InternalException ex) {
                player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while adding new head from the storage: " + ex.getMessage()));
            }
        } else {
            GuiService.openChooseTrack(player, headLocation, headTexture);
        }
    }

    private boolean hasHeadBlocksItemInHand(Player player) {
        return HeadService.getHeads().stream().anyMatch(i -> HeadUtils.areEquals(i.getItemStack(), player.getInventory().getItemInMainHand()));
    }
}
