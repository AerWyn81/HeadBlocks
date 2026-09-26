package fr.aerwyn81.headblocks.visual.renderers;

import be.seeseemelk.mockbukkit.MockBukkit;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExtraRenderersTest {

    private World world;
    private Location anchor;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        world = mock(World.class);
        anchor = mock(Location.class);
        when(anchor.getWorld()).thenReturn(world);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    class Texts {

        @Test
        void text_spawnsItsColoredLinesWithAFittingHitbox() {
            TextDisplay display = mock(TextDisplay.class);
            Interaction interaction = mock(Interaction.class);
            when(world.spawn(anchor, TextDisplay.class)).thenReturn(display);
            when(world.spawn(anchor, Interaction.class)).thenReturn(interaction);
            var content = HeadContent.of(ContentKind.TEXT, "&6Hello\\nWorld!", Map.of("background", "none", "shadow", "true"));

            var entities = new TextDisplayRenderer().spawn(anchor, content, new RenderSettings(2.0, false, 0));

            assertThat(entities).containsExactly(display, interaction);
            verify(display).setText("§6Hello\nWorld!");
            verify(display).setBillboard(Display.Billboard.CENTER);
            verify(display).setShadowed(true);
            verify(display).setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            verify(interaction).setInteractionWidth(1.5f);
            verify(interaction).setInteractionHeight(1.2f);
        }

        @Test
        void text_heightGrowsWithItsLines() {
            var content = HeadContent.of(ContentKind.TEXT, "a\\nb\\nc", null);

            assertThat(new TextDisplayRenderer().height(content, new RenderSettings(1.0, false, 0), List.of()))
                    .isCloseTo(0.9, org.assertj.core.data.Offset.offset(1e-6));
        }

        @Test
        void text_shortTextKeepsAMinimalHitbox() {
            var renderer = new TextDisplayRenderer();
            var content = HeadContent.of(ContentKind.TEXT, "?", null);

            assertThat(renderer.width(content, 1f)).isEqualTo(0.4);
            assertThat(renderer.height(content, 1f)).isEqualTo(0.4);
        }

        @Test
        void text_billboardOption() {
            assertThat(TextDisplayRenderer.billboardOf(HeadContent.of(ContentKind.TEXT, "a", Map.of("billboard", "fixed"))))
                    .isEqualTo(Display.Billboard.FIXED);
            assertThat(TextDisplayRenderer.billboardOf(HeadContent.of(ContentKind.TEXT, "a", Map.of("billboard", "sideways"))))
                    .isEqualTo(Display.Billboard.CENTER);
        }

        @Test
        void text_backgroundAcceptsHexColors() {
            assertThat(TextDisplayRenderer.backgroundOf("#FF0000")).isEqualTo(Color.fromARGB(255, 255, 0, 0));
            assertThat(TextDisplayRenderer.backgroundOf("80FF0000")).isEqualTo(Color.fromARGB(0x80, 255, 0, 0));
            assertThat(TextDisplayRenderer.backgroundOf("NONE")).isEqualTo(Color.fromARGB(0, 0, 0, 0));
            assertThat(TextDisplayRenderer.backgroundOf("blue")).isNull();
            assertThat(TextDisplayRenderer.backgroundOf("")).isNull();
            assertThat(TextDisplayRenderer.backgroundOf(null)).isNull();
        }

        @Test
        void text_onlyFixedTextsSpin() {
            TextDisplay fixed = mock(TextDisplay.class);
            TextDisplay facing = mock(TextDisplay.class);
            when(fixed.getBillboard()).thenReturn(Display.Billboard.FIXED);
            when(fixed.isValid()).thenReturn(true);
            when(fixed.getTransformation()).thenReturn(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1), new AxisAngle4f()));
            when(facing.getBillboard()).thenReturn(Display.Billboard.CENTER);

            new TextDisplayRenderer().spin(List.of(fixed, facing), 90f, new RenderSettings(1, false, 0), 20);

            verify(fixed).setTransformation(any(Transformation.class));
            verify(facing, never()).setTransformation(any());
        }
    }

    @Nested
    class Frames {

        private ItemFrame frame;
        private Location blockLocation;

        @BeforeEach
        void setUpFrame() {
            Block block = mock(Block.class);
            blockLocation = mock(Location.class);
            frame = mock(ItemFrame.class);
            when(anchor.getBlock()).thenReturn(block);
            when(block.getLocation()).thenReturn(blockLocation);
        }

        @Test
        void frame_isAFixedInvisibleFrameFacingThePlayer() {
            when(world.spawn(blockLocation, ItemFrame.class)).thenReturn(frame);

            var entities = new ItemFrameRenderer().spawn(anchor, HeadContent.of(ContentKind.FRAME, "DIAMOND", null),
                    new RenderSettings(1, true, 90f));

            assertThat(entities).containsExactly(frame);
            verify(frame).setFacingDirection(BlockFace.WEST, true);
            verify(frame).setItem(new ItemStack(Material.DIAMOND), false);
            verify(frame).setItemDropChance(0);
            verify(frame).setVisible(false);
            verify(frame).setFixed(true);
            verify(frame).setGlowing(true);
        }

        @Test
        void frame_facingOptionLaysItOnTheFloor() {
            when(world.spawn(blockLocation, ItemFrame.class)).thenReturn(frame);

            new ItemFrameRenderer().spawn(anchor, HeadContent.of(ContentKind.FRAME, "DIAMOND", Map.of("facing", "UP")),
                    new RenderSettings(1, false, 90f));

            verify(frame).setFacingDirection(BlockFace.UP, true);
        }

        @Test
        void frame_facingOfFallsBackToTheYaw() {
            assertThat(ItemFrameRenderer.facingOf(HeadContent.of(ContentKind.FRAME, "DIAMOND", Map.of("facing", "down")), 0))
                    .isEqualTo(BlockFace.DOWN);
            assertThat(ItemFrameRenderer.facingOf(HeadContent.of(ContentKind.FRAME, "DIAMOND", Map.of("facing", "NORTH")), 0))
                    .isEqualTo(BlockFace.SOUTH);
            assertThat(ItemFrameRenderer.facingOf(HeadContent.of(ContentKind.FRAME, "DIAMOND", null), 180))
                    .isEqualTo(BlockFace.NORTH);
        }

        @Test
        void frame_optionsMakeItVisibleAndLit() {
            GlowItemFrame glowFrame = mock(GlowItemFrame.class);
            when(world.spawn(blockLocation, GlowItemFrame.class)).thenReturn(glowFrame);

            var entities = new ItemFrameRenderer().spawn(anchor,
                    HeadContent.of(ContentKind.FRAME, "DIAMOND", Map.of("lit", "true", "visible", "true")), new RenderSettings(1, false, 0));

            assertThat(entities).containsExactly(glowFrame);
            verify(glowFrame).setVisible(true);
        }

        @Test
        void frame_spinTurnsTheItem() {
            when(frame.isValid()).thenReturn(true);

            new ItemFrameRenderer().spin(List.of(frame), 45f, new RenderSettings(1, false, 0), 20);

            verify(frame).setRotation(Rotation.CLOCKWISE_45);
        }

        @Test
        void frame_rotationFollowsTheAngle() {
            assertThat(ItemFrameRenderer.rotationOf(0)).isEqualTo(Rotation.NONE);
            assertThat(ItemFrameRenderer.rotationOf(90)).isEqualTo(Rotation.CLOCKWISE);
            assertThat(ItemFrameRenderer.rotationOf(350)).isEqualTo(Rotation.NONE);
            assertThat(ItemFrameRenderer.rotationOf(-45)).isEqualTo(Rotation.COUNTER_CLOCKWISE_45);
        }

        @Test
        void frame_hologramSitsAboveTheBlock() {
            assertThat(new ItemFrameRenderer().height(null, new RenderSettings(3, false, 0), List.of())).isEqualTo(1.0);
        }
    }

    @Nested
    class Mobs {

        @Test
        void mob_wearsItsEquipmentAndOptions() {
            Zombie zombie = mock(Zombie.class);
            EntityEquipment equipment = mock(EntityEquipment.class);
            when(zombie.getEquipment()).thenReturn(equipment);
            when(world.spawnEntity(anchor, EntityType.ZOMBIE)).thenReturn(zombie);
            var content = HeadContent.of(ContentKind.MOB, "ZOMBIE",
                    Map.of("head", "DIAMOND_HELMET", "hand", "IRON_SWORD", "baby", "true", "invisible", "true"));

            new MobRenderer().spawn(anchor, content, new RenderSettings(1, false, 0));

            verify(equipment).setItem(EquipmentSlot.HEAD, new ItemStack(Material.DIAMOND_HELMET));
            verify(equipment).setItem(EquipmentSlot.HAND, new ItemStack(Material.IRON_SWORD));
            verify(equipment, never()).setItem(eq(EquipmentSlot.CHEST), any());
            verify(zombie).setBaby();
            verify(zombie).setInvisible(true);
            verify(zombie).setAI(false);
        }

        @Test
        void mob_withoutOptions_isLeftAsIs() {
            Zombie zombie = mock(Zombie.class);
            EntityEquipment equipment = mock(EntityEquipment.class);
            when(zombie.getEquipment()).thenReturn(equipment);
            when(world.spawnEntity(anchor, EntityType.ZOMBIE)).thenReturn(zombie);

            new MobRenderer().spawn(anchor, HeadContent.of(ContentKind.MOB, "ZOMBIE", null), new RenderSettings(1, false, 0));

            verifyNoInteractions(equipment);
            verify(zombie, never()).setBaby();
            verify(zombie, never()).setInvisible(anyBoolean());
        }

        @Test
        void armorStand_options() {
            ArmorStand stand = mock(ArmorStand.class);
            when(world.spawnEntity(anchor, EntityType.ARMOR_STAND)).thenReturn(stand);
            var content = HeadContent.of(ContentKind.MOB, "ARMOR_STAND", Map.of("small", "true", "arms", "true", "baseplate", "false"));

            new MobRenderer().spawn(anchor, content, new RenderSettings(1, false, 0));

            verify(stand).setSmall(true);
            verify(stand).setArms(true);
            verify(stand).setBasePlate(false);
        }

        @Test
        void freeze_stopsTheEntity() {
            Cat cat = mock(Cat.class);

            MobRenderer.freeze(cat, new RenderSettings(1, true, 45f));

            verify(cat).setAI(false);
            verify(cat).setGravity(false);
            verify(cat).setSilent(true);
            verify(cat).setGlowing(true);
            verify(cat).setRotation(45f, 0);
        }
    }
}
