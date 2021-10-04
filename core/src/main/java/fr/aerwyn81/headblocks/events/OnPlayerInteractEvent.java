package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadClickEvent;
import fr.aerwyn81.headblocks.api.events.HeadDeletedEvent;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.PlayerUtils;
import fr.aerwyn81.headblocks.utils.XSound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;
import java.util.stream.Stream;

public class OnPlayerInteractEvent implements Listener {

    private final HeadBlocks main;
    private final ConfigHandler configHandler;
    private final LanguageHandler languageHandler;

    public OnPlayerInteractEvent(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();
        this.languageHandler = main.getLanguageHandler();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (Stream.of(Action.LEFT_CLICK_BLOCK, Action.RIGHT_CLICK_BLOCK).noneMatch(a -> a == e.getAction()) || main.getVersionCompatibility().isLeftHand(e)) {
            return;
        }

        if (e.getClickedBlock() == null) {
            return;
        }

        Location clickedLocation = e.getClickedBlock().getLocation();
        UUID headUuid = main.getHeadHandler().getHeadAt(clickedLocation);

        if (headUuid == null) {
            return;
        }

        Player player = e.getPlayer();

        if (e.getAction() == Action.LEFT_CLICK_BLOCK && player.getGameMode() == GameMode.CREATIVE && PlayerUtils.hasPermission(player, "headblocks.admin")) {
            if (!player.isSneaking()) {
                e.setCancelled(true);
                player.sendMessage(languageHandler.getMessage("Messages.CreativeSneakRemoveHead"));
                return;
            }

            if (configHandler.shouldResetPlayerData()) {
                main.getStorageHandler().removeHead(headUuid);
            }

            main.getHeadHandler().removeHead(headUuid);
            player.sendMessage(languageHandler.getMessage("Messages.HeadRemoved")
                    .replaceAll("%x%", String.valueOf(clickedLocation.getX()))
                    .replaceAll("%y%", String.valueOf(clickedLocation.getY()))
                    .replaceAll("%z%", String.valueOf(clickedLocation.getZ()))
                    .replaceAll("%world%", clickedLocation.getWorld() != null ? clickedLocation.getWorld().getName() : FormatUtils.translate("&cUnknownWorld")));

            Bukkit.getPluginManager().callEvent(new HeadDeletedEvent(headUuid, clickedLocation));
            return;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.use")) {
            String message = languageHandler.getMessage("Messages.NoPermissionBlock");

            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }
            return;
        }

        if (main.getStorageHandler().hasAlreadyClaimedHead(player.getUniqueId(), headUuid)) {
            String message = languageHandler.getMessageWithPlaceholders(player, "Messages.AlreadyClaimHead");

            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }

            try {
                XSound.play(player, configHandler.getHeadClickAlreadyOwnSound());
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(FormatUtils.translate("&cError cannot play sound on head click! Cannot parse provided name..."));
            }
            return;
        }

        main.getStorageHandler().savePlayer(player.getUniqueId(), headUuid);

        String message = languageHandler.getMessageWithPlaceholders(player, "Messages.HeadClicked");
        if (!message.trim().isEmpty()) {
            player.sendMessage(message);
        }

        try {
            XSound.play(player, configHandler.getHeadClickNotOwnSound());
        } catch (Exception ex) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError cannot play sound on head click! Cannot parse provided name..."));
        }

        if (configHandler.getHeadClickCommands().size() != 0) {
            Bukkit.getScheduler().runTaskLater(main, () -> configHandler.getHeadClickCommands().forEach(reward ->
                    main.getServer().dispatchCommand(main.getServer().getConsoleSender(), FormatUtils.translate(reward.replaceAll("%player%", player.getName())))), 1L);
        }

        main.getRewardHandler().giveReward(player);

        Bukkit.getPluginManager().callEvent(new HeadClickEvent(headUuid, player, clickedLocation));
    }
}
