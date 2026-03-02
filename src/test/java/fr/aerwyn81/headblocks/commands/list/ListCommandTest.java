package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private LanguageService languageService;

    @Mock
    private Player playerSender;

    @Mock
    private ConsoleCommandSender consoleSender;

    private List command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.message(anyString(), anyString())).thenReturn("mock-message");
        command = new List(registry);
    }

    @Nested
    class EmptyHeads {

        @Test
        void noHeadLocations_sendsListHeadEmpty() {
            when(headService.getHeadLocations()).thenReturn(new ArrayList<>());

            boolean result = command.perform(playerSender, new String[]{"list"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.ListHeadEmpty");
            verify(playerSender).sendMessage("mock-message");
        }

        @Test
        void noHeadLocations_consoleSender_sendsListHeadEmpty() {
            when(headService.getHeadLocations()).thenReturn(new ArrayList<>());

            boolean result = command.perform(consoleSender, new String[]{"list"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.ListHeadEmpty");
            verify(consoleSender).sendMessage("mock-message");
        }
    }

    @Nested
    class ConsoleSender {

        @Test
        void singleChargedHead_sendsColorizedName() {
            HeadLocation hl = mock(HeadLocation.class);
            when(hl.isCharged()).thenReturn(true);
            when(hl.getNameOrUuid()).thenReturn("TestHead");
            ArrayList<HeadLocation> headLocations = new ArrayList<>(java.util.List.of(hl));
            when(headService.getHeadLocations()).thenReturn(headLocations);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"list"});

                assertThat(result).isTrue();
                // Console sender gets the title message
                verify(consoleSender).sendMessage("mock-message");
                // Console sender gets the head name with color code
                verify(consoleSender).sendMessage("&6TestHead");
            }
        }

        @Test
        void singleUnchargedHead_sendsStrikethroughName() {
            HeadLocation hl = mock(HeadLocation.class);
            when(hl.isCharged()).thenReturn(false);
            when(hl.getNameOrUuid()).thenReturn("BrokenHead");
            ArrayList<HeadLocation> headLocations = new ArrayList<>(java.util.List.of(hl));
            when(headService.getHeadLocations()).thenReturn(headLocations);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"list"});

                assertThat(result).isTrue();
                verify(consoleSender).sendMessage("&c&oBrokenHead");
            }
        }

        @Test
        void multipleHeads_allDisplayed() {
            HeadLocation hl1 = mock(HeadLocation.class);
            when(hl1.isCharged()).thenReturn(true);
            when(hl1.getNameOrUuid()).thenReturn("Head1");

            HeadLocation hl2 = mock(HeadLocation.class);
            when(hl2.isCharged()).thenReturn(true);
            when(hl2.getNameOrUuid()).thenReturn("Head2");

            HeadLocation hl3 = mock(HeadLocation.class);
            when(hl3.isCharged()).thenReturn(false);
            when(hl3.getNameOrUuid()).thenReturn("Head3");

            ArrayList<HeadLocation> headLocations = new ArrayList<>(java.util.List.of(hl1, hl2, hl3));
            when(headService.getHeadLocations()).thenReturn(headLocations);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"list"});

                assertThat(result).isTrue();
                // Title + 3 heads
                verify(consoleSender, times(4)).sendMessage(anyString());
            }
        }
    }

    @Nested
    class PlayerSenderTests {

        @Test
        void singleChargedHead_sendsRichMessage() {
            HeadLocation hl = mock(HeadLocation.class);
            when(hl.isCharged()).thenReturn(true);
            when(hl.getNameOrUuid()).thenReturn("TestHead");
            UUID headUuid = UUID.randomUUID();
            when(hl.getUuid()).thenReturn(headUuid);
            Location loc = mock(Location.class);
            World world = mock(World.class);
            when(loc.getWorld()).thenReturn(world);
            when(world.getName()).thenReturn("world");
            when(loc.getX()).thenReturn(10.0);
            when(loc.getY()).thenReturn(64.0);
            when(loc.getZ()).thenReturn(20.0);
            when(hl.getLocation()).thenReturn(loc);

            ArrayList<HeadLocation> headLocations = new ArrayList<>(java.util.List.of(hl));
            when(headService.getHeadLocations()).thenReturn(headLocations);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
                 MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("world 10 64 20");

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"list"});

                assertThat(result).isTrue();
                // Player sender gets rich messages via spigot
                verify(spigot, atLeastOnce()).sendMessage(any(net.md_5.bungee.api.chat.BaseComponent.class));
                // Should request Remove and Teleport messages
                verify(languageService).message("Chat.Box.Remove");
                verify(languageService).message("Chat.Box.Teleport");
            }
        }

        @Test
        void chargedHead_nullWorld_noClickEvent() {
            HeadLocation hl = mock(HeadLocation.class);
            when(hl.isCharged()).thenReturn(true);
            when(hl.getNameOrUuid()).thenReturn("TestHead");
            Location loc = mock(Location.class);
            when(loc.getWorld()).thenReturn(null); // null world
            when(hl.getLocation()).thenReturn(loc);

            ArrayList<HeadLocation> headLocations = new ArrayList<>(java.util.List.of(hl));
            when(headService.getHeadLocations()).thenReturn(headLocations);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
                 MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("null 10 64 20");

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"list"});

                assertThat(result).isTrue();
                verify(spigot, atLeastOnce()).sendMessage(any(net.md_5.bungee.api.chat.BaseComponent.class));
            }
        }

        @Test
        void unchargedHead_showsWorldNotFoundHover() {
            HeadLocation hl = mock(HeadLocation.class);
            when(hl.isCharged()).thenReturn(false);
            when(hl.getNameOrUuid()).thenReturn("BrokenHead");
            when(hl.getConfigWorldName()).thenReturn("deleted_world");

            ArrayList<HeadLocation> headLocations = new ArrayList<>(java.util.List.of(hl));
            when(headService.getHeadLocations()).thenReturn(headLocations);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"list"});

                assertThat(result).isTrue();
                verify(languageService).message("Chat.LineWorldNotFound");
                verify(spigot, atLeastOnce()).sendMessage(any(net.md_5.bungee.api.chat.BaseComponent.class));
            }
        }
    }

    @Nested
    class Pagination {

        @Test
        void secondArg_asPageNumber_paginatesCorrectly() {
            // Create enough heads that console would see all of them
            // (console has Integer.MAX_VALUE pageHeight so all will show)
            HeadLocation hl1 = mock(HeadLocation.class);
            when(hl1.isCharged()).thenReturn(true);
            when(hl1.getNameOrUuid()).thenReturn("Head1");
            HeadLocation hl2 = mock(HeadLocation.class);
            when(hl2.isCharged()).thenReturn(true);
            when(hl2.getNameOrUuid()).thenReturn("Head2");

            ArrayList<HeadLocation> headLocations = new ArrayList<>(java.util.List.of(hl1, hl2));
            when(headService.getHeadLocations()).thenReturn(headLocations);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"list", "1"});

                assertThat(result).isTrue();
                verify(consoleSender, times(3)).sendMessage(anyString()); // title + 2 heads
            }
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void anyArgs_returnsEmptyList() {
            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"list"});

            assertThat(result).isEmpty();
        }

        @Test
        void withSecondArg_returnsEmptyList() {
            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"list", ""});

            assertThat(result).isEmpty();
        }
    }
}
