package fr.aerwyn81.headblocks.events;

import com.cryptomorin.xseries.XSound;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HeadClickEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.utils.bukkit.FireworkUtils;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
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

public class OnPlayerInteractEvent implements Listener {

    private final ServiceRegistry registry;

    public OnPlayerInteractEvent(ServiceRegistry registry) {
        this.registry = registry;
    }

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
            player.sendMessage(registry.getLanguageService().message("Messages.PluginReloading"));
            return;
        }

        Location clickedLocation = block.getLocation();

        // Check if the head is a head of the plugin
        HeadLocation headLocation = registry.getHeadService().getHeadAt(clickedLocation);
        if (headLocation == null) {
            return;
        }

        // Check if there is a storage issue
        if (registry.getStorageService().isStorageError()) {
            e.setCancelled(true);
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return;
        }

        // Check if the player has the permission to click on the head
        if (!PlayerUtils.hasPermission(player, "headblocks.use")) {
            String message = registry.getLanguageService().message("Messages.NoPermissionBlock");

            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }
            return;
        }

        // Get hunt for this head (1:1 relationship)
        HBHunt hunt = registry.getHuntService().getHuntById(headLocation.getHuntId());

        if (hunt == null) {
            LogUtil.warning("Head {0} at {1} has no hunt assigned. Ignoring click.",
                    headLocation.getUuid(), headLocation.getLocation());
            return;
        }

        if (!hunt.isActive()) {
            String msg = registry.getLanguageService().message("Messages.HuntHeadInactive");
            if (!msg.trim().isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }

        // Single-hunt click handling
        handleHuntClick(player, headLocation, clickedLocation, block, hunt);
    }

    private void handleHuntClick(Player player, HeadLocation headLocation, Location clickedLocation,
                                 Block block, HBHunt hunt) {
        HuntConfig huntConfig = hunt.getConfig();

        registry.getStorageService().getHeadsPlayer(player.getUniqueId()).whenComplete(allPlayerHeads -> {
            try {
                ArrayList<UUID> huntPlayerHeads = registry.getStorageService().getHeadsPlayerForHunt(
                        player.getUniqueId(), hunt.getId());

                // Check access-gate behaviors first (scheduled) — these gate the entire hunt
                var accessResult = hunt.evaluateAccessGates(player, headLocation);
                if (!accessResult.allowed()) {
                    if (accessResult.denyMessage() != null && !accessResult.denyMessage().isEmpty()) {
                        player.sendMessage(accessResult.denyMessage());
                    }
                    return;
                }

                if (huntPlayerHeads.contains(headLocation.getUuid())) {
                    // Already found in this hunt
                    showAlreadyClaimed(player, headLocation, clickedLocation, huntConfig, hunt.getId());

                    Bukkit.getPluginManager().callEvent(
                            new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, false, List.of(hunt.getId())));
                    return;
                }

                // Check remaining behaviors (ordered, timed, etc.)
                var behaviorResult = hunt.evaluateBehaviors(player, headLocation);
                if (!behaviorResult.allowed()) {
                    if (behaviorResult.denyMessage() != null && !behaviorResult.denyMessage().isEmpty()) {
                        player.sendMessage(behaviorResult.denyMessage());
                    }
                    return;
                }

                // Prepare updated hunt heads for reward calculation
                huntPlayerHeads.add(headLocation.getUuid());

                // Check inventory slots
                if (!registry.getRewardService().hasPlayerSlotsRequired(player, huntPlayerHeads, huntConfig)) {
                    var message = registry.getLanguageService().message("Messages.InventoryFullReward");
                    if (!message.trim().isEmpty()) {
                        player.sendMessage(message);
                    }
                    return;
                }

                // Register find for this hunt
                registry.getStorageService().addHeadForHunt(player.getUniqueId(), headLocation.getUuid(), hunt.getId());

                // Notify behaviors
                hunt.notifyHeadFound(player, headLocation);

                // Give hunt-specific rewards
                registry.getRewardService().giveReward(player, huntPlayerHeads, headLocation, huntConfig, hunt.getId());

                // Give special head rewards
                for (var reward : headLocation.getRewards()) {
                    reward.execute(player, headLocation, registry);
                }

                // Hide the head for this player if enabled
                var packetEventsHook = HeadBlocks.getInstance().getPacketEventsHook();
                if (packetEventsHook != null && packetEventsHook.isEnabled()
                        && packetEventsHook.getHeadHidingListener() != null) {
                    packetEventsHook.getHeadHidingListener().addFoundHead(player, headLocation.getUuid());
                }

                // Success sound
                String songName = huntConfig.getHeadClickSoundFound();
                if (!songName.trim().isEmpty()) {
                    try {
                        XSound.play(songName, s -> s.forPlayers(player));
                    } catch (Exception ex) {
                        LogUtil.error("Error cannot play sound on head click! Cannot parse provided name...");
                    }
                }

                // Title
                if (huntConfig.isHeadClickTitleEnabled()) {
                    String firstLine = registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(),
                            headLocation, huntConfig.getHeadClickTitleFirstLine(), hunt.getId());
                    String subTitle = registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(),
                            headLocation, huntConfig.getHeadClickTitleSubTitle(), hunt.getId());
                    int fadeIn = huntConfig.getHeadClickTitleFadeIn();
                    int stay = huntConfig.getHeadClickTitleStay();
                    int fadeOut = huntConfig.getHeadClickTitleFadeOut();
                    player.sendTitle(firstLine, subTitle, fadeIn, stay, fadeOut);
                }

                // Firework
                if (huntConfig.isFireworkEnabled()) {
                    List<Color> colors = registry.getConfigService().headClickFireworkColors();
                    List<Color> fadeColors = registry.getConfigService().headClickFireworkFadeColors();
                    boolean isFlickering = registry.getConfigService().fireworkFlickerEnabled();
                    int power = registry.getConfigService().headClickFireworkPower();

                    Location loc = power == 0 ? clickedLocation.clone() : clickedLocation.clone().add(0, 0.5, 0);
                    FireworkUtils.launchFirework(loc, isFlickering,
                            colors.isEmpty(), colors, fadeColors.isEmpty(), fadeColors,
                            power, block.getType() == Material.PLAYER_WALL_HEAD);
                }

                Bukkit.getPluginManager().callEvent(
                        new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, true, List.of(hunt.getId())));
            } catch (InternalException ex) {
                LogUtil.error("Error processing hunt {0} click for player {1}: {2}",
                        hunt.getId(), player.getName(), ex.getMessage());
            }
        });
    }

    private void showAlreadyClaimed(Player player, HeadLocation headLocation,
                                    Location clickedLocation, HuntConfig config, String huntId) {
        String message = registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(),
                headLocation, registry.getLanguageService().message("Messages.AlreadyClaimHead"), huntId);
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
                player.sendMessage(registry.getLanguageService().message("Messages.ErrorCannotPlaySound"));
                LogUtil.error("Error cannot play sound on head click: {0}", ex.getMessage());
            }
        }

        if (registry.getConfigService().headClickParticlesEnabled()) {
            String particleName = registry.getConfigService().headClickParticlesAlreadyOwnType();
            int amount = registry.getConfigService().headClickParticlesAmount();
            ArrayList<String> colors = registry.getConfigService().headClickParticlesColors();

            try {
                ParticlesUtils.spawn(clickedLocation, Particle.valueOf(particleName), amount, colors, player);
            } catch (Exception ex) {
                LogUtil.error("Error particle name {0} cannot be parsed!", particleName);
            }
        }
    }
}
