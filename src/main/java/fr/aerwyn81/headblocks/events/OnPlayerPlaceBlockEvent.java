package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadCreatedEvent;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.HuntService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.*;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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

            var message = LanguageService.getMessage("Messages.NoPermissionBlock");
            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }

            return;
        }

        if (!player.isSneaking() || player.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.CreativeSneakAddHead"));
            return;
        }

        Location headLocation = headBlock.getLocation();
        headLocation = headLocation.clone().add(0.5, 0, 0.5);

        if (HeadService.getHeadAt(headLocation) != null) {
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
        if (headTexture == null) {
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            LogUtil.error("Error, head texture not resolved when trying to save the head for player {0}", player.getName());
            return;
        }

        UUID headUuid;
        try {
            headUuid = HeadService.saveHeadLocation(headLocation, headTexture);
        } catch (InternalException ex) {
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            LogUtil.error("Error while trying to create new HeadBlocks from the storage: {0}", ex.getMessage());
            return;
        }

        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            ParticlesUtils.spawn(headLocation, Particle.valueOf("HAPPY_VILLAGER"), 10, null, player);
        } else {
            ParticlesUtils.spawn(headLocation, Particle.VILLAGER_HAPPY, 10, null, player);
        }

        player.sendMessage(LocationUtils.parseLocationPlaceholders(LanguageService.getMessage("Messages.HeadPlaced"), headLocation));

        // Auto-assign head to selected hunt
        String selectedHuntId = HuntService.getSelectedHunt(player.getUniqueId());
        try {
            HuntService.assignHeadToHunt(headUuid, selectedHuntId);
        } catch (Exception ex) {
            LogUtil.error("Error assigning head to hunt {0}: {1}", selectedHuntId, ex.getMessage());
        }

        if ("default".equals(selectedHuntId)) {
            TextComponent msg = new TextComponent(MessageUtils.colorize(
                    LanguageService.getPrefix() + " &7Assigned to &edefault&7. "));
            TextComponent clickable = new TextComponent(MessageUtils.colorize("&a&l[Reassign]"));
            clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(MessageUtils.colorize("&7Click to reassign this head"))));
            clickable.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                    "/headblocks hunt transfer " + headUuid + " "));
            msg.addExtra(clickable);
            player.spigot().sendMessage(msg);
        }

        Bukkit.getPluginManager().callEvent(new HeadCreatedEvent(headUuid, headLocation, selectedHuntId));
    }

    private boolean hasHeadBlocksItemInHand(Player player) {
        return HeadService.getHeads().stream().anyMatch(i -> HeadUtils.areEquals(i.getItemStack(), player.getInventory().getItemInMainHand()));
    }
}
