package fr.aerwyn81.headblocks.utils.chat;

import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatPageUtilsTest {

    @Mock
    LanguageService languageService;

    @Mock
    Player player;

    @Mock
    ConsoleCommandSender consoleSender;

    private ChatPageUtils createForPlayer(int size, String... args) {
        var utils = new ChatPageUtils(player, languageService);
        utils.currentPage(args.length > 0 ? args : new String[]{"cmd"});
        utils.entriesCount(size);
        return utils;
    }

    private ChatPageUtils createForConsole(int size) {
        var utils = new ChatPageUtils(consoleSender, languageService);
        utils.currentPage(new String[]{"cmd"});
        utils.entriesCount(size);
        return utils;
    }

    @Nested
    class ConstructorTests {

        @Test
        void playerSender_hasPageHeight8() {
            var utils = new ChatPageUtils(player, languageService);
            assertThat(utils.getPageHeight()).isEqualTo(8);
        }

        @Test
        void consoleSender_hasMaxPageHeight() {
            var utils = new ChatPageUtils(consoleSender, languageService);
            assertThat(utils.getPageHeight()).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @Nested
    class FirstPosTests {

        @Test
        void firstPage_returnsZero() {
            var utils = createForPlayer(20, "cmd");
            assertThat(utils.getFirstPos()).isEqualTo(0);
        }

        @Test
        void secondPage_returnsPageHeight() {
            var utils = createForPlayer(20, "cmd", "2");
            assertThat(utils.getFirstPos()).isEqualTo(8);
        }

        @Test
        void thirdPage_returnsTwicePageHeight() {
            var utils = createForPlayer(30, "cmd", "3");
            assertThat(utils.getFirstPos()).isEqualTo(16);
        }
    }

    @Nested
    class CurrentPageTests {

        @Test
        void singleArg_defaultsToPage1() {
            var utils = new ChatPageUtils(player, languageService);
            utils.currentPage(new String[]{"cmd"});
            utils.entriesCount(20);
            assertThat(utils.getFirstPos()).isEqualTo(0);
        }

        @Test
        void validPageNumber_isUsed() {
            var utils = createForPlayer(30, "cmd", "3");
            assertThat(utils.getFirstPos()).isEqualTo(16);
        }

        @Test
        void negativePageNumber_clampedTo1() {
            var utils = createForPlayer(20, "cmd", "-1");
            assertThat(utils.getFirstPos()).isEqualTo(0);
        }

        @Test
        void zeroPageNumber_clampedTo1() {
            var utils = createForPlayer(20, "cmd", "0");
            assertThat(utils.getFirstPos()).isEqualTo(0);
        }

        @Test
        void nonNumericArg_defaultsToPage1() {
            var utils = createForPlayer(20, "cmd", "abc");
            assertThat(utils.getFirstPos()).isEqualTo(0);
        }

        @Test
        void pageExceedingTotal_clampedToLastPage() {
            var utils = createForPlayer(10, "cmd", "999");
            // 10 items, pageHeight=8 => totalPage=2, so clamped to page 2
            assertThat(utils.getFirstPos()).isEqualTo(8);
        }
    }

    @Nested
    class EntriesCountTests {

        @Test
        void emptyList_sizeIsZero() {
            var utils = createForPlayer(0, "cmd");
            assertThat(utils.getSize()).isEqualTo(0);
        }

        @Test
        void singleItem_sizeIsOne() {
            var utils = createForPlayer(1, "cmd");
            assertThat(utils.getSize()).isEqualTo(1);
        }

        @Test
        void exactPageHeight_onePage() {
            var utils = createForPlayer(8, "cmd");
            // 8 items / 8 pageHeight = 1 page, firstPos of page 1 = 0
            assertThat(utils.getFirstPos()).isEqualTo(0);
            assertThat(utils.getSize()).isEqualTo(8);
        }

        @Test
        void moreThanPageHeight_multiplePages() {
            var utils = createForPlayer(20, "cmd", "2");
            assertThat(utils.getFirstPos()).isEqualTo(8);
            assertThat(utils.getSize()).isEqualTo(20);
        }
    }

    @Nested
    class ConsoleTests {

        @Test
        void consoleSender_allItemsFitOnOnePage() {
            var utils = createForConsole(1000);
            assertThat(utils.getFirstPos()).isEqualTo(0);
            assertThat(utils.getPageHeight()).isEqualTo(Integer.MAX_VALUE);
        }
    }
}
