package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.placeholders.InternalPlaceholders;
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
        if (sender instanceof ConsoleCommandSender) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cThis command cannot be performed by console without player in argument."));
            return true;
        }

        Player player;

        if (args.length >= 2 && !NumberUtils.isDigits(args[1])) {
            player = Bukkit.getOfflinePlayer(args[1]).getPlayer();
        } else {
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

        int pageNumber;
        int pageHeight = 8;
        int totalPage = (headsSpawned.size() / pageHeight) + (headsSpawned.size() % pageHeight == 0 ? 0 : 1);

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

        sender.sendMessage(InternalPlaceholders.parse(player, languageHandler.getMessage("Chat.StatsTitleLine")
                        .replaceAll("%headCount%", String.valueOf(playerHeads.size()))));

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
            tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + sender.getName() + " " + (location.getX() + 0.5) + " " + (location.getY() + 1) + " " + (location.getZ() + 0.5 + " 0.0 90.0")));
            tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.Teleport")).create()));

            TextComponent space = new TextComponent(" ");
            sender.spigot().sendMessage(own, space, tp, space, msg, space);
        }

        if (headsSpawned.size() > pageHeight) {
            TextComponent c1 = new TextComponent(FormatUtils.translate(languageHandler.getMessage("Chat.PreviousPage")));
            c1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks stats " + player.getName() + " " + (pageNumber - 1)));
            c1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.PreviousPage")).create()));

            TextComponent c2 = new TextComponent(FormatUtils.translate(languageHandler.getMessage("Chat.NextPage")));
            c2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks stats " + player.getName() + " " + (pageNumber + 1)));
            c2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.NextPage")).create()));

            sender.spigot().sendMessage(c1, new TextComponent(languageHandler.getMessage("Chat.PageFooter").replaceAll("%pageNumber%", String.valueOf(pageNumber)).replaceAll("%totalPage%", String.valueOf(totalPage))), c2);
        }

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
    }
}
