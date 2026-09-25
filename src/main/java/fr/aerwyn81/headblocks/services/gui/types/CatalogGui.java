package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.LoadableHead;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.VisualForm;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CatalogGui {

    public enum Category {
        ALL(Material.COMPASS),
        HEADS(Material.PLAYER_HEAD),
        BLOCKS(Material.GRASS_BLOCK),
        ITEMS(Material.DIAMOND),
        TEXTS(Material.NAME_TAG),
        MOBS(Material.EGG),
        EXTERNAL(Material.ENDER_EYE);

        private final Material icon;

        Category(Material icon) {
            this.icon = icon;
        }

        public static Category of(HBHead head) {
            var content = head.getContent();
            if (content == null) {
                return HEADS;
            }

            return switch (content.kind()) {
                case HEAD -> HEADS;
                case BLOCK -> BLOCKS;
                case ITEM, FRAME -> ITEMS;
                case TEXT -> TEXTS;
                case MOB -> MOBS;
                case EXTERNAL -> EXTERNAL;
            };
        }
    }

    private record State(String huntId, Category category) {
    }

    private static final String NO_HUNT = "";

    private final ServiceRegistry registry;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    public CatalogGui(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void clearState(UUID playerUuid) {
        states.remove(playerUuid);
    }

    public void open(Player player) {
        var state = states.computeIfAbsent(player.getUniqueId(),
                uuid -> new State(registry.getHuntService().getSelectedHunt(uuid), Category.ALL));
        if (!NO_HUNT.equals(state.huntId()) && !registry.getHuntService().huntExists(state.huntId())) {
            state = new State(registry.getHuntService().getSelectedHunt(player.getUniqueId()), state.category());
            states.put(player.getUniqueId(), state);
        }

        var ls = registry.getLanguageService();
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                ls.message("Gui.CatalogTitle"), false, 5);

        var heads = registry.getHeadService().getHeads();
        if (heads.isEmpty()) {
            menu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setName(ls.message("Gui.NoHeads"))
                    .toItemStack(), true));
        }

        int slot = 0;
        for (var head : heads) {
            if (state.category() != Category.ALL && Category.of(head) != state.category()) {
                continue;
            }

            menu.addItem(slot++, catalogItem(head, state));
        }

        var finalState = state;
        menu.setPaginationButtonBuilder((type, inventory) -> switch (type) {
            case BACK_BUTTON -> categoryButton(finalState, heads);
            case CURRENT_BUTTON -> huntButton(finalState);
            default -> null;
        });

        player.openInventory(menu.getInventory());
    }

    private ItemGUI catalogItem(HBHead head, State state) {
        var ls = registry.getLanguageService();
        var icon = head.getItemStack().clone();

        var lore = new ArrayList<String>();
        var meta = icon.getItemMeta();
        if (meta != null && meta.getLore() != null) {
            lore.addAll(meta.getLore());
        }

        var huntName = huntName(state.huntId());
        var render = renderName(head, state.huntId());

        for (var line : ls.messageList("Gui.CatalogItemLore")) {
            lore.add(line.replace("%hunt%", huntName)
                    .replace("%render%", render));
        }

        return new ItemGUI(new ItemBuilder(icon).setLore(lore).toItemStack(), true)
                .addOnClickEvent(event -> give((Player) event.getWhoClicked(), head, state, event.isShiftClick()));
    }

    private void give(Player player, HBHead head, State state, boolean stack) {
        var ls = registry.getLanguageService();

        if (head instanceof LoadableHead loadable && !loadable.isLoaded()) {
            player.sendMessage(ls.message("Messages.HeadNotYetLoaded").replace("%id%", loadable.getDisplayId()));
            return;
        }

        if (PlayerUtils.getEmptySlots(player) < 1) {
            player.sendMessage(ls.message("Messages.InventoryFull"));
            return;
        }

        ItemStack item = head.getItemStack().clone();
        if (!NO_HUNT.equals(state.huntId())) {
            var hunt = registry.getHuntService().getHuntById(state.huntId());
            if (hunt != null) {
                item = HeadUtils.withHunt(item, hunt.getId(), ls.message("Head.HuntLore")
                        .replace("%hunt%", hunt.getDisplayName()));
            }
        }

        item.setAmount(stack ? item.getMaxStackSize() : 1);
        player.getInventory().addItem(item);
        player.sendMessage(ls.message("Messages.HeadGiven"));
    }

    private ItemGUI categoryButton(State state, List<HBHead> heads) {
        var ls = registry.getLanguageService();
        var lore = new ArrayList<String>();
        for (var category : available(heads)) {
            var name = ls.message("Gui.CatalogCategory" + capitalize(category));
            lore.add((category == state.category() ? "&a▶ " : "&7  ") + name);
        }
        lore.add("");
        lore.add(ls.message("Gui.CatalogCategoryClick"));

        return new ItemGUI(new ItemBuilder(state.category().icon)
                .setName(ls.message("Gui.CatalogCategoryName"))
                .setLore(lore.stream().map(MessageUtils::colorize).toList())
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    var categories = available(heads);
                    var next = categories.get((categories.indexOf(state.category()) + 1) % categories.size());
                    states.put(event.getWhoClicked().getUniqueId(), new State(state.huntId(), next));
                    open((Player) event.getWhoClicked());
                });
    }

    private ItemGUI huntButton(State state) {
        var ls = registry.getLanguageService();
        var huntName = huntName(state.huntId());

        return new ItemGUI(new ItemBuilder(Material.TARGET)
                .setName(ls.message("Gui.CatalogHuntName").replace("%hunt%", huntName))
                .setLore(ls.messageList("Gui.CatalogHuntLore").stream().map(l -> l.replace("%hunt%", huntName)).toList())
                .toItemStack(), true)
                .addOnClickEvent(event -> openHuntPicker((Player) event.getWhoClicked()));
    }

    private void openHuntPicker(Player player) {
        var ls = registry.getLanguageService();
        var state = states.get(player.getUniqueId());
        if (state == null) {
            open(player);
            return;
        }

        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                ls.message("Gui.CatalogHuntPickerTitle"), false, 5);

        menu.addItem(0, new ItemGUI(new ItemBuilder(Material.BARRIER)
                .setName(ls.message("Gui.CatalogNoHuntName"))
                .setLore(ls.messageList("Gui.CatalogNoHuntLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    states.put(player.getUniqueId(), new State(NO_HUNT, state.category()));
                    open(player);
                }));

        int slot = 1;
        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            var render = renderModeName(hunt);
            menu.addItem(slot++, new ItemGUI(new ItemBuilder(hunt.getIconMaterial())
                    .setName(ls.message("Gui.HuntSelectionItemName").replace("%huntName%", hunt.getDisplayName()))
                    .setLore(ls.messageList("Gui.CatalogHuntPickerLore").stream().map(l -> l
                            .replace("%state%", hunt.getState().getLocalizedName(ls))
                            .replace("%render%", render)).toList())
                    .toItemStack(), true)
                    .addOnClickEvent(event -> {
                        states.put(player.getUniqueId(), new State(hunt.getId(), state.category()));
                        open(player);
                    }));
        }

        menu.setPaginationButtonBuilder((type, inventory) -> {
            if (type == HBPaginationButtonType.BACK_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(ls.message("Gui.Back"))
                        .setLore(ls.messageList("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> open(player));
            }
            return null;
        });

        player.openInventory(menu.getInventory());
    }

    private List<Category> available(List<HBHead> heads) {
        var categories = new ArrayList<Category>();
        categories.add(Category.ALL);
        for (var category : Category.values()) {
            if (category != Category.ALL && heads.stream().anyMatch(h -> Category.of(h) == category)) {
                categories.add(category);
            }
        }
        return categories;
    }

    private String huntName(String huntId) {
        var ls = registry.getLanguageService();
        if (NO_HUNT.equals(huntId)) {
            return ls.message("Gui.CatalogNoHunt");
        }

        var hunt = registry.getHuntService().getHuntById(huntId);
        return hunt != null ? hunt.getDisplayName() : huntId;
    }

    private String renderName(HBHead head, String huntId) {
        if (NO_HUNT.equals(huntId)) {
            return registry.getLanguageService().message("Gui.RenderDependsOnHunt");
        }

        var content = head.getContent();
        return formName(registry.getVisualService().formOf(content, huntId), content);
    }

    private String renderModeName(HBHunt hunt) {
        return formName(VisualForm.resolve(HeadContent.head(""), hunt.getConfig().getRenderMode()), null);
    }

    private String formName(VisualForm form, HeadContent content) {
        var ls = registry.getLanguageService();
        if (form.isBlockBased()) {
            return ls.message("Gui.RenderBlock");
        }
        if (form == VisualForm.ITEM_DISPLAY || form == VisualForm.BLOCK_DISPLAY || form == VisualForm.TEXT_DISPLAY) {
            return ls.message("Gui.RenderDisplay");
        }
        return content != null && content.kind() == ContentKind.EXTERNAL
                ? ls.message("Gui.RenderExternal").replace("%plugin%", content.provider())
                : ls.message("Gui.RenderEntity");
    }

    private static String capitalize(Category category) {
        var name = category.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
