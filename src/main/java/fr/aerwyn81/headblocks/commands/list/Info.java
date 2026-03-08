package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
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
    private final ServiceRegistry registry;

    public Info(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        var player = (Player) sender;

        Location targetLoc = player.getTargetBlock(null, 100).getLocation();

        HeadLocation headLocation = registry.getHeadService().getHeadAt(targetLoc);

        if (headLocation == null) {
            player.sendMessage(registry.getLanguageService().message("Messages.NoTargetHeadBlock"));
            return true;
        }

        player.sendMessage(MessageUtils.colorize("&7----------- [ &e&oTarget head information &7]-----------"));
        player.sendMessage("");

        TextComponent msgName = new TextComponent(registry.getLanguageService().message("Chat.Info.Name") + headLocation.getNameOrUnnamed(registry.getLanguageService().message("Gui.Unnamed")));
        msgName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(registry.getLanguageService().message("Chat.Info.HoverCopyName"))));
        msgName.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, headLocation.getName()));
        player.spigot().sendMessage(msgName);

        TextComponent msgUuid = new TextComponent(registry.getLanguageService().message("Chat.Info.Uuid") + headLocation.getUuid());
        msgUuid.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(registry.getLanguageService().message("Chat.Info.HoverCopyUuid"))));
        msgUuid.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, headLocation.getUuid().toString()));
        player.spigot().sendMessage(msgUuid);

        String huntId = headLocation.getHuntId();
        player.spigot().sendMessage(new TextComponent(registry.getLanguageService().message("Chat.Info.Hunt") + huntId));

        TextComponent msgLoc = new TextComponent(registry.getLanguageService().message("Chat.Info.Location") + LocationUtils.toFormattedString(headLocation.getLocation()));
        msgLoc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(registry.getLanguageService().message("Chat.Info.HoverLocationTp"))));

        var world = headLocation.getLocation().getWorld();
        if (world != null) {
            msgLoc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks tp " + headLocation.getLocation().getWorld().getName() + " " + headLocation.getX() + " " + (headLocation.getLocation().getY() + 1) + " " + headLocation.getZ() + " 0.0 90.0"));
        } else {
            LogUtil.error("Error when attempting to teleport to the head; the name of the head world is empty.");
        }

        player.spigot().sendMessage(msgLoc);

        player.spigot().sendMessage(new TextComponent(registry.getLanguageService().message("Chat.Info.Loaded") + headLocation.isCharged()));
        player.spigot().sendMessage(new TextComponent(registry.getLanguageService().message("Chat.Info.OrderIndex") + headLocation.getDisplayedOrderIndex(registry.getLanguageService().message("Gui.NoOrder"))));

        player.sendMessage("");
        player.sendMessage(registry.getLanguageService().message("Chat.Info.HintsTitle"));
        player.spigot().sendMessage(new TextComponent(registry.getLanguageService().message("Chat.Info.HintSound") + headLocation.isHintSoundEnabled()));
        player.spigot().sendMessage(new TextComponent(registry.getLanguageService().message("Chat.Info.HintActionBar") + headLocation.isHintActionBarEnabled()));

        var rewards = headLocation.getRewards();
        if (!rewards.isEmpty()) {
            player.sendMessage("");
            var msgRewards = new TextComponent(registry.getLanguageService().message("Chat.Info.RewardsTitle") + "(" + rewards.size() + ") " + registry.getLanguageService().message("Chat.Info.HoverForMore"));

            var hoverText = new StringBuilder();
            for (int i = 0; i < rewards.size(); i++) {
                var reward = rewards.get(i);

                var typeColor = switch (reward.type()) {
                    case MESSAGE -> "&a";
                    case COMMAND -> "&6";
                    case BROADCAST -> "&b";
                    default -> "&7";
                };

                hoverText.append(MessageUtils.colorize(typeColor + reward.type().name() + "&7: " + reward.value()));
                if (i < rewards.size() - 1) {
                    hoverText.append("\n");
                }
            }

            msgRewards.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText.toString())));
            msgRewards.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks options rewards " + headLocation.getUuid()));
            player.spigot().sendMessage(msgRewards);
        }

        player.sendMessage("");
        player.sendMessage(MessageUtils.colorize("&7----------------------------------------------"));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
