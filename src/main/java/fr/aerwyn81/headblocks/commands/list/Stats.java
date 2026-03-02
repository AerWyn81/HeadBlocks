package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.chat.ChatPageUtils;
import fr.aerwyn81.headblocks.utils.internal.CommandsUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

@HBAnnotations(command = "stats", permission = "headblocks.admin", alias = "s")
public class Stats implements Cmd {
    private final ServiceRegistry registry;

    public Stats(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        PlayerProfileLight playerProfileLight = CommandsUtils.extractAndGetPlayerUuidByName(registry, sender, args, true);
        if (playerProfileLight == null) {
            return true;
        }

        ArrayList<UUID> heads;

        try {
            heads = registry.getStorageService().getHeads();
        } catch (InternalException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            return true;
        }

        if (heads.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ListHeadEmpty"));
            return true;
        }

        registry.getStorageService().getHeadsPlayer(playerProfileLight.uuid()).whenComplete(pHeads -> {
            var playerHeads = new ArrayList<>(pHeads);

            heads.sort(Comparator.comparingInt(playerHeads::indexOf));

            ChatPageUtils cpu = new ChatPageUtils(sender, registry.getLanguageService())
                    .entriesCount(heads.size())
                    .currentPage(args);

            String message = registry.getLanguageService().message("Chat.LineTitle");
            if (sender instanceof Player) {
                TextComponent titleComponent = new TextComponent(registry.getPlaceholdersService().parse(playerProfileLight.name(), playerProfileLight.uuid(), registry.getLanguageService().message("Chat.StatsTitleLine")
                        .replace("%headCount%", String.valueOf(playerHeads.size()))));
                cpu.addTitleLine(titleComponent);
            } else {
                sender.sendMessage(message);
            }

            for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize(); i++) {
                UUID uuid = heads.get(i);

                HeadLocation headLocation = null;

                var chargedHead = registry.getHeadService().getChargedHeadLocations().stream().filter(h -> h.getUuid().equals(uuid)).findFirst();
                if (chargedHead.isPresent()) {
                    headLocation = chargedHead.get();
                }

                var hover = registry.getLanguageService().message("Chat.Hover.HeadIsNotOnThisServer");

                if (headLocation != null) {
                    hover = LocationUtils.parseLocationPlaceholders(registry.getLanguageService().message("Chat.LineCoordinate"), headLocation.getLocation());
                }

                var headName = headLocation != null ? headLocation.getName() : uuid.toString();
                if (headName.isEmpty()) {
                    headName = uuid.toString();
                }

                if (sender instanceof Player) {
                    TextComponent msg = new TextComponent(MessageUtils.colorize("&6" + headName));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));

                    TextComponent own;
                    if (playerHeads.stream().anyMatch(s -> s.equals(uuid))) {
                        own = new TextComponent(registry.getLanguageService().message("Chat.Box.Own"));
                        own.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(registry.getLanguageService().message("Chat.Hover.Own"))));
                    } else {
                        own = new TextComponent(registry.getLanguageService().message("Chat.Box.NotOwn"));
                        own.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(registry.getLanguageService().message("Chat.Hover.NotOwn"))));
                    }

                    TextComponent tp = new TextComponent(registry.getLanguageService().message("Chat.Box.Teleport"));

                    if (headLocation != null) {
                        var location = headLocation.getLocation();

                        if (location.getWorld() != null) {
                            tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks tp " + location.getWorld().getName() + " " + location.getX() + " " + (location.getY() + 1) + " " + location.getZ() + " 0.0 90.0"));
                        }
                        tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(registry.getLanguageService().message("Chat.Hover.Teleport"))));
                    } else {
                        tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(registry.getLanguageService().message("Chat.Hover.BlockedTeleport"))));
                    }

                    TextComponent space = new TextComponent(" ");
                    cpu.addLine(own, space, tp, space, msg, space);
                } else {
                    sender.sendMessage((playerHeads.stream().anyMatch(s -> s.equals(uuid)) ?
                            registry.getLanguageService().message("Chat.Box.Own") : registry.getLanguageService().message("Chat.Box.NotOwn")) + " " +
                            MessageUtils.colorize("&6" + headName));
                }
            }

            cpu.addPageLine("stats " + playerProfileLight.name());
            cpu.build();
        });

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
    }
}
