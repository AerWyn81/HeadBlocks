package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.placeholders.InternalPlaceholders;
import fr.aerwyn81.headblocks.utils.ChatPageUtils;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@HBAnnotations(command = "stats", permission = "headblocks.admin")
public class Stats implements Cmd {
    private final LanguageHandler languageHandler;
    private final HeadHandler headHandler;
    private final StorageHandler storageHandler;

    public Stats(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
        this.headHandler = main.getHeadHandler();
        this.storageHandler = main.getStorageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player;

        if (args.length >= 2 && !NumberUtils.isDigits(args[1])) {
            player = Bukkit.getOfflinePlayer(args[1]).getPlayer();
        } else {
            if (sender instanceof ConsoleCommandSender) {
                HeadBlocks.log.sendMessage(FormatUtils.translate("&cThis command cannot be performed by console without player in argument."));
                return true;
            }

            player = (Player) sender;
        }

        if (player == null) {
            sender.sendMessage(languageHandler.getMessage("Messages.PlayerNotFound"));
            return true;
        }

        ArrayList<Pair<UUID, Location>> playerHeads = storageHandler.getHeadsPlayer(player.getUniqueId()).stream()
                .map(headHandler::getHeadByUUID)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        ArrayList<Pair<UUID, Location>> headsSpawned = headHandler.getHeadLocations();
        if (headsSpawned.size() == 0) {
            sender.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        headsSpawned.sort(Comparator.comparingInt(playerHeads::indexOf));

        ChatPageUtils cpu = new ChatPageUtils(languageHandler, sender)
                .entriesCount(headsSpawned.size())
                .currentPage(args);

        String message = languageHandler.getMessage("Chat.LineTitle");
        if (sender instanceof Player) {
            TextComponent titleComponent = new TextComponent(InternalPlaceholders.parse(player, languageHandler.getMessage("Chat.StatsTitleLine")
                    .replaceAll("%headCount%", String.valueOf(playerHeads.size()))));
            cpu.addTitleLine(titleComponent);
        } else {
            sender.sendMessage(message);
        }

        for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize(); i++) {
            UUID uuid = headsSpawned.get(i).getValue0();
            Location location = headsSpawned.get(i).getValue1();

            String hover = languageHandler.getMessage("Chat.LineCoordinate")
                    .replaceAll("%worldName%", location.getWorld() != null ? location.getWorld().getName() : FormatUtils.translate("&cUnknownWorld"))
                    .replaceAll("%x%", String.valueOf(location.getBlockX()))
                    .replaceAll("%y%", String.valueOf(location.getBlockY()))
                    .replaceAll("%z%", String.valueOf(location.getBlockZ()));

            if (sender instanceof Player) {
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
                tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + sender.getName() + " " + (location.getX() + 0.5) + " " + (location.getY() + 1) + " " + (location.getZ() + 0.5 + " 0.0 90.0")));
                tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.Teleport")).create()));

                TextComponent space = new TextComponent(" ");
                cpu.addLine(own, space, tp, space, msg, space);
            } else {
                sender.sendMessage((playerHeads.stream().anyMatch(s -> s.getValue0() == uuid) ?
                                languageHandler.getMessage("Chat.Box.Own") : languageHandler.getMessage("Chat.Box.NotOwn")) + " " +
                                FormatUtils.translate("&6" + uuid));
            }
        }

        cpu.addPageLine("stats");
        cpu.build();
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
    }
}
