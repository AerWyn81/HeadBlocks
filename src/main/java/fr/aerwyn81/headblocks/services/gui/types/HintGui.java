package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HintMode;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class HintGui extends GuiBase {

    private static final ConcurrentHashMap<UUID, HintMode> guiViewHint = new ConcurrentHashMap<>();

    public HintGui(ServiceRegistry registry) {
        super(registry);
    }

    public void clearCache() {
        guiViewHint.clear();
    }

    public void clearCache(UUID playerUuid) {
        guiViewHint.remove(playerUuid);
    }

    public void openHintGui(Player player) {
        registry.getGuiService().openHuntSelectionOrDirect(player, this::openHintGuiForHunt);
    }

    public void openHintGuiForHunt(Player player, HBHunt hunt) {
        HBMenu hintMenu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.TitleHint"), true, 5);

        List<HeadLocation> headLocations = registry.getHeadService().getHeadLocationsForHunt(hunt);

        if (!guiViewHint.containsKey(player.getUniqueId())) {
            guiViewHint.put(player.getUniqueId(), HintMode.SOUND);
        }

        var playerSelectedMode = guiViewHint.get(player.getUniqueId());
        var currentModeFormatted = playerSelectedMode.getLocalizedName(registry.getLanguageService());

        if (headLocations.isEmpty()) {
            hintMenu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setName(registry.getLanguageService().message("Gui.NoHeads"))
                    .toItemStack(), true));
        } else {
            for (int i = 0; i < headLocations.size(); i++) {
                var headLocation = headLocations.get(i);

                boolean isHintEnabled = isHintEnabled(playerSelectedMode, headLocation);

                var orderItemGui = new ItemGUI(new ItemBuilder(getHeadItemStackFromCache(headLocation))
                        .setName(LocationUtils.parseLocationPlaceholders(registry.getLanguageService().message("Gui.HintItemName")
                                .replace("%headName%", headLocation.getNameOrUnnamed(registry.getLanguageService().message("Gui.Unnamed"))), headLocation.getLocation()))
                        .setLore(registry.getLanguageService().messageList("Gui.HintItemLore").stream().map(s -> s
                                        .replace("%mode%", currentModeFormatted)
                                        .replace("%state%", registry.getLanguageService().message(isHintEnabled ? "Gui.Enabled" : "Gui.Disabled")))
                                .toList()).toItemStack(), true)
                        .addOnClickEvent(event -> onHintItemClick(event, player, hunt, playerSelectedMode, headLocation, headLocations));

                hintMenu.addItem(i, orderItemGui);
            }
        }

        if (registry.getHuntService().isMultiHunt()) {
            hintMenu.setPaginationButtonBuilder((type, inventory) -> {
                if (type == HBPaginationButtonType.BACK_BUTTON) {
                    return new ItemGUI(registry.getConfigService().guiBackIcon()
                            .setName(registry.getLanguageService().message("Gui.Back"))
                            .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                            .toItemStack())
                            .addOnClickEvent(event -> openHintGui((Player) event.getWhoClicked()));
                }

                return null;
            });
        }

        player.openInventory(hintMenu.getInventory());
    }

    private static boolean isHintEnabled(HintMode mode, HeadLocation headLocation) {
        if (mode == HintMode.SOUND) {
            return headLocation.isHintSoundEnabled();
        } else if (mode == HintMode.ACTIONBAR) {
            return headLocation.isHintActionBarEnabled();
        } else {
            throw new IllegalStateException("Internal, invalid hint mode: " + mode);
        }
    }

    private void onHintItemClick(InventoryClickEvent event, Player player, HBHunt hunt, HintMode playerSelectedMode,
                                 HeadLocation headLocation, List<HeadLocation> headLocations) {
        if (event.getClick() == ClickType.DROP) {
            guiViewHint.put(player.getUniqueId(), playerSelectedMode.next());
        } else {
            switch (playerSelectedMode) {
                case SOUND -> toggleSoundHint(event, headLocation, headLocations);
                case ACTIONBAR -> toggleActionBarHint(event, headLocation, headLocations);
            }
        }

        openHintGuiForHunt((Player) event.getWhoClicked(), hunt);
    }

    private void toggleSoundHint(InventoryClickEvent event, HeadLocation headLocation, List<HeadLocation> headLocations) {
        var hintBefore = headLocation.isHintSoundEnabled();

        if (event.isLeftClick() && !hintBefore) {
            applyHint(event, ClickType.LEFT, true, headLocation, headLocations, HeadLocation::setHintSound);
        } else if (event.isRightClick() && hintBefore) {
            applyHint(event, ClickType.RIGHT, false, headLocation, headLocations, HeadLocation::setHintSound);
        }
    }

    private void toggleActionBarHint(InventoryClickEvent event, HeadLocation headLocation, List<HeadLocation> headLocations) {
        var hintBefore = headLocation.isHintActionBarEnabled();

        if (event.isLeftClick() && !hintBefore) {
            applyHint(event, ClickType.LEFT, true, headLocation, headLocations, HeadLocation::setHintActionBar);
        } else if (event.isRightClick() && hintBefore) {
            applyHint(event, ClickType.RIGHT, false, headLocation, headLocations, HeadLocation::setHintActionBar);
        }
    }

    private void applyHint(InventoryClickEvent event, ClickType singleHeadClick, boolean enabled, HeadLocation headLocation,
                           List<HeadLocation> headLocations, BiConsumer<HeadLocation, Boolean> setter) {
        if (event.getClick() == singleHeadClick) {
            setter.accept(headLocation, enabled);
            registry.getHeadService().saveHeadInConfig(headLocation);
        } else {
            for (HeadLocation head : headLocations) {
                setter.accept(head, enabled);
            }

            registry.getHeadService().saveAllHeadsInConfig();
        }
    }
}
