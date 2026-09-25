package fr.aerwyn81.headblocks.services;

import com.cryptomorin.xseries.XSound;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HeadClickEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.utils.bukkit.FireworkUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HeadClaimService {

    public enum Outcome {
        STORAGE_ERROR,
        NO_PERMISSION,
        NO_HUNT,
        HUNT_INACTIVE,
        PROCESSING
    }

    private final ServiceRegistry registry;

    public HeadClaimService(ServiceRegistry registry) {
        this.registry = registry;
    }

    public Outcome click(Player player, HeadLocation headLocation, Location clickedLocation, boolean wallHead) {
        if (registry.getStorageService().isStorageError()) {
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return Outcome.STORAGE_ERROR;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.use")) {
            String message = registry.getLanguageService().message("Messages.NoPermissionBlock");

            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }
            return Outcome.NO_PERMISSION;
        }

        HBHunt hunt = registry.getHuntService().getHuntById(headLocation.getHuntId());

        if (hunt == null) {
            LogUtil.warning("Head {0} at {1} has no hunt assigned. Ignoring click.",
                    headLocation.getUuid(), headLocation.getLocation());
            return Outcome.NO_HUNT;
        }

        if (!hunt.isActive()) {
            String msg = registry.getLanguageService().message("Messages.HuntHeadInactive");
            if (!msg.trim().isEmpty()) {
                player.sendMessage(msg);
            }
            return Outcome.HUNT_INACTIVE;
        }

        handleHuntClick(player, headLocation, clickedLocation, wallHead, hunt);
        return Outcome.PROCESSING;
    }

    private void handleHuntClick(Player player, HeadLocation headLocation, Location clickedLocation,
                                 boolean wallHead, HBHunt hunt) {
        HuntConfig huntConfig = hunt.getConfig();

        registry.getStorageService().getHeadsPlayer(player.getUniqueId()).whenComplete(player, allPlayerHeads -> {
            try {
                ArrayList<UUID> huntPlayerHeads = registry.getStorageService().getHeadsPlayerForHunt(
                        player.getUniqueId(), hunt.getId());

                var accessResult = hunt.evaluateAccessGates(player, headLocation);
                if (!accessResult.allowed()) {
                    if (accessResult.denyMessage() != null && !accessResult.denyMessage().isEmpty()) {
                        player.sendMessage(accessResult.denyMessage());
                    }
                    return;
                }

                if (huntPlayerHeads.contains(headLocation.getUuid())) {
                    showAlreadyClaimed(player, headLocation, clickedLocation, huntConfig, hunt.getId());

                    Bukkit.getPluginManager().callEvent(
                            new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, false, List.of(hunt.getId())));
                    return;
                }

                var requirementResult = hunt.evaluateRequirements(player, headLocation);
                if (!requirementResult.satisfied()) {
                    if (requirementResult.reason() != null && !requirementResult.reason().isEmpty()) {
                        player.sendMessage(requirementResult.reason());
                    }
                    return;
                }

                var behaviorResult = hunt.evaluateBehaviors(player, headLocation);
                if (!behaviorResult.allowed()) {
                    if (behaviorResult.denyMessage() != null && !behaviorResult.denyMessage().isEmpty()) {
                        player.sendMessage(behaviorResult.denyMessage());
                    }
                    return;
                }

                huntPlayerHeads.add(headLocation.getUuid());

                if (!registry.getRewardService().hasPlayerSlotsRequired(player, huntPlayerHeads, huntConfig)) {
                    var message = registry.getLanguageService().message("Messages.InventoryFullReward");
                    if (!message.trim().isEmpty()) {
                        player.sendMessage(message);
                    }
                    return;
                }

                registry.getStorageService().addHeadForHunt(player.getUniqueId(), headLocation.getUuid(), hunt.getId());

                hunt.notifyHeadFound(player, headLocation);
                registry.getAreaEnforcementService().onHeadFound(player, hunt, huntPlayerHeads.size());

                registry.getRewardService().giveReward(player, huntPlayerHeads, headLocation, huntConfig, hunt.getId());

                for (var reward : headLocation.getRewards()) {
                    reward.execute(player, headLocation, registry);
                }

                registry.getVisibilityService().onHeadFound(player, headLocation);

                String songName = huntConfig.getHeadClickSoundFound();
                if (!songName.trim().isEmpty()) {
                    try {
                        XSound.play(songName, s -> s.forPlayers(player));
                    } catch (Exception ex) {
                        LogUtil.error("Error cannot play sound on head click! Cannot parse provided name...");
                    }
                }

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

                if (huntConfig.isFireworkEnabled()) {
                    List<Color> colors = registry.getConfigService().headClickFireworkColors();
                    List<Color> fadeColors = registry.getConfigService().headClickFireworkFadeColors();
                    boolean isFlickering = registry.getConfigService().fireworkFlickerEnabled();
                    int power = registry.getConfigService().headClickFireworkPower();

                    Location loc = power == 0 ? clickedLocation.clone() : clickedLocation.clone().add(0, 0.5, 0);
                    FireworkUtils.launchFirework(loc, isFlickering,
                            colors.isEmpty(), colors, fadeColors.isEmpty(), fadeColors,
                            power, wallHead);
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
                ParticlesUtils.spawn(clickedLocation, ParticlesUtils.resolve(particleName), amount, colors, player);
            } catch (Exception ex) {
                LogUtil.error("Error particle name {0} cannot be parsed!", particleName);
            }
        }
    }
}
