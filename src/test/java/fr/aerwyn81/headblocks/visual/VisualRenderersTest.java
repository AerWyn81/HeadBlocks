package fr.aerwyn81.headblocks.visual;

import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.VisualForm;
import fr.aerwyn81.headblocks.hooks.VisualProviderHook;
import fr.aerwyn81.headblocks.visual.renderers.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class VisualRenderersTest {

    private final HeadContent lantern = HeadContent.of(ContentKind.BLOCK, "LANTERN", null);

    @Nested
    class Registry {

        @Test
        void everyInternalFormHasItsRenderer() {
            var renderers = new VisualRenderers(Map.of());

            assertThat(renderers.block(VisualForm.HEAD_BLOCK)).isInstanceOf(HeadBlockRenderer.class);
            assertThat(renderers.block(VisualForm.BLOCK)).isInstanceOf(PlainBlockRenderer.class);
            assertThat(renderers.entity(VisualForm.ITEM_DISPLAY, null)).isInstanceOf(ItemDisplayRenderer.class);
            assertThat(renderers.entity(VisualForm.BLOCK_DISPLAY, lantern)).isInstanceOf(BlockDisplayRenderer.class);
            assertThat(renderers.entity(VisualForm.MOB, null)).isInstanceOf(MobRenderer.class);
        }

        @Test
        void externalForm_usesTheAvailableProvider() {
            VisualProviderHook nexo = mock(VisualProviderHook.class);
            when(nexo.isAvailable()).thenReturn(true);
            var renderers = new VisualRenderers(Map.of("nexo", nexo));

            assertThat(renderers.entity(VisualForm.EXTERNAL, HeadContent.external("nexo", "tree", null))).isSameAs(nexo);
            assertThat(renderers.entity(VisualForm.EXTERNAL, HeadContent.external("mythicmobs", "boss", null))).isNull();
            assertThat(renderers.entity(VisualForm.EXTERNAL, null)).isNull();
        }

        @Test
        void externalIcon_comesFromTheProvider() {
            VisualProviderHook nexo = mock(VisualProviderHook.class);
            ItemStack icon = mock(ItemStack.class);
            var content = HeadContent.external("nexo", "tree", null);
            when(nexo.isAvailable()).thenReturn(true);
            when(nexo.icon(content)).thenReturn(icon);

            assertThat(new VisualRenderers(Map.of("nexo", nexo)).icon(content)).isSameAs(icon);
        }
    }

    @Nested
    class Blocks {

        @Test
        void plainBlock_placesTheSavedBlockData() {
            Block block = mock(Block.class);
            BlockData data = mock(BlockData.class);
            var content = HeadContent.of(ContentKind.BLOCK, "LANTERN", Map.of("data", "minecraft:lantern[hanging=true]"));

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.createBlockData("minecraft:lantern[hanging=true]")).thenReturn(data);

                assertThat(new PlainBlockRenderer().place(block, content, 0)).isTrue();
            }

            verify(block).setBlockData(data);
        }

        @Test
        void plainBlock_unknownMaterial_isNotPlaced() {
            Block block = mock(Block.class);

            assertThat(new PlainBlockRenderer().place(block, HeadContent.of(ContentKind.BLOCK, "NOT_A_BLOCK", null), 0)).isFalse();
            verifyNoInteractions(block);
        }

        @Test
        void plainBlock_matchesByMaterial() {
            Block block = mock(Block.class);
            when(block.getType()).thenReturn(Material.LANTERN, Material.STONE);
            var renderer = new PlainBlockRenderer();

            assertThat(renderer.matches(block, lantern)).isTrue();
            assertThat(renderer.matches(block, lantern)).isFalse();
            assertThat(renderer.height()).isEqualTo(1.0);
        }

        @Test
        void headBlock_placesARotatedHead() {
            Block block = mock(Block.class);
            Rotatable rotatable = mock(Rotatable.class);
            when(block.getBlockData()).thenReturn(rotatable);

            assertThat(new HeadBlockRenderer().place(block, HeadContent.head(""), 90f)).isTrue();

            verify(block).setType(Material.PLAYER_HEAD);
            verify(rotatable).setRotation(BlockFace.WEST);
            verify(block).setBlockData(rotatable);
        }
    }

    @Nested
    class Entities {

        @Test
        void itemDisplay_spawnsADisplayAndAHitbox() {
            World world = mock(World.class);
            Location anchor = mock(Location.class);
            when(anchor.getWorld()).thenReturn(world);
            ItemDisplay display = mock(ItemDisplay.class);
            Interaction interaction = mock(Interaction.class);
            when(world.spawn(anchor, ItemDisplay.class)).thenReturn(display);
            when(world.spawn(anchor, Interaction.class)).thenReturn(interaction);

            var entities = new ItemDisplayRenderer().spawn(anchor, HeadContent.of(ContentKind.ITEM, "DIAMOND", null),
                    new RenderSettings(2.0, true, 0));

            assertThat(entities).containsExactly(display, interaction);
            verify(display).setGlowing(true);
            verify(interaction).setInteractionWidth(1.0f);
            verify(interaction).setInteractionHeight(1.0f);
        }

        @Test
        void itemDisplay_heightFollowsTheScale() {
            assertThat(new ItemDisplayRenderer().height(null, new RenderSettings(3.0, false, 0), List.of())).isEqualTo(1.5);
        }

        @Test
        void itemDisplay_spinInterpolatesTheRotation() {
            ItemDisplay display = mock(ItemDisplay.class);
            when(display.isValid()).thenReturn(true);
            when(display.getTransformation()).thenReturn(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1), new AxisAngle4f()));
            Interaction interaction = mock(Interaction.class);

            new ItemDisplayRenderer().spin(List.of(display, interaction), 90f, new RenderSettings(1, false, 0), 20);

            verify(display).setInterpolationDuration(20);
            verify(display).setTransformation(any(Transformation.class));
            verifyNoInteractions(interaction);
        }

        @Test
        void mob_spinTurnsTheBody() {
            Entity mob = mock(Entity.class);
            when(mob.isValid()).thenReturn(true);

            new MobRenderer().spin(List.of(mob), 45f, new RenderSettings(1, false, 90f), 20);

            verify(mob).setRotation(135f, 0);
        }

        @Test
        void mob_isAnchored() {
            assertThat(new MobRenderer().anchored()).isTrue();
            assertThat(new ItemDisplayRenderer().anchored()).isFalse();
        }

        @Test
        void mob_rejectsUnknownAndLifelessTypes() {
            assertThatThrownBy(() -> MobRenderer.typeOf(HeadContent.of(ContentKind.MOB, "DRAGONFLY", null)))
                    .hasMessageContaining("unknown entity type");
            assertThatThrownBy(() -> MobRenderer.typeOf(HeadContent.of(ContentKind.MOB, "ARROW", null)))
                    .hasMessageContaining("cannot be used");
            assertThat(MobRenderer.typeOf(HeadContent.of(ContentKind.MOB, "cat", null)).name()).isEqualTo("CAT");
        }
    }

    @Nested
    class WallHeads {

        @Test
        void wallHead_isPlacedBackOnTheWall() {
            Block block = mock(Block.class);
            org.bukkit.block.data.Directional directional = mock(org.bukkit.block.data.Directional.class);
            when(block.getBlockData()).thenReturn(directional);

            new HeadBlockRenderer().place(block, HeadContent.withWall(HeadContent.head("")), 270f);

            verify(block).setType(Material.PLAYER_WALL_HEAD);
            verify(directional).setFacing(BlockFace.EAST);
        }

        @Test
        void withWall_keepsTheOtherOptions() {
            var content = HeadContent.withWall(new HeadContent(ContentKind.HEAD, "", null, Map.of("owner", "x")));

            assertThat(content.optionBoolean("wall", false)).isTrue();
            assertThat(content.option("owner")).isEqualTo("x");
        }
    }
}
