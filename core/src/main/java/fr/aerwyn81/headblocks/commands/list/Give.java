package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.stream.Collectors;

@HBAnnotations(command = "give", permission = "headblocks.admin", isPlayerCommand = true)
public class Give implements Cmd {
    private final LanguageHandler languageHandler;
    private final HeadHandler headHandler;

    public Give(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
        this.headHandler = main.getHeadHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 2) {
            Player pTemp = Bukkit.getPlayer(args[1]);

            if (pTemp == null) {
                player.sendMessage(languageHandler.getMessage("Messages.PlayerNotFound"));
                return true;
            }

            player = pTemp;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(languageHandler.getMessage("Messages.InventoryFull"));
            return true;
        }

        ItemStack headPlugin = headHandler.getPluginHead();
        player.getInventory().addItem(headPlugin);

        player.sendMessage(languageHandler.getMessage("Messages.HeadGiven"));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
    }
}
