package fr.aerwyn81.headblocks.data.hunt.requirement;

/**
 * What one check answers: met, unmet with a reason, or impossible to evaluate right now.
 */
public record RequirementResult(Status status, String reason) {
    public enum Status {
        SATISFIED,
        UNMET,
        UNRESOLVABLE
    }

    private static final RequirementResult SATISFIED = new RequirementResult(Status.SATISFIED, null);
    private static final RequirementResult UNRESOLVABLE = new RequirementResult(Status.UNRESOLVABLE, null);

    public static RequirementResult ok() {
        return SATISFIED;
    }

    public static RequirementResult unresolvable() {
        return UNRESOLVABLE;
    }

    public static RequirementResult unmet(String reason) {
        return new RequirementResult(Status.UNMET, reason);
    }

    public boolean satisfied() {
        return status != Status.UNMET;
    }

    public boolean isUnresolvable() {
        return status == Status.UNRESOLVABLE;
    }
}
