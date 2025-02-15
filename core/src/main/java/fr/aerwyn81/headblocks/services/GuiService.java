package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
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
import java.util.stream.IntStream;

public class GuiService {

    private static HashMap<UUID, ItemStack> headItemCache;

    public static void initialize() {
        headItemCache = new HashMap<>();
    }

    public static void clearCache() {
        if (headItemCache != null) {
            headItemCache.clear();
        }
    }

    private static ItemStack getHeadItemStackFromCache(HeadLocation headLocation) {
        var headUuid = headLocation.getUuid();

        if (!headItemCache.containsKey(headUuid)) {
            String texture;
            try {
                texture = StorageService.getHeadTexture(headUuid);
            } catch (InternalException e) {
                texture = "";
            }

            headItemCache.put(headUuid, HeadUtils.applyTextureToItemStack(new ItemStack(Material.PLAYER_HEAD), texture));
        }

        return headItemCache.get(headLocation.getUuid()).clone();
    }

    public static void openOptionsGui(Player p) {
        HBMenu optionsMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOptions"), false, 2);

        //Ordering, Removing, Per-head actions, One-time global head click

        optionsMenu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.HOPPER)
                .setName(LanguageService.getMessage("Gui.OrderName"))
                .setLore(LanguageService.getMessages("Gui.OrderLore"))
                .toItemStack(), true)
                .addOnClickEvent(e -> openOrderGui((Player) e.getWhoClicked())));

        optionsMenu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.CLOCK)
                .setName(LanguageService.getMessage("Gui.ClickCounterName"))
                .setLore(LanguageService.getMessages("Gui.ClickCounterLore"))
                .toItemStack(), true)
                .addOnClickEvent(e -> openClickCounterGui((Player) e.getWhoClicked())));

        //optionsMenu.setItem(0, 14,  new ItemGUI(new ItemBuilder(Material.DIAMOND)
        //        .setName("Rewards")
        //        .setLore("Lore")
        //        .toItemStack(), true)
        //        .addOnClickEvent(event -> {}));

        int[] borders = { 0,  1,  2,  3,  4,  5,  6,  7,  8, 9,  10, 11, 14, 15, 16, 17 };
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> optionsMenu.setItem(0, borders[index], new ItemGUI(ConfigService.getGuiBorderIcon().setName("§7").toItemStack()))
        );

        p.openInventory(optionsMenu.getInventory());
    }

    public static void openOrderGui(Player p) {
        HBMenu orderMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOrder"), true, 5);

        List<HeadLocation> headLocations = HeadService.getHeadLocations()
                .stream()
                .sorted((Comparator.comparingInt(HeadLocation::getOrderIndex)))
                .toList();

        if (headLocations.isEmpty()) {
            orderMenu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setName(LanguageService.getMessage("Gui.NoHeads"))
                    .toItemStack(), true));
        } else {
            for (int i = 0; i < headLocations.size(); i++) {
                HeadLocation headLocation = headLocations.get(i);

                var orderItemGui = new ItemGUI(new ItemBuilder(getHeadItemStackFromCache(headLocation))
                        .setName(LocationUtils.parseLocationPlaceholders(LanguageService.getMessage("Gui.OrderItemName")
                                .replaceAll("%headName%", headLocation.getNameOrUnnamed()), headLocation.getLocation()))
                        .setLore(LanguageService.getMessages("Gui.OrderItemLore").stream().map(s ->
                                        s.replaceAll("%position%", headLocation.getDisplayedOrderIndex()))
                                .collect(Collectors.toList())).toItemStack(), true)
                        .addOnClickEvent(event -> {
                            if (event.getClick() == ClickType.LEFT) {
                                if (headLocation.getOrderIndex() != -1) {
                                    headLocation.setOrderIndex(headLocation.getOrderIndex() - 1);
                                    HeadService.saveHeadInConfig(headLocation);
                                }
                            } else if (event.getClick() == ClickType.RIGHT) {
                                if (headLocation.getOrderIndex() != headLocations.size() + 1) {
                                    headLocation.setOrderIndex(headLocation.getOrderIndex() + 1);
                                    HeadService.saveHeadInConfig(headLocation);
                                }
                            }

                            openOrderGui((Player) event.getWhoClicked());
                        });

                orderMenu.addItem(i, orderItemGui);
            }
        }

        p.openInventory(orderMenu.getInventory());
    }

    public static void openClickCounterGui(Player p) {
        HBMenu clickCounterMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleClickCounter"), true, 5);

        List<HeadLocation> headLocations = HeadService.getHeadLocations()
                .stream()
                .sorted(((o1, o2) -> o2.getOrderIndex() - o1.getOrderIndex()))
                .toList();

        if (headLocations.isEmpty()) {
            clickCounterMenu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setName(LanguageService.getMessage("Gui.NoHeads"))
                    .toItemStack(), true));
        } else {
            for (int i = 0; i < headLocations.size(); i++) {
                HeadLocation headLocation = headLocations.get(i);

                var orderItemGui = new ItemGUI(new ItemBuilder(getHeadItemStackFromCache(headLocation))
                        .setName(LocationUtils.parseLocationPlaceholders(LanguageService.getMessage("Gui.CounterClickItemName")
                                .replaceAll("%headName%", headLocation.getNameOrUnnamed()), headLocation.getLocation()))
                        .setLore(LanguageService.getMessages("Gui.CounterClickItemLore").stream().map(s ->
                                        s.replaceAll("%count%", headLocation.getDisplayedHitCount()))
                                .collect(Collectors.toList())).toItemStack(), true)
                        .addOnClickEvent(event -> {
                            if (event.getClick() == ClickType.LEFT) {
                                if (headLocation.getHitCount() != -1) {
                                    headLocation.setHitCount(headLocation.getHitCount() - 1);
                                    HeadService.saveHeadInConfig(headLocation);
                                }
                            } else if (event.getClick() == ClickType.RIGHT) {
                                headLocation.setHitCount(headLocation.getHitCount() + 1);
                                HeadService.saveHeadInConfig(headLocation);
                            }

                            openClickCounterGui((Player) event.getWhoClicked());
                        });

                clickCounterMenu.addItem(i, orderItemGui);
            }
        }

        p.openInventory(clickCounterMenu.getInventory());
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
