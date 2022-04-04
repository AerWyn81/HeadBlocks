package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class PlaceholderHook extends PlaceholderExpansion {

    private final HeadBlocksAPI headBlocksAPI;

    public PlaceholderHook(HeadBlocks main) {
        this.headBlocksAPI = main.getHeadBlocksAPI();
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

        if (identifier.equals("current")) {
            return "" + headBlocksAPI.getPlayerHeads(player.getUniqueId()).size();
        }

        if (identifier.equals("max")) {
            return "" + headBlocksAPI.getTotalHeadSpawnCount();
        }

        if (identifier.equals("left")) {
            return "" + headBlocksAPI.getLeftPlayerHeadToMax(player.getUniqueId());
        }

        return null;
    }
}