package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.services.GuiService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

@HBAnnotations(command = "options", permission = "headblocks.admin", isPlayerCommand = true, alias = "o")
public class Options implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (args.length > 1) {
            switch (args[1])
            {
                case "order":
                    GuiService.getOrderManager().openOrderGui((Player) sender);
                    return true;
                case "counter":
                    GuiService.getClickCounterManager().openClickCounterGui((Player) sender);
                    return true;
                case "hint":
                    GuiService.getHintManager().openHintGui((Player) sender);
                    return true;
                case "rewards":
                    GuiService.getRewardsManager().openRewardsSelectionGui((Player) sender);
                    return true;
            }
        }

        GuiService.openOptionsGui((Player) sender);
        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(Arrays.asList("counter", "hint", "order", "rewards")) : new ArrayList<>();
    }
}