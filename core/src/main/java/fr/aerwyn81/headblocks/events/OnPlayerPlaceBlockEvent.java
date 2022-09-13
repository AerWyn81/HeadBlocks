package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadCreatedEvent;
import fr.aerwyn81.headblocks.handlers.HeadService;
import fr.aerwyn81.headblocks.handlers.HologramService;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

public class OnPlayerPlaceBlockEvent implements Listener {
    private final HeadBlocks main;
    private final LanguageHandler languageHandler;

    public OnPlayerPlaceBlockEvent(HeadBlocks main) {
        this.main = main;
        this.languageHandler = main.getLanguageHandler();
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block headBlock = e.getBlockPlaced();

        if (!hasHeadBlocksItemInHand(player)) {
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

        if (!player.isSneaking() || player.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.CreativeSneakAddHead"));
            return;
        }

        Location headLocation = headBlock.getLocation();

        if (HeadService.getHeadAt(headLocation) != null) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.HeadAlreadyExistHere"));
            return;
        }

        // Check if there is a storage issue
        if (main.getStorageHandler().hasStorageError()) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
            return;
        }

        UUID headUuid;
        try {
            headUuid = HeadService.saveHeadLocation(headLocation);
        } catch (InternalException ex) {
            player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create new HeadBlocks from the storage: " + ex.getMessage()));
            return;
        }

        HologramService.showNotFoundTo(player, headLocation);

        ParticlesUtils.spawn(headLocation, Particle.VILLAGER_HAPPY, 10, null, player);

        player.sendMessage(MessageUtils.parseLocationPlaceholders(languageHandler.getMessage("Messages.HeadPlaced"), headBlock.getLocation()));

        Bukkit.getPluginManager().callEvent(new HeadCreatedEvent(headUuid, headLocation));
    }

    private boolean hasHeadBlocksItemInHand(Player player) {
        return HeadService.getHeads().stream().anyMatch(i -> HeadUtils.areEquals(i.getItemStack(), player.getInventory().getItemInMainHand()));
    }
}
