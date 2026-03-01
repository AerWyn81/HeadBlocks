package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.CommandsUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@HBAnnotations(command = "progress", permission = "headblocks.commands.progress", alias = "p")
public class Progress implements Cmd {
    private final ServiceRegistry registry;

    public Progress(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        PlayerProfileLight playerProfileLight = CommandsUtils.extractAndGetPlayerUuidByName(registry, sender, args, PlayerUtils.hasPermission(sender, "headblocks.commands.progress.other"));
        if (playerProfileLight == null) {
            return true;
        }

        if (registry.getHuntService().isMultiHunt()) {
            showMultiHuntProgress(sender, playerProfileLight);
        } else {
            showLegacyProgress(sender, playerProfileLight);
        }

        return true;
    }

    private void showLegacyProgress(CommandSender sender, PlayerProfileLight profile) {
        List<String> messages = registry.getLanguageService().messageList("Messages.ProgressCommand");
        if (!messages.isEmpty()) {
            messages.forEach(msg ->
                    sender.sendMessage(registry.getPlaceholdersService().parse(profile.name(), profile.uuid(), msg)));
        }
    }

    private void showMultiHuntProgress(CommandSender sender, PlayerProfileLight profile) {
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntProgressHeader")
                .replaceAll("%player%", profile.name()));

        for (Hunt hunt : registry.getHuntService().getAllHunts()) {
            try {
                ArrayList<java.util.UUID> huntHeads = registry.getStorageService().getHeadsPlayerForHunt(
                        profile.uuid(), hunt.getId());
                int current = huntHeads.size();
                int total = hunt.getHeadCount();

                String progress = MessageUtils.createProgressBar(current, total,
                        registry.getConfigService().progressBarBars(),
                        registry.getConfigService().progressBarSymbol(),
                        registry.getConfigService().progressBarCompletedColor(),
                        registry.getConfigService().progressBarNotCompletedColor());

                sender.sendMessage(MessageUtils.colorize(
                        registry.getLanguageService().message("Messages.HuntProgressEntry")
                                .replaceAll("%hunt%", hunt.getId())
                                .replaceAll("%displayName%", hunt.getDisplayName())
                                .replaceAll("%state%", hunt.getState().getLocalizedName(registry.getLanguageService()))
                                .replaceAll("%current%", String.valueOf(current))
                                .replaceAll("%max%", String.valueOf(total))
                                .replaceAll("%progress%", progress)));
            } catch (InternalException e) {
                LogUtil.error("Error retrieving hunt progress for {0} in hunt {1}: {2}",
                        profile.name(), hunt.getId(), e.getMessage());
            }
        }
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
    }
}
