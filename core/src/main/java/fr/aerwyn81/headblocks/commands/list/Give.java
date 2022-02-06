package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@HBAnnotations(command = "give", permission = "headblocks.admin", isPlayerCommand = true)
public class Give implements Cmd {
    private final HeadBlocks main;
    private final LanguageHandler languageHandler;
    private final HeadHandler headHandler;

    public Give(HeadBlocks main) {
        this.main = main;
        this.languageHandler = main.getLanguageHandler();
        this.headHandler = main.getHeadHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length > 1) {
            Player pTemp = Bukkit.getPlayer(args[1]);

            if (pTemp == null) {
                player.sendMessage(languageHandler.getMessage("Messages.PlayerNotFound"));
                return true;
            }

            player = pTemp;
        }

        ArrayList<ItemStack> heads = headHandler.getHeads();
        if (heads.size() == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        ArrayList<ItemStack> headsToGive = new ArrayList<>();
        if (args.length > 2 && args[2].equals("*")) {
            headsToGive = headHandler.getHeads();
        } else if (args.length > 2) {
            Integer id = FormatUtils.parseIntOrNull(args[2]);
            if (id == null) {
                id = 1;
            }

            headsToGive.add(headHandler.getHeads().get(--id));
        } else {
            player.sendMessage(languageHandler.getMessage("Messages.ErrorCommand"));
            return true;
        }

        if (getEmptySlots(player.getInventory()) < headsToGive.size()) {
            player.sendMessage(languageHandler.getMessage("Messages.InventoryFull"));
            return true;
        }

        Player finalPlayer = player;
        headsToGive.forEach(h -> finalPlayer.getInventory().addItem(h));
        player.sendMessage(languageHandler.getMessage("Messages.HeadGiven"));
        return true;
    }

    public int getEmptySlots(Inventory inventory) {
        int i = 0;
        for (ItemStack is : inventory.getStorageContents()) {
            if (is != null && is.getType() != Material.AIR)
                continue;
            i++;
        }
        return i;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        ArrayList<String> items = new ArrayList<>();
        if (args.length == 3 && main.getHeadHandler().getHeads().size() > 0) {
            items.add("*");
            items.addAll(IntStream.range(1, main.getHeadHandler().getHeads().size() + 1)
                    .boxed()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        }

        return items;
    }
}
