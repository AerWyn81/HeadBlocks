package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadCreatedEvent;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.PlayerUtils;
import fr.aerwyn81.headblocks.utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

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

        // Prevent some issue if HeadDatabase is not yet running
        if (HeadBlocks.isHeadDatabaseActive && !HeadBlocks.isHeadDatabaseLoaded) {
            return;
        }

        if (!hasHeadBlocksItemInHand(player)) {
            return;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            return;
        }

        if (!player.isSneaking()) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.CreativeSneakAddHead"));
            return;
        }

        Location headLocation = headBlock.getLocation();

        if (main.getHeadHandler().getHeadAt(headLocation) != null) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.HeadAlreadyExistHere"));
            return;
        }

        UUID headUuid = main.getHeadHandler().addLocation(headLocation);

        if (Version.getCurrent().isNewerOrSameThan(Version.v1_13)) {
            ParticlesUtils.spawn(headLocation, Particle.VILLAGER_HAPPY, 10, null, player);
        }

        player.sendMessage(languageHandler.getMessage("Messages.HeadPlaced")
                .replaceAll("%x%", String.valueOf(headBlock.getX()))
                .replaceAll("%y%", String.valueOf(headBlock.getY()))
                .replaceAll("%z%", String.valueOf(headBlock.getZ()))
                .replaceAll("%world%", headBlock.getWorld().getName()));

        Bukkit.getPluginManager().callEvent(new HeadCreatedEvent(headUuid, headLocation));
    }

    private boolean hasHeadBlocksItemInHand(Player player) {
        if (Version.getCurrent() == Version.v1_8) {
            return main.getHeadHandler().getHeads().stream().anyMatch(i -> i.isSimilar((ItemStack) main.getLegacySupport().getItemStackInHand(player)));
        }

        return main.getHeadHandler().getHeads().stream().anyMatch(i -> i.isSimilar(player.getInventory().getItemInMainHand()));
    }
}
