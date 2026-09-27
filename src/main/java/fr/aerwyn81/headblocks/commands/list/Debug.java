package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.utils.runnables.CompletableBukkitFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@HBAnnotations(command = "debug", permission = "headblocks.debug", isVisible = false)
public class Debug implements Cmd {
    private static final String COUNT_PLACEHOLDER = "%count%";
    private static final String ERROR_DETAIL_SEPARATOR = "&c: &e";
    private static final String PLAYER_HEADS_ERROR = " &cError when retrieving heads for player &e";
    private static final String TEXTURE = "texture";
    private static final String RESYNC = "resync";
    private static final String DATABASE = "database";
    private static final String RANDOM = "random";

    private final ServiceRegistry registry;

    public Debug(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(registry.getLanguageService().message("Messages.DebugUsage"));
            return;
        }

        switch (args[1]) {
            case TEXTURE -> handleTexture(sender, args);
            case "give" -> handleGive(sender, args);
            case "holograms" -> {
                registry.getHologramService().unload();
                registry.getHologramService().load();

                sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &aHolograms reloaded!"));
            }
            case RESYNC -> handleResync(sender, args);
            default ->
                    sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cUnknown debug command!"));
        }
    }

    private void handleTexture(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return;
        }

        var blockView = ((Player) sender).getTargetBlock(null, 50);
        if (blockView.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.NoTargetHeadBlock"));
            return;
        }

        var blockLocation = blockView.getLocation();
        var block = blockLocation.getBlock();

        var tempBlock = block.getLocation().clone().add(0, 1, 0).getBlock();
        if (!tempBlock.isEmpty() && !HeadUtils.isPlayerHead(block)) {
            sender.sendMessage("Block at " + blockLocation.toVector() + " is not empty: " + block.getType());
            return;
        }

        if (!HeadUtils.isPlayerHead(blockView)) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cBlock is not a player head!"));
            return;
        }

        if (args.length < 3 || args[2].isEmpty()) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cTexture cannot be empty!"));
            return;
        }

        var headLoc = registry.getHeadService().getHeadAt(blockLocation);
        if (headLoc == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.NoTargetHeadBlock"));
            return;
        }

        if (registry.getHeadService().isSpawned(headLoc.getUuid())) {
            sender.sendMessage(registry.getLanguageService().message("Messages.SpawnHeadNotEditable"));
            return;
        }

        var applied = HeadUtils.applyTextureToBlock(blockLocation.getBlock(), args[2]) && saveTexture(headLoc, args[2]);

        if (applied) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &aTexture applied!"));
        } else {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cError trying to apply the texture, check logs."));
        }
    }

    private boolean saveTexture(HeadLocation headLoc, String texture) {
        try {
            registry.getStorageService().createOrUpdateHead(headLoc.getUuid(), texture);
            if (headLoc.getContent() == null || headLoc.getContent().kind() == ContentKind.HEAD) {
                headLoc.setContent(HeadContent.head(texture));
                registry.getHeadService().saveHeadInConfig(headLoc);
            }
            return true;
        } catch (InternalException e) {
            LogUtil.error("Error with storage, head new texture not saved: {0}", e.getMessage());
            return false;
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length >= 4 && args.length < 6) {
            var pName = args[2];
            var type = args[3];

            CompletableBukkitFuture.runAsync(HeadBlocks.getInstance(), () -> giveDebugHeads(sender, args, pName, type));
        } else {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cInvalid arguments: give <all|player_name> <all|random|ordered> <numberOfHeads>"));
        }
    }

    private void giveDebugHeads(CommandSender sender, String[] args, String pName, String type) {
        var startTime = System.currentTimeMillis();

        java.util.List<UUID> heads;

        try {
            heads = registry.getStorageService().getHeads();
        } catch (InternalException e) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cError when retrieving heads from storage: &e" + e.getMessage()));
            return;
        }

        var debugPlayers = resolveDebugPlayers(sender, pName);
        if (debugPlayers == null) {
            return;
        }

        var headsToGive = headsToGive(sender, args, pName, type, heads, debugPlayers);
        if (headsToGive == null) {
            return;
        }

        LogUtil.info("");
        LogUtil.info("> Using debug give commands...");
        LogUtil.info("> Param type: {0}", type);
        LogUtil.info("> Param player(s): {0}", pName);
        LogUtil.info("> Real players: {0}", String.join(",", debugPlayers.stream().map(p -> p.uuid().toString()).toList()));
        LogUtil.info("> Start processing...");

        var count = 0;

        for (var playerEntry : headsToGive.entrySet()) {
            if (giveHeads(playerEntry.getKey(), playerEntry.getValue())) {
                count++;
            }
        }

        LogUtil.info("");
        LogUtil.info("> Finish!");

        var stopTime = System.currentTimeMillis();

        sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &aCommand performed in &e" + (stopTime - startTime) + "ms &a! Updated: &e" + count + "&a of &e" + debugPlayers.size() + " players&a."));
    }

    private ArrayList<PlayerProfileLight> resolveDebugPlayers(CommandSender sender, String pName) {
        var debugPlayers = new ArrayList<PlayerProfileLight>();

        if (pName.equals("all")) {
            try {
                var players = registry.getStorageService().getAllPlayers();
                players.forEach(uuid -> debugPlayers.add(new PlayerProfileLight(uuid)));
            } catch (InternalException e) {
                sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cError when retrieving players from storage: &e" + e.getMessage()));
                return null;
            }
        } else {
            try {
                var playerFound = registry.getStorageService().getPlayerByName(pName);
                if (playerFound == null) {
                    sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cPlayer &e" + pName + " &cnot found!"));
                    return null;
                }

                debugPlayers.add(playerFound);
            } catch (InternalException e) {
                sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cError when retrieving player &e" + pName + " &cfrom storage: &e" + e.getMessage()));
                return null;
            }
        }

        return debugPlayers;
    }

    private HashMap<UUID, List<UUID>> headsToGive(CommandSender sender, String[] args, String pName, String type,
                                                  java.util.List<UUID> heads, java.util.List<PlayerProfileLight> debugPlayers) {
        if (type.equals("all")) {
            return allHeadsToGive(sender, pName, heads, debugPlayers);
        }

        if (type.equals("ordered") || type.equals(RANDOM)) {
            var number = requestedHeadCount(sender, args, heads.size());
            if (number == null) {
                return null;
            }

            return pickedHeadsToGive(sender, pName, type.equals(RANDOM), number, heads, debugPlayers);
        }

        LogUtil.error(" Type {0}{1}", type, " &cis not supported! &7&o(all/ordered/random)");
        return null;
    }

    private HashMap<UUID, List<UUID>> allHeadsToGive(CommandSender sender, String pName, java.util.List<UUID> heads,
                                                     java.util.List<PlayerProfileLight> debugPlayers) {
        var headsToGive = new HashMap<UUID, List<UUID>>();
        for (var player : debugPlayers) {
            var toGive = new ArrayList<>(heads);
            removeFoundHeads(sender, pName, player, toGive);
            if (!toGive.isEmpty()) {
                headsToGive.put(player.uuid(), toGive);
            }
        }
        return headsToGive;
    }

    private HashMap<UUID, List<UUID>> pickedHeadsToGive(CommandSender sender, String pName, boolean random, int number,
                                                        java.util.List<UUID> heads, java.util.List<PlayerProfileLight> debugPlayers) {
        var headsToGive = new HashMap<UUID, List<UUID>>();
        var toGive = new ArrayList<>(heads);
        for (var player : debugPlayers) {
            removeFoundHeads(sender, pName, player, toGive);

            var picked = random
                    ? pickRandomUUIDs(toGive, number)
                    : toGive.subList(0, Math.min(number, toGive.size()));
            if (!picked.isEmpty()) {
                headsToGive.put(player.uuid(), picked);
            }
        }
        return headsToGive;
    }

    private void removeFoundHeads(CommandSender sender, String pName, PlayerProfileLight player, ArrayList<UUID> toGive) {
        try {
            toGive.removeAll(registry.getStorageService().getHeadsPlayer(player.uuid()).asFuture().get());
        } catch (Exception e) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + PLAYER_HEADS_ERROR + pName + ERROR_DETAIL_SEPARATOR + e.getMessage()));
            toGive.clear();
        }
    }

    private Integer requestedHeadCount(CommandSender sender, String[] args, int available) {
        if (args.length < 5) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cOrdered or random type require a number of players!"));
            return null;
        }

        int number;
        try {
            number = Integer.parseInt(args[4]);
            if (number < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cNumber &e" + args[4] + " &cis not a number!"));
            return null;
        }

        if (number > available) {
            sender.sendMessage(MessageUtils.colorize(registry.getLanguageService().prefix() + " &cThere are not as many heads as provided!" + " &7&o(Provided: " + number + ", max: " + available + ")"));
            return null;
        }

        return number;
    }

    private boolean giveHeads(UUID playerUuid, List<UUID> headUuids) {
        LogUtil.info("");
        LogUtil.info("> Processing {0}: Giving {1} head(s)...", playerUuid, headUuids.size());

        try {
            for (var entryHead : headUuids) {
                registry.getStorageService().addHead(playerUuid, entryHead);
            }

            LogUtil.info("> Gived!");
            return true;
        } catch (Exception ex) {
            LogUtil.error("> Error saving player found head in storage: {0}", ex.getMessage());
            return false;
        }
    }

    private void handleResync(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ResyncUsage"));
            return;
        }

        var force = args.length >= 4 && args[3].equalsIgnoreCase("--force");

        switch (args[2]) {
            case DATABASE -> handleResyncDatabase(sender, force);
            case "locations" -> handleResyncLocations(sender);
            default -> sender.sendMessage(registry.getLanguageService().message("Messages.ResyncUnknownType"));
        }
    }

    private void handleResyncDatabase(CommandSender sender, boolean force) {
        CompletableBukkitFuture.runAsync(HeadBlocks.getInstance(), () -> resyncDatabase(sender, force));
    }

    private void resyncDatabase(CommandSender sender, boolean force) {
        try {
            if (registry.getStorageService().isStorageError()) {
                sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
                return;
            }

            // MySQL requires --force (user must backup manually)
            if (registry.getConfigService().databaseEnabled() && !force) {
                sender.sendMessage(registry.getLanguageService().message("Messages.ResyncMySQLRequiresForce"));
                return;
            }

            // Check for multi-server setup
            var distinctServerIds = registry.getStorageService().getDistinctServerIds();

            if (distinctServerIds.size() > 1 && !force) {
                sender.sendMessage(registry.getLanguageService().message("Messages.ResyncMultiServerDetected"));
                sender.sendMessage(registry.getLanguageService().message("Messages.ResyncMultiServerCount")
                        .replace(COUNT_PLACEHOLDER, String.valueOf(distinctServerIds.size()))
                        .replace("%serverIds%", String.join(", ", distinctServerIds)));
                sender.sendMessage(registry.getLanguageService().message("Messages.ResyncMultiServerWarningDb"));
                sender.sendMessage(registry.getLanguageService().message("Messages.ResyncOperationCancelled"));
                return;
            }

            sendCurrentServerId(sender);

            // Get heads from database for current server
            var dbHeads = registry.getStorageService().getHeadsByServerId();
            var locationHeadUuids = registry.getHeadService().getHeadLocations().stream()
                    .map(HeadLocation::getUuid)
                    .collect(Collectors.toSet());

            // Find heads in DB that are not in locations.yml
            var headsToRemove = dbHeads.stream()
                    .filter(uuid -> !locationHeadUuids.contains(uuid))
                    .toList();

            if (headsToRemove.isEmpty()) {
                sender.sendMessage(registry.getLanguageService().message("Messages.ResyncDatabaseAlreadyInSync"));
                return;
            }

            sender.sendMessage(registry.getLanguageService().message("Messages.ResyncDatabaseFoundHeads")
                    .replace(COUNT_PLACEHOLDER, String.valueOf(headsToRemove.size())));

            // Backup database before making changes (SQLite only, MySQL users already backed up manually)
            if (!registry.getConfigService().databaseEnabled() && !backupBeforeResync(sender)) {
                return;
            }

            int removed = removeHeadsFromDatabase(headsToRemove);

            sender.sendMessage(registry.getLanguageService().message("Messages.ResyncDatabaseSuccess")
                    .replace(COUNT_PLACEHOLDER, String.valueOf(removed)));

        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ResyncError")
                    .replace("%error%", e.getMessage()));
            LogUtil.error("Resync database error: {0}", e.getMessage());
        }
    }

    private void sendCurrentServerId(CommandSender sender) {
        var currentServerId = registry.getStorageService().getServerIdentifier();
        if (!currentServerId.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ResyncCurrentServerId")
                    .replace("%serverId%", currentServerId));
        }
    }

    private boolean backupBeforeResync(CommandSender sender) {
        var backupResult = registry.getStorageService().backupDatabase("save-resync-");
        if (backupResult != null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ResyncDatabaseBackupSuccess")
                    .replace("%fileName%", backupResult));
            return true;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.ResyncDatabaseBackupError"));
        return false;
    }

    private int removeHeadsFromDatabase(java.util.List<UUID> headsToRemove) {
        int removed = 0;
        for (var headUuid : headsToRemove) {
            try {
                registry.getStorageService().removeHead(headUuid, true);
                removed++;
                LogUtil.info("Resync: Removed head {0} from database", headUuid);
            } catch (InternalException e) {
                LogUtil.error("Resync: Failed to remove head {0}: {1}", headUuid, e.getMessage());
            }
        }
        return removed;
    }

    private void handleResyncLocations(CommandSender sender) {
        // Snapshot: the live list can be mutated while the per-region tasks are being scheduled,
        // which would desynchronize the completion counter from the number of scheduled tasks.
        var headLocations = new ArrayList<>(registry.getHeadService().getHeadLocations());

        if (headLocations.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ListHeadEmpty"));
            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.ResyncLocationsStarting")
                .replace(COUNT_PLACEHOLDER, String.valueOf(headLocations.size())));

        AtomicInteger restored = new AtomicInteger();
        AtomicInteger textureApplied = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger remaining = new AtomicInteger(headLocations.size());

        Runnable reportWhenDone = () -> {
            if (remaining.decrementAndGet() > 0) {
                return;
            }

            reportResync(sender, restored.get(), textureApplied.get(), skipped.get(), failed.get());
        };

        for (var headLocation : headLocations) {
            var location = headLocation.getLocation();
            if (location == null || location.getWorld() == null) {
                failed.incrementAndGet();
                reportWhenDone.run();
                continue;
            }

            HeadBlocks.getScheduler().runTask(location, () -> {
                try {
                    switch (resyncHead(headLocation, location)) {
                        case RESTORED -> restored.incrementAndGet();
                        case TEXTURE_APPLIED -> textureApplied.incrementAndGet();
                        case SKIPPED -> skipped.incrementAndGet();
                        case FAILED -> failed.incrementAndGet();
                    }
                } catch (InternalException e) {
                    LogUtil.error("Resync locations: Error processing head {0}: {1}", headLocation.getUuid(), e.getMessage());
                    failed.incrementAndGet();
                } finally {
                    reportWhenDone.run();
                }
            });
        }
    }

    private enum ResyncOutcome {
        RESTORED, TEXTURE_APPLIED, SKIPPED, FAILED
    }

    private ResyncOutcome resyncHead(HeadLocation headLocation, Location location) throws InternalException {
        var visualService = registry.getVisualService();
        if (visualService.isEntityRendered(headLocation)) {
            visualService.respawn(headLocation);
            return ResyncOutcome.SKIPPED;
        }

        var content = headLocation.getContent();
        if (content != null && content.kind() == ContentKind.BLOCK) {
            return location.getBlock().isEmpty() && visualService.placeBlock(headLocation)
                    ? ResyncOutcome.RESTORED
                    : ResyncOutcome.SKIPPED;
        }

        var block = location.getBlock();
        if (!HeadUtils.isPlayerHead(block)) {
            return visualService.placeBlock(headLocation) ? ResyncOutcome.RESTORED : ResyncOutcome.FAILED;
        }

        var texture = expectedTexture(headLocation, content);
        if (texture == null || texture.isEmpty()) {
            LogUtil.warning("Resync locations: No texture found for head {0}", headLocation.getUuid());
            return ResyncOutcome.FAILED;
        }

        if (texture.equals(HeadUtils.getHeadTexture(block))) {
            return ResyncOutcome.SKIPPED;
        }
        return HeadUtils.applyTextureToBlock(block, texture) ? ResyncOutcome.TEXTURE_APPLIED : ResyncOutcome.FAILED;
    }

    private String expectedTexture(HeadLocation headLocation, HeadContent content) throws InternalException {
        return content != null && content.kind() == ContentKind.HEAD && !content.value().isEmpty()
                ? content.value()
                : registry.getStorageService().getHeadTexture(headLocation.getUuid());
    }

    private void reportResync(CommandSender sender, int restored, int textureApplied, int skipped, int failed) {
        String message = registry.getLanguageService().message("Messages.ResyncLocationsSuccess")
                .replace("%restored%", String.valueOf(restored))
                .replace("%textureApplied%", String.valueOf(textureApplied))
                .replace("%skipped%", String.valueOf(skipped))
                .replace("%failed%", String.valueOf(failed));

        if (sender instanceof Player player) {
            HeadBlocks.getScheduler().runNow(player, () -> player.sendMessage(message));
            return;
        }

        sender.sendMessage(message);
    }

    public static List<UUID> pickRandomUUIDs(List<UUID> uuidList, int numberOfElements) {
        int safeNumberOfElements = Math.min(numberOfElements, uuidList.size());

        return IntStream.range(0, uuidList.size())
                .boxed()
                .toList()
                .stream()
                .map(uuidList::get)
                .collect(Collectors.collectingAndThen(Collectors.toList(), shuffledList -> {
                    Collections.shuffle(shuffledList);
                    return shuffledList.stream().limit(safeNumberOfElements).toList();
                }));
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> new ArrayList<>(Stream.of(TEXTURE, "give", "holograms", RESYNC)
                    .filter(s -> s.startsWith(args[1])).toList());
            case 3 -> {
                var completion = new ArrayList<String>();

                if (TEXTURE.equals(args[1])) {
                    completion.addAll(registry.getConfigService().heads().stream()
                            .filter(s -> s.startsWith("default"))
                            .map(s -> s.replace("default:", "")).toList());
                } else if ("give".equals(args[1])) {
                    completion.add("all");
                    completion.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                } else if (RESYNC.equals(args[1])) {
                    completion.addAll(Stream.of(DATABASE, "locations")
                            .filter(s -> s.startsWith(args[2])).toList());
                }

                yield completion;
            }
            case 4 -> {
                var completion = new ArrayList<String>();

                if (args[1].equals("give") && !args[2].isEmpty()) {
                    completion.addAll(Stream.of("all", "ordered", RANDOM)
                            .filter(s -> s.startsWith(args[3])).toList());
                } else if (args[1].equals(RESYNC) && args[2].equals(DATABASE)) {
                    completion.add("--force");
                }

                yield completion;
            }
            default -> new ArrayList<>();
        };

    }
}
