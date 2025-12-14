package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.utils.runnables.CompletableBukkitFuture;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@HBAnnotations(command = "debug", permission = "headblocks.debug", isVisible = false)
public class Debug implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        switch (args[1]) {
            case "texture" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(LanguageService.getMessage("Messages.PlayerOnly"));
                    return true;
                }

                var blockView = ((Player) sender).getTargetBlock(null, 50);
                var blockLocation = blockView.getLocation();
                var block = blockLocation.getBlock();

                var tempBlock = block.getLocation().clone().add(0, 1, 0).getBlock();
                if (!tempBlock.isEmpty() && !HeadUtils.isPlayerHead(block)) {
                    sender.sendMessage("Block at " + blockLocation.toVector() + " is not empty: " + block.getType());
                    return true;
                }

                if (!HeadUtils.isPlayerHead(blockView)) {
                    sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cBlock is not a player head!"));
                    return true;
                }

                if (args.length < 3 || args[2].isEmpty()) {
                    sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cTexture cannot be empty!"));
                    return true;
                }

                var headLoc = HeadService.getHeadAt(blockLocation);
                if (headLoc == null) {
                    sender.sendMessage(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
                    return true;
                }

                var applied = HeadUtils.applyTextureToBlock(blockLocation.getBlock(), args[2]);

                if (applied) {
                    try {
                        StorageService.createOrUpdateHead(headLoc.getUuid(), args[2]);
                    } catch (InternalException e) {
                        LogUtil.error("Error with storage, head new texture not saved: {0}", e.getMessage());
                        applied = false;
                    }
                }

                if (applied) {
                    sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &aTexture applied!"));
                } else {
                    sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cError trying to apply the texture, check logs."));
                }
                return true;
            }
            case "give" -> {
                if (args.length >= 4 && args.length < 6) {
                    var pName = args[2];
                    var type = args[3];

                    CompletableBukkitFuture.runAsync(HeadBlocks.getInstance(), () ->
                    {
                        var startTime = System.currentTimeMillis();

                        ArrayList<UUID> heads;

                        try {
                            heads = StorageService.getHeads();
                        } catch (InternalException e) {
                            sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cError when retrieving heads from storage: &e" + e.getMessage()));
                            return;
                        }

                        var debugPlayers = new ArrayList<PlayerProfileLight>();

                        if (pName.equals("all")) {
                            try {
                                var players = StorageService.getAllPlayers();
                                players.forEach(uuid -> debugPlayers.add(new PlayerProfileLight(uuid)));
                            } catch (InternalException e) {
                                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cError when retrieving players from storage: &e" + e.getMessage()));
                                return;
                            }
                        } else {
                            try {
                                var playerFound = StorageService.getPlayerByName(pName);
                                if (playerFound == null) {
                                    sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cPlayer &e" + pName + " &cnot found!"));
                                    return;
                                }

                                debugPlayers.add(playerFound);
                            } catch (InternalException e) {
                                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cError when retrieving player &e" + pName + " &cfrom storage: &e" + e.getMessage()));
                                return;
                            }
                        }

                        var headsToGive = new HashMap<UUID, List<UUID>>();

                        if (type.equals("all")) {
                            for (var player : debugPlayers) {
                                var toGive = new ArrayList<>(heads);
                                try {
                                    toGive.removeAll(StorageService.getHeadsPlayer(player.uuid()).asFuture().get());
                                } catch (Exception e) {
                                    sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cError when retrieving heads for player &e" + pName + "&c: &e" + e.getMessage()));
                                    toGive.clear();
                                }
                                if (!toGive.isEmpty()) {
                                    headsToGive.put(player.uuid(), toGive);
                                }
                            }

                        } else if (type.equals("ordered") || type.equals("random")) {
                            if (args.length < 5) {
                                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cOrdered or random type require a number of players!"));
                                return;
                            }

                            int number;
                            try {
                                number = Integer.parseInt(args[4]);
                                if (number < 1) {
                                    throw new NumberFormatException();
                                }
                            } catch (NumberFormatException e) {
                                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cNumber &e" + args[4] + " &cis not a number!"));
                                return;
                            }

                            if (number > heads.size()) {
                                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cThere are not as many heads as provided!" + " &7&o(Provided: " + number + ", max: " + heads.size() + ")"));
                                return;
                            }

                            var toGive = new ArrayList<>(heads);

                            if (type.equals("random")) {
                                for (var player : debugPlayers) {
                                    try {
                                        toGive.removeAll(StorageService.getHeadsPlayer(player.uuid()).asFuture().get());
                                    } catch (Exception e) {
                                        sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cError when retrieving heads for player &e" + pName + "&c: &e" + e.getMessage()));
                                        toGive.clear();
                                    }

                                    var pickedHeads = pickRandomUUIDs(toGive, number);

                                    if (!pickedHeads.isEmpty()) {
                                        headsToGive.put(player.uuid(), pickedHeads);
                                    }
                                }
                            } else {
                                for (var player : debugPlayers) {
                                    try {
                                        toGive.removeAll(StorageService.getHeadsPlayer(player.uuid()).asFuture().get());
                                    } catch (Exception e) {
                                        sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cError when retrieving heads for player &e" + pName + "&c: &e" + e.getMessage()));
                                        toGive.clear();
                                    }

                                    var orderedGive = toGive.subList(0, Math.min(number, toGive.size()));
                                    if (!orderedGive.isEmpty()) {
                                        headsToGive.put(player.uuid(), orderedGive);
                                    }
                                }
                            }
                        } else {
                            LogUtil.error(" Type {0}{1}", type, " &cis not supported! &7&o(all/ordered/random)");
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
                            LogUtil.info("");
                            LogUtil.info("> Processing {0}: Giving {1} head(s)...", playerEntry.getKey(), playerEntry.getValue().size());

                            try {
                                for (var entryHead : playerEntry.getValue()) {
                                    StorageService.addHead(playerEntry.getKey(), entryHead);
                                }

                                LogUtil.info("> Gived!");
                            } catch (Exception ex) {
                                LogUtil.error("> Error saving player found head in storage: {0}", ex.getMessage());
                                continue;
                            }

                            count++;
                        }

                        LogUtil.info("");
                        LogUtil.info("> Finish!");

                        var stopTime = System.currentTimeMillis();

                        sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &aCommand performed in &e" + (stopTime - startTime) + "ms &a! Updated: &e" + count + "&a of &e" + debugPlayers.size() + " players&a."));
                    });
                } else {
                    sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cInvalid arguments: give <all|player_name> <all|random|ordered> <numberOfHeads>"));
                }

                return true;
            }
            case "holograms" -> {
                HologramService.unload();
                HologramService.load();

                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &aHolograms reloaded!"));
                return true;
            }
            case "resync" -> {
                if (args.length < 3) {
                    sender.sendMessage(LanguageService.getMessage("Messages.ResyncUsage"));
                    return true;
                }

                var force = args.length >= 4 && args[3].equalsIgnoreCase("--force");

                switch (args[2]) {
                    case "database" -> {
                        return handleResyncDatabase(sender, force);
                    }
                    case "locations" -> {
                        handleResyncLocations(sender);
                        return true;
                    }
                    default -> {
                        sender.sendMessage(LanguageService.getMessage("Messages.ResyncUnknownType"));
                        return true;
                    }
                }
            }
        }

        sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cUnknown debug command!"));
        return true;
    }

    private boolean handleResyncDatabase(CommandSender sender, boolean force) {
        CompletableBukkitFuture.runAsync(HeadBlocks.getInstance(), () -> {
            try {
                if (StorageService.hasStorageError()) {
                    sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
                    return;
                }

                // MySQL requires --force (user must backup manually)
                if (ConfigService.isDatabaseEnabled() && !force) {
                    sender.sendMessage(LanguageService.getMessage("Messages.ResyncMySQLRequiresForce"));
                    return;
                }

                // Check for multi-server setup
                var distinctServerIds = StorageService.getDistinctServerIds();

                if (distinctServerIds.size() > 1 && !force) {
                    sender.sendMessage(LanguageService.getMessage("Messages.ResyncMultiServerDetected"));
                    sender.sendMessage(LanguageService.getMessage("Messages.ResyncMultiServerCount")
                            .replaceAll("%count%", String.valueOf(distinctServerIds.size()))
                            .replaceAll("%serverIds%", String.join(", ", distinctServerIds)));
                    sender.sendMessage(LanguageService.getMessage("Messages.ResyncMultiServerWarningDb"));
                    sender.sendMessage(LanguageService.getMessage("Messages.ResyncOperationCancelled"));
                    return;
                }

                var currentServerId = StorageService.getServerIdentifier();
                if (!currentServerId.isEmpty()) {
                    sender.sendMessage(LanguageService.getMessage("Messages.ResyncCurrentServerId")
                            .replaceAll("%serverId%", currentServerId));
                }

                // Get heads from database for current server
                var dbHeads = StorageService.getHeadsByServerId();
                var locationHeadUuids = HeadService.getHeadLocations().stream()
                        .map(HeadLocation::getUuid)
                        .collect(Collectors.toSet());

                // Find heads in DB that are not in locations.yml
                var headsToRemove = dbHeads.stream()
                        .filter(uuid -> !locationHeadUuids.contains(uuid))
                        .toList();

                if (headsToRemove.isEmpty()) {
                    sender.sendMessage(LanguageService.getMessage("Messages.ResyncDatabaseAlreadyInSync"));
                    return;
                }

                sender.sendMessage(LanguageService.getMessage("Messages.ResyncDatabaseFoundHeads")
                        .replaceAll("%count%", String.valueOf(headsToRemove.size())));

                // Backup database before making changes (SQLite only, MySQL users already backed up manually)
                if (!ConfigService.isDatabaseEnabled()) {
                    var backupResult = StorageService.backupDatabase("save-resync-");
                    if (backupResult != null) {
                        sender.sendMessage(LanguageService.getMessage("Messages.ResyncDatabaseBackupSuccess")
                                .replaceAll("%fileName%", backupResult));
                    } else {
                        sender.sendMessage(LanguageService.getMessage("Messages.ResyncDatabaseBackupError"));
                        return;
                    }
                }

                int removed = 0;
                for (var headUuid : headsToRemove) {
                    try {
                        StorageService.removeHead(headUuid, true);
                        removed++;
                        LogUtil.info("Resync: Removed head {0} from database", headUuid);
                    } catch (InternalException e) {
                        LogUtil.error("Resync: Failed to remove head {0}: {1}", headUuid, e.getMessage());
                    }
                }

                sender.sendMessage(LanguageService.getMessage("Messages.ResyncDatabaseSuccess")
                        .replaceAll("%count%", String.valueOf(removed)));

            } catch (InternalException e) {
                sender.sendMessage(LanguageService.getMessage("Messages.ResyncError")
                        .replaceAll("%error%", e.getMessage()));
                LogUtil.error("Resync database error: {0}", e.getMessage());
            }
        });

        return true;
    }

    private void handleResyncLocations(CommandSender sender) {
        var headLocations = HeadService.getHeadLocations();

        if (headLocations.isEmpty()) {
            sender.sendMessage(LanguageService.getMessage("Messages.ListHeadEmpty"));
            return;
        }

        sender.sendMessage(LanguageService.getMessage("Messages.ResyncLocationsStarting")
                .replaceAll("%count%", String.valueOf(headLocations.size())));

        // Run on main thread since we're modifying blocks
        Bukkit.getScheduler().runTask(HeadBlocks.getInstance(), () -> {
            int restored = 0;
            int textureApplied = 0;
            int skipped = 0;
            int failed = 0;

            for (var headLocation : headLocations) {
                var location = headLocation.getLocation();
                if (location == null || location.getWorld() == null) {
                    failed++;
                    continue;
                }

                try {
                    var texture = StorageService.getHeadTexture(headLocation.getUuid());
                    if (texture == null || texture.isEmpty()) {
                        LogUtil.warning("Resync locations: No texture found for head {0}", headLocation.getUuid());
                        failed++;
                        continue;
                    }

                    var block = location.getBlock();

                    if (HeadUtils.isPlayerHead(block)) {
                        // Block is already a head, check if texture matches
                        var currentTexture = HeadUtils.getHeadTexture(block);
                        if (texture.equals(currentTexture)) {
                            skipped++;
                            continue;
                        }

                        if (HeadUtils.applyTextureToBlock(block, texture)) {
                            textureApplied++;
                        } else {
                            failed++;
                        }
                    } else {
                        // Block is not a head, create it
                        block.setType(Material.PLAYER_HEAD);
                        if (HeadUtils.applyTextureToBlock(block, texture)) {
                            restored++;
                        } else {
                            failed++;
                        }
                    }
                } catch (InternalException e) {
                    LogUtil.error("Resync locations: Error processing head {0}: {1}", headLocation.getUuid(), e.getMessage());
                    failed++;
                }
            }

            sender.sendMessage(LanguageService.getMessage("Messages.ResyncLocationsSuccess")
                    .replaceAll("%restored%", String.valueOf(restored))
                    .replaceAll("%textureApplied%", String.valueOf(textureApplied))
                    .replaceAll("%skipped%", String.valueOf(skipped))
                    .replaceAll("%failed%", String.valueOf(failed)));
        });
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
                    return shuffledList.stream().limit(safeNumberOfElements).collect(Collectors.toList());
                }));
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 2 -> new ArrayList<>(Stream.of("texture", "give", "holograms", "resync")
                    .filter(s -> s.startsWith(args[1])).collect(Collectors.toList()));
            case 3 -> {
                var completion = new ArrayList<String>();

                switch (args[1]) {
                    case "texture" -> completion.addAll(ConfigService.getHeads().stream()
                            .filter(s -> s.startsWith("default"))
                            .map(s -> s.replace("default:", "")).toList());
                    case "give" -> {
                        completion.add("all");
                        completion.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                    }
                    case "resync" -> completion.addAll(Stream.of("database", "locations")
                            .filter(s -> s.startsWith(args[2])).toList());
                }

                yield completion;
            }
            case 4 -> {
                var completion = new ArrayList<String>();

                if (args[1].equals("give") && !args[2].isEmpty()) {
                    completion.addAll(Stream.of("all", "ordered", "random")
                            .filter(s -> s.startsWith(args[3])).toList());
                } else if (args[1].equals("resync") && args[2].equals("database")) {
                    completion.add("--force");
                }

                yield completion;
            }
            default -> new ArrayList<>();
        };

    }
}
