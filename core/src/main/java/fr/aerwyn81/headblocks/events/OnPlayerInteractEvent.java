package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadClickEvent;
import fr.aerwyn81.headblocks.api.events.HeadDeletedEvent;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.handlers.PlaceholdersHandler;
import fr.aerwyn81.headblocks.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        Block block = e.getClickedBlock();

        // Check if the correct hand is used
        if (block == null || e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Check if clickedBlock is a head
        if (block.getType() != Material.PLAYER_WALL_HEAD && block.getType() != Material.PLAYER_HEAD) {
            return;
        }

        Player player = e.getPlayer();

        if (HeadBlocks.isReloadInProgress) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.PluginReloading"));
            return;
        }

        Location clickedLocation = block.getLocation();

        // Check if the head is a head of the plugin
        UUID headUuid = main.getHeadHandler().getHeadAt(clickedLocation);
        if (headUuid == null) {
            return;
        }

        // Check if there is a storage issue
        if (main.getStorageHandler().hasStorageError()) {
            e.setCancelled(true);
            player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
            return;
        }

        // Actions to destroy the head only if player has the permission and the creative gamemode
        if (e.getAction() == Action.LEFT_CLICK_BLOCK && player.getGameMode() == GameMode.CREATIVE && PlayerUtils.hasPermission(player, "headblocks.admin")) {
            if (!player.isSneaking()) {
                e.setCancelled(true);
                player.sendMessage(languageHandler.getMessage("Messages.CreativeSneakRemoveHead"));
                return;
            }

            // Remove the head
            try {
                main.getHeadHandler().removeHeadLocation(headUuid, configHandler.shouldResetPlayerData());
            } catch (InternalException ex) {
                player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove a head (" + headUuid + ") from the storage: " + ex.getMessage()));
            }

            // Send player success message
            player.sendMessage(MessageUtils.parseLocationPlaceholders(languageHandler.getMessage("Messages.HeadRemoved"), clickedLocation));

            // Trigger the event HeadDeleted
            Bukkit.getPluginManager().callEvent(new HeadDeletedEvent(headUuid, clickedLocation));
            return;
        }

        // Check if the player has the permission to click on the head
        if (!PlayerUtils.hasPermission(player, "headblocks.use")) {
            String message = languageHandler.getMessage("Messages.NoPermissionBlock");

            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }
            return;
        }

        try {
            // Check if the player has already clicked on the head
            if (main.getStorageHandler().hasHead(player.getUniqueId(), headUuid)) {
                String message = PlaceholdersHandler.parse(player, languageHandler.getMessage("Messages.AlreadyClaimHead"));

                if (!message.trim().isEmpty()) {
                    player.sendMessage(message);
                }

                // Already own song if not empty
                String songName = configHandler.getHeadClickAlreadyOwnSound();
                if (!songName.trim().isEmpty()) {
                    try {
                        XSound.play(player, configHandler.getHeadClickAlreadyOwnSound());
                    } catch (Exception ex) {
                        player.sendMessage(languageHandler.getMessage("Messages.ErrorCannotPlaySound"));
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError cannot play sound on head click: " + ex.getMessage()));
                    }
                }

                // Already own particles if enabled
                if (configHandler.isHeadClickParticlesEnabled()) {
                    String particleName = configHandler.getHeadClickParticlesAlreadyOwnType();
                    int amount = configHandler.getHeadClickParticlesAmount();
                    ArrayList<String> colors = configHandler.getHeadClickParticlesColors();

                    try {
                        ParticlesUtils.spawn(clickedLocation, Particle.valueOf(particleName), amount, colors, player);
                    } catch (Exception ex) {
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError particle name " + particleName + " cannot be parsed!"));
                    }
                }

                // Trigger the event HeadClick with no success because the player already own the head
                Bukkit.getPluginManager().callEvent(new HeadClickEvent(headUuid, player, clickedLocation, false));
                return;
            }

            // Save player click in storage
            main.getStorageHandler().addHead(player.getUniqueId(), headUuid);
        } catch (InternalException ex) {
            player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to save a head found by " + player.getName() + " from the storage: " + ex.getMessage()));
            return;
        }

        // Success messages if not empty
        List<String> messages = configHandler.getHeadClickMessages();
        if (messages.size() != 0) {
            player.sendMessage(PlaceholdersHandler.parse(player, messages));
        }

        // Success song if not empty
        String songName = configHandler.getHeadClickNotOwnSound();
        if (!songName.trim().isEmpty()) {
            try {
                XSound.play(player, songName);
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError cannot play sound on head click! Cannot parse provided name..."));
            }
        }

        // Send title to the player if enabled
        if (configHandler.isHeadClickTitleEnabled()) {
            String firstLine = PlaceholdersHandler.parse(player, configHandler.getHeadClickTitleFirstLine());
            String subTitle = PlaceholdersHandler.parse(player, configHandler.getHeadClickTitleSubTitle());
            int fadeIn = configHandler.getHeadClickTitleFadeIn();
            int stay = configHandler.getHeadClickTitleStay();
            int fadeOut = configHandler.getHeadClickTitleFadeOut();

            player.sendTitle(firstLine, subTitle, fadeIn, stay, fadeOut);
        }

        // Fire firework if enabled
        if (configHandler.isFireworkEnabled()) {
            List<Color> colors = configHandler.getHeadClickFireworkColors();
            List<Color> fadeColors = configHandler.getHeadClickFireworkFadeColors();
            boolean isFlickering = configHandler.isFireworkFlickerEnabled();
            int power = configHandler.getHeadClickFireworkPower();

            Location loc = power == 0 ? clickedLocation.clone() : clickedLocation.clone().add(0, 0.5, 0);

            FireworkUtils.launchFirework(loc, isFlickering,
                    colors.size() == 0, colors, fadeColors.size() == 0, fadeColors, power, block.getType() == Material.PLAYER_WALL_HEAD);
        }

        // Prevent trigger commands rewards if current is contained in tieredRewards and enabled in config
        if (!main.getConfigHandler().isPreventCommandsOnTieredRewardsLevel()) {
            int playerHeads;
            try {
                playerHeads = main.getStorageHandler().getHeadsPlayer(player.getUniqueId()).size();
            } catch (InternalException ex) {
                player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving heads of " + player.getName() + " from the storage: " + ex.getMessage()));                return;
            }

            if (!main.getRewardHandler().currentIsContainedInTiered(playerHeads)) {
                // Commands list if not empty
                if (configHandler.getHeadClickCommands().size() != 0) {
                    Bukkit.getScheduler().runTaskLater(main, () -> configHandler.getHeadClickCommands().forEach(reward ->
                            main.getServer().dispatchCommand(main.getServer().getConsoleSender(), PlaceholdersHandler.parse(player, reward))), 1L);
                }
            }
        }

        // Check and reward if triggerRewards is used
        main.getRewardHandler().giveReward(player);

        // Trigger the event HeadClick with success
        Bukkit.getPluginManager().callEvent(new HeadClickEvent(headUuid, player, clickedLocation, true));
    }
}
