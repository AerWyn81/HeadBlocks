package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.PlaceholdersService;
import fr.aerwyn81.headblocks.services.StorageService;
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
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@HBAnnotations(command = "stats", permission = "headblocks.admin", alias = "s")
public class Stats implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        PlayerProfileLight playerProfileLight = CommandsUtils.extractAndGetPlayerUuidByName(sender, args, true);
        if (playerProfileLight == null) {
            return true;
        }

        ArrayList<HeadLocation> headsSpawned = new ArrayList<>(HeadService.getChargedHeadLocations());
        if (headsSpawned.isEmpty()) {
            sender.sendMessage(LanguageService.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        StorageService.getHeadsPlayer(playerProfileLight.uuid(), playerProfileLight.name()).whenComplete(pHeads -> {
            var playerHeads = pHeads.stream()
                    .map(HeadService::getHeadByUUID)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new));

            headsSpawned.sort(Comparator.comparingInt(playerHeads::indexOf));

            ChatPageUtils cpu = new ChatPageUtils(sender)
                    .entriesCount(headsSpawned.size())
                    .currentPage(args);

            String message = LanguageService.getMessage("Chat.LineTitle");
            if (sender instanceof Player) {
                TextComponent titleComponent = new TextComponent(PlaceholdersService.parse(playerProfileLight.name(), playerProfileLight.uuid(), LanguageService.getMessage("Chat.StatsTitleLine")
                        .replaceAll("%headCount%", String.valueOf(playerHeads.size()))));
                cpu.addTitleLine(titleComponent);
            } else {
                sender.sendMessage(message);
            }

            for (int i = cpu.getFirstPos(); i < cpu.getFirstPos() + cpu.getPageHeight() && i < cpu.getSize(); i++) {
                UUID uuid = headsSpawned.get(i).getUuid();
                Location location = headsSpawned.get(i).getLocation();

                String hover = LocationUtils.parseLocationPlaceholders(LanguageService.getMessage("Chat.LineCoordinate"), location);

                var headLocation = HeadService.getHeadByUUID(uuid);
                var headName = headLocation != null ? headLocation.getName() : uuid.toString();
                if (headName.isEmpty()) {
                    headName = uuid.toString();
                }

                if (sender instanceof Player) {
                    TextComponent msg = new TextComponent(MessageUtils.colorize("&6" + headName));
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));

                    TextComponent own;
                    if (playerHeads.stream().anyMatch(s -> s.getUuid() == uuid)) {
                        own = new TextComponent(LanguageService.getMessage("Chat.Box.Own"));
                        own.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.Own"))));
                    } else {
                        own = new TextComponent(LanguageService.getMessage("Chat.Box.NotOwn"));
                        own.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.NotOwn"))));
                    }

                    TextComponent tp = new TextComponent(LanguageService.getMessage("Chat.Box.Teleport"));

                    if (location.getWorld() != null) {
                        tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/headblocks tp " + location.getWorld().getName() + " " + (location.getX() + 0.5) + " " + (location.getY() + 1) + " " + (location.getZ() + 0.5 + " 0.0 90.0")));
                    }
                    tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(LanguageService.getMessage("Chat.Hover.Teleport"))));

                    TextComponent space = new TextComponent(" ");
                    cpu.addLine(own, space, tp, space, msg, space);
                } else {
                    sender.sendMessage((playerHeads.stream().anyMatch(s -> s.getUuid().equals(uuid)) ?
                            LanguageService.getMessage("Chat.Box.Own") : LanguageService.getMessage("Chat.Box.NotOwn")) + " " +
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
