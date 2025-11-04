package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.utils.runnables.CompletableBukkitFuture;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@HBAnnotations(command = "debug", permission = "headblocks.debug", isPlayerCommand = true, isVisible = false, args = {"texture", "give", "holograms"})
public class Debug implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args[1].equals("texture")) {
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

            if (args.length < 3 || args[2].isEmpty())
            {
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
                    HeadBlocks.log.sendMessage("&cError with storage, head new texture not saved: " + e.getMessage());
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

        if (args[1].equals("give") && args.length >= 4 && args.length < 6) {
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
                    HeadBlocks.log.sendMessage(MessageUtils.colorize(" &cType &e" + type + " &cis not supported! &7&o(all/ordered/random)"));
                    return;
                }

                HeadBlocks.log.sendMessage(MessageUtils.colorize(""));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&6> Using debug give commands..."));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&6> Param type: &7" + type));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&6> Param player(s): &7" + pName));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&6> Real players: &7" + String.join(",", debugPlayers.stream().map(p -> p.uuid().toString()).toList())));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&6> Start processing..."));

                var count = 0;

                for (var playerEntry : headsToGive.entrySet()) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize(""));
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&6> Processing &e" + playerEntry.getKey() + "&6... Giving &e" + playerEntry.getValue().size() + " &6heads..."));

                    try {
                        for (var entryHead : playerEntry.getValue()) {
                            StorageService.addHead(playerEntry.getKey(), entryHead);
                        }

                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&a> Gived!"));
                    } catch (Exception ex) {
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&c> Error saving player found head in storage: " + ex.getMessage()));
                        continue;
                    }

                    count++;
                }

                HeadBlocks.log.sendMessage(MessageUtils.colorize(""));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&6> Finish!"));

                var stopTime = System.currentTimeMillis();

                sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &aCommand performed in &e" + (stopTime - startTime) + "ms &a! Updated: &e" + count + "&a of &e" + debugPlayers.size() + " players&a."));
            });

            return true;
        }

        if (args[1].equals("holograms")) {
            HologramService.unload();
            HologramService.load();

            sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &aHolograms reloaded!"));
            return true;
        }

        sender.sendMessage(MessageUtils.colorize(LanguageService.getPrefix() + " &cUnknown debug command!"));

        return true;
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
            case 2 -> new ArrayList<>(List.of("texture", "give", "holograms"));
            case 3 -> {
                var completion = new ArrayList<String>();

                if (args[1].equals("texture")) {
                    completion.addAll(ConfigService.getHeads().stream()
                            .filter(s -> s.startsWith("default"))
                            .map(s -> s.replace("default:", "")).toList());
                } else if (args[1].equals("give")) {
                    completion.add("all");
                    completion.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                }

                yield completion;
            }
            case 4 -> {
                var completion = new ArrayList<String>();

                if (!args[2].isEmpty()) {
                    completion.addAll(List.of("all", "ordered", "random"));
                }

                yield completion;
            }
            default -> new ArrayList<>();
        };

    }
}
