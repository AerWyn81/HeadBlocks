package fr.aerwyn81.headblocks.data;

public record TimedRunData(String huntId, long startTimeMillis, float startYaw) {

    public TimedRunData(String huntId, long startTimeMillis) {
        this(huntId, startTimeMillis, 0f);
    }
}
