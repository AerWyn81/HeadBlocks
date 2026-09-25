package fr.aerwyn81.headblocks.hooks.visual;

import be.seeseemelk.mockbukkit.MockBukkit;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ItemProvidersTest {

    private final ItemStack chair = new ItemStack(Material.PAPER);

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void itemsAdder_readsItsRegistry() {
        var stack = mock(CustomStack.class);
        when(stack.getItemStack()).thenReturn(chair);

        try (MockedStatic<CustomStack> customStack = mockStatic(CustomStack.class);
             MockedStatic<ItemsAdder> itemsAdder = mockStatic(ItemsAdder.class)) {
            customStack.when(() -> CustomStack.isInRegistry("ns:chair")).thenReturn(true);
            customStack.when(() -> CustomStack.getInstance("ns:chair")).thenReturn(stack);
            itemsAdder.when(ItemsAdder::areItemsLoaded).thenReturn(true);
            var hook = new ItemsAdderHook();

            assertThat(hook.item("ns:chair")).isSameAs(chair);
            assertThat(hook.item("ns:table")).isNull();
            assertThat(hook.exists("ns:chair")).isTrue();
            assertThat(hook.isReady()).isTrue();
        }
    }

    @Test
    void prefixesMatchTheirPlugin() {
        assertThat(new NexoHook().prefix()).isEqualTo("nexo");
        assertThat(new NexoHook().pluginName()).isEqualTo("Nexo");
        assertThat(new OraxenHook().prefix()).isEqualTo("oraxen");
        assertThat(new OraxenHook().pluginName()).isEqualTo("Oraxen");
        assertThat(new ItemsAdderHook().prefix()).isEqualTo("itemsadder");
        assertThat(new ItemsAdderHook().pluginName()).isEqualTo("ItemsAdder");
    }
}
