package fr.aerwyn81.headblocks.hooks.visual;

import be.seeseemelk.mockbukkit.MockBukkit;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemProviderHookTest {

    private final NamespacedKey providerKey = new NamespacedKey("nexo", "id");

    private ItemStack chair;
    private FakeItemHook hook;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        chair = new ItemStack(Material.PAPER);
        var meta = chair.getItemMeta();
        meta.setCustomModelData(12);
        meta.getPersistentDataContainer().set(providerKey, PersistentDataType.STRING, "chair");
        chair.setItemMeta(meta);
        hook = new FakeItemHook(Map.of("chair", chair));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void icon_keepsTheLookButDropsTheProviderData() {
        var icon = hook.icon(HeadContent.external("fake", "chair", null));

        assertThat(icon.getItemMeta().getCustomModelData()).isEqualTo(12);
        assertThat(icon.getItemMeta().getPersistentDataContainer().getKeys()).isEmpty();
        assertThat(chair.getItemMeta().getPersistentDataContainer().has(providerKey, PersistentDataType.STRING)).isTrue();
    }

    @Test
    void icon_ofAnUnknownItem_isABarrier() {
        assertThat(hook.icon(HeadContent.external("fake", "table", null)).getType()).isEqualTo(Material.BARRIER);
    }

    @Test
    void exists_asksTheProvider() {
        assertThat(hook.exists("chair")).isTrue();
        assertThat(hook.exists("table")).isFalse();
    }

    @Test
    void spawn_displaysTheProviderItem() {
        World world = mock(World.class);
        Location anchor = mock(Location.class);
        ItemDisplay display = mock(ItemDisplay.class);
        when(anchor.getWorld()).thenReturn(world);
        when(world.spawn(anchor, ItemDisplay.class)).thenReturn(display);
        when(world.spawn(anchor, Interaction.class)).thenReturn(mock(Interaction.class));

        hook.spawn(anchor, HeadContent.external("fake", "chair", null), new RenderSettings(1, false, 0));

        verify(display).setItemStack(chair);
    }

    @Test
    void spawn_unknownItem_failsBeforeSpawningAnything() {
        World world = mock(World.class);
        Location anchor = mock(Location.class);
        when(anchor.getWorld()).thenReturn(world);

        assertThatThrownBy(() -> hook.spawn(anchor, HeadContent.external("fake", "table", null), new RenderSettings(1, false, 0)))
                .hasMessageContaining("unknown Fake item table");
        verify(world, never()).spawn(any(Location.class), any(Class.class));
    }

    static class FakeItemHook extends ItemProviderHook {
        private final Map<String, ItemStack> items;

        FakeItemHook(Map<String, ItemStack> items) {
            this.items = items;
        }

        @Override
        public String prefix() {
            return "fake";
        }

        @Override
        public String pluginName() {
            return "Fake";
        }

        @Override
        protected ItemStack item(String id) {
            return items.get(id);
        }
    }
}
