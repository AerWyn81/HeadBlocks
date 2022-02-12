package fr.aerwyn81.headblocks.api;

import fr.aerwyn81.headblocks.HeadBlocks;
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
        return main.getStorageHandler().getHeadsPlayer(playerUuid);
    }

    public int getTotalHeadSpawnCount() {
        return main.getHeadHandler().getHeadLocations().size();
    }

    public int getLeftPlayerHeadToMax(UUID playerUuid) {
        return getTotalHeadSpawnCount() - getPlayerHeads(playerUuid).size();
    }

    public ArrayList<Pair<UUID, Integer>> getTopPlayers(int limit) {
        return main.getStorageHandler().getTopPlayers(limit);
    }
}
