package fr.aerwyn81.headblocks.commands;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.placeholders.InternalPlaceholders;
import fr.aerwyn81.headblocks.runnables.ParticlesTask;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.PlayerUtils;
import fr.aerwyn81.headblocks.utils.Version;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class HBCommands implements CommandExecutor {

    private final HeadBlocks main;
    private final ConfigHandler configHandler;
    private final LanguageHandler languageHandler;
    private final HeadHandler headHandler;
    private final StorageHandler storageHandler;

    private final HeadBlocksAPI headBlocksAPI;

    private int totalPage;

    public HBCommands(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();
        this.languageHandler = main.getLanguageHandler();
        this.headHandler = main.getHeadHandler();
        this.storageHandler = main.getStorageHandler();
        this.headBlocksAPI = main.getHeadBlocksAPI();

        totalPage = 1;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            helpCommand(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(languageHandler.getMessage("Messages.PlayerOnly"));
            return true;
        }

        Player player = (Player) sender;

        switch (args[0]) {
            case "give":
                giveCommand(player, args);
                return true;
            case "list":
                listCommand(player, args);
                return true;
            case "reload":
                reloadCommand(player);
                return true;
            case "me":
                meCommand(player);
                return true;
            case "remove":
                removeCommand(player, args);
                return true;
            case "removeall":
                removeAllCommand(player, args);
                return true;
            case "reset":
                resetCommand(player, args);
                return true;
            case "resetall":
                resetAllCommand(player, args);
                return true;
            case "version":
                versionCommand(player);
                return true;
            case "stats":
                statsCommand(player, args);
                return true;
            default:
                helpCommand(player);
                return true;
        }
    }

    private void giveCommand(Player player, String[] args) {
        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        if (args.length == 2) {
            Player pTemp = Bukkit.getPlayer(args[1]);

            if (pTemp == null) {
                player.sendMessage(languageHandler.getMessage("Messages.PlayerNotFound"));
                return;
            }

            player = pTemp;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(languageHandler.getMessage("Messages.InventoryFull"));
            return;
        }

        ItemStack headPlugin = main.getHeadHandler().getPluginHead();
        player.getInventory().addItem(headPlugin);

        player.sendMessage(languageHandler.getMessage("Messages.HeadGiven"));
    }

    private void listCommand(Player player, String[] args) {
        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        List<Pair<UUID, Location>> headLocations = headHandler.getHeadLocations();

        if (headLocations.size() == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return;
        }

        int pageNumber;
        int pageHeight = 8;
        totalPage = (headLocations.size() / pageHeight) + (headLocations.size() % pageHeight == 0 ? 0 : 1);

        if (args.length == 1) {
            pageNumber = 1;
        } else if (NumberUtils.isDigits(args[args.length - 1])) {
            try {
                pageNumber = NumberUtils.createInteger(args[args.length - 1]);
                pageNumber = (totalPage < pageNumber) ? totalPage : Math.max(pageNumber, 1);
            } catch (NumberFormatException exception) {
                pageNumber = 1;
            }
            if (pageNumber <= 0) {
                pageNumber = 1;
            }
        } else {
            pageNumber = 1;
        }

        int firstPos = ((pageNumber - 1) * pageHeight);

        player.sendMessage(languageHandler.getMessage("Chat.LineTitle"));

        for (int i = firstPos; i < firstPos + pageHeight && i < headLocations.size(); i++) {
            UUID uuid = headLocations.get(i).getValue0();
            Location location = headLocations.get(i).getValue1();

            String hover = languageHandler.getMessage("Chat.LineCoordinate")
                    .replaceAll("%worldName%", location.getWorld() != null ? location.getWorld().getName() : FormatUtils.translate("&cUnknownWorld"))
                    .replaceAll("%x%", String.valueOf(location.getBlockX()))
                    .replaceAll("%y%", String.valueOf(location.getBlockY()))
                    .replaceAll("%z%", String.valueOf(location.getBlockZ()));

            TextComponent msg = new TextComponent(FormatUtils.translate("&6" + uuid));
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));

            TextComponent del = new TextComponent(languageHandler.getMessage("Chat.Box.Remove"));
            del.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb remove " + uuid));
            del.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.Remove")).create()));

            TextComponent tp = new TextComponent(languageHandler.getMessage("Chat.Box.Teleport"));
            tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + player.getName() + " " + (location.getX() + 0.5) + " " + (location.getY() + 1) + " " + (location.getZ() + 0.5 + " 0.0 90.0")));
            tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.Teleport")).create()));

            TextComponent space = new TextComponent(" ");
            player.spigot().sendMessage(del, space, tp, space, msg, space);
        }

        if (headLocations.size() > pageHeight) {
            TextComponent c1 = new TextComponent(FormatUtils.translate(languageHandler.getMessage("Chat.PreviousPage")));
            c1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb list " + (pageNumber - 1)));
            c1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.PreviousPage")).create()));

            TextComponent c2 = new TextComponent(FormatUtils.translate(languageHandler.getMessage("Chat.NextPage")));
            c2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb list " + (pageNumber + 1)));
            c2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.NextPage")).create()));

            player.spigot().sendMessage(c1,
                    new TextComponent(languageHandler.getMessage("Chat.PageFooter").replaceAll("%pageNumber%", String.valueOf(pageNumber)).replaceAll("%totalPage%", String.valueOf(totalPage))),
                    c2);
        }
    }

    private void reloadCommand(Player player) {
        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        main.reloadConfig();
        main.getConfigHandler().loadConfiguration();

        main.getLanguageHandler().setLanguage(main.getConfigHandler().getLanguage());
        main.getLanguageHandler().pushMessages();

        main.getHeadHandler().loadConfiguration();
        main.getHeadHandler().loadLocations();

        main.getStorageHandler().getStorage().close();
        main.getStorageHandler().getDatabase().close();

        main.getStorageHandler().initStorage();
        main.getStorageHandler().getStorage().init();

        main.getStorageHandler().openConnection();
        main.getStorageHandler().getDatabase().load();

        Bukkit.getScheduler().cancelTasks(main);
        if (configHandler.isParticlesEnabled()) {
            if (Version.getCurrent().isOlderThan(Version.v1_13)) {
                HeadBlocks.log.sendMessage(FormatUtils.translate("&cParticles is enabled but not supported before 1.13 included."));
            } else {
                main.setParticlesTask(new ParticlesTask(main));
                main.getParticlesTask().runTaskTimer(main, 0, configHandler.getParticlesDelay());
            }
        }

        player.sendMessage(languageHandler.getMessage("Messages.ReloadComplete"));
    }

    private void meCommand(Player player) {
        if (!PlayerUtils.hasPermission(player, "headblocks.use")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        int max = headBlocksAPI.getTotalHeadSpawnCount();
        if (max == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return;
        }

        int current = headBlocksAPI.getPlayerHeads(player.getUniqueId()).size();

        int bars = configHandler.getProgressBarBars();
        String symbol = configHandler.getProgressBarSymbol();
        String completedColor = configHandler.getProgressBarCompletedColor();
        String notCompletedColor = configHandler.getProgressBarNotCompletedColor();

        String progressBar = FormatUtils.createProgressBar(current, max, bars, symbol, completedColor, notCompletedColor);

        List<String> messages = languageHandler.getMessages("Messages.MeCommand");
        if (messages.size() != 0) {
            languageHandler.getMessages("Messages.MeCommand").forEach(msg -> player.sendMessage(InternalPlaceholders
                    .parse(player, msg.replaceAll("%progress%", progressBar))));
        }
    }

    private void removeCommand(Player player, String[] args) {
        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        if (args.length != 2) {
            player.sendMessage(languageHandler.getMessage("Messages.ErrorCommand"));
            return;
        }

        Pair<UUID, Location> head = headHandler.getHeadByUUID(UUID.fromString(args[1]));
        if (head == null) {
            player.sendMessage(languageHandler.getMessage("Messages.RemoveLocationError"));
            return;
        }

        if (configHandler.shouldResetPlayerData()) {
            storageHandler.removeHead(head.getValue0());
        }

        headHandler.removeHead(head.getValue0());

        Location loc = head.getValue1();
        player.sendMessage(languageHandler.getMessage("Messages.HeadRemoved")
                .replaceAll("%world%", loc.getWorld() != null ? loc.getWorld().getName() : FormatUtils.translate("&cUnknownWorld"))
                .replaceAll("%x%", String.valueOf(loc.getBlockX()))
                .replaceAll("%y%", String.valueOf(loc.getBlockY()))
                .replaceAll("%z%", String.valueOf(loc.getBlockZ())));
    }

    private void removeAllCommand(Player player, String[] args) {
        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        List<Pair<UUID, Location>> headLocations = headHandler.getHeadLocations();
        int headCount = headLocations.size();

        if (headLocations.size() == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return;
        }

        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        if (hasConfirmInCommand) {
            new ArrayList<>(headLocations).forEach(head -> {
                if (configHandler.shouldResetPlayerData()) {
                    storageHandler.removeHead(head.getValue0());
                }

                headHandler.removeHead(head.getValue0());
            });

            player.sendMessage(languageHandler.getMessage("Messages.RemoveAllSuccess")
                    .replaceAll("%headCount%", String.valueOf(headCount)));
            return;
        }

        player.sendMessage(languageHandler.getMessage("Messages.RemoveAllConfirm")
                .replaceAll("%headCount%", String.valueOf(headCount)));
    }

    private void resetCommand(Player player, String[] args) {
        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        if (args.length != 2) {
            player.sendMessage(languageHandler.getMessage("Messages.ErrorCommand"));
            return;
        }

        Player pTemp = Bukkit.getOfflinePlayer(args[1]).getPlayer();

        if (pTemp == null) {
            player.sendMessage(languageHandler.getMessage("Messages.PlayerNotFound"));
            return;
        }

        if (!storageHandler.containsPlayer(pTemp.getUniqueId())) {
            player.sendMessage(languageHandler.getMessage("Messages.NoHeadFound"));
            return;
        }

        storageHandler.resetPlayer(pTemp.getUniqueId());
        player.sendMessage(languageHandler.getMessage("Messages.PlayerReset")
                .replaceAll("%player%", pTemp.getName()));
    }

    private void resetAllCommand(Player player, String[] args) {
        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        List<UUID> allPlayers = storageHandler.getAllPlayers();

        if (allPlayers.size() == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ResetAllNoData"));
            return;
        }

        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        if (hasConfirmInCommand) {
            allPlayers.forEach(storageHandler::resetPlayer);
            player.sendMessage(languageHandler.getMessage("Messages.ResetAllSuccess")
                    .replaceAll("%playerCount%", String.valueOf(allPlayers.size())));
            return;
        }

        player.sendMessage(languageHandler.getMessage("Messages.ResetAllConfirm")
                .replaceAll("%playerCount%", String.valueOf(allPlayers.size())));
    }

    private void helpCommand(CommandSender sender) {
        sender.sendMessage(languageHandler.getMessage("Help.LineTop"));
        sender.sendMessage(languageHandler.getMessage("Help.Help"));

        if (PlayerUtils.hasPermission(sender, "headblocks.admin")) {
            sender.sendMessage(languageHandler.getMessage("Help.Give"));
            sender.sendMessage(languageHandler.getMessage("Help.Remove"));
            sender.sendMessage(languageHandler.getMessage("Help.RemoveAll"));
            sender.sendMessage(languageHandler.getMessage("Help.List"));
            sender.sendMessage(languageHandler.getMessage("Help.Stats"));
            sender.sendMessage(languageHandler.getMessage("Help.Reset"));
            sender.sendMessage(languageHandler.getMessage("Help.ResetAll"));
            sender.sendMessage(languageHandler.getMessage("Help.Reload"));
        }

        if (PlayerUtils.hasPermission(sender, "headblocks.use")) {
            sender.sendMessage(languageHandler.getMessage("Help.Me"));
        }

        sender.sendMessage(languageHandler.getMessage("Help.Version"));
    }

    private void versionCommand(CommandSender sender) {
        sender.sendMessage(languageHandler.getMessage("Messages.Version")
                .replaceAll("%version%", main.getDescription().getVersion()));
    }

    private void statsCommand(Player player, String[] args) {
        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            player.sendMessage(languageHandler.getMessage("Messages.NoPermission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(languageHandler.getMessage("Messages.ErrorCommand"));
            return;
        }

        Player pTemp = Bukkit.getOfflinePlayer(args[1]).getPlayer();

        List<Pair<UUID, Location>> playerHeads = new ArrayList<>();
        if (pTemp != null) {
            playerHeads = storageHandler.getHeadsPlayer(pTemp.getUniqueId())
                    .stream().map(headHandler::getHeadByUUID).filter(Objects::nonNull).collect(Collectors.toList());
        }

        List<Pair<UUID, Location>> headsSpawned = headHandler.getHeadLocations();
        if (headsSpawned.size() == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return;
        }

        headsSpawned.sort(Comparator.comparingInt(playerHeads::indexOf));

        int pageNumber;
        int pageHeight = 8;
        totalPage = (headsSpawned.size() / pageHeight) + (headsSpawned.size() % pageHeight == 0 ? 0 : 1);

        if (args.length == 2) {
            pageNumber = 1;
        } else if (NumberUtils.isDigits(args[args.length - 1])) {
            try {
                pageNumber = NumberUtils.createInteger(args[args.length - 1]);
                pageNumber = (totalPage < pageNumber) ? totalPage : Math.max(pageNumber, 1);
            } catch (NumberFormatException exception) {
                pageNumber = 1;
            }
            if (pageNumber <= 0) {
                pageNumber = 1;
            }
        } else {
            pageNumber = 1;
        }

        int firstPos = ((pageNumber - 1) * pageHeight);

        player.sendMessage(languageHandler.getMessage("Chat.StatsTitleLine")
                .replaceAll("%player%", pTemp.getName())
                .replaceAll("%headCount%", String.valueOf(playerHeads.size()))
                .replaceAll("%maxHead%", String.valueOf(headBlocksAPI.getTotalHeadSpawnCount())));

        for (int i = firstPos; i < firstPos + pageHeight && i < headsSpawned.size(); i++) {
            UUID uuid = headsSpawned.get(i).getValue0();
            Location location = headsSpawned.get(i).getValue1();

            String hover = languageHandler.getMessage("Chat.LineCoordinate")
                    .replaceAll("%worldName%", location.getWorld() != null ? location.getWorld().getName() : FormatUtils.translate("&cUnknownWorld"))
                    .replaceAll("%x%", String.valueOf(location.getBlockX()))
                    .replaceAll("%y%", String.valueOf(location.getBlockY()))
                    .replaceAll("%z%", String.valueOf(location.getBlockZ()));

            TextComponent msg = new TextComponent(FormatUtils.translate("&6" + uuid));
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));

            TextComponent own;
            if (playerHeads.stream().anyMatch(s -> s.getValue0() == uuid)) {
                own = new TextComponent(languageHandler.getMessage("Chat.Box.Own"));
                own.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.Own")).create()));
            } else {
                own = new TextComponent(languageHandler.getMessage("Chat.Box.NotOwn"));
                own.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.NotOwn")).create()));
            }

            TextComponent tp = new TextComponent(languageHandler.getMessage("Chat.Box.Teleport"));
            tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + player.getName() + " " + (location.getX() + 0.5) + " " + (location.getY() + 1) + " " + (location.getZ() + 0.5 + " 0.0 90.0")));
            tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.Teleport")).create()));

            TextComponent space = new TextComponent(" ");
            player.spigot().sendMessage(own, space, tp, space, msg, space);
        }

        if (headsSpawned.size() > pageHeight) {
            TextComponent c1 = new TextComponent(FormatUtils.translate(languageHandler.getMessage("Chat.PreviousPage")));
            c1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb stats " + pTemp.getName() + " " + (pageNumber - 1)));
            c1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.PreviousPage")).create()));

            TextComponent c2 = new TextComponent(FormatUtils.translate(languageHandler.getMessage("Chat.NextPage")));
            c2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb stats " + pTemp.getName() + " " + (pageNumber + 1)));
            c2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.NextPage")).create()));

            player.spigot().sendMessage(c1, new TextComponent(languageHandler.getMessage("Chat.PageFooter").replaceAll("%pageNumber%", String.valueOf(pageNumber)).replaceAll("%totalPage%", String.valueOf(totalPage))), c2);
        }
    }
}
