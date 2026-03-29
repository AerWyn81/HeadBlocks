package fr.aerwyn81.headblocks.utils.message.color.patterns;

import fr.aerwyn81.headblocks.utils.message.color.IridiumColorAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;

class ColorPatternsTest {

    @BeforeAll
    static void initIridiumColorAPI() {
        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            bk.when(Bukkit::getVersion).thenReturn("git-Spigot-xxx (MC: 1.20.1)");
            try {
                Class.forName("fr.aerwyn81.headblocks.utils.message.color.IridiumColorAPI");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    class GradientPatternTest {

        private final GradientPattern pattern = new GradientPattern();

        @Test
        void process_withMatchingPattern_delegatesToIridiumColorAPI() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.color(eq("Hello"), any(Color.class), any(Color.class)))
                        .thenReturn("coloredHello");

                String result = pattern.process("<GRADIENT:FF0000>Hello</GRADIENT:0000FF>");

                assertThat(result).isEqualTo("coloredHello");
                api.verify(() -> IridiumColorAPI.color(
                        eq("Hello"),
                        eq(new Color(0xFF0000)),
                        eq(new Color(0x0000FF))
                ));
            }
        }

        @Test
        void process_withNoMatchingPattern_returnsUnchanged() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "plain text without gradient";

                String result = pattern.process(input);

