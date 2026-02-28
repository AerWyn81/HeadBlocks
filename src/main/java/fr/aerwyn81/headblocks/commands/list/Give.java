package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@HBAnnotations(command = "give", permission = "headblocks.admin", isPlayerCommand = true, alias = "g")
public class Give implements Cmd {

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length > 1) {
            Player pTemp = Bukkit.getPlayer(args[1]);

            if (pTemp == null) {
                player.sendMessage(LanguageService.getMessage("Messages.PlayerNotConnected", args[1]));
                return true;
            }

            player = pTemp;
        }

        ArrayList<HBHead> hbHeads = HeadService.getHeads();
        if (hbHeads.isEmpty()) {
            player.sendMessage(LanguageService.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        ArrayList<HBHead> headsToGive = new ArrayList<>();
        if (args.length > 2 && args[2].equals("*")) {
            headsToGive = hbHeads;
        } else if (args.length < 3) {
            headsToGive.addAll(hbHeads);
        } else {
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
            if (finalId > hbHeads.size() - 1) {
                player.sendMessage(LanguageService.getMessage("Messages.ErrorCommand"));
                return true;
            }

            headsToGive.add(hbHeads.get(finalId));
        }

        if (PlayerUtils.getEmptySlots(player) < headsToGive.size()) {
            player.sendMessage(LanguageService.getMessage("Messages.InventoryFull"));
            return true;
        }

        int headGiven = 0;
        for (HBHead head : headsToGive) {
            if (head instanceof HBHeadHDB headHDB) {
                if (!headHDB.isLoaded()) {
                    player.sendMessage(LanguageService.getMessage("Messages.HeadNotYetLoaded")
                            .replaceAll("%id%", String.valueOf(headHDB.getId())));
                    continue;
                }
            }

            player.getInventory().addItem(head.getItemStack());
            headGiven++;
        }


        if (headGiven != 0) {
            player.sendMessage(LanguageService.getMessage("Messages.HeadGiven"));
        }

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(p -> p.toLowerCase().startsWith(args[1]))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        int headCount = HeadService.getHeads().size();

        ArrayList<String> items = new ArrayList<>();
        if (args.length == 3 && headCount > 0) {
            if (headCount > 1) {
                items.add("*");

                items.addAll(IntStream.range(1, headCount + 1)
                        .boxed()
                        .map(Object::toString)
                        .toList());
            }
        }

        return items;
    }
}
