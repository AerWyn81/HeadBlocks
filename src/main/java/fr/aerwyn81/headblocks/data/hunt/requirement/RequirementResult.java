package fr.aerwyn81.headblocks.data.hunt.requirement;

/**
 * Outcome of a single requirement check.
 * <p>
 * {@code reason} is the player facing sentence explaining why the requirement is not met. It is
 * meant to be listed among the other blocking reasons, so it carries no prefix and no line break.
 * <p>
 * {@link Status#UNRESOLVABLE} is the third answer a requirement can give: it could not be evaluated
 * at all (an unloaded world, an optional plugin that went away). Such a requirement lets the click
 * through, but {@link RequirementSet} also keeps it out of the {@code ANY} quorum so it cannot
 * unlock a hunt in place of the condition the admin actually configured.
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

    /**
     * The requirement cannot be evaluated right now. It never blocks a click and never counts as a
     * met condition either.
     */
    public static RequirementResult unresolvable() {
        return UNRESOLVABLE;
    }

    public static RequirementResult unmet(String reason) {
        return new RequirementResult(Status.UNMET, reason);
    }

    /**
     * Whether the click may go through as far as this requirement is concerned.
     */
    public boolean satisfied() {
        return status != Status.UNMET;
    }

    public boolean isUnresolvable() {
        return status == Status.UNRESOLVABLE;
    }
}
