package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.ChatPageUtils;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
        ArrayList<Pair<UUID, Location>> headLocations = headHandler.getHeadLocations();

        if (headLocations.size() == 0) {
            sender.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        ChatPageUtils cpu = new ChatPageUtils(languageHandler, sender)
                .entriesCount(headLocations.size())
                .currentPage(args);

        String message = languageHandler.getMessage("Chat.LineTitle");
        if (sender instanceof Player) {
            TextComponent titleComponent = new TextComponent(message);
            cpu.addTitleLine(titleComponent);
        } else {
            sender.sendMessage(message);
        }

        for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize(); i++) {
            UUID uuid = headLocations.get(i).getValue0();
            Location location = headLocations.get(i).getValue1();

            String hover = languageHandler.getMessage("Chat.LineCoordinate")
                    .replaceAll("%worldName%", location.getWorld() != null ? location.getWorld().getName() : FormatUtils.translate("&cUnknownWorld"))
                    .replaceAll("%x%", String.valueOf(location.getBlockX()))
                    .replaceAll("%y%", String.valueOf(location.getBlockY()))
                    .replaceAll("%z%", String.valueOf(location.getBlockZ()));

            TextComponent msg = new TextComponent(FormatUtils.translate("&6" + uuid));
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));

            if (sender instanceof Player) {
                Player player = (Player) sender;

                TextComponent del = new TextComponent(languageHandler.getMessage("Chat.Box.Remove"));
                del.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb remove " + uuid));
                del.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.Remove")).create()));

                TextComponent tp = new TextComponent(languageHandler.getMessage("Chat.Box.Teleport"));
                tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + player.getName() + " " + (location.getX() + 0.5) + " " + (location.getY() + 1) + " " + (location.getZ() + 0.5 + " 0.0 90.0")));
                tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(languageHandler.getMessage("Chat.Hover.Teleport")).create()));

                TextComponent space = new TextComponent(" ");

                cpu.addLine(del, space, tp, space, msg, space);
            } else {
                sender.sendMessage(FormatUtils.translate("&6" + uuid));
            }
        }

        cpu.addPageLine("list");
        cpu.build();
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
