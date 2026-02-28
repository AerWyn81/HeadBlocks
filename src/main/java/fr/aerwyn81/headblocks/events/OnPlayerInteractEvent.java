package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadClickEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
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
import java.util.UUID;
import java.util.stream.Collectors;

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

        // Get hunts for this head
        List<Hunt> allHunts = HuntService.getHuntsForHead(headLocation.getUuid());
        List<Hunt> activeHunts = allHunts.stream()
                .filter(Hunt::isActive)
                .collect(Collectors.toList());

        if (activeHunts.isEmpty()) {
            if (!allHunts.isEmpty()) {
                // Head has hunts but all are inactive
                String msg = LanguageService.getMessage("Messages.HuntHeadInactive");
                if (!msg.trim().isEmpty()) {
                    player.sendMessage(msg);
                }
                return;
            }

            // No hunts assigned — data inconsistency (migration should have assigned all heads to default)
            LogUtil.warning("Head {0} at {1} has no hunt assigned. Ignoring click.",
                    headLocation.getUuid(), headLocation.getLocation());
            return;
        }

        // Multi-hunt aware click handling
        handleHuntClick(player, headLocation, clickedLocation, block, activeHunts);
    }

    private void handleHuntClick(Player player, HeadLocation headLocation, Location clickedLocation,
                                 Block block, List<Hunt> activeHunts) {
        Hunt primaryHunt = activeHunts.get(0); // highest priority (list is sorted)
        HuntConfig primaryConfig = primaryHunt.getConfig();

        StorageService.getHeadsPlayer(player.getUniqueId()).whenComplete(allPlayerHeads -> {
            var playerHeads = new ArrayList<>(allPlayerHeads);
            boolean globallyFound = playerHeads.contains(headLocation.getUuid());
            boolean anyNewFind = false;
            List<String> foundHuntIds = new ArrayList<>();

            for (Hunt hunt : activeHunts) {
                try {
                    ArrayList<UUID> huntPlayerHeads = StorageService.getHeadsPlayerForHunt(
                            player.getUniqueId(), hunt.getId());

                    if (huntPlayerHeads.contains(headLocation.getUuid())) {
                        continue; // Already found in this hunt
                    }

                    // Check behaviors
                    var behaviorResult = hunt.evaluateBehaviors(player, headLocation);
                    if (!behaviorResult.allowed()) {
                        if (behaviorResult.denyMessage() != null && !behaviorResult.denyMessage().isEmpty()) {
                            player.sendMessage(behaviorResult.denyMessage());
                        }
                        continue;
                    }

                    // Check hit count per hunt
                    if (headLocation.getHitCount() != -1) {
                        int count = StorageService.getPlayerCountForHeadInHunt(
                                headLocation.getUuid(), hunt.getId());
                        if (count >= headLocation.getHitCount()) {
                            player.sendMessage(LanguageService.getMessage("Messages.HitClickMax")
                                    .replaceAll("%count%", headLocation.getDisplayedHitCount()));
                            continue;
                        }
                    }

                    // Prepare updated hunt heads for reward calculation
                    huntPlayerHeads.add(headLocation.getUuid());

                    // Check inventory slots
                    if (!RewardService.hasPlayerSlotsRequired(player, huntPlayerHeads, hunt.getConfig())) {
                        var message = LanguageService.getMessage("Messages.InventoryFullReward");
                        if (!message.trim().isEmpty()) {
                            player.sendMessage(message);
                        }
                        continue;
                    }

                    // Register find for this hunt (also updates storage cache globally)
                    StorageService.addHeadForHunt(player.getUniqueId(), headLocation.getUuid(), hunt.getId());

                    // Update local tracking for subsequent iterations
                    if (!globallyFound) {
                        playerHeads.add(headLocation.getUuid());
                        globallyFound = true;
                    }

                    // Notify behaviors
                    hunt.notifyHeadFound(player, headLocation);

                    // Give hunt-specific rewards
                    RewardService.giveReward(player, huntPlayerHeads, headLocation, hunt.getConfig());

                    // Give special head rewards only on first new find
                    if (!anyNewFind) {
                        for (var reward : headLocation.getRewards()) {
                            reward.execute(player, headLocation);
                        }
                    }

                    foundHuntIds.add(hunt.getId());
                    anyNewFind = true;
                } catch (InternalException ex) {
                    LogUtil.error("Error processing hunt {0} click for player {1}: {2}",
                            hunt.getId(), player.getName(), ex.getMessage());
                }
            }

            if (anyNewFind) {
                // Hide the head for this player if enabled
                var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
                if (packetEventsHook != null && packetEventsHook.isEnabled()
                        && packetEventsHook.getHeadHidingListener() != null) {
                    packetEventsHook.getHeadHidingListener().addFoundHead(player, headLocation.getUuid());
                }

                // Success sound using primary hunt config
                String songName = primaryConfig.getHeadClickSoundFound();
                if (!songName.trim().isEmpty()) {
                    try {
                        XSound.play(songName, s -> s.forPlayers(player));
                    } catch (Exception ex) {
                        LogUtil.error("Error cannot play sound on head click! Cannot parse provided name...");
                    }
                }

                // Title using primary hunt config
                if (primaryConfig.isHeadClickTitleEnabled()) {
                    String firstLine = PlaceholdersService.parse(player.getName(), player.getUniqueId(),
                            headLocation, primaryConfig.getHeadClickTitleFirstLine());
                    String subTitle = PlaceholdersService.parse(player.getName(), player.getUniqueId(),
                            headLocation, primaryConfig.getHeadClickTitleSubTitle());
                    int fadeIn = primaryConfig.getHeadClickTitleFadeIn();
                    int stay = primaryConfig.getHeadClickTitleStay();
                    int fadeOut = primaryConfig.getHeadClickTitleFadeOut();
                    player.sendTitle(firstLine, subTitle, fadeIn, stay, fadeOut);
                }

                // Firework using primary hunt config
                if (primaryConfig.isFireworkEnabled()) {
                    List<Color> colors = ConfigService.getHeadClickFireworkColors();
                    List<Color> fadeColors = ConfigService.getHeadClickFireworkFadeColors();
                    boolean isFlickering = ConfigService.isFireworkFlickerEnabled();
                    int power = ConfigService.getHeadClickFireworkPower();

                    Location loc = power == 0 ? clickedLocation.clone() : clickedLocation.clone().add(0, 0.5, 0);
                    FireworkUtils.launchFirework(loc, isFlickering,
                            colors.isEmpty(), colors, fadeColors.isEmpty(), fadeColors,
                            power, block.getType() == Material.PLAYER_WALL_HEAD);
                }

                Bukkit.getPluginManager().callEvent(
                        new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, true, foundHuntIds));
            } else {
                // All hunts already found — show "already claimed" with primary config
                showAlreadyClaimed(player, headLocation, clickedLocation, primaryConfig);

                var allHuntIds = activeHunts.stream().map(Hunt::getId).collect(java.util.stream.Collectors.toList());
                Bukkit.getPluginManager().callEvent(
                        new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, false, allHuntIds));
            }
        });
    }

    private void showAlreadyClaimed(Player player, HeadLocation headLocation,
                                    Location clickedLocation, HuntConfig config) {
        String message = PlaceholdersService.parse(player.getName(), player.getUniqueId(),
                headLocation, LanguageService.getMessage("Messages.AlreadyClaimHead"));
        if (!message.trim().isEmpty()) {
            player.sendMessage(message);
        }

        if (config.isHeadClickEjectEnabled()) {
            var power = config.getHeadClickEjectPower();
            var oppositeDir = player.getLocation().getDirection().multiply(-1).normalize();
            oppositeDir = oppositeDir.multiply(power).setY(0.3);
            player.setVelocity(oppositeDir);
        }

        String songName = config.getHeadClickSoundAlreadyOwn();
        if (!songName.trim().isEmpty()) {
            try {
                XSound.play(songName, s -> s.forPlayers(player));
            } catch (Exception ex) {
                player.sendMessage(LanguageService.getMessage("Messages.ErrorCannotPlaySound"));
                LogUtil.error("Error cannot play sound on head click: {0}", ex.getMessage());
            }
        }

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
    }
}
