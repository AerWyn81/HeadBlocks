package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.PlaceholdersService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.CommandsUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@HBAnnotations(command = "progress", permission = "headblocks.commands.progress", alias = "p")
public class Progress implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        PlayerProfileLight playerProfileLight = CommandsUtils.extractAndGetPlayerUuidByName(sender, args, PlayerUtils.hasPermission(sender, "headblocks.commands.progress.other"));
        if (playerProfileLight == null) {
            return true;
        }

        int max = HeadService.getChargedHeadLocations().size();
        if (max == 0) {
            sender.sendMessage(LanguageService.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        List<String> messages = LanguageService.getMessages("Messages.ProgressCommand");
        if (!messages.isEmpty()) {
            messages.forEach(msg ->
                    sender.sendMessage(PlaceholdersService.parse(playerProfileLight.name(), playerProfileLight.uuid(), msg)));
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
