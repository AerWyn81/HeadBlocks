package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HandleProviderHookTest {

    private final HeadContent content = HeadContent.external("fake", "model", null);
    private final RenderSettings settings = new RenderSettings(1.0, false, 90f);

    private World world;
    private Location anchor;
    private Interaction base;
    private FakeHook hook;

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        anchor = mock(Location.class);
        base = mock(Interaction.class);
        when(anchor.getWorld()).thenReturn(world);
        lenient().when(world.spawn(anchor, Interaction.class)).thenReturn(base);
        lenient().when(base.getUniqueId()).thenReturn(UUID.randomUUID());
        hook = new FakeHook();
    }

    @Test
    void spawn_sizesTheBaseWithTheModelHitbox() {
        var entities = hook.spawn(anchor, content, settings);

        assertThat(entities).containsExactly(base);
        assertThat(hook.lastBase).isSameAs(base);
        verify(base).setInteractionWidth(0.5f);
        verify(base).setInteractionHeight(1.5f);
        verify(base).setResponsive(true);
    }

    @Test
    void spawn_failure_removesTheBase() {
        hook.failure = new IllegalStateException("boom");

        assertThatThrownBy(() -> hook.spawn(anchor, content, settings)).hasMessage("boom");
        verify(base).remove();
    }

    @Test
    void spawn_withoutHandle_removesTheBase() {
        hook.handle = null;

        assertThatThrownBy(() -> hook.spawn(anchor, content, settings)).hasMessageContaining("cannot render model");
        verify(base).remove();
    }

    @Test
    void spawn_withoutBase_returnsTheProviderEntities() {
        Entity mob = mock(Entity.class);
        when(mob.getUniqueId()).thenReturn(UUID.randomUUID());
        hook.base = false;
        hook.extra = List.of(mob);

        assertThat(hook.spawn(anchor, content, settings)).containsExactly(mob);
        assertThat(hook.lastBase).isNull();
        verify(world, never()).spawn(anchor, Interaction.class);
    }

    @Test
    void spawn_withoutAnyEntity_destroysTheHandle() {
        hook.base = false;

        assertThatThrownBy(() -> hook.spawn(anchor, content, settings)).hasMessageContaining("did not spawn");
        assertThat(hook.destroyed).containsExactly("handle");
    }

    @Test
    void despawn_destroysTheHandleOnlyOnce() {
        var entities = hook.spawn(anchor, content, settings);

        hook.despawn(entities);
        hook.despawn(entities);

        assertThat(hook.destroyed).containsExactly("handle");
        verify(base, times(2)).remove();
    }

    @Test
    void despawn_providerFailure_stillRemovesTheEntities() {
        var entities = hook.spawn(anchor, content, settings);
        hook.destroyFailure = true;

        hook.despawn(entities);

        verify(base).remove();
    }

    @Test
    void height_comesFromTheHitbox() {
        var entities = hook.spawn(anchor, content, settings);

        assertThat(hook.height(content, settings, entities)).isEqualTo(1.5);
        assertThat(hook.height(content, settings, List.of())).isEqualTo(1.0);
    }

    @Test
    void visibility_reachesTheHandle() {
        var entities = hook.spawn(anchor, content, settings);
        Player player = mock(Player.class);

        hook.setVisible(player, entities, false);
        hook.setVisible(player, List.of(), true);

        assertThat(hook.visibility).containsExactly("handle:false");
    }

    @Test
    void spin_turnsTheBase() {
        var entities = hook.spawn(anchor, content, settings);
        when(base.isValid()).thenReturn(true);

        hook.spin(entities, 45f, settings, 20);

        verify(base).setRotation(135f, 0);
    }

    static class FakeHook extends HandleProviderHook<String> {
        String handle = "handle";
        RuntimeException failure;
        boolean base = true;
        boolean destroyFailure;
        List<Entity> extra = List.of();
        Interaction lastBase;
        final List<String> destroyed = new ArrayList<>();
        final List<String> visibility = new ArrayList<>();

        @Override
        public String prefix() {
            return "fake";
        }

        @Override
        public String pluginName() {
            return "Fake";
        }

        @Override
        public boolean exists(String id) {
            return true;
        }

        @Override
        public ItemStack icon(HeadContent content) {
            return null;
        }

        @Override
        protected boolean usesBase() {
            return base;
        }

        @Override
        protected String create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
            lastBase = base;
            if (failure != null) {
                throw failure;
            }
            return handle;
        }

        @Override
        protected List<Entity> entitiesOf(String handle) {
            return extra;
        }

        @Override
        protected void destroy(String handle) {
            if (destroyFailure) {
                throw new IllegalStateException("gone");
            }
            destroyed.add(handle);
        }

        @Override
        protected Hitbox hitbox(String handle, HeadContent content, RenderSettings settings) {
            return new Hitbox(0.5, 1.5);
        }

        @Override
        protected void setVisible(String handle, Player player, boolean visible) {
            visibility.add(handle + ":" + visible);
        }
    }
}
