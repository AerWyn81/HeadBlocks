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

@HBAnnotations(command = "rename", permission = "headblocks.admin", isPlayerCommand = true, alias = "r")
public class Rename implements Cmd {
    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var player = (Player) sender;

        Location targetLoc = player.getTargetBlock(null, 100).getLocation();

        HeadLocation headLocation = HeadService.getHeadAt(targetLoc);

        if (headLocation == null) {
            player.sendMessage(LanguageService.getMessage("Messages.NoTargetHeadBlock"));
            return true;
        }
        if(args.length == 0) {
            player.sendMessage(MessageUtils.colorize("&cUsage: /hb rename <name>"));
            return true;
        }
        args[0] = "";
        String name = String.join(" ", args);
        headLocation.setName(name);
        HeadService.saveHeadInConfig(headLocation);
        player.spigot().sendMessage(new TextComponent(LanguageService.getMessage("Chat.Rename") + MessageUtils.colorize(name)));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
