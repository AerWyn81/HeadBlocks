package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadClickEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.FireworkUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.bukkit.XSound;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
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
import java.util.Random;

public class OnPlayerInteractEvent implements Listener {

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

        // Prevent interactions with players in gamemode creative
        if (player.getGameMode() == GameMode.CREATIVE && e.getAction() == Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (HeadBlocks.isReloadInProgress) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.PluginReloading"));
            return;
        }

        Location clickedLocation = block.getLocation();

        // Check if the head is a head of the plugin
        HeadLocation headLocation = HeadService.getHeadAt(clickedLocation);
        if (headLocation == null) {
            return;
        }

        // Check if there is a storage issue
        if (StorageService.hasStorageError()) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            return;
        }

        // Check if the player has the permission to click on the head
        if (!PlayerUtils.hasPermission(player, "headblocks.use")) {
            String message = LanguageService.getMessage("Messages.NoPermissionBlock");

            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }
            return;
        }

        try {
            // Check if the player has already clicked on the head
            if (StorageService.hasHead(player.getUniqueId(), headLocation.getUuid())) {
                String message = PlaceholdersService.parse(player.getName(), player.getUniqueId(), LanguageService.getMessage("Messages.AlreadyClaimHead"));

                if (!message.trim().isEmpty()) {
                    player.sendMessage(message);
                }

                // Already own song if not empty
                String songName = ConfigService.getHeadClickAlreadyOwnSound();
                if (!songName.trim().isEmpty()) {
                    try {
                        XSound.play(player, ConfigService.getHeadClickAlreadyOwnSound());
                    } catch (Exception ex) {
                        player.sendMessage(LanguageService.getMessage("Messages.ErrorCannotPlaySound"));
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError cannot play sound on head click: " + ex.getMessage()));
                    }
                }

                // Already own particles if enabled
                if (ConfigService.isHeadClickParticlesEnabled()) {
                    String particleName = ConfigService.getHeadClickParticlesAlreadyOwnType();
                    int amount = ConfigService.getHeadClickParticlesAmount();
                    ArrayList<String> colors = ConfigService.getHeadClickParticlesColors();

                    try {
                        ParticlesUtils.spawn(clickedLocation, Particle.valueOf(particleName), amount, colors, player);
                    } catch (Exception ex) {
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError particle name " + particleName + " cannot be parsed!"));
                    }
                }

                // Trigger the event HeadClick with no success because the player already own the head
                Bukkit.getPluginManager().callEvent(new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, false));
                return;
            }

            // Check head order
            if (headLocation.getOrderIndex() != -1) {
                var playerHeads = StorageService.getHeadsPlayer(player.getUniqueId(), player.getName());

                if (HeadService.getChargedHeadLocations().stream()
                        .filter(h -> h.getUuid() != headLocation.getUuid() && !playerHeads.contains(h.getUuid()))
                        .anyMatch(h -> h.getOrderIndex() <= headLocation.getOrderIndex())) {
                    player.sendMessage(LanguageService.getMessage("Messages.OrderClickError")
                            .replaceAll("%name%", headLocation.getDisplayedName()));
                    return;
                }
            }

            if (headLocation.getHitCount() != -1) {
                var players = StorageService.getPlayers(headLocation.getUuid());

                if (players.size() == headLocation.getHitCount()) {
                    player.sendMessage(LanguageService.getMessage("Messages.HitClickMax")
                            .replaceAll("%count%", headLocation.getDisplayedHitCount()));
                    return;
                }
            }

            // Save player click in storage
            StorageService.addHead(player.getUniqueId(), headLocation.getUuid());
        } catch (InternalException ex) {
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to save a head found by " + player.getName() + " from the storage: " + ex.getMessage()));
            return;
        }

        // Success messages if not empty
        List<String> messages = ConfigService.getHeadClickMessages();
        if (messages.size() != 0) {
            player.sendMessage(PlaceholdersService.parse(player, messages));
        }

        // Success song if not empty
        String songName = ConfigService.getHeadClickNotOwnSound();
        if (!songName.trim().isEmpty()) {
            try {
                XSound.play(player, songName);
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError cannot play sound on head click! Cannot parse provided name..."));
            }
        }

        // Send title to the player if enabled
        if (ConfigService.isHeadClickTitleEnabled()) {
            String firstLine = PlaceholdersService.parse(player.getName(), player.getUniqueId(), ConfigService.getHeadClickTitleFirstLine());
            String subTitle = PlaceholdersService.parse(player.getName(), player.getUniqueId(), ConfigService.getHeadClickTitleSubTitle());
            int fadeIn = ConfigService.getHeadClickTitleFadeIn();
            int stay = ConfigService.getHeadClickTitleStay();
            int fadeOut = ConfigService.getHeadClickTitleFadeOut();

            player.sendTitle(firstLine, subTitle, fadeIn, stay, fadeOut);
        }

        // Fire firework if enabled
        if (ConfigService.isFireworkEnabled()) {
            List<Color> colors = ConfigService.getHeadClickFireworkColors();
            List<Color> fadeColors = ConfigService.getHeadClickFireworkFadeColors();
            boolean isFlickering = ConfigService.isFireworkFlickerEnabled();
            int power = ConfigService.getHeadClickFireworkPower();

            Location loc = power == 0 ? clickedLocation.clone() : clickedLocation.clone().add(0, 0.5, 0);

            FireworkUtils.launchFirework(loc, isFlickering,
                    colors.size() == 0, colors, fadeColors.size() == 0, fadeColors, power, block.getType() == Material.PLAYER_WALL_HEAD);
        }

        // Prevent trigger commands rewards if current is contained in tieredRewards and enabled in config
        if (!ConfigService.isPreventCommandsOnTieredRewardsLevel()) {
            int playerHeads;
            try {
                playerHeads = StorageService.getHeadsPlayer(player.getUniqueId(), player.getName()).size();
            } catch (InternalException ex) {
                player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving heads of " + player.getName() + " from the storage: " + ex.getMessage()));
                return;
            }

            if (!RewardService.currentIsContainedInTiered(playerHeads)) {
                // Commands list if not empty
                if (ConfigService.getHeadClickCommands().size() != 0) {
                    var plugin = HeadBlocks.getInstance();
                    var isRandomCommand = ConfigService.isHeadClickCommandsRandomized();

                    if (isRandomCommand) {
                        String randomCommand = ConfigService.getHeadClickCommands().get(new Random().nextInt(ConfigService.getHeadClickCommands().size()));
                        Bukkit.getScheduler().runTaskLater(plugin, () ->
                                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), PlaceholdersService.parse(player.getName(), player.getUniqueId(), randomCommand)), 1L);
                    } else {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> ConfigService.getHeadClickCommands().forEach(reward ->
                                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), PlaceholdersService.parse(player.getName(), player.getUniqueId(), reward))), 1L);
                    }
                }
            }
        }

        // Show hologram
        HologramService.showFoundTo(player, clickedLocation);

        // Check and reward if triggerRewards is used
        RewardService.giveReward(player);

        // Trigger the event HeadClick with success
        Bukkit.getPluginManager().callEvent(new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, true));
    }
}
