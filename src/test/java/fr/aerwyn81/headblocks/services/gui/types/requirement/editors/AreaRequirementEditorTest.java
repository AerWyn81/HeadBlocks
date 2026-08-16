package fr.aerwyn81.headblocks.services.gui.types.requirement.editors;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaMessageMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.CuboidAreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.WorldGuardAreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.services.ChatPromptService;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The area editor, driven through its menu items and its chat prompt.
 * <p>
 * Both regressions this covers were invisible in the menu: a value silently overwritten when a
 * prompt opened, and a value silently dropped when its slot was hidden.
 */
class AreaRequirementEditorTest {

    private static final int TYPE_SLOT = 10;
    private static final int REGION_SLOT = 12;
    private static final int BLOCK_EXIT_SLOT = 15;
    private static final int VALIDATE_SLOT = 31;

    private static final String AREA_WORLD = "world";
    private static final String ADMIN_WORLD = "world_nether";

    private ServerMock server;
    private ServiceRegistry registry;
    private ChatPromptService prompts;
    private Player player;
    private AreaRequirementEditor editor;

    private AtomicReference<Requirement> done;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld(AREA_WORLD);
        server.addSimpleWorld(ADMIN_WORLD);

        LanguageService language = mock(LanguageService.class);
        lenient().when(language.message(anyString())).thenAnswer(call -> call.getArgument(0));
        lenient().when(language.messageList(anyString())).thenReturn(List.of("lore"));

