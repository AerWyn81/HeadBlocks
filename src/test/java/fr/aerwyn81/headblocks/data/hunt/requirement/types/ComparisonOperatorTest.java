package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonOperatorTest {

    @Test
    void equals_isCaseInsensitiveAndTrims() {
        assertThat(ComparisonOperator.EQUALS.test(" Yes ", "yes")).isTrue();
        assertThat(ComparisonOperator.EQUALS.test("yes", "no")).isFalse();
    }

    @Test
    void notEquals_isTheOppositeOfEquals() {
        assertThat(ComparisonOperator.NOT_EQUALS.test("yes", "no")).isTrue();
        assertThat(ComparisonOperator.NOT_EQUALS.test("yes", "YES")).isFalse();
    }

    @Test
    void contains_matchesSubstrings() {
        assertThat(ComparisonOperator.CONTAINS.test("VIP Gold", "vip")).isTrue();
        assertThat(ComparisonOperator.CONTAINS.test("Gold", "vip")).isFalse();
    }

    @Test
    void ordering_comparesNumbers() {
        assertThat(ComparisonOperator.GREATER_THAN.test("10", "5")).isTrue();
        assertThat(ComparisonOperator.GREATER_THAN.test("5", "5")).isFalse();
        assertThat(ComparisonOperator.GREATER_OR_EQUALS.test("5", "5")).isTrue();
        assertThat(ComparisonOperator.LESS_THAN.test("4.5", "5")).isTrue();
        assertThat(ComparisonOperator.LESS_OR_EQUALS.test("5", "5")).isTrue();
        assertThat(ComparisonOperator.LESS_OR_EQUALS.test("6", "5")).isFalse();
    }

    @Test
    void ordering_acceptsCommaDecimals() {
        assertThat(ComparisonOperator.GREATER_THAN.test("10,5", "10.2")).isTrue();
    }

    @Test
    void ordering_nonNumericValue_isNeverSatisfied() {
        assertThat(ComparisonOperator.GREATER_THAN.test("%unresolved%", "5")).isFalse();
        assertThat(ComparisonOperator.LESS_THAN.test("3", "not a number")).isFalse();
    }

    @Test
    void test_nullValues_areTreatedAsEmpty() {
        assertThat(ComparisonOperator.EQUALS.test(null, "")).isTrue();
        assertThat(ComparisonOperator.GREATER_THAN.test(null, "1")).isFalse();
    }

    @Test
    void fromSymbol_acceptsSymbolsAndNames() {
        assertThat(ComparisonOperator.fromSymbol(">=")).isEqualTo(ComparisonOperator.GREATER_OR_EQUALS);
        assertThat(ComparisonOperator.fromSymbol("CONTAINS")).isEqualTo(ComparisonOperator.CONTAINS);
        assertThat(ComparisonOperator.fromSymbol("unknown")).isEqualTo(ComparisonOperator.EQUALS);
        assertThat(ComparisonOperator.fromSymbol(null)).isEqualTo(ComparisonOperator.EQUALS);
    }

    @Test
    void next_cyclesThroughEveryOperator() {
        ComparisonOperator operator = ComparisonOperator.EQUALS;
        for (int i = 0; i < ComparisonOperator.values().length; i++) {
            operator = operator.next();
        }

        assertThat(operator).isEqualTo(ComparisonOperator.EQUALS);
    }
}
