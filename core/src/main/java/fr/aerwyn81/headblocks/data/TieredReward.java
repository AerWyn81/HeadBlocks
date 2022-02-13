package fr.aerwyn81.headblocks.data;

import java.util.List;

public class TieredReward {
    private final int level;
    private final List<String> messages;
    private final List<String> commands;
    private final List<String> broadcastMessages;

    public TieredReward(int level, List<String> messages, List<String> commands, List<String> broadcastMessages) {
        this.level = level;
        this.messages = messages;
        this.commands = commands;
        this.broadcastMessages = broadcastMessages;
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
}
