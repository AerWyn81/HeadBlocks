package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NpcProviderHookTest {

    private final HeadContent content = HeadContent.external("fake", "Notch", null);
    private final RenderSettings settings = new RenderSettings(1.0, false, 0);

    private Location anchor;
    private Interaction base;
    private Player player;
    private FakeNpcHook hook;

    @BeforeEach
    void setUp() {
        World world = mock(World.class);
        anchor = mock(Location.class);
        base = mock(Interaction.class);
        player = mock(Player.class);
        when(anchor.getWorld()).thenReturn(world);
        when(world.spawn(anchor, Interaction.class)).thenReturn(base);
        when(base.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        hook = new FakeNpcHook();
    }

    @Test
    void hitbox_fitsAPlayerSizedNpc() {
        hook.spawn(anchor, content, new RenderSettings(2.0, false, 0));

        verify(base).setInteractionWidth(1.6f);
        verify(base).setInteractionHeight(4.0f);
    }

    @Test
    void hitbox_canBeConfigured() {
        hook.spawn(anchor, HeadContent.external("fake", "Notch", Map.of("width", "2", "height", "3")), settings);

        verify(base).setInteractionWidth(2.0f);
        verify(base).setInteractionHeight(3.0f);
    }

    @Test
    void hiding_isRememberedForTheSpawnEvents() {
        List<Entity> entities = hook.spawn(anchor, content, settings);
        var npc = hook.created.get(0);

        hook.setVisible(player, entities, false);
        hook.setVisible(player, entities, false);

        assertThat(hook.isHidden(npc, player)).isTrue();
        assertThat(hook.calls).containsExactly("hide");
    }

    @Test
    void showing_onlyTargetsHiddenPlayers() {
        List<Entity> entities = hook.spawn(anchor, content, settings);
        var npc = hook.created.get(0);

        hook.setVisible(player, entities, true);
        hook.setVisible(player, entities, false);
        hook.setVisible(player, entities, true);

        assertThat(hook.isHidden(npc, player)).isFalse();
        assertThat(hook.calls).containsExactly("hide", "show");
    }

    @Test
    void despawnedNpc_isForgotten() {
        List<Entity> entities = hook.spawn(anchor, content, settings);
        var npc = hook.created.get(0);
        hook.setVisible(player, entities, false);

        hook.despawn(entities);

        assertThat(hook.destroyed).containsExactly(npc);
        assertThat(hook.isHidden(npc, player)).isFalse();
        assertThat(hook.isHidden(null, player)).isFalse();
    }

    @Test
    void anySkinNameExists() {
        assertThat(hook.exists("Notch")).isTrue();
        assertThat(hook.exists(" ")).isFalse();
        assertThat(hook.exists(null)).isFalse();
        assertThat(hook.icon(content).getType()).isEqualTo(org.bukkit.Material.PLAYER_HEAD);
    }

    static class FakeNpcHook extends NpcProviderHook<Object> {
        final List<Object> created = new ArrayList<>();
        final List<Object> destroyed = new ArrayList<>();
        final List<String> calls = new ArrayList<>();

        @Override
        public String prefix() {
            return "fake";
        }

        @Override
        public String pluginName() {
            return "Fake";
        }

        @Override
        protected Object createNpc(Location anchor, HeadContent content, RenderSettings settings) {
            var npc = new Object();
            created.add(npc);
            return npc;
        }

        @Override
        protected void destroyNpc(Object npc) {
            destroyed.add(npc);
        }

        @Override
        protected void hide(Object npc, Player player) {
            calls.add("hide");
        }

        @Override
        protected void show(Object npc, Player player) {
            calls.add("show");
        }
    }
}
