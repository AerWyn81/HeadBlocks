package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadClickEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.FireworkUtils;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.bukkit.XSeries.XSound;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
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

public class OnPlayerInteractEvent implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();

        // Check if the correct hand is used
        if (block == null || e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Check if clickedBlock is a head
        if (!HeadUtils.isPlayerHead(block)) {
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

        // Check if the player has already clicked on the head
        StorageService.getHeadsPlayer(player.getUniqueId()).whenComplete(p -> {
            var playerHeads = new ArrayList<>(p);

            if (playerHeads.contains(headLocation.getUuid())) {
                String message = PlaceholdersService.parse(player.getName(), player.getUniqueId(), headLocation, LanguageService.getMessage("Messages.AlreadyClaimHead"));

                if (!message.trim().isEmpty()) {
                    player.sendMessage(message);
                }

                if (ConfigService.isHeadClickEjectEnabled()) {
                    var power = ConfigService.getHeadClickEjectPower();

                    var oppositeDir = player.getLocation().getDirection().multiply(-1).normalize();
                    oppositeDir = oppositeDir.multiply(power).setY(0.3);
                    player.setVelocity(oppositeDir);
                }

                // Already own song if not empty
                String songName = ConfigService.getHeadClickAlreadyOwnSound();
                if (!songName.trim().isEmpty()) {
                    try {
                        XSound.play(ConfigService.getHeadClickAlreadyOwnSound(), s -> s.forPlayers(player));
                    } catch (Exception ex) {
                        player.sendMessage(LanguageService.getMessage("Messages.ErrorCannotPlaySound"));
                        LogUtil.error("Error cannot play sound on head click: {0}", ex.getMessage());
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
                        LogUtil.error("Error particle name {0} cannot be parsed!", particleName);
                    }
                }

                // Trigger the event HeadClick with no success because the player already own the head
                Bukkit.getPluginManager().callEvent(new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, false));
                return;
            }

            // Check head order
            if (headLocation.getOrderIndex() != -1) {
                if (HeadService.getChargedHeadLocations().stream()
                        .filter(h -> h.getUuid() != headLocation.getUuid() && !playerHeads.contains(h.getUuid()))
                        .anyMatch(h -> h.getOrderIndex() <= headLocation.getOrderIndex())) {
                    player.sendMessage(PlaceholdersService.parse(player.getName(), player.getUniqueId(), headLocation,
                            LanguageService.getMessage("Messages.OrderClickError")
                                    .replaceAll("%name%", headLocation.getNameOrUnnamed())));
                    return;
                }
            }

            // Check hit count
            if (headLocation.getHitCount() != -1) {
                try {
                    var players = StorageService.getPlayers(headLocation.getUuid());

                    if (players.size() == headLocation.getHitCount()) {
                        player.sendMessage(LanguageService.getMessage("Messages.HitClickMax")
                                .replaceAll("%count%", headLocation.getDisplayedHitCount()));
                        return;
                    }
                } catch (InternalException ex) {
                    LogUtil.error("Error retrieving players from storage when calculating hit count: {0}", ex.getMessage());
                    return;
                }
            }

            playerHeads.add(headLocation.getUuid());

            if (!RewardService.hasPlayerSlotsRequired(player, playerHeads)) {
                var message = LanguageService.getMessage("Messages.InventoryFullReward");
                if (!message.trim().isEmpty()) {
                    player.sendMessage(message);
                }

                return;
            }

            // Save player click in storage
            try {
                StorageService.addHead(player.getUniqueId(), headLocation.getUuid());
            } catch (InternalException ex) {
                LogUtil.error("Error saving player found head in storage: {0}", ex.getMessage());
                return;
            }

            // Hide the head for this player if enabled
            var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
            if (packetEventsHook != null && packetEventsHook.isEnabled() && packetEventsHook.getHeadHidingListener() != null) {
                packetEventsHook.getHeadHidingListener().addFoundHead(player, headLocation.getUuid());
            }

            // Give reward if triggerRewards is used
            RewardService.giveReward(player, playerHeads, headLocation);

            // Give special head rewards
            for (var reward : headLocation.getRewards()) {
                reward.execute(player, headLocation);
            }

            // Success song if not empty
            String songName = ConfigService.getHeadClickNotOwnSound();
            if (!songName.trim().isEmpty()) {
                try {
                    XSound.play(songName, s -> s.forPlayers(player));
                } catch (Exception ex) {
                    LogUtil.error("Error cannot play sound on head click! Cannot parse provided name...");
                }
            }

            // Send title to the player if enabled
            if (ConfigService.isHeadClickTitleEnabled()) {
                String firstLine = PlaceholdersService.parse(player.getName(), player.getUniqueId(), headLocation, ConfigService.getHeadClickTitleFirstLine());
                String subTitle = PlaceholdersService.parse(player.getName(), player.getUniqueId(), headLocation, ConfigService.getHeadClickTitleSubTitle());
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
                        colors.isEmpty(), colors, fadeColors.isEmpty(), fadeColors, power, block.getType() == Material.PLAYER_WALL_HEAD);
            }

            // Trigger the event HeadClick with success
            Bukkit.getPluginManager().callEvent(new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, true));
        });
    }
}
