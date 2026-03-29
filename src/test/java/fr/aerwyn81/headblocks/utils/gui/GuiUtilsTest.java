package fr.aerwyn81.headblocks.utils.gui;

import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import fr.aerwyn81.headblocks.utils.message.color.IridiumColorAPI;
import fr.aerwyn81.headblocks.utils.runnables.BukkitFutureResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuiUtilsTest {

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
    class HBMenuTests {

        @Mock
        JavaPlugin owner;

        @Mock
        GuiService guiService;

        private HBMenu createMenu(String name, int rowsPerPage) {
            try (MockedStatic<IridiumColorAPI> api = mockStatic(IridiumColorAPI.class)) {
                api.when(() -> IridiumColorAPI.process(anyString())).thenAnswer(inv -> inv.getArgument(0));
                return new HBMenu(owner, guiService, name, false, rowsPerPage);
            }
        }

        @Nested
        class PageSizeTests {

            @Test
            void oneRow_returns9() {
                var menu = createMenu("test", 1);
                assertThat(menu.getPageSize()).isEqualTo(9);
            }

            @Test
            void threeRows_returns27() {
                var menu = createMenu("test", 3);
                assertThat(menu.getPageSize()).isEqualTo(27);
            }

            @Test
            void sixRows_returns54() {
                var menu = createMenu("test", 6);
                assertThat(menu.getPageSize()).isEqualTo(54);
            }
        }

        @Nested
        class MaxPageTests {

            @Test
            void noItems_returnsOne() {
                var menu = createMenu("test", 3);
                assertThat(menu.getMaxPage()).isEqualTo(1);
            }

            @Test
            void itemsWithinOnePage_returnsOne() {
                var menu = createMenu("test", 3);
                var item = new ItemGUI(mock(ItemStack.class));
                menu.addItem(0, item);
                assertThat(menu.getMaxPage()).isEqualTo(1);
            }

            @Test
            void itemsSpanningTwoPages() {
                var menu = createMenu("test", 1);
                var item = mock(ItemStack.class);
                menu.addItem(0, new ItemGUI(item));
                menu.addItem(10, new ItemGUI(item));
                // pageSize=9, highestSlot=10 => ceil(11/9) = 2
                assertThat(menu.getMaxPage()).isEqualTo(2);
            }

            @Test
            void itemsExactlyFillOnePage() {
                var menu = createMenu("test", 1);
                var item = mock(ItemStack.class);
                for (int i = 0; i < 9; i++) {
                    menu.addItem(i, new ItemGUI(item));
                }
                // highestSlot=8 => ceil(9/9) = 1
                assertThat(menu.getMaxPage()).isEqualTo(1);
            }
        }

        @Nested
        class HighestFilledSlotTests {

            @Test
            void noItems_returnsZero() {
                var menu = createMenu("test", 3);
                assertThat(menu.getHighestFilledSlot()).isEqualTo(0);
            }

            @Test
            void singleItemAtSlot5() {
                var menu = createMenu("test", 3);
                menu.addItem(5, new ItemGUI(mock(ItemStack.class)));
                assertThat(menu.getHighestFilledSlot()).isEqualTo(5);
            }

            @Test
            void multipleItems_returnsHighest() {
                var menu = createMenu("test", 3);
                var item = mock(ItemStack.class);
                menu.addItem(2, new ItemGUI(item));
                menu.addItem(15, new ItemGUI(item));
                menu.addItem(7, new ItemGUI(item));
                assertThat(menu.getHighestFilledSlot()).isEqualTo(15);
            }
        }

        @Nested
        class PageNavigationTests {

            @Test
            void nextPage_incrementsWhenNotOnLastPage() {
                var menu = createMenu("test", 1);
                var item = mock(ItemStack.class);
                menu.addItem(0, new ItemGUI(item));
                menu.addItem(10, new ItemGUI(item));
                // maxPage=2, currentPage=0

                var viewer = mock(HumanEntity.class);
                var invView = mock(InventoryView.class);
                when(viewer.getOpenInventory()).thenReturn(invView);
                var topInv = mock(org.bukkit.inventory.Inventory.class);
                when(invView.getTopInventory()).thenReturn(topInv);
                when(topInv.getHolder()).thenReturn(null);

                menu.nextPage(viewer);
                assertThat(menu.getCurrentPage()).isEqualTo(1);
            }

            @Test
            void nextPage_doesNotExceedMaxPage() {
                var menu = createMenu("test", 1);
                var item = mock(ItemStack.class);
                menu.addItem(0, new ItemGUI(item));
                menu.addItem(10, new ItemGUI(item));
                menu.setCurrentPage(1);
                // maxPage=2, currentPage=1 (last page)

                var viewer = mock(HumanEntity.class);
                menu.nextPage(viewer);
                assertThat(menu.getCurrentPage()).isEqualTo(1);
            }

            @Test
            void previousPage_decrementsWhenNotOnFirstPage() {
                var menu = createMenu("test", 1);
                var item = mock(ItemStack.class);
                menu.addItem(0, new ItemGUI(item));
                menu.addItem(10, new ItemGUI(item));
                menu.setCurrentPage(1);

                var viewer = mock(HumanEntity.class);
                var invView = mock(InventoryView.class);
                when(viewer.getOpenInventory()).thenReturn(invView);
                var topInv = mock(org.bukkit.inventory.Inventory.class);
                when(invView.getTopInventory()).thenReturn(topInv);
                when(topInv.getHolder()).thenReturn(null);

                menu.previousPage(viewer);
                assertThat(menu.getCurrentPage()).isEqualTo(0);
            }

            @Test
            void previousPage_doesNotGoBelowZero() {
                var menu = createMenu("test", 1);
                menu.setCurrentPage(0);

                var viewer = mock(HumanEntity.class);
                menu.previousPage(viewer);
                assertThat(menu.getCurrentPage()).isEqualTo(0);
            }
        }

        @Nested
        class SetItemAndGetItemTests {

            @Test
            void setItem_andGetItem_roundTrip() {
                var menu = createMenu("test", 3);
                var item = new ItemGUI(mock(ItemStack.class));
                menu.setItem(0, 5, item);
                assertThat(menu.getItem(0, 5)).isSameAs(item);
            }

            @Test
            void getItem_negativeSlot_returnsNull() {
                var menu = createMenu("test", 3);
                assertThat(menu.getItem(-1)).isNull();
            }

            @Test
            void removeItem_removesEntry() {
                var menu = createMenu("test", 3);
                var item = new ItemGUI(mock(ItemStack.class));
                menu.addItem(3, item);
                menu.removeItem(3);
                assertThat(menu.getItem(3)).isNull();
            }
        }
    }

    @Nested
    class ItemGUITests {

        @Mock
        ItemStack icon;

        @Mock
        ItemStack blockedIcon;

        @Test
        void constructor_setsIconAndDefaultClickable() {
            var item = new ItemGUI(icon);
            assertThat(item.getIcon()).isSameAs(icon);
            assertThat(item.isClickable()).isTrue();
        }

        @Test
        void constructor_withClickableFalse() {
            var item = new ItemGUI(icon, false);
            assertThat(item.isClickable()).isFalse();
        }

        @Test
        void setIcon_updatesIcon() {
            var item = new ItemGUI(icon);
            var newIcon = mock(ItemStack.class);
            item.setIcon(newIcon);
            assertThat(item.getIcon()).isSameAs(newIcon);
        }

        @Test
        void iconBlocked_getterSetter() {
            var item = new ItemGUI(icon);
            assertThat(item.getIconBlocked()).isNull();
            item.setIconBlocked(blockedIcon);
            assertThat(item.getIconBlocked()).isSameAs(blockedIcon);
        }

        @Test
        void setOnClickEvent_setsHandler() {
            var item = new ItemGUI(icon);
            Consumer<InventoryClickEvent> handler = e -> {
            };
            item.setOnClickEvent(handler);
            assertThat(item.getOnClickEvent()).isSameAs(handler);
        }

        @Test
        void addOnClickEvent_returnsThis() {
            var item = new ItemGUI(icon);
            Consumer<InventoryClickEvent> handler = e -> {
            };
            var result = item.addOnClickEvent(handler);
            assertThat(result).isSameAs(item);
            assertThat(item.getOnClickEvent()).isSameAs(handler);
        }

        @Test
        void onClickEvent_initiallyNull() {
            var item = new ItemGUI(icon);
            assertThat(item.getOnClickEvent()).isNull();
        }
    }

    @Nested
    class HBPaginationButtonTypeTests {

        @Test
        void getSlot_returnsCorrectValues() {
            assertThat(HBPaginationButtonType.BACK_BUTTON.getSlot()).isEqualTo(0);
            assertThat(HBPaginationButtonType.PREV_BUTTON.getSlot()).isEqualTo(3);
            assertThat(HBPaginationButtonType.CURRENT_BUTTON.getSlot()).isEqualTo(4);
            assertThat(HBPaginationButtonType.NEXT_BUTTON.getSlot()).isEqualTo(5);
            assertThat(HBPaginationButtonType.CLOSE_BUTTON.getSlot()).isEqualTo(8);
            assertThat(HBPaginationButtonType.UNASSIGNED.getSlot()).isEqualTo(1);
        }

        @Test
        void forSlot_knownSlots() {
            assertThat(HBPaginationButtonType.forSlot(0)).isEqualTo(HBPaginationButtonType.BACK_BUTTON);
            assertThat(HBPaginationButtonType.forSlot(3)).isEqualTo(HBPaginationButtonType.PREV_BUTTON);
            assertThat(HBPaginationButtonType.forSlot(4)).isEqualTo(HBPaginationButtonType.CURRENT_BUTTON);
            assertThat(HBPaginationButtonType.forSlot(5)).isEqualTo(HBPaginationButtonType.NEXT_BUTTON);
            assertThat(HBPaginationButtonType.forSlot(8)).isEqualTo(HBPaginationButtonType.CLOSE_BUTTON);
        }

        @Test
        void forSlot_unknownSlot_returnsUnassigned() {
            assertThat(HBPaginationButtonType.forSlot(7)).isEqualTo(HBPaginationButtonType.UNASSIGNED);
            assertThat(HBPaginationButtonType.forSlot(99)).isEqualTo(HBPaginationButtonType.UNASSIGNED);
        }

        @Test
        void forSlot_slot1_returnsUnassigned() {
            assertThat(HBPaginationButtonType.forSlot(1)).isEqualTo(HBPaginationButtonType.UNASSIGNED);
        }
    }

    @Nested
    class BukkitFutureResultTests {

        @Mock
        org.bukkit.plugin.Plugin plugin;

        @Test
        void of_createsInstance() {
            var future = CompletableFuture.completedFuture("hello");
            var result = BukkitFutureResult.of(plugin, future);
            assertThat(result).isNotNull();
        }

        @Test
        void join_returnsCompletedValue() {
            var future = CompletableFuture.completedFuture(42);
            var result = BukkitFutureResult.of(plugin, future);
            assertThat(result.join()).isEqualTo(42);
        }

        @Test
        void join_returnsNullForNullFuture() {
            var future = CompletableFuture.<String>completedFuture(null);
            var result = BukkitFutureResult.of(plugin, future);
            assertThat(result.join()).isNull();
        }

        @Test
        void asFuture_returnsCompletableFuture() {
            var future = CompletableFuture.completedFuture("value");
            var result = BukkitFutureResult.of(plugin, future);
            var returned = result.asFuture();
            assertThat(returned).isCompletedWithValue("value");
        }

        @Test
        void asFuture_isNotSameInstance() {
            var future = CompletableFuture.completedFuture("value");
            var result = BukkitFutureResult.of(plugin, future);
            assertThat(result.asFuture()).isNotSameAs(future);
        }
    }
}
