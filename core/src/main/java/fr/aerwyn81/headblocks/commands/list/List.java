package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.chat.ChatPageUtils;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@HBAnnotations(command = "list", permission = "headblocks.admin", alias = "l")
public class List implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        ArrayList<HeadLocation> headLocations = new ArrayList<>(HeadService.getHeadLocations());

        if (headLocations.isEmpty()) {
            sender.sendMessage(LanguageService.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        ChatPageUtils cpu = new ChatPageUtils(sender)
                .entriesCount(headLocations.size())
                .currentPage(args);

        String message = LanguageService.getMessage("Chat.LineTitle");
        if (sender instanceof Player) {
            TextComponent titleComponent = new TextComponent(message);
            cpu.addTitleLine(titleComponent);
        } else {
            sender.sendMessage(message);
        }

        for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize(); i++) {
            HeadLocation headLocation = headLocations.get(i);

            TextComponent msg = new TextComponent(MessageUtils.colorize((headLocation.isCharged() ? "&6" : "&7| &c&o") + headLocation.getNameOrUuid()));
            TextComponent space = new TextComponent(" ");

            if (headLocation.isCharged()) {
                if (sender instanceof Player) {
                    String hover = LocationUtils.parseLocationPlaceholders(LanguageService.getMessage("Chat.LineCoordinate"), headLocation.getLocation());

                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));

                    TextComponent del = new TextComponent(LanguageService.getMessage("Chat.Box.Remove"));
                    del.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks remove " + headLocation.getUuid()));
                    del.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.Remove"))));

                    TextComponent tp = new TextComponent(LanguageService.getMessage("Chat.Box.Teleport"));

                    if (headLocation.getLocation().getWorld() != null) {
                        tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks tp " + headLocation.getLocation().getWorld().getName() + " " + (headLocation.getLocation().getX() + 0.5) + " " + (headLocation.getLocation().getY() + 1) + " " + (headLocation.getLocation().getZ() + 0.5 + " 0.0 90.0")));
                    }

                    tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.Teleport"))));

                    cpu.addLine(del, space, tp, space, msg, space);
                } else {
                    sender.sendMessage(MessageUtils.colorize("&6" + headLocation.getNameOrUuid()));
                }
            } else {
                if (sender instanceof Player) {
                    String hover = MessageUtils.colorize(LanguageService.getMessage("Chat.LineWorldNotFound")
                            .replaceAll("%world%", headLocation.getConfigWorldName()));

                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
                    cpu.addLine(msg, space);
                } else {
                    sender.sendMessage(MessageUtils.colorize("&c&o" + headLocation.getNameOrUuid()));
                }
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
