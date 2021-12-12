package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.UUID;

@HBAnnotations(command = "list", permission = "headblocks.admin")
public class List implements Cmd {
    private final LanguageHandler languageHandler;
    private final HeadHandler headHandler;

    public List(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
        this.headHandler = main.getHeadHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        ArrayList<Pair<UUID, Location>> headLocations = headHandler.getHeadLocations();

        if (headLocations.size() == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        int pageNumber;
        int pageHeight = 8;
        int totalPage = (headLocations.size() / pageHeight) + (headLocations.size() % pageHeight == 0 ? 0 : 1);

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

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
