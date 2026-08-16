package fr.aerwyn81.headblocks.services.gui.types.requirement;

import be.seeseemelk.mockbukkit.MockBukkit;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaMessageMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.CuboidAreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.PermissionRequirement;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The Requirements menu, driven the way a player drives it: open, click an item, read the menu that
 * comes back. The items expose their click handler, so the whole flow runs without a real server.
 */
class RequirementsGuiTest {

    private static final int FIRST_ENTRY_SLOT = 9;
    private static final int ADD_SLOT = 30;
    private static final int VALIDATE_SLOT = 32;

    /**
     * The picker lays the types out from slot 10, in declaration order.
     */
    private static final int PICKER_FIRST_SLOT = 10;

    private ServiceRegistry registry;
    private Player player;
    private RequirementsGui gui;

    private AtomicReference<RequirementSet> validated;
    private AtomicReference<Player> cancelled;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();

        LanguageService language = mock(LanguageService.class);
        lenient().when(language.message(anyString())).thenAnswer(call -> call.getArgument(0));
        lenient().when(language.messageList(anyString())).thenReturn(List.of("lore"));

        ConfigService config = mock(ConfigService.class);
        lenient().when(config.guiBorderIcon()).thenAnswer(call -> new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE));
        lenient().when(config.guiBackIcon()).thenAnswer(call -> new ItemBuilder(Material.ARROW));

        PluginProvider pluginProvider = mock(PluginProvider.class);
        lenient().when(pluginProvider.getJavaPlugin()).thenReturn(MockBukkit.createMockPlugin());
        lenient().when(pluginProvider.isPlaceholderApiActive()).thenReturn(true);

        registry = mock(ServiceRegistry.class);
        lenient().when(registry.getLanguageService()).thenReturn(language);
        lenient().when(registry.getConfigService()).thenReturn(config);
        lenient().when(registry.getPluginProvider()).thenReturn(pluginProvider);
        lenient().when(registry.getGuiService()).thenReturn(mock(GuiService.class));

        player = mock(Player.class);
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        gui = new RequirementsGui(registry);
        validated = new AtomicReference<>();
        cancelled = new AtomicReference<>();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- Driving the menu ---

    private void open(RequirementSet initial) {
        gui.open(player, initial, validated::set, cancelled::set);
    }

    /**
     * The menu behind the last inventory the player was shown. {@code HBMenu} is the holder of the
     * inventory it builds, which is how a click finds its way back to the item handlers.
     */
    private HBMenu lastMenu() {
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(player, atLeastOnce()).openInventory(captor.capture());
        return (HBMenu) captor.getValue().getHolder();
    }

    private void click(ItemGUI item) {
        click(item, false);
    }

    private void click(ItemGUI item, boolean rightClick) {
        assertThat(item.getOnClickEvent()).as("item is clickable").isNotNull();

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        lenient().when(event.getWhoClicked()).thenReturn(player);
        lenient().when(event.isRightClick()).thenReturn(rightClick);

        item.getOnClickEvent().accept(event);
    }

    private ItemGUI pickerItem(RequirementType type) {
        click(lastMenu().getItem(0, ADD_SLOT));
        return lastMenu().getItem(0, PICKER_FIRST_SLOT + type.ordinal());
    }

    // --- Fixtures ---

    private AreaRequirement area() {
        return new AreaRequirement(registry,
                new CuboidAreaProvider("world", 0, 60, 0, 10, 70, 10),
                null, false, false, AreaMessageMode.CHAT);
    }

    private RequirementSet setOf(Requirement... requirements) {
        return new RequirementSet(registry, RequirementMode.ALL, List.of(requirements));
    }

    // =========================================================================
    // 1. Single-instance types
    // =========================================================================

    @Test
    void picker_areaNotAddedYet_isSelectable() {
        open(setOf(new PermissionRequirement(registry, "hb.vip")));

        ItemGUI item = pickerItem(RequirementType.AREA);

        assertThat(item.getOnClickEvent()).isNotNull();
        assertThat(item.getIcon().getType()).isEqualTo(RequirementType.AREA.getIcon());
    }

    @Test
    void picker_areaAlreadyAdded_isBlocked() {
        open(setOf(area()));

        ItemGUI item = pickerItem(RequirementType.AREA);

        // Everything downstream reads a single area per hunt: a second one would be evaluated on
        // click but never enforced, and never reported.
        assertThat(item.getOnClickEvent()).isNull();
        assertThat(item.getIcon().getType()).isEqualTo(Material.BARRIER);
    }

    @Test
    void picker_areaRemoved_becomesSelectableAgain() {
        open(setOf(area()));

        click(lastMenu().getItem(0, FIRST_ENTRY_SLOT), true);

        assertThat(pickerItem(RequirementType.AREA).getOnClickEvent()).isNotNull();
    }

    @Test
    void picker_repeatableTypes_staySelectableWhenAlreadyAdded() {
        open(setOf(new PermissionRequirement(registry, "hb.vip")));

        // Only the area is single-instance; stacking permissions is a legitimate configuration.
        assertThat(pickerItem(RequirementType.PERMISSION).getOnClickEvent()).isNotNull();
    }

    @Test
    void picker_typeNeedingAMissingPlugin_isBlocked() {
        when(registry.getPluginProvider().isPlaceholderApiActive()).thenReturn(false);
        open(RequirementSet.empty());

        ItemGUI item = pickerItem(RequirementType.PLACEHOLDER);

        assertThat(item.getOnClickEvent()).isNull();
        assertThat(item.getIcon().getType()).isEqualTo(Material.BARRIER);
    }

    // =========================================================================
    // 2. Validation hands back what the menu was given
    // =========================================================================

    @Test
    void validate_returnsTheRequirementsAndTheMode() {
        PermissionRequirement permission = new PermissionRequirement(registry, "hb.vip");
        open(new RequirementSet(registry, RequirementMode.ANY, List.of(permission)));

        click(lastMenu().getItem(0, VALIDATE_SLOT));

        assertThat(validated.get()).isNotNull();
        assertThat(validated.get().getMode()).isEqualTo(RequirementMode.ANY);
        assertThat(validated.get().getRequirements()).containsExactly(permission);
    }

    @Test
    void validate_carriesOverTheEntriesTheMenuNeverShowed() {
        Map<String, Object> unreadable = Map.of("type", "written-by-a-newer-version", "field", 42);
        open(new RequirementSet(registry, RequirementMode.ALL,
                List.of(new PermissionRequirement(registry, "hb.vip")), List.of(unreadable)));

        click(lastMenu().getItem(0, VALIDATE_SLOT));

        // Saving rewrites the whole section from this set: dropping what the menu could not display
        // would delete it from the hunt file.
        assertThat(validated.get().getPreserved()).containsExactly(unreadable);
    }

    @Test
    void validate_afterRemovingEverything_returnsAnEmptySet() {
        open(setOf(new PermissionRequirement(registry, "hb.vip")));

        click(lastMenu().getItem(0, FIRST_ENTRY_SLOT), true);
        click(lastMenu().getItem(0, VALIDATE_SLOT));

        assertThat(validated.get().isEmpty()).isTrue();
    }

    // =========================================================================
    // 3. Mode switch and session hygiene
    // =========================================================================

    @Test
    void modeItem_cyclesBetweenAllAndAny() {
        open(setOf(new PermissionRequirement(registry, "hb.vip")));

        click(lastMenu().getItem(0, 4));
        click(lastMenu().getItem(0, VALIDATE_SLOT));

        assertThat(validated.get().getMode()).isEqualTo(RequirementMode.ANY);
    }

    @Test
    void clearState_dropsTheSession() {
        open(setOf(new PermissionRequirement(registry, "hb.vip")));
        HBMenu menu = lastMenu();

        gui.clearState(player.getUniqueId());
        click(menu.getItem(0, VALIDATE_SLOT));

        // No session left: the menu closes instead of validating a set nobody owns anymore.
        assertThat(validated.get()).isNull();
        verify(player).closeInventory();
    }

    @Test
    void openTypePicker_withoutASession_closesTheMenu() {
        open(setOf(new PermissionRequirement(registry, "hb.vip")));
        HBMenu menu = lastMenu();

        gui.clearState(player.getUniqueId());
        click(menu.getItem(0, ADD_SLOT));

        verify(player).closeInventory();
    }
}
