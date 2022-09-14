package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GuiService {

    private static HashMap<String, ItemBuilder> headItemCache;
    private static HashMap<UUID, String> headsTextureCache;

    public static void initialize() {
        if (headItemCache == null) {
            headItemCache = new HashMap<>();
            headsTextureCache = new HashMap<>();
        } else {
            headItemCache.clear();
            headsTextureCache.clear();
        }
    }

    private static ItemBuilder getHeadItemStackFromCache(HeadLocation headLocation) {
        if (!headsTextureCache.containsKey(headLocation.getUuid())) {
            String texture;
            try {
                texture = StorageService.getHeadTexture(headLocation.getUuid());
            } catch (InternalException e) {
                texture = "";
            }

            headsTextureCache.put(headLocation.getUuid(), texture);
        }

        var texture = headsTextureCache.get(headLocation.getUuid());
        if (!headItemCache.containsKey(texture)) {
            var headItem = HeadUtils.applyTextureToItemStack(new ItemStack(Material.PLAYER_HEAD), texture);
            headItemCache.put(texture, new ItemBuilder(headItem)
                    .setName(headLocation.getUuid().toString()));
        }

        return headItemCache.get(texture);
    }

    public static void openOptionsGui(Player p) {
        HBMenu optionsMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOptions"), false, 2);

        //Ordering, Removing, Per-head actions, One-time global head click

        var orderItemGui = new ItemGUI(new ItemBuilder(Material.HOPPER)
                .setName("Order")
                .setLore("Lore")
                .toItemStack(), true)
                .addOnClickEvent(e -> openOrderGui((Player) e.getWhoClicked()));
        optionsMenu.setItem(0, 12, orderItemGui);

        var oneTimeItemGui = new ItemGUI(new ItemBuilder(Material.CLOCK)
                .setName("OneTime")
                .setLore("Lore")
                .toItemStack(), true)
                .addOnClickEvent(event -> {});
        optionsMenu.setItem(0, 13, oneTimeItemGui);

        var rewardsItemGui = new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName("Rewards")
                .setLore("Lore")
                .toItemStack(), true)
                .addOnClickEvent(event -> {});
        optionsMenu.setItem(0, 14, rewardsItemGui);

        p.openInventory(optionsMenu.getInventory());
    }

    private static void openOrderGui(Player p) {
        HBMenu orderMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOrder"), true, 5);

        List<HeadLocation> headLocations = HeadService.getHeadLocations()
                .stream()
                .sorted(Comparator.comparingInt(HeadLocation::getOrderIndex))
                .collect(Collectors.toList());

        for (int i = 0; i < headLocations.size(); i++) {
            HeadLocation headLocation = headLocations.get(i);

            var orderItemGui = new ItemGUI(getHeadItemStackFromCache(headLocation)
                    .setLore(" " + headLocation.getOrderIndex()).toItemStack(), true)
                    .addOnClickEvent(event -> {
                        if (event.getClick() == ClickType.LEFT) {
                            if (headLocation.getOrderIndex() != -1) {
                                headLocation.setOrderIndex(headLocation.getOrderIndex() - 1);
                            }
                        } else if (event.getClick() == ClickType.RIGHT) {
                            if (headLocation.getOrderIndex() != headLocations.size() + 1) {
                                headLocation.setOrderIndex(headLocation.getOrderIndex() + 1);
                            }
                        }

                        openOrderGui((Player) event.getWhoClicked());
                    });

            orderMenu.addItem(i, orderItemGui);
        }

        p.openInventory(orderMenu.getInventory());
    }

    public static ItemGUI getDefaultPaginationButtonBuilder(HBPaginationButtonType type, HBMenu inventory) {
        switch (type) {
            case BACK_BUTTON:
                if (inventory.isNestedMenu()) {
                    return new ItemGUI(ConfigService.getGuiBackIcon()
                            .setName(LanguageService.getMessage("Gui.Back"))
                            .setLore(LanguageService.getMessages("Gui.BackLore"))
                            .toItemStack()
                    ).addOnClickEvent(e -> openOptionsGui((Player) e.getWhoClicked()));
                } else {
                    return new ItemGUI(ConfigService.getGuiBorderIcon().setName("§7").toItemStack());
                }
            case PREV_BUTTON:
                if (inventory.getCurrentPage() > 0) {
                    return new ItemGUI(ConfigService.getGuiPreviousIcon()
                            .setName(LanguageService.getMessage("Gui.Previous"))
                            .setLore(LanguageService.getMessages("Gui.PreviousLore")
                                    .stream().map(s -> s.replaceAll("%page%", String.valueOf(inventory.getCurrentPage()))).collect(Collectors.toList()))
                            .toItemStack()
                    ).addOnClickEvent(event -> inventory.previousPage(event.getWhoClicked()));
                } else {
                    return new ItemGUI(ConfigService.getGuiBorderIcon().setName("§7").toItemStack());
                }
            case NEXT_BUTTON:
                if (inventory.getCurrentPage() < inventory.getMaxPage() - 1) {
                    return new ItemGUI(ConfigService.getGuiNextIcon()
                            .setName(LanguageService.getMessage("Gui.Next"))
                            .setLore(LanguageService.getMessages("Gui.NextLore")
                                    .stream().map(s -> s.replaceAll("%page%", String.valueOf((inventory.getCurrentPage() + 2)))).collect(Collectors.toList()))
                            .toItemStack()
                    ).addOnClickEvent(event -> inventory.nextPage(event.getWhoClicked()));
                } else {
                    return new ItemGUI(ConfigService.getGuiBorderIcon().setName("§7").toItemStack());
                }
            case CLOSE_BUTTON:
                return new ItemGUI(ConfigService.getGuiCloseIcon()
                        .setName(LanguageService.getMessage("Gui.Close"))
                        .setLore(LanguageService.getMessages("Gui.CloseLore"))
                        .toItemStack()
                ).addOnClickEvent(event -> event.getWhoClicked().closeInventory());
            default:
                return new ItemGUI(ConfigService.getGuiBorderIcon().setName("§7").toItemStack());
        }
    }
}
