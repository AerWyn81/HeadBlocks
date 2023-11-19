package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@HBAnnotations(command = "info", permission = "headblocks.admin", isPlayerCommand = true, alias = "i")
public class Info implements Cmd {
    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var player = (Player) sender;

        Location targetLoc = player.getTargetBlock(null, 100).getLocation();

        HeadLocation headLocation = HeadService.getHeadAt(targetLoc);

        if (headLocation == null) {
            player.sendMessage(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
            return true;
        }

        player.sendMessage(MessageUtils.colorize("&7----------- [ &e&oTarget head information &7]-----------"));
        player.sendMessage("");

        TextComponent msgName = new TextComponent(LanguageService.getMessage("Chat.Info.Name") + headLocation.getDisplayedName());
        msgName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Info.HoverCopyName"))));
        msgName.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, headLocation.getName()));
        player.spigot().sendMessage(msgName);

        TextComponent msgUuid = new TextComponent(LanguageService.getMessage("Chat.Info.Uuid") + headLocation.getUuid());
        msgUuid.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Info.HoverCopyUuid"))));
        msgUuid.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, headLocation.getUuid().toString()));
        player.spigot().sendMessage(msgUuid);

        TextComponent msgLoc = new TextComponent(LanguageService.getMessage("Chat.Info.Location") + LocationUtils.toFormattedString(headLocation.getLocation()));
        msgLoc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Info.HoverLocationTp"))));
        msgLoc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hb tp " + headLocation.getLocation().getWorld().getName() + " " + (headLocation.getLocation().getX() + 0.5) + " " + (headLocation.getLocation().getY() + 1) + " " + (headLocation.getLocation().getZ() + 0.5 + " 0.0 90.0")));
        player.spigot().sendMessage(msgLoc);

        player.spigot().sendMessage(new TextComponent(LanguageService.getMessage("Chat.Info.Loaded") + headLocation.isCharged()));
        player.spigot().sendMessage(new TextComponent(LanguageService.getMessage("Chat.Info.HitCount") + headLocation.getDisplayedHitCount()));
        player.spigot().sendMessage(new TextComponent(LanguageService.getMessage("Chat.Info.OrderIndex") + headLocation.getDisplayedOrderIndex()));

        player.sendMessage("");
        player.sendMessage(MessageUtils.colorize("&7----------------------------------------------"));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
