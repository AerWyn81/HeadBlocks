package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.services.gui.types.ClickCounterGui;
import fr.aerwyn81.headblocks.services.gui.types.HintGui;
import fr.aerwyn81.headblocks.services.gui.types.OrderGui;
import fr.aerwyn81.headblocks.services.gui.types.RewardsGui;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GuiService {

    private static final RewardsGui rewardsManager = new RewardsGui();
    private static final OrderGui orderManager = new OrderGui();
    private static final ClickCounterGui clickCounterManager = new ClickCounterGui();
    private static final HintGui hintManager = new HintGui();

    public static void clearCache() {
        GuiBase.clearSharedCache();

        rewardsManager.clearCache();
        hintManager.clearCache();
    }

    public static RewardsGui getRewardsManager() {
        return rewardsManager;
    }

    public static OrderGui getOrderManager() {
        return orderManager;
    }

    public static ClickCounterGui getClickCounterManager() {
        return clickCounterManager;
    }

    public static HintGui getHintManager() {
        return hintManager;
    }

    public static void openOptionsGui(Player p) {
        var optionsMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOptions"), false, 2);

        optionsMenu.setItem(0, 11, new ItemGUI(new ItemBuilder(Material.HOPPER)
                .setName(LanguageService.getMessage("Gui.OrderName"))
                .setLore(LanguageService.getMessages("Gui.OrderLore"))
                .toItemStack(), true)
                .addOnClickEvent(e -> orderManager.openOrderGui((Player) e.getWhoClicked())));

        optionsMenu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.CLOCK)
                .setName(LanguageService.getMessage("Gui.ClickCounterName"))
                .setLore(LanguageService.getMessages("Gui.ClickCounterLore"))
                .toItemStack(), true)
                .addOnClickEvent(e -> clickCounterManager.openClickCounterGui((Player) e.getWhoClicked())));

        optionsMenu.setItem(0, 14, new ItemGUI(new ItemBuilder(Material.ENDER_EYE)
                .setName(LanguageService.getMessage("Gui.HintName"))
                .setLore(LanguageService.getMessages("Gui.HintLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> hintManager.openHintGui((Player) event.getWhoClicked())));

        optionsMenu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName(LanguageService.getMessage("Gui.RewardsName"))
                .setLore(LanguageService.getMessages("Gui.RewardsLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> rewardsManager.openRewardsSelectionGui((Player) event.getWhoClicked(), null)));

        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 13, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> optionsMenu.setItem(0, borders[index], new ItemGUI(ConfigService.getGuiBorderIcon().setName("§7").toItemStack()))
        );

        p.openInventory(optionsMenu.getInventory());
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