        ConfigService config = mock(ConfigService.class);
        lenient().when(config.guiBorderIcon()).thenAnswer(call -> new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE));
        lenient().when(config.guiBackIcon()).thenAnswer(call -> new ItemBuilder(Material.ARROW));

        PluginProvider pluginProvider = mock(PluginProvider.class);
        lenient().when(pluginProvider.getJavaPlugin()).thenReturn(MockBukkit.createMockPlugin());

        prompts = new ChatPromptService();

        registry = mock(ServiceRegistry.class);
        lenient().when(registry.getLanguageService()).thenReturn(language);
        lenient().when(registry.getConfigService()).thenReturn(config);
        lenient().when(registry.getPluginProvider()).thenReturn(pluginProvider);
        lenient().when(registry.getGuiService()).thenReturn(mock(GuiService.class));
        lenient().when(registry.getChatPromptService()).thenReturn(prompts);

        // The admin is deliberately somewhere else than the area being edited.
        player = mock(Player.class);
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player.getWorld()).thenReturn(server.getWorld(ADMIN_WORLD));

        editor = new AreaRequirementEditor(registry);
        done = new AtomicReference<>();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- Driving the editor ---

    private void open(AreaRequirement existing) {
        editor.open(player, existing, done::set, p -> {
        });
    }

    private HBMenu lastMenu() {
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(player, atLeastOnce()).openInventory(captor.capture());
        return (HBMenu) captor.getValue().getHolder();
    }

    private void click(int slot) {
        ItemGUI item = lastMenu().getItem(0, slot);
        assertThat(item).as("item at slot " + slot).isNotNull();
        assertThat(item.getOnClickEvent()).as("item at slot " + slot + " is clickable").isNotNull();

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        lenient().when(event.getWhoClicked()).thenReturn(player);

        item.getOnClickEvent().accept(event);
    }

    private AreaRequirement validate() {
        click(VALIDATE_SLOT);
        assertThat(done.get()).isInstanceOf(AreaRequirement.class);
        return (AreaRequirement) done.get();
    }

    // --- Fixtures ---

    private World areaWorld() {
        return server.getWorld(AREA_WORLD);
    }

    private Location returnPoint() {
        return new Location(areaWorld(), 5.5, 65, 5.5, 90f, 0f);
    }

    private AreaRequirement cuboidRequirement(boolean blockExit, Location returnPoint) {
        return new AreaRequirement(registry,
                new CuboidAreaProvider(AREA_WORLD, 0, 60, 0, 10, 70, 10),
                returnPoint, blockExit, false, AreaMessageMode.CHAT);
    }

    private AreaRequirement worldGuardRequirement(String worldName, String regionId) {
        return new AreaRequirement(registry,
                new WorldGuardAreaProvider(worldName, regionId),
                null, false, false, AreaMessageMode.CHAT);
    }

    // =========================================================================
    // 1. Seeding an existing requirement
    // =========================================================================

    @Test
    void open_existingCuboid_keepsItOnValidateWithoutTouchingAnything() {
        open(cuboidRequirement(false, null));

        AreaProvider provider = validate().area();

        assertThat(provider).isInstanceOf(CuboidAreaProvider.class);
        CuboidAreaProvider cuboid = (CuboidAreaProvider) provider;
        assertThat(cuboid.getWorldName()).isEqualTo(AREA_WORLD);
        assertThat(cuboid.getMinX()).isEqualTo(0);
        assertThat(cuboid.getMaxZ()).isEqualTo(10);
    }

    @Test
    void open_existingRequirement_keepsItsOptions() {
        open(new AreaRequirement(registry,
                new CuboidAreaProvider(AREA_WORLD, 0, 60, 0, 10, 70, 10),
                returnPoint(), true, true, AreaMessageMode.TITLE));

        AreaRequirement result = validate();

        assertThat(result.blockExit()).isTrue();
        assertThat(result.resetOnLeave()).isTrue();
        assertThat(result.messageMode()).isEqualTo(AreaMessageMode.TITLE);
    }

    // =========================================================================
    // 2. The return point outlives a hidden slot
    // =========================================================================

    @Test
    void blockExit_toggledOffThenOn_keepsTheReturnPoint() {
        Location point = returnPoint();
        open(cuboidRequirement(true, point));

        // The return point slot is only drawn while the exit is blocked, so this hides it and
        // brings it back. The draft holds the point on its own, so it survives the round trip.
        click(BLOCK_EXIT_SLOT);
        click(BLOCK_EXIT_SLOT);

        AreaRequirement result = validate();

        assertThat(result.blockExit()).isTrue();
        assertThat(result.returnPoint()).isEqualTo(point);
        assertThat(result.isComplete()).isTrue();
    }

    @Test
    void blockExit_turnedOff_stillRemembersTheReturnPointForLater() {
        Location point = returnPoint();
        open(cuboidRequirement(true, point));

        click(BLOCK_EXIT_SLOT);

        AreaRequirement result = validate();

        // The point is unused while the exit is not blocked, but it is kept so that turning the
        // option back on in a later edition does not ask the admin to pick it again.
        assertThat(result.blockExit()).isFalse();
        assertThat(result.returnPoint()).isEqualTo(point);
    }

    @Test
    void validate_isBlockedWhileTheExitIsBlockedWithoutAReturnPoint() {
        open(cuboidRequirement(true, null));

        ItemGUI validateItem = lastMenu().getItem(0, VALIDATE_SLOT);

        assertThat(validateItem.getOnClickEvent()).isNull();
        assertThat(validateItem.getIcon().getType()).isEqualTo(Material.BARRIER);
    }

    // =========================================================================
    // 3. The WorldGuard region keeps its own world
    // =========================================================================

    @Test
    void promptRegion_existingRegion_keepsItsWorldRatherThanTheAdminsOne() {
        open(worldGuardRequirement(AREA_WORLD, "spawn_hunt"));

        click(REGION_SLOT);
        prompts.process(player, "spawn_hunt");

        AreaProvider provider = validate().area();

        // Rebinding to the admin's world would point at a region that does not exist there, which
        // makes the area unavailable and stops it gating anything — silently.
        assertThat(provider).isInstanceOf(WorldGuardAreaProvider.class);
        assertThat(((WorldGuardAreaProvider) provider).getWorldName()).isEqualTo(AREA_WORLD);
        assertThat(((WorldGuardAreaProvider) provider).getRegionId()).isEqualTo("spawn_hunt");
    }

    @Test
    void promptRegion_existingRegionRenamed_stillKeepsItsWorld() {
        open(worldGuardRequirement(AREA_WORLD, "spawn_hunt"));

        click(REGION_SLOT);
        prompts.process(player, "another_region");

        WorldGuardAreaProvider provider = (WorldGuardAreaProvider) validate().area();

        assertThat(provider.getWorldName()).isEqualTo(AREA_WORLD);
        assertThat(provider.getRegionId()).isEqualTo("another_region");
    }

    @Test
    void promptRegion_brandNewRegion_bindsToTheWorldTheAdminIsIn() {
        open(null);

        click(TYPE_SLOT);
        click(REGION_SLOT);
        prompts.process(player, "fresh_region");

        WorldGuardAreaProvider provider = (WorldGuardAreaProvider) validate().area();

        assertThat(provider.getWorldName()).isEqualTo(ADMIN_WORLD);
        assertThat(provider.getRegionId()).isEqualTo("fresh_region");
    }

    @Test
    void promptRegion_cancelled_leavesTheRegionUntouched() {
        open(worldGuardRequirement(AREA_WORLD, "spawn_hunt"));

        click(REGION_SLOT);
        prompts.cancel(player.getUniqueId());

        WorldGuardAreaProvider provider = (WorldGuardAreaProvider) validate().area();

        assertThat(provider.getWorldName()).isEqualTo(AREA_WORLD);
        assertThat(provider.getRegionId()).isEqualTo("spawn_hunt");
    }

    // =========================================================================
    // 4. Type switch and state hygiene
    // =========================================================================

    @Test
    void typeToggle_switchesBetweenCuboidAndWorldGuard() {
        open(null);

        click(TYPE_SLOT);
        click(REGION_SLOT);
        prompts.process(player, "a_region");

        assertThat(validate().area()).isInstanceOf(WorldGuardAreaProvider.class);
    }

    @Test
    void validate_withoutAnArea_isBlocked() {
        open(null);

        ItemGUI validateItem = lastMenu().getItem(0, VALIDATE_SLOT);

        assertThat(validateItem.getOnClickEvent()).isNull();
    }

    @Test
    void clearState_dropsTheDraft() {
        open(worldGuardRequirement(AREA_WORLD, "spawn_hunt"));
        editor.clearState(player.getUniqueId());

        open(null);

        // A fresh draft: no region carried over from the edition that was abandoned.
        assertThat(lastMenu().getItem(0, VALIDATE_SLOT).getOnClickEvent()).isNull();
    }
}
