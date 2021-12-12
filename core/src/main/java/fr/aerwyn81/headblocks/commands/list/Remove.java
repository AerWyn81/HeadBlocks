package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

@HBAnnotations(command = "remove", permission = "headblocks.admin", args = { "headUUID" })
public class Remove implements Cmd {
    private final HeadBlocks main;
    private final ConfigHandler configHandler;
    private final LanguageHandler languageHandler;
    private final HeadHandler headHandler;
    private final StorageHandler storageHandler;

    public Remove(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();
        this.languageHandler = main.getLanguageHandler();
        this.headHandler = main.getHeadHandler();
        this.storageHandler = main.getStorageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length != 2) {
            player.sendMessage(languageHandler.getMessage("Messages.ErrorCommand"));
            return true;
        }

        Pair<UUID, Location> head = headHandler.getHeadByUUID(UUID.fromString(args[1]));
        if (head == null) {
            player.sendMessage(languageHandler.getMessage("Messages.RemoveLocationError"));
            return true;
        }

        if (configHandler.shouldResetPlayerData()) {
            storageHandler.removeHead(head.getValue0());
        }

        headHandler.removeHead(head.getValue0());

        Location loc = head.getValue1();
        player.sendMessage(languageHandler.getMessage("Messages.HeadRemoved")
                .replaceAll("%world%", loc.getWorld() != null ? loc.getWorld().getName() : FormatUtils.translate("&cUnknownWorld"))
                .replaceAll("%x%", String.valueOf(loc.getBlockX()))
                .replaceAll("%y%", String.valueOf(loc.getBlockY()))
                .replaceAll("%z%", String.valueOf(loc.getBlockZ())));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? main.getHeadHandler().getHeadLocations().stream()
                .map(Pair::getValue0)
                .map(UUID::toString)
                .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
    }
}
