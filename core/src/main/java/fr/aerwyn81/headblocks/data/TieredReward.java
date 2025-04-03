package fr.aerwyn81.headblocks.data;

import java.util.List;

public class TieredReward {
    private final int level;
    private final List<String> messages;
    private final List<String> commands;
    private final List<String> broadcastMessages;
    private final int slotsRequired;
    private final boolean isRandom;

    public TieredReward(int level, List<String> messages, List<String> commands, List<String> broadcastMessages, int slotsRequired, boolean isRandom) {
        this.level = level;
        this.messages = messages;
        this.commands = commands;
        this.broadcastMessages = broadcastMessages;
        this.slotsRequired = slotsRequired;
        this.isRandom = isRandom;
    }

    public int getLevel() {
        return level;
    }

    public List<String> getMessages() {
        return messages;
    }

    public List<String> getCommands() {
        return commands;
    }

    public List<String> getBroadcastMessages() {
        return broadcastMessages;
    }

    public int getSlotsRequired() {
        return slotsRequired;
    }

    public boolean isRandom() {
        return isRandom;
    }
}
