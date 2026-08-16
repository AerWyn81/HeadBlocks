package fr.aerwyn81.headblocks.data.hunt.requirement.types;

/**
 * How a placeholder requirement compares: numerically when both sides are numbers, as text otherwise.
 */
public enum ComparisonOperator {
    EQUALS("=", false),
    NOT_EQUALS("!=", false),
    GREATER_THAN(">", true),
    GREATER_OR_EQUALS(">=", true),
    LESS_THAN("<", true),
    LESS_OR_EQUALS("<=", true),
    CONTAINS("contains", false);

    private final String symbol;
    private final boolean numeric;

    ComparisonOperator(String symbol, boolean numeric) {
        this.symbol = symbol;
        this.numeric = numeric;
    }

    public String getSymbol() {
        return symbol;
    }

    public ComparisonOperator next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean test(String actual, String expected) {
        String left = actual == null ? "" : actual.trim();
        String right = expected == null ? "" : expected.trim();

        if (numeric) {
            Double leftNumber = parse(left);
            Double rightNumber = parse(right);
            if (leftNumber == null || rightNumber == null) {
                return false;
            }

            int comparison = Double.compare(leftNumber, rightNumber);
            return switch (this) {
                case GREATER_THAN -> comparison > 0;
                case GREATER_OR_EQUALS -> comparison >= 0;
                case LESS_THAN -> comparison < 0;
                default -> comparison <= 0;
            };
        }

        return switch (this) {
            case NOT_EQUALS -> !left.equalsIgnoreCase(right);
            case CONTAINS -> left.toLowerCase().contains(right.toLowerCase());
            default -> left.equalsIgnoreCase(right);
        };
    }

    private Double parse(String value) {
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static ComparisonOperator fromSymbol(String symbol) {
        if (symbol == null) {
            return EQUALS;
        }

        for (ComparisonOperator operator : values()) {
            if (operator.symbol.equalsIgnoreCase(symbol) || operator.name().equalsIgnoreCase(symbol)) {
                return operator;
            }
        }

        return EQUALS;
    }
}
