package fr.aerwyn81.headblocks.utils.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleColorTest {

    private static final String ESC = "";

    @Test
    void toString_returnsRawAnsiCode() {
        assertThat(ConsoleColor.GREEN.toString()).isEqualTo(ESC + "[32m");
        assertThat(ConsoleColor.RED.toString()).isEqualTo(ESC + "[31m");
        assertThat(ConsoleColor.RESET.toString()).isEqualTo(ESC + "[0m");
    }

    @Test
    void apply_wrapsTextWithColorAndReset() {
        String result = ConsoleColor.GREEN.apply("hello");

        assertThat(result).isEqualTo(ESC + "[32mhello" + ESC + "[0m");
    }

    @Test
    void apply_emptyText_stillWrapsWithCodes() {
        String result = ConsoleColor.YELLOW.apply("");

        assertThat(result).isEqualTo(ESC + "[33m" + ESC + "[0m");
    }

    @Test
    void apply_isReversibleByStrippingAnsi() {
        // Verify the wrapped text can be recovered by stripping ANSI codes —
        // useful sanity check that we don't double-encode.
        String wrapped = ConsoleColor.BRIGHT_BLUE.apply("plugin loaded");
        String stripped = wrapped.replaceAll(ESC + "\\[\\d+m", "");

        assertThat(stripped).isEqualTo("plugin loaded");
    }

    @Test
    void allColorsHaveDistinctCodes() {
        long distinct = java.util.Arrays.stream(ConsoleColor.values())
                .map(ConsoleColor::toString)
                .distinct()
                .count();

        assertThat(distinct).isEqualTo(ConsoleColor.values().length);
    }
}
