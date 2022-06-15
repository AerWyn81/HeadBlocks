package fr.aerwyn81.headblocks.api;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.InternalException;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HeadBlocksAPI {

    private final HeadBlocks main;

    public HeadBlocksAPI() {
        main = HeadBlocks.getInstance();
    }

    public List<UUID> getPlayerHeads(UUID playerUuid) {
        try {
            return main.getStorageHandler().getHeadsPlayer(playerUuid);
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("Error while trying to communicate with the storage : " + ex.getMessage()));
            return new ArrayList<>();
        }
    }

    public int getTotalHeadSpawnCount() {
        return main.getHeadHandler().getHeadLocations().size();
    }

    public int getLeftPlayerHeadToMax(UUID playerUuid) {
        return getTotalHeadSpawnCount() - getPlayerHeads(playerUuid).size();
    }

    public ArrayList<Pair<String, Integer>> getTopPlayers(int limit) {
        try {
            return main.getStorageHandler().getTopPlayers(limit);
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("Error while trying to communicate with the storage : " + ex.getMessage()));
            return new ArrayList<>();
        }
    }
}
