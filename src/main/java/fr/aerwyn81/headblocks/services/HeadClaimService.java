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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeadClaimService {

    public enum Outcome {
        STORAGE_ERROR,
        NO_PERMISSION,
        NO_HUNT,
        HUNT_INACTIVE,
        PROCESSING
    }

    private static final int MAX_REPLAYS = 100;

    private final ServiceRegistry registry;
    private final Set<String> claiming = ConcurrentHashMap.newKeySet();

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

        handleHuntClick(player, headLocation, clickedLocation, wallHead, hunt, 0);
        return Outcome.PROCESSING;
    }

    private void handleHuntClick(Player player, HeadLocation headLocation, Location clickedLocation,
                                 boolean wallHead, HBHunt hunt, int replays) {
        HuntConfig huntConfig = hunt.getConfig();

        registry.getStorageService().getHeadsPlayer(player.getUniqueId()).whenComplete(player, allPlayerHeads ->
                processHuntClick(player, headLocation, clickedLocation, wallHead, hunt, replays, huntConfig));
    }

    private void processHuntClick(Player player, HeadLocation headLocation, Location clickedLocation,
                                  boolean wallHead, HBHunt hunt, int replays, HuntConfig huntConfig) {
        var claimKey = player.getUniqueId() + ":" + hunt.getId();
        if (!claiming.add(claimKey)) {
            replayLater(player, headLocation, clickedLocation, wallHead, hunt, replays);
            return;
        }

        boolean writing = false;
        try {
            var huntPlayerHeads = registry.getStorageService().getHeadsPlayerForHunt(
                    player.getUniqueId(), hunt.getId());

            if (!canClaim(player, headLocation, clickedLocation, hunt, huntConfig, huntPlayerHeads)) {
                return;
            }

            boolean spawned = registry.getHeadService().isSpawned(headLocation.getUuid());
            var commitResult = hunt.commitBehaviors(player, headLocation);
            if (!commitResult.allowed()) {
                sendIfPresent(player, commitResult.denyMessage());
                return;
            }

            writing = true;
            registry.getScheduler().runTaskAsync(() ->
                    saveFoundHead(player, headLocation, clickedLocation, wallHead, hunt, huntPlayerHeads, spawned));
        } catch (InternalException ex) {
            LogUtil.error("Error processing hunt {0} click for player {1}: {2}",
                    hunt.getId(), player.getName(), ex.getMessage());
        } finally {
            if (!writing) {
                claiming.remove(claimKey);
            }
        }
    }

    private void replayLater(Player player, HeadLocation headLocation, Location clickedLocation,
                             boolean wallHead, HBHunt hunt, int replays) {
        if (replays >= MAX_REPLAYS) {
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return;
        }

        registry.getScheduler().runTaskLater(player, () -> {
            if (player.isOnline() && hunt.isActive()) {
                handleHuntClick(player, headLocation, clickedLocation, wallHead, hunt, replays + 1);
            }
        }, 1L);
    }

    private boolean canClaim(Player player, HeadLocation headLocation, Location clickedLocation, HBHunt hunt,
                             HuntConfig huntConfig, List<UUID> huntPlayerHeads) {
        var accessResult = hunt.evaluateAccessGates(player, headLocation);
        if (!accessResult.allowed()) {
            sendIfPresent(player, accessResult.denyMessage());
            return false;
        }

        if (huntPlayerHeads.contains(headLocation.getUuid())) {
            showAlreadyClaimed(player, headLocation, clickedLocation, huntConfig, hunt.getId());

            Bukkit.getPluginManager().callEvent(
                    new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, false, List.of(hunt.getId())));
            return false;
        }

        var requirementResult = hunt.evaluateRequirements(player, headLocation);
        if (!requirementResult.satisfied()) {
            sendIfPresent(player, requirementResult.reason());
            return false;
        }

        var behaviorResult = hunt.evaluateBehaviors(player, headLocation);
        if (!behaviorResult.allowed()) {
            sendIfPresent(player, behaviorResult.denyMessage());
            return false;
        }

        huntPlayerHeads.add(headLocation.getUuid());

        if (!registry.getRewardService().hasPlayerSlotsRequired(player, huntPlayerHeads, huntConfig)) {
            var message = registry.getLanguageService().message("Messages.InventoryFullReward");
            if (!message.trim().isEmpty()) {
                player.sendMessage(message);
            }
            return false;
        }

        return true;
    }

    private static void sendIfPresent(Player player, String message) {
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    private void saveFoundHead(Player player, HeadLocation headLocation, Location clickedLocation, boolean wallHead,
                               HBHunt hunt, List<UUID> huntPlayerHeads, boolean spawned) {
        var claimKey = player.getUniqueId() + ":" + hunt.getId();
        try {
            if (spawned) {
                registry.getSpawnService().storeFound(headLocation);
            }
            registry.getStorageService().addHeadForHunt(player.getUniqueId(), headLocation.getUuid(), hunt.getId());
        } catch (Exception ex) {
            LogUtil.error("Error saving head {0} found by {1} in hunt {2}: {3}",
                    headLocation.getUuid(), player.getName(), hunt.getId(), ex.getMessage());
            registry.getScheduler().runTask(player,
                    () -> player.sendMessage(registry.getLanguageService().message("Messages.StorageError")));
            return;
        } finally {
            claiming.remove(claimKey);
        }

        registry.getScheduler().runTask(player,
                () -> onHeadFound(player, headLocation, clickedLocation, wallHead, hunt, huntPlayerHeads));
    }

    private void onHeadFound(Player player, HeadLocation headLocation, Location clickedLocation, boolean wallHead,
                             HBHunt hunt, List<UUID> huntPlayerHeads) {
        HuntConfig huntConfig = hunt.getConfig();

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
            FireworkUtils.launchFirework(loc, isFlickering, colors, fadeColors, power, wallHead);
        }

        Bukkit.getPluginManager().callEvent(
                new HeadClickEvent(headLocation.getUuid(), player, clickedLocation, true, List.of(hunt.getId())));
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
            var colors = registry.getConfigService().headClickParticlesColors();

            try {
                ParticlesUtils.spawn(clickedLocation, ParticlesUtils.resolve(particleName), amount, colors, player);
            } catch (Exception ex) {
                LogUtil.error("Error particle name {0} cannot be parsed!", particleName);
            }
        }
    }
}
