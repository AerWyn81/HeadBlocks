package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.PlaceholdersService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.CommandsUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.utils.runnables.BukkitFutureResult;
import org.bukkit.Bukkit;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private HeadService headService;

    @Mock
    private PlaceholdersService placeholdersService;

    @Mock
    private Player playerSender;

    @Mock
    private ConsoleCommandSender consoleSender;

    private Stats command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getPlaceholdersService()).thenReturn(placeholdersService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.message(anyString(), anyString())).thenReturn("mock-message");
        command = new Stats(registry);
    }

    private HeadLocation createChargedHeadLocation(UUID headUuid, String name) {
        HeadLocation hl = mock(HeadLocation.class);
        lenient().when(hl.getUuid()).thenReturn(headUuid);
        lenient().when(hl.getName()).thenReturn(name);
        Location loc = mock(Location.class);
        World world = mock(World.class);
        lenient().when(loc.getWorld()).thenReturn(world);
        lenient().when(world.getName()).thenReturn("world");
        lenient().when(loc.getX()).thenReturn(10.0);
        lenient().when(loc.getY()).thenReturn(64.0);
        lenient().when(loc.getZ()).thenReturn(20.0);
        lenient().when(hl.getLocation()).thenReturn(loc);
        return hl;
    }

    @Nested
    class PlayerNotFound {

        @Test
        void consoleWithoutPlayerArg_returnsTrue() {
            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class)) {
                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, consoleSender, new String[]{"stats"}, true))
                        .thenReturn(null);

                boolean result = command.perform(consoleSender, new String[]{"stats"});

                assertThat(result).isTrue();
            }
        }

        @Test
        void playerNotFoundInStorage_returnsTrue() {
            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class)) {
                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, playerSender, new String[]{"stats", "unknownPlayer"}, true))
                        .thenReturn(null);

                boolean result = command.perform(playerSender, new String[]{"stats", "unknownPlayer"});

                assertThat(result).isTrue();
            }
        }
    }

    @Nested
    class StorageError {

        @Test
        void getHeadsThrowsException_sendsStorageError() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "TestPlayer", "TestPlayer");

            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class)) {
                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, playerSender, new String[]{"stats", "TestPlayer"}, true))
                        .thenReturn(profile);
                when(storageService.getHeads()).thenThrow(new InternalException("db error"));

                boolean result = command.perform(playerSender, new String[]{"stats", "TestPlayer"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.StorageError");
                verify(playerSender).sendMessage("mock-message");
            }
        }
    }

    @Nested
    class EmptyHeads {

        @Test
        void noHeadsInConfig_sendsListHeadEmpty() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "TestPlayer", "TestPlayer");

            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class)) {
                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, playerSender, new String[]{"stats", "TestPlayer"}, true))
                        .thenReturn(profile);
                when(storageService.getHeads()).thenReturn(new ArrayList<>());

                boolean result = command.perform(playerSender, new String[]{"stats", "TestPlayer"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.ListHeadEmpty");
                verify(playerSender).sendMessage("mock-message");
            }
        }
    }

    @Nested
    class SuccessfulLookup {

        @Test
        void consoleSender_withHeadsFound_sendsMessages() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            UUID headUuid1 = UUID.randomUUID();
            UUID headUuid2 = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "TestPlayer", "TestPlayer");

            ArrayList<UUID> heads = new ArrayList<>(java.util.List.of(headUuid1, headUuid2));
            Set<UUID> playerHeads = new HashSet<>(java.util.List.of(headUuid1));

            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
                 MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {

                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, consoleSender, new String[]{"stats", "TestPlayer"}, true))
                        .thenReturn(profile);
                when(storageService.getHeads()).thenReturn(heads);

                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("world 10 64 20");

                HeadLocation hl1 = createChargedHeadLocation(headUuid1, "Head1");
                HeadLocation hl2 = createChargedHeadLocation(headUuid2, "Head2");

                ArrayList<HeadLocation> chargedHeads = new ArrayList<>(java.util.List.of(hl1, hl2));
                when(headService.getChargedHeadLocations()).thenReturn(chargedHeads);

                BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
                when(storageService.getHeadsPlayer(pUuid)).thenReturn(futureResult);

                doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<Set<UUID>> consumer = invocation.getArgument(0);
                    if (consumer != null) {
                        consumer.accept(playerHeads);
                    }
                    return null;
                }).when(futureResult).whenComplete(any());

                boolean result = command.perform(consoleSender, new String[]{"stats", "TestPlayer"});

                assertThat(result).isTrue();
                // Console sends lineTitle + lines for each head
                verify(consoleSender).sendMessage("mock-message"); // Chat.LineTitle
                // Two head entries: one owned, one not owned
                verify(consoleSender, atLeast(2)).sendMessage(anyString());
            }
        }

        @Test
        void consoleSender_headNotCharged_usesUuidAsName() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            UUID headUuid1 = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "TestPlayer", "TestPlayer");

            ArrayList<UUID> heads = new ArrayList<>(java.util.List.of(headUuid1));
            Set<UUID> playerHeads = new HashSet<>();

            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {

                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, consoleSender, new String[]{"stats", "TestPlayer"}, true))
                        .thenReturn(profile);
                when(storageService.getHeads()).thenReturn(heads);

                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                // No charged head locations for this UUID
                when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>());

                BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
                when(storageService.getHeadsPlayer(pUuid)).thenReturn(futureResult);

                doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<Set<UUID>> consumer = invocation.getArgument(0);
                    if (consumer != null) {
                        consumer.accept(playerHeads);
                    }
                    return null;
                }).when(futureResult).whenComplete(any());

                boolean result = command.perform(consoleSender, new String[]{"stats", "TestPlayer"});

                assertThat(result).isTrue();
                // The head name should fall back to UUID since no HeadLocation is found
                verify(consoleSender, atLeast(1)).sendMessage(contains(headUuid1.toString()));
            }
        }

        @Test
        void consoleSender_headWithEmptyName_usesUuidAsName() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            UUID headUuid1 = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "TestPlayer", "TestPlayer");

            ArrayList<UUID> heads = new ArrayList<>(java.util.List.of(headUuid1));
            Set<UUID> playerHeads = new HashSet<>();

            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
                 MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {

                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, consoleSender, new String[]{"stats", "TestPlayer"}, true))
                        .thenReturn(profile);
                when(storageService.getHeads()).thenReturn(heads);

                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("world 10 64 20");

                HeadLocation hl1 = createChargedHeadLocation(headUuid1, ""); // empty name
                when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>(java.util.List.of(hl1)));

                BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
                when(storageService.getHeadsPlayer(pUuid)).thenReturn(futureResult);

                doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<Set<UUID>> consumer = invocation.getArgument(0);
                    if (consumer != null) {
                        consumer.accept(playerHeads);
                    }
                    return null;
                }).when(futureResult).whenComplete(any());

                boolean result = command.perform(consoleSender, new String[]{"stats", "TestPlayer"});

                assertThat(result).isTrue();
                // Empty name => should use UUID
                verify(consoleSender, atLeast(1)).sendMessage(contains(headUuid1.toString()));
            }
        }

        @Test
        void playerSender_withHeadsFound_buildsRichMessages() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            UUID headUuid1 = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "TestPlayer", "TestPlayer");

            ArrayList<UUID> heads = new ArrayList<>(java.util.List.of(headUuid1));
            Set<UUID> playerHeads = new HashSet<>(java.util.List.of(headUuid1));

            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
                 MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {

                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, playerSender, new String[]{"stats", "TestPlayer"}, true))
                        .thenReturn(profile);
                when(storageService.getHeads()).thenReturn(heads);

                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("world 10 64 20");
                when(placeholdersService.parse(anyString(), any(UUID.class), anyString())).thenReturn("parsed-title");

                HeadLocation hl1 = createChargedHeadLocation(headUuid1, "Head1");

                ArrayList<HeadLocation> chargedHeads = new ArrayList<>(java.util.List.of(hl1));
                when(headService.getChargedHeadLocations()).thenReturn(chargedHeads);

                BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
                when(storageService.getHeadsPlayer(pUuid)).thenReturn(futureResult);

                doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<Set<UUID>> consumer = invocation.getArgument(0);
                    if (consumer != null) {
                        consumer.accept(playerHeads);
                    }
                    return null;
                }).when(futureResult).whenComplete(any());

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"stats", "TestPlayer"});

                assertThat(result).isTrue();
                // Player sender gets rich messages via spigot().sendMessage()
                verify(spigot, atLeastOnce()).sendMessage(any(net.md_5.bungee.api.chat.BaseComponent.class));
            }
        }

        @Test
        void playerSender_headNotOwned_showsNotOwnBox() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            UUID headUuid1 = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "TestPlayer", "TestPlayer");

            ArrayList<UUID> heads = new ArrayList<>(java.util.List.of(headUuid1));
            Set<UUID> playerHeads = new HashSet<>(); // empty = player owns nothing

            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
                 MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {

                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, playerSender, new String[]{"stats", "TestPlayer"}, true))
                        .thenReturn(profile);
                when(storageService.getHeads()).thenReturn(heads);

                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("world 10 64 20");
                when(placeholdersService.parse(anyString(), any(UUID.class), anyString())).thenReturn("parsed-title");

                HeadLocation hl1 = createChargedHeadLocation(headUuid1, "Head1");

                ArrayList<HeadLocation> chargedHeads = new ArrayList<>(java.util.List.of(hl1));
                when(headService.getChargedHeadLocations()).thenReturn(chargedHeads);

                BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
                when(storageService.getHeadsPlayer(pUuid)).thenReturn(futureResult);

                doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<Set<UUID>> consumer = invocation.getArgument(0);
                    if (consumer != null) {
                        consumer.accept(playerHeads);
                    }
                    return null;
                }).when(futureResult).whenComplete(any());

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"stats", "TestPlayer"});

                assertThat(result).isTrue();
                // Should have asked for NotOwn box message
                verify(languageService).message("Chat.Box.NotOwn");
                verify(languageService).message("Chat.Hover.NotOwn");
            }
        }

        @Test
        void playerSender_headNotOnThisServer_showsBlockedTeleport() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            UUID headUuid1 = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "TestPlayer", "TestPlayer");

            ArrayList<UUID> heads = new ArrayList<>(java.util.List.of(headUuid1));
            Set<UUID> playerHeads = new HashSet<>();

            try (MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {

                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(registry, playerSender, new String[]{"stats", "TestPlayer"}, true))
                        .thenReturn(profile);
                when(storageService.getHeads()).thenReturn(heads);

                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
                when(placeholdersService.parse(anyString(), any(UUID.class), anyString())).thenReturn("parsed-title");

                // No charged head found => head is not on this server
                when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>());

                BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
                when(storageService.getHeadsPlayer(pUuid)).thenReturn(futureResult);

                doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<Set<UUID>> consumer = invocation.getArgument(0);
                    if (consumer != null) {
                        consumer.accept(playerHeads);
                    }
                    return null;
                }).when(futureResult).whenComplete(any());

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"stats", "TestPlayer"});

                assertThat(result).isTrue();
                verify(languageService).message("Chat.Hover.HeadIsNotOnThisServer");
                verify(languageService).message("Chat.Hover.BlockedTeleport");
            }
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void secondArg_returnsOnlinePlayerNames() {
            Player p1 = mock(Player.class);
            when(p1.getName()).thenReturn("Alice");
            Player p2 = mock(Player.class);
            when(p2.getName()).thenReturn("Bob");

            @SuppressWarnings("unchecked")
            Collection<Player> onlinePlayers = (Collection<Player>) (Collection<?>) java.util.List.of(p1, p2);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getOnlinePlayers).thenReturn(onlinePlayers);

                ArrayList<String> result = command.tabComplete(playerSender, new String[]{"stats", ""});

                assertThat(result).containsExactly("Alice", "Bob");
            }
        }

        @Test
        void firstArg_returnsEmpty() {
            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"stats"});

            assertThat(result).isEmpty();
        }

        @Test
        void thirdArg_returnsEmpty() {
            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"stats", "player", "3"});

            assertThat(result).isEmpty();
        }
    }
}
