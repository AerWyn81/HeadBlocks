package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadCreatedEvent;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.PlayerUtils;
import org.bukkit.Bukkit;
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

        if (!PlayerUtils.hasPermission(player, "headblocks.admin") || !((ItemStack) main.getVersionCompatibility().getItemStackInHand(player)).isSimilar(main.getHeadHandler().getPluginHead())) {
            return;
        }

        if (!player.isSneaking()) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.CreativeSneakAddHead"));
            return;
        }

        if (main.getHeadHandler().getHeadAt(headBlock.getLocation()) != null) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.HeadAlreadyExistHere"));
            return;
        }

        UUID headUuid = main.getHeadHandler().addLocation(headBlock.getLocation());
        main.getVersionCompatibility().spawnParticle(headBlock.getLocation());

        player.sendMessage(languageHandler.getMessage("Messages.HeadPlaced")
                .replaceAll("%x%", String.valueOf(headBlock.getX()))
                .replaceAll("%y%", String.valueOf(headBlock.getY()))
                .replaceAll("%z%", String.valueOf(headBlock.getZ()))
                .replaceAll("%world%", headBlock.getWorld().getName()));

        Bukkit.getPluginManager().callEvent(new HeadCreatedEvent(headUuid, headBlock.getLocation()));
    }
}
