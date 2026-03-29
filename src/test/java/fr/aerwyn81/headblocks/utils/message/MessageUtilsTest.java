package fr.aerwyn81.headblocks.utils.message;

import fr.aerwyn81.headblocks.utils.message.color.IridiumColorAPI;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class MessageUtilsTest {

    @BeforeAll
    static void initIridiumColorAPI() {
        // IridiumColorAPI has a static initializer that calls Bukkit.getVersion().
        // Force class loading with a mocked Bukkit so the initializer succeeds.
        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(Bukkit::getVersion).thenReturn("git-Spigot-xxx (MC: 1.20.1)");
            try {
                Class.forName("fr.aerwyn81.headblocks.utils.message.color.IridiumColorAPI");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void colorize_delegatesToIridiumColorAPI() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process("&aHello")).thenReturn("§aHello");

            assertThat(MessageUtils.colorize("&aHello")).isEqualTo("§aHello");
        }
    }

    @Test
    void unColorize_delegatesToStripColorFormatting() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.stripColorFormatting("§aHello")).thenReturn("Hello");

            assertThat(MessageUtils.unColorize("§aHello")).isEqualTo("Hello");
        }
    }

    @Test
    void createProgressBar_50percent_halfCompleted() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = MessageUtils.createProgressBar(5, 10, 10, "|", "&a", "&7");
            assertThat(result).isEqualTo("&a|&a|&a|&a|&a|&7|&7|&7|&7|&7|");
        }
    }

    @Test
    void createProgressBar_0percent_allNotCompleted() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = MessageUtils.createProgressBar(0, 10, 10, "#", "&a", "&c");
            assertThat(result).isEqualTo("&c#&c#&c#&c#&c#&c#&c#&c#&c#&c#");
        }
    }

    @Test
    void createProgressBar_100percent_allCompleted() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = MessageUtils.createProgressBar(10, 10, 10, "|", "&a", "&7");
            assertThat(result).isEqualTo("&a|&a|&a|&a|&a|&a|&a|&a|&a|&a|");
        }
    }

    @Test
    void createProgressBar_partialProgress_roundsDown() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            // 1/3 = 33.3% of 10 bars = 3.33 → truncated to 3
            String result = MessageUtils.createProgressBar(1, 3, 10, "|", "&a", "&7");
            assertThat(result).isEqualTo("&a|&a|&a|&7|&7|&7|&7|&7|&7|&7|");
        }
    }

    @Test
    void centerMessage_withoutCenterTag_returnsUnchanged() {
        String message = "Hello World";
        assertThat(MessageUtils.centerMessage(message)).isEqualTo("Hello World");
    }

    @Test
    void centerMessage_withCenterTag_stripsTagAndPadsSpaces() {
        String result = MessageUtils.centerMessage("{center}Hi");
        assertThat(result).doesNotContain("{center}");
        assertThat(result).contains("Hi");
        assertThat(result.trim()).isEqualTo("Hi");
    }

    @Test
    void centerMessage_multipleCenterTags_allStripped() {
        String result = MessageUtils.centerMessage("{center}A{center}B");
        assertThat(result).doesNotContain("{center}");
        assertThat(result).contains("AB");
    }

    @Test
    void sendCenteredString_shortMessage_paddedWithSpaces() {
        String result = MessageUtils.sendCenteredString("Hi");
        assertThat(result).startsWith(" ");
        assertThat(result).contains("Hi");
    }

    @Test
    void sendCenteredString_emptyString_returnsSpacesAndNewline() {
        String result = MessageUtils.sendCenteredString("");
        assertThat(result).endsWith("\n");
    }

    @Test
    void sendCenteredString_withColorCodes_ignoresColorCodesInCalculation() {
        String result = MessageUtils.sendCenteredString("§aHi");
        String resultNoColor = MessageUtils.sendCenteredString("Hi");
        // Both should have the same centering since §a is ignored for pixel calc
        assertThat(result.indexOf("§aHi")).isEqualTo(resultNoColor.indexOf("Hi"));
    }

    @Test
    void sendCenteredString_boldText_usesWiderPixelSize() {
        String normalResult = MessageUtils.sendCenteredString("ABC");
        String boldResult = MessageUtils.sendCenteredString("§lABC");
        int normalSpaces = normalResult.indexOf('A');
        int boldSpaces = boldResult.indexOf('A');
        // §l = 2 chars prefix, so actual space offset = indexOf('A') - 2
        assertThat(boldSpaces - 2).isLessThan(normalSpaces);
    }

    @Test
    void sendCenteredString_multiline_centersEachLine() {
        String result = MessageUtils.sendCenteredString("Hi\nHi");
        String[] lines = result.split("\n");
        assertThat(lines).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void getRandomColors_returnsBetween1And3Colors() {
        for (int i = 0; i < 20; i++) {
            var colors = MessageUtils.getRandomColors();
            assertThat(colors).hasSizeBetween(1, 3);
        }
    }

    @Test
    void createProgressBar_maxIsZero_allNotCompleted() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = MessageUtils.createProgressBar(5, 0, 10, "#", "&a", "&c");

            assertThat(result).isEqualTo("&c#&c#&c#&c#&c#&c#&c#&c#&c#&c#");
        }
    }

    @Test
    void createProgressBar_currentExceedsMax_clampedToFull() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = MessageUtils.createProgressBar(15, 10, 10, "|", "&a", "&7");

            // Math.min clamps progressBars to totalBars
            assertThat(result).isEqualTo("&a|&a|&a|&a|&a|&a|&a|&a|&a|&a|");
        }
    }

    @Test
    void createProgressBar_currentIsZero_allNotCompleted() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = MessageUtils.createProgressBar(0, 5, 8, "X", "&2", "&4");

            assertThat(result).isEqualTo("&4X&4X&4X&4X&4X&4X&4X&4X");
        }
    }

    @Test
    void createProgressBar_currentEqualsMax_allCompleted() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = MessageUtils.createProgressBar(7, 7, 6, "=", "&b", "&8");

            assertThat(result).isEqualTo("&b=&b=&b=&b=&b=&b=");
        }
    }

    @Test
    void createProgressBar_negativeMax_treatedAsAllNotCompleted() {
        try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
            api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = MessageUtils.createProgressBar(3, -5, 4, "o", "&a", "&c");

            assertThat(result).isEqualTo("&co&co&co&co");
        }
    }

    @Test
    void centerMessage_plainTextWithoutTag_returnsExactSameString() {
        String input = "No centering here";

        assertThat(MessageUtils.centerMessage(input)).isSameAs(input);
    }

    @Test
    void centerMessage_emptyString_returnsEmptyString() {
        assertThat(MessageUtils.centerMessage("")).isEqualTo("");
    }

    @Test
    void centerMessage_onlyCenterTag_returnsSpacePaddedEmpty() {
        String result = MessageUtils.centerMessage("{center}");

        assertThat(result).doesNotContain("{center}");
        assertThat(result.trim()).isEmpty();
    }

    @Test
    void getRandomColors_colorsAreValidBukkitColors() {
        for (int i = 0; i < 10; i++) {
            var colors = MessageUtils.getRandomColors();
            for (var color : colors) {
                assertThat(color.getRed()).isBetween(0, 255);
                assertThat(color.getGreen()).isBetween(0, 255);
                assertThat(color.getBlue()).isBetween(0, 255);
            }
        }
    }

    @Test
    void sendCenteredString_multilineString_eachLineEndedWithNewline() {
        String result = MessageUtils.sendCenteredString("Line1\nLine2\nLine3");
        String[] lines = result.split("\n", -1);

        // Each centered line gets a trailing \n, so split produces at least 3 non-empty entries
        int nonEmpty = 0;
        for (String line : lines) {
            if (!line.isEmpty()) {
                nonEmpty++;
            }
        }
        assertThat(nonEmpty).isGreaterThanOrEqualTo(3);
    }

    @Test
    void sendCenteredString_veryLongLine_wrapsIntoMultipleLines() {
        // Build a line that exceeds the 154px center width so the wrap logic triggers
        String longLine = "ABCDEFGHIJ ".repeat(20).trim();

        String result = MessageUtils.sendCenteredString(longLine);
        String[] lines = result.split("\n");

        assertThat(lines.length).isGreaterThan(1);
    }

    @Test
    void sendCenteredString_singleCharacter_paddedAndEndedWithNewline() {
        String result = MessageUtils.sendCenteredString("A");

        assertThat(result).endsWith("\n");
        assertThat(result).contains("A");
        assertThat(result.indexOf('A')).isGreaterThan(0);
    }
}
