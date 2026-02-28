package fr.aerwyn81.headblocks.data.hunt.behavior;

public record BehaviorResult(boolean allowed, String denyMessage) {

    public static BehaviorResult allow() {
        return new BehaviorResult(true, null);
    }

    public static BehaviorResult deny(String message) {
        return new BehaviorResult(false, message);
    }
}
