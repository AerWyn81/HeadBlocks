package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;

@HBAnnotations(command = "removeall", permission = "headblocks.admin")
public class RemoveAll implements Cmd {
    private static final String HEAD_COUNT_PLACEHOLDER = "%headCount%";

    private final ServiceRegistry registry;

    public RemoveAll(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        ArrayList<HeadLocation> headLocations = new ArrayList<>(registry.getHeadService().getChargedHeadLocations());
        int headCount = headLocations.size();

        if (headLocations.isEmpty()) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ListHeadEmpty"));
            return;
        }

        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        if (hasConfirmInCommand) {
            sender.sendMessage(registry.getLanguageService().message("Messages.RemoveAllInProgress")
                    .replace(HEAD_COUNT_PLACEHOLDER, String.valueOf(headCount)));

            registry.getHeadService().removeAllHeadLocationsAsync(headLocations, registry.getConfigService().resetPlayerData(), headRemoved -> {
                if (headRemoved == 0) {
                    sender.sendMessage(registry.getLanguageService().message("Messages.RemoveAllError")
                            .replace(HEAD_COUNT_PLACEHOLDER, String.valueOf(headCount)));
                    return;
                }

                sender.sendMessage(registry.getLanguageService().message("Messages.RemoveAllSuccess")
                        .replace(HEAD_COUNT_PLACEHOLDER, String.valueOf(headRemoved)));
            });

            return;
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.RemoveAllConfirm")
                .replace(HEAD_COUNT_PLACEHOLDER, String.valueOf(headCount)));
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(Collections.singleton("--confirm")) : new ArrayList<>();
    }
}
