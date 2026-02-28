package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.services.gui.types.*;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GuiService {

    private static final RewardsGui rewardsManager = new RewardsGui();
    private static final OrderGui orderManager = new OrderGui();
    private static final HintGui hintManager = new HintGui();
    private static final BehaviorSelectionGui behaviorSelectionManager = new BehaviorSelectionGui();
    private static final TimedConfigGui timedConfigManager = new TimedConfigGui();

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

    public static HintGui getHintManager() {
        return hintManager;
    }

    public static BehaviorSelectionGui getBehaviorSelectionManager() {
        return behaviorSelectionManager;
    }

    public static TimedConfigGui getTimedConfigManager() {
        return timedConfigManager;
    }

    public static void openHuntSelectionOrDirect(Player player, BiConsumer<Player, Hunt> callback) {
        var hunts = HuntService.getAllHunts();

        if (!HuntService.isMultiHunt()) {
            callback.accept(player, hunts.iterator().next());
            return;
        }

        var huntSelectionMenu = new HBMenu(HeadBlocks.getInstance(),
                LanguageService.getMessage("Gui.TitleHuntSelection"), true, 5);

        int index = 0;
        for (Hunt hunt : hunts) {
            Material iconMaterial;
            try {
                iconMaterial = Material.valueOf(hunt.getIcon().toUpperCase());
            } catch (Exception e) {
                iconMaterial = Material.CHEST_MINECART;
            }

            var headCount = HeadService.getHeadLocationsForHunt(hunt).size();

            var huntItemGui = new ItemGUI(new ItemBuilder(iconMaterial)
                    .setName(LanguageService.getMessage("Gui.HuntSelectionItemName")
                            .replaceAll("%huntName%", hunt.getDisplayName()))
                    .setLore(LanguageService.getMessages("Gui.HuntSelectionItemLore").stream().map(s -> s
                                    .replaceAll("%huntName%", hunt.getDisplayName())
                                    .replaceAll("%headCount%", String.valueOf(headCount))
                                    .replaceAll("%state%", hunt.getState().getLocalizedName()))
                            .collect(Collectors.toList()))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> callback.accept((Player) event.getWhoClicked(), hunt));

            huntSelectionMenu.addItem(index, huntItemGui);
            index++;
        }

        player.openInventory(huntSelectionMenu.getInventory());
    }

    public static void openOptionsGui(Player p) {
        var optionsMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOptions"), false, 2);

        optionsMenu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.ENDER_EYE)
                .setName(LanguageService.getMessage("Gui.HintName"))
                .setLore(LanguageService.getMessages("Gui.HintLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> hintManager.openHintGui((Player) event.getWhoClicked())));

        optionsMenu.setItem(0, 14, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName(LanguageService.getMessage("Gui.RewardsName"))
                .setLore(LanguageService.getMessages("Gui.RewardsLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> rewardsManager.openRewardsSelectionGui((Player) event.getWhoClicked(), null)));

        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 16, 17};
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
