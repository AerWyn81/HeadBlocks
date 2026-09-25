package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;

@HBAnnotations(command = "give", permission = "headblocks.admin", alias = "g")
public class Give implements Cmd {
    private final ServiceRegistry registry;

    public Give(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player target;

        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(registry.getLanguageService().message("Messages.PlayerNotConnected", args[1]));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return true;
        }

        registry.getGuiService().getCatalogGui().open(target);
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return new ArrayList<>();
        }

        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(p -> p.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
