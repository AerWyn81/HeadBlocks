package fr.aerwyn81.headblocks.data;

import java.util.List;

public record TieredReward(int level, List<String> messages, List<String> commands, List<String> broadcastMessages,
                           int slotsRequired, boolean isRandom) {
}
