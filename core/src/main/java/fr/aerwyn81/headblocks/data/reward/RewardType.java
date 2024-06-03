package fr.aerwyn81.headblocks.data.reward;

public enum RewardType {
    UNKNOWN, MESSAGE, COMMAND, BROADCAST;

    static public RewardType of(String t) {
        RewardType[] types = RewardType.values();
        for (RewardType type : types)
            if (type.name().equals(t))
                return type;
        return null;
    }
}