package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HintMode;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HintGui extends GuiBase {

    private static final ConcurrentHashMap<UUID, HintMode> guiViewHint = new ConcurrentHashMap<>();

    public void clearCache() {
        guiViewHint.clear();
    }

    public void openHintGui(Player player) {
        HBMenu hintMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleHint"), true, 5);

        List<HeadLocation> headLocations = HeadService.getHeadLocations();

        if (!guiViewHint.containsKey(player.getUniqueId())) {
            guiViewHint.put(player.getUniqueId(), HintMode.SOUND);
        }

        var playerSelectedMode = guiViewHint.get(player.getUniqueId());
        var currentModeFormatted = playerSelectedMode.getLocalizedName();

        if (headLocations.isEmpty()) {
            hintMenu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setName(LanguageService.getMessage("Gui.NoHeads"))
                    .toItemStack(), true));
        } else {
            for (int i = 0; i < headLocations.size(); i++) {
                var headLocation = headLocations.get(i);

                boolean isHintEnabled;

                if (playerSelectedMode == HintMode.SOUND)
                    isHintEnabled = headLocation.isHintSoundEnabled();
                else if (playerSelectedMode == HintMode.ACTIONBAR)
                    isHintEnabled = headLocation.isHintActionBarEnabled();
                else
                    throw new IllegalStateException("Internal, invalid hint mode: " + playerSelectedMode);

                var orderItemGui = new ItemGUI(new ItemBuilder(getHeadItemStackFromCache(headLocation))
                        .setName(LocationUtils.parseLocationPlaceholders(LanguageService.getMessage("Gui.HintItemName")
                                .replaceAll("%headName%", headLocation.getNameOrUnnamed()), headLocation.getLocation()))
                        .setLore(LanguageService.getMessages("Gui.HintItemLore").stream().map(s -> s
                                        .replaceAll("%mode%", currentModeFormatted)
                                        .replaceAll("%state%", isHintEnabled
                                                ? LanguageService.getMessage("Gui.Enabled")
                                                : LanguageService.getMessage("Gui.Disabled")))
                                .collect(Collectors.toList())).toItemStack(), true)
                        .addOnClickEvent(event -> {
                            if (event.getClick() == ClickType.MIDDLE) {
                                guiViewHint.put(player.getUniqueId(), playerSelectedMode.next());
                            } else {
                                switch (playerSelectedMode) {
                                    case SOUND -> {
                                        var hintBefore = headLocation.isHintSoundEnabled();

                                        if (event.isLeftClick() && !hintBefore) {
                                            if (event.getClick() == ClickType.LEFT) {
                                                headLocation.setHintSound(true);
                                                HeadService.saveHeadInConfig(headLocation);
                                            } else {
                                                for (HeadLocation head : HeadService.getHeadLocations()) {
                                                    head.setHintSound(true);
                                                }

                                                HeadService.saveAllHeadsInConfig();
                                            }
                                        } else if (event.isRightClick() && hintBefore) {
                                            if (event.getClick() == ClickType.RIGHT) {
                                                headLocation.setHintSound(false);
                                                HeadService.saveHeadInConfig(headLocation);
                                            } else {
                                                for (HeadLocation head : HeadService.getHeadLocations()) {
                                                    head.setHintSound(false);
                                                }

                                                HeadService.saveAllHeadsInConfig();
                                            }
                                        }
                                    }
                                    case ACTIONBAR -> {
                                        var hintBefore = headLocation.isHintActionBarEnabled();

                                        if (event.isLeftClick() && !hintBefore) {
                                            if (event.getClick() == ClickType.LEFT) {
                                                headLocation.setHintActionBar(true);
                                                HeadService.saveHeadInConfig(headLocation);
                                            } else {
                                                for (HeadLocation head : HeadService.getHeadLocations()) {
                                                    head.setHintActionBar(true);
                                                }

                                                HeadService.saveAllHeadsInConfig();
                                            }
                                        } else if (event.isRightClick() && hintBefore) {
                                            if (event.getClick() == ClickType.RIGHT) {
                                                headLocation.setHintActionBar(false);
                                                HeadService.saveHeadInConfig(headLocation);
                                            } else {
                                                for (HeadLocation head : HeadService.getHeadLocations()) {
                                                    head.setHintActionBar(false);
                                                }

                                                HeadService.saveAllHeadsInConfig();
                                            }
                                        }
                                    }
                                }
                            }

                            openHintGui((Player) event.getWhoClicked());
                        });

                hintMenu.addItem(i, orderItemGui);
            }
        }

        player.openInventory(hintMenu.getInventory());
    }
}
