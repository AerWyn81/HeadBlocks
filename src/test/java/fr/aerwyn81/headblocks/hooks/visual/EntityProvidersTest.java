package fr.aerwyn81.headblocks.hooks.visual;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.DespawnMode;
import io.lumine.mythic.core.mobs.MobExecutor;
import kr.toxicity.model.api.BetterModel;
import lol.pyr.znpcsplus.api.NpcApi;
import lol.pyr.znpcsplus.api.NpcApiProvider;
import lol.pyr.znpcsplus.api.entity.EntityProperty;
import lol.pyr.znpcsplus.api.entity.EntityPropertyRegistry;
import lol.pyr.znpcsplus.api.npc.NpcEntry;
import lol.pyr.znpcsplus.api.npc.NpcRegistry;
import lol.pyr.znpcsplus.api.npc.NpcType;
import lol.pyr.znpcsplus.api.npc.NpcTypeRegistry;
import lol.pyr.znpcsplus.api.skin.SkinDescriptor;
import lol.pyr.znpcsplus.api.skin.SkinDescriptorFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EntityProvidersTest {

    private final RenderSettings settings = new RenderSettings(1.0, false, 0);

    private World world;
    private Location anchor;
    private Interaction base;
    private Player player;

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        anchor = mock(Location.class);
        base = mock(Interaction.class);
        player = mock(Player.class);
        when(anchor.getWorld()).thenReturn(world);
        lenient().when(world.spawn(anchor, Interaction.class)).thenReturn(base);
        lenient().when(base.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    @Nested
    class MythicMobs {

        private MobExecutor mobs;

        private MockedStatic<MythicBukkit> mythic() {
            MythicBukkit mythicBukkit = mock(MythicBukkit.class);
            mobs = mock(MobExecutor.class);
            when(mythicBukkit.getMobManager()).thenReturn(mobs);
            var mocked = mockStatic(MythicBukkit.class);
            mocked.when(MythicBukkit::inst).thenReturn(mythicBukkit);
            return mocked;
        }

        @Test
        void mythicMob_isSpawnedFrozenAndReleasedThroughMythic() {
            MythicMob mob = mock(MythicMob.class);
            ActiveMob active = mock(ActiveMob.class);
            AbstractEntity abstractEntity = mock(AbstractEntity.class);
            AbstractLocation location = mock(AbstractLocation.class);
            Zombie zombie = mock(Zombie.class);
            when(zombie.getUniqueId()).thenReturn(UUID.randomUUID());
            when(active.getEntity()).thenReturn(abstractEntity);
            when(abstractEntity.getBukkitEntity()).thenReturn(zombie);
            when(mob.spawn(location, 3.0)).thenReturn(active);

            try (var ignored = mythic(); MockedStatic<BukkitAdapter> adapter = mockStatic(BukkitAdapter.class)) {
                when(mobs.getMythicMob("SkeletonKing")).thenReturn(Optional.of(mob));
                adapter.when(() -> BukkitAdapter.adapt(anchor)).thenReturn(location);
                var hook = new MythicMobsHook();

                var entities = hook.spawn(anchor, HeadContent.external("mythicmobs", "SkeletonKing", Map.of("level", "3")), settings);
                hook.despawn(entities);

                assertThat(entities).containsExactly(zombie);
                verify(active).setDespawnMode(DespawnMode.NEVER);
                verify(zombie).setAI(false);
                verify(active).setDespawned();
                verify(mobs).unregisterActiveMob(active);
                verify(zombie).remove();
                verify(world, never()).spawn(anchor, Interaction.class);
            }
        }

        @Test
        void unknownMythicMob_fails() {
            try (var ignored = mythic()) {
                when(mobs.getMythicMob("Nope")).thenReturn(Optional.empty());
                var hook = new MythicMobsHook();

                assertThatThrownBy(() -> hook.spawn(anchor, HeadContent.external("mythicmobs", "Nope", null), settings))
                        .hasMessageContaining("unknown MythicMob Nope");
                assertThat(hook.exists("Nope")).isFalse();
            }
        }

        @Test
        void icon_isTheSpawnEggOfTheMob() {
            MythicMob mob = mock(MythicMob.class);
            when(mob.getEntityTypeString()).thenReturn("SKELETON");

            try (var ignored = mythic()) {
                when(mobs.getMythicMob("SkeletonKing")).thenReturn(Optional.of(mob));
                when(mobs.getMythicMob("Nope")).thenReturn(Optional.empty());
                var hook = new MythicMobsHook();

                assertThat(hook.icon(HeadContent.external("mythicmobs", "SkeletonKing", null)).getType()).isEqualTo(Material.SKELETON_SPAWN_EGG);
                assertThat(hook.icon(HeadContent.external("mythicmobs", "Nope", null)).getType()).isEqualTo(Material.ZOMBIE_SPAWN_EGG);
                assertThat(hook.anchored()).isTrue();
            }
        }
    }

    @Nested
    class ZNPCs {

        @Test
        void npc_isNotSavedAndHidesPerPlayer() {
            NpcApi api = mock(NpcApi.class);
            NpcTypeRegistry types = mock(NpcTypeRegistry.class);
            NpcRegistry npcs = mock(NpcRegistry.class);
            EntityPropertyRegistry properties = mock(EntityPropertyRegistry.class);
            SkinDescriptorFactory skins = mock(SkinDescriptorFactory.class);
            NpcEntry entry = mock(NpcEntry.class);
            lol.pyr.znpcsplus.api.npc.Npc npc = mock(lol.pyr.znpcsplus.api.npc.Npc.class);
            @SuppressWarnings("unchecked")
            EntityProperty<SkinDescriptor> skinProperty = mock(EntityProperty.class);
            SkinDescriptor descriptor = mock(SkinDescriptor.class);
            NpcType type = mock(NpcType.class);
            when(api.getNpcTypeRegistry()).thenReturn(types);
            when(api.getNpcRegistry()).thenReturn(npcs);
            when(api.getPropertyRegistry()).thenReturn(properties);
            when(api.getSkinDescriptorFactory()).thenReturn(skins);
            when(types.getByName("player")).thenReturn(type);
            when(npcs.create(anyString(), eq(world), eq(type), any())).thenReturn(entry);
            when(entry.getNpc()).thenReturn(npc);
            when(entry.getId()).thenReturn("headblocks_1");
            when(properties.getByName("skin", SkinDescriptor.class)).thenReturn(skinProperty);
            when(skins.createStaticDescriptor("Notch")).thenReturn(descriptor);
            when(npc.getWorld()).thenReturn(world);
            when(player.getWorld()).thenReturn(world);

            try (MockedStatic<NpcApiProvider> provider = mockStatic(NpcApiProvider.class)) {
                provider.when(NpcApiProvider::get).thenReturn(api);
                var hook = new ZNPCsHook();

                var entities = hook.spawn(anchor, HeadContent.external("znpcs", "Notch", null), settings);
                hook.setVisible(player, entities, false);
                hook.setVisible(player, entities, true);
                hook.despawn(entities);

                verify(entry).setSave(false);
                verify(entry).setProcessed(true);
                verify(npc).setProperty(skinProperty, descriptor);
                verify(npc).setEnabled(true);
                verify(npc).hide(player);
                verify(npc).show(player);
                verify(npcs).delete("headblocks_1");
            }
        }

        @Test
        void unknownType_fails() {
            NpcApi api = mock(NpcApi.class);
            NpcTypeRegistry types = mock(NpcTypeRegistry.class);
            when(api.getNpcTypeRegistry()).thenReturn(types);

            try (MockedStatic<NpcApiProvider> provider = mockStatic(NpcApiProvider.class)) {
                provider.when(NpcApiProvider::get).thenReturn(api);

                assertThatThrownBy(() -> new ZNPCsHook().spawn(anchor, HeadContent.external("znpcs", "x", Map.of("type", "dragon")), settings))
                        .hasMessageContaining("unknown ZNPCsPlus type dragon");
                verify(base).remove();
            }
        }
    }

    @Nested
    class ModelEngine {

        @Test
        void model_isAttachedToTheHitboxBase() {
            ModelBlueprint blueprint = mock(ModelBlueprint.class);
            ModeledEntity modeled = mock(ModeledEntity.class);
            ActiveModel active = mock(ActiveModel.class);
            com.ticxo.modelengine.api.entity.Hitbox hitbox = mock(com.ticxo.modelengine.api.entity.Hitbox.class);
            when(blueprint.getMainHitbox()).thenReturn(hitbox);
            when(hitbox.getWidth()).thenReturn(2.0);
            when(hitbox.getHeight()).thenReturn(3.0);

            try (MockedStatic<ModelEngineAPI> api = mockStatic(ModelEngineAPI.class)) {
                api.when(() -> ModelEngineAPI.getBlueprint("dragon")).thenReturn(blueprint);
                api.when(() -> ModelEngineAPI.createModeledEntity(base)).thenReturn(modeled);
                api.when(() -> ModelEngineAPI.createActiveModel(blueprint)).thenReturn(active);
                var hook = new ModelEngineHook();

                var entities = hook.spawn(anchor, HeadContent.external("modelengine", "dragon", null), new RenderSettings(2.0, true, 0));
                hook.despawn(entities);

                assertThat(entities).containsExactly(base);
                verify(base).setInteractionWidth(4.0f);
                verify(base).setInteractionHeight(6.0f);
                verify(active).setScale(2.0);
                verify(active).setGlowing(true);
                verify(modeled).addModel(active, true);
                verify(modeled).setBaseEntityVisible(false);
                verify(modeled).destroy();
            }
        }

        @Test
        void unknownModel_fails() {
            try (MockedStatic<ModelEngineAPI> api = mockStatic(ModelEngineAPI.class)) {
                var hook = new ModelEngineHook();

                assertThatThrownBy(() -> hook.spawn(anchor, HeadContent.external("modelengine", "nope", null), settings))
                        .hasMessageContaining("unknown ModelEngine model nope");
                assertThat(hook.exists("nope")).isFalse();
                verify(base).remove();
            }
        }
    }

    @Nested
    class BetterModels {

        @Test
        void isReady_onceItsModelsAreLoaded() {
            try (MockedStatic<BetterModel> betterModel = mockStatic(BetterModel.class)) {
                betterModel.when(BetterModel::modelKeys).thenReturn(java.util.Set.of());
                var hook = new BetterModelHook();

                assertThat(hook.isReady()).isFalse();

                betterModel.when(BetterModel::modelKeys).thenReturn(java.util.Set.of("golem"));
                assertThat(hook.isReady()).isTrue();
            }
        }

        @Test
        void unknownModel_fails() {
            try (MockedStatic<BetterModel> betterModel = mockStatic(BetterModel.class)) {
                var hook = new BetterModelHook();

                assertThatThrownBy(() -> hook.spawn(anchor, HeadContent.external("bettermodel", "nope", null), settings))
                        .hasMessageContaining("unknown BetterModel model nope");
                assertThat(hook.exists("nope")).isFalse();
                verify(base).remove();
            }
        }
    }
}
