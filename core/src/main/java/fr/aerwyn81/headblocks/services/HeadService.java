package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadDefault;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.data.head.types.HBHeadPlayer;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.GlobalConstants;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class HeadService {
    private static ArrayList<HBHead> heads;
    private static HashMap<UUID, HeadMove> headMoves;

    public static void initialize() {
        heads = new ArrayList<>();
        headMoves = new HashMap<>();

        load();
    }

    public static void load() {
        heads.clear();
        headMoves.clear();

        loadHeads();
    }

    private static void loadHeads() {
        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eLoading heads:"));

        List<String> headsConfig = ConfigService.getHeads();

        for (int i = 0; i < headsConfig.size(); i++) {
            String configHead = headsConfig.get(i);
            String[] parts = configHead.split(":");

            if (parts.length != 2) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cInvalid format for " + configHead + " in heads configuration section (l." + i + 1 + ")"));
                continue;
            }

            if (parts[1].trim().equals("")) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cValue cannot be empty for " + configHead + " in heads configuration section (l." + i + 1 + ")"));
                continue;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);

            ItemMeta headMeta = head.getItemMeta();
            if (headMeta == null) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cError trying to get meta of the head " + head + ". Is your server version supported?"));
                continue;
            }

            headMeta.setDisplayName(LanguageService.getMessage("Head.Name"));
            headMeta.setLore(LanguageService.getMessages("Head.Lore"));
            headMeta.getPersistentDataContainer().set(GlobalConstants.HB_KEY, PersistentDataType.STRING, "");

            switch (parts[0]) {
                case "player":
                    OfflinePlayer p;

                    try {
                        p = Bukkit.getOfflinePlayer(parts[1]);
                    } catch (Exception ex) {
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cCannot parse the player UUID " + configHead + ". Please provide a correct UUID"));
                        continue;
                    }

                    SkullMeta meta = (SkullMeta) headMeta;
                    meta.setOwningPlayer(p);
                    head.setItemMeta(meta);

                    heads.add(new HBHeadPlayer(head));
                    break;
                case "default":
                    head.setItemMeta(headMeta);
                    heads.add(HeadUtils.createHead(new HBHeadDefault(head), parts[1]));
                    break;
                case "hdb":
                    if (!HeadBlocks.getInstance().isHeadDatabaseActive()) {
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cCannot load hdb head " + configHead + " without HeadDatabase installed"));
                        continue;
                    }

                    head.setItemMeta(headMeta);
                    heads.add(new HBHeadHDB(head, parts[1]));
                    break;
                default:
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cThe " + parts[0] + " type is not yet supported!"));
            }
        }

        if (heads.size() == 0) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cNo head has been loaded!"));
        } else {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &e" + heads.size() + " &aheads has been loaded!"));
        }
    }

    public static ArrayList<HBHead> getHeads() {
        return heads;
    }

    public static HashMap<UUID, HeadMove> getHeadMoves() {
        return headMoves;
    }

    public static void clearHeadMoves() {
        headMoves.clear();
    }
}
