package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class PlaceholderHook extends PlaceholderExpansion {

    private final HeadBlocks main;

    public PlaceholderHook(HeadBlocks main) {
        this.main = main;
    }

    @Override
    public boolean canRegister() {
        return Bukkit.getPluginManager().getPlugin("HeadBlocks") != null;
    }

    @Override
    public boolean register() {
        if (!canRegister()) {
            return false;
        }

        return super.register();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "headblocks";
    }

    @Override
    public String getAuthor() {
        return "AerWyn81";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) return "";

        if (identifier.equals("current") || identifier.equals("left")) {
            int current;
            try {
                current = main.getStorageHandler().getHeadsPlayer(player.getUniqueId()).size();
            } catch (InternalException ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("Error while retrieving heads of " + player.getName() + ": " + ex.getMessage()));
                return "-1";
            }

            if (identifier.equals("current")) {
                return "" + current;
            } else {
                return "" + (main.getHeadHandler().getChargedHeadLocations().size() - current);
            }
        }

        if (identifier.equals("max")) {
            return "" +  main.getHeadHandler().getChargedHeadLocations().size();
        }

        return null;
    }
}