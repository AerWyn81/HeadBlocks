package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@HBAnnotations(command = "leave", permission = "headblocks.use", isPlayerCommand = true)
public class Leave implements Cmd {
    private final ServiceRegistry registry;

    public Leave(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!TimedRunManager.isInRun(player.getUniqueId())) {
            player.sendMessage(registry.getLanguageService().message("Messages.TimedNoActiveRun"));
            return true;
        }

        TimedRunManager.leaveRun(player.getUniqueId());
        player.sendMessage(registry.getLanguageService().message("Messages.TimedLeft"));
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
