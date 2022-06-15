package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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

        ArrayList<HBHead> HBHeads = headHandler.getHeads();
        if (HBHeads.size() == 0) {
            player.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        ArrayList<HBHead> headsToGive = new ArrayList<>();
        if (args.length > 2 && args[2].equals("*")) {
            headsToGive = headHandler.getHeads();
        } else if (args.length > 2) {
            int id;

            try {
                id = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                id = 1;
            }

            if (id < 1) {
                id = 1;
            }

            int finalId = --id;
            if (finalId > HBHeads.size() - 1) {
                player.sendMessage(languageHandler.getMessage("Messages.ErrorCommand"));
                return true;
            }

            headsToGive.add(headHandler.getHeads().get(finalId));
        } else {
            player.sendMessage(languageHandler.getMessage("Messages.ErrorCommand"));
            return true;
        }

        if (getEmptySlots(player) < headsToGive.size()) {
            player.sendMessage(languageHandler.getMessage("Messages.InventoryFull"));
            return true;
        }

        int headGiven = 0;
        for (HBHead head : headsToGive) {
            if (head instanceof HBHeadHDB) {
                HBHeadHDB headHDB = (HBHeadHDB) head;

                if (!headHDB.isLoaded()) {
                    player.sendMessage(languageHandler.getMessage("Messages.HeadNotYetLoaded")
                            .replaceAll("%id%", String.valueOf(headHDB.getId())));
                    continue;
                }
            }

            player.getInventory().addItem(head.getItemStack());
            headGiven++;
        }


        if (headGiven != 0) {
            player.sendMessage(languageHandler.getMessage("Messages.HeadGiven"));
        }

        return true;
    }

    public int getEmptySlots(Player player) {
        int i = 0;

        ItemStack[] items;
        if (Version.getCurrent() == Version.v1_8) {
            items = (ItemStack[]) main.getLegacySupport().getInventoryContent(player);
        } else {
            items = player.getInventory().getStorageContents();
        }

        for (ItemStack is : items) {
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