                assertThat(result).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }

        @Test
        void process_withEmptyString_returnsEmpty() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String result = pattern.process("");

                assertThat(result).isEmpty();
                api.verifyNoInteractions();
            }
        }

        @Test
        void process_withMultipleMatches_replacesAll() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.color(eq("Hi"), any(Color.class), any(Color.class)))
                        .thenReturn("[Hi]");
                api.when(() -> IridiumColorAPI.color(eq("World"), any(Color.class), any(Color.class)))
                        .thenReturn("[World]");

                String result = pattern.process("<GRADIENT:AA0000>Hi</GRADIENT:00AA00> <GRADIENT:BB0000>World</GRADIENT:0000BB>");

                assertThat(result).isEqualTo("[Hi] [World]");
            }
        }

        @Test
        void regex_matchesSixDigitHexCodes() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.color(anyString(), any(Color.class), any(Color.class)))
                        .thenReturn("ok");

                assertThat(pattern.process("<GRADIENT:abcdef>text</GRADIENT:ABCDEF>")).isEqualTo("ok");
                assertThat(pattern.process("<GRADIENT:123456>text</GRADIENT:789abc>")).isEqualTo("ok");
            }
        }

        @Test
        void regex_doesNotMatchInvalidHex() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "<GRADIENT:GGGGGG>text</GRADIENT:000000>";

                assertThat(pattern.process(input)).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }
    }

    @Nested
    class OldHBPatternTest {

        private final OldHBPattern pattern = new OldHBPattern();

        @Test
        void process_withMatchingPattern_delegatesToIridiumColorAPI() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                ChatColor mockColor = ChatColor.RED;
                api.when(() -> IridiumColorAPI.getColor("FF0000")).thenReturn(mockColor);

                String result = pattern.process("{#FF0000}");

                assertThat(result).isEqualTo(mockColor + "");
            }
        }

        @Test
        void process_extractsCorrectHexSubstring() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                ChatColor mockColor = ChatColor.BLUE;
                api.when(() -> IridiumColorAPI.getColor("aabbcc")).thenReturn(mockColor);

                String result = pattern.process("prefix{#aabbcc}suffix");

                assertThat(result).isEqualTo("prefix" + mockColor + "suffix");
            }
        }

        @Test
        void process_withNoMatchingPattern_returnsUnchanged() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "no old hb pattern here";

                String result = pattern.process(input);

                assertThat(result).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }

        @Test
        void process_withEmptyString_returnsEmpty() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                assertThat(pattern.process("")).isEmpty();
                api.verifyNoInteractions();
            }
        }

        @Test
        void process_withMultipleMatches_replacesAll() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.getColor("FF0000")).thenReturn(ChatColor.RED);
                api.when(() -> IridiumColorAPI.getColor("00FF00")).thenReturn(ChatColor.GREEN);

                String result = pattern.process("{#FF0000}Hello {#00FF00}World");

                assertThat(result).isEqualTo(ChatColor.RED + "Hello " + ChatColor.GREEN + "World");
            }
        }

        @Test
        void regex_doesNotMatchWithoutBraces() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "#FF0000";

                assertThat(pattern.process(input)).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }
    }

    @Nested
    class RainbowPatternTest {

        private final RainbowPattern pattern = new RainbowPattern();

        @Test
        void process_withMatchingPattern_delegatesToIridiumColorAPI() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.rainbow("Hello", 100f)).thenReturn("rainbowHello");

                String result = pattern.process("<RAINBOW100>Hello</RAINBOW>");

                assertThat(result).isEqualTo("rainbowHello");
                api.verify(() -> IridiumColorAPI.rainbow("Hello", 100f));
            }
        }

        @Test
        void process_withNoMatchingPattern_returnsUnchanged() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "no rainbow here";

                String result = pattern.process(input);

                assertThat(result).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }

        @Test
        void process_withEmptyString_returnsEmpty() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                assertThat(pattern.process("")).isEmpty();
                api.verifyNoInteractions();
            }
        }

        @Test
        void process_withMultipleMatches_replacesAll() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.rainbow("Hi", 50f)).thenReturn("[Hi]");
                api.when(() -> IridiumColorAPI.rainbow("World", 75f)).thenReturn("[World]");

                String result = pattern.process("<RAINBOW50>Hi</RAINBOW> <RAINBOW75>World</RAINBOW>");

                assertThat(result).isEqualTo("[Hi] [World]");
            }
        }

        @Test
        void regex_matchesSaturationValues1To3Digits() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.rainbow(anyString(), anyFloat())).thenReturn("ok");

                assertThat(pattern.process("<RAINBOW1>x</RAINBOW>")).isEqualTo("ok");
                assertThat(pattern.process("<RAINBOW50>x</RAINBOW>")).isEqualTo("ok");
                assertThat(pattern.process("<RAINBOW255>x</RAINBOW>")).isEqualTo("ok");
            }
        }

        @Test
        void regex_doesNotMatchFourDigitSaturation() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "<RAINBOW1234>text</RAINBOW>";

                assertThat(pattern.process(input)).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }

        @Test
        void regex_doesNotMatchWithoutSaturation() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "<RAINBOW>text</RAINBOW>";

                assertThat(pattern.process(input)).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }
    }

    @Nested
    class SolidPatternTest {

        private final SolidPattern pattern = new SolidPattern();

        @Test
        void process_withSolidTagSyntax_delegatesToIridiumColorAPI() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.getColor("FF0000")).thenReturn(ChatColor.RED);

                String result = pattern.process("<SOLID:FF0000>Hello");

                assertThat(result).isEqualTo(ChatColor.RED + "Hello");
            }
        }

        @Test
        void process_withHashBraceSyntax_delegatesToIridiumColorAPI() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.getColor("00FF00")).thenReturn(ChatColor.GREEN);

                String result = pattern.process("#{00FF00}Hello");

                assertThat(result).isEqualTo(ChatColor.GREEN + "Hello");
            }
        }

        @Test
        void process_withNoMatchingPattern_returnsUnchanged() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "plain text";

                String result = pattern.process(input);

                assertThat(result).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }

        @Test
        void process_withEmptyString_returnsEmpty() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                assertThat(pattern.process("")).isEmpty();
                api.verifyNoInteractions();
            }
        }

        @Test
        void process_withMultipleMatches_replacesAll() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.getColor("FF0000")).thenReturn(ChatColor.RED);
                api.when(() -> IridiumColorAPI.getColor("0000FF")).thenReturn(ChatColor.BLUE);

                String result = pattern.process("<SOLID:FF0000>Hello #{0000FF}World");

                assertThat(result).isEqualTo(ChatColor.RED + "Hello " + ChatColor.BLUE + "World");
            }
        }

        @Test
        void process_withMixedSyntaxes_replacesAll() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.getColor("aabbcc")).thenReturn(ChatColor.GRAY);

                String result = pattern.process("<SOLID:aabbcc>one #{aabbcc}two");

                assertThat(result).isEqualTo(ChatColor.GRAY + "one " + ChatColor.GRAY + "two");
            }
        }

        @Test
        void regex_doesNotMatchInvalidHex() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                String input = "<SOLID:ZZZZZZ>text";

                assertThat(pattern.process(input)).isEqualTo(input);
                api.verifyNoInteractions();
            }
        }

        @Test
        void regex_solidSyntaxIsCaseInsensitiveForHex() {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.getColor(anyString())).thenReturn(ChatColor.WHITE);

                pattern.process("<SOLID:aAbBcC>text");

                api.verify(() -> IridiumColorAPI.getColor("aAbBcC"));
            }
        }
    }
}
