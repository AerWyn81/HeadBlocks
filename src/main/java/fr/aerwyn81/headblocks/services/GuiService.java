package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.services.gui.types.*;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GuiService {
    private final ConfigService configService;
    private final LanguageService languageService;
    private final HuntService huntService;
    private final HeadService headService;
    private final PluginProvider pluginProvider;

    private final RewardsGui rewardsManager;
    private final OrderGui orderManager;
    private final HintGui hintManager;
    private final BehaviorSelectionGui behaviorSelectionManager;
    private final TimedConfigGui timedConfigManager;

    // --- Constructor ---

    public GuiService(ConfigService configService, LanguageService languageService,
                      HuntService huntService, HeadService headService, PluginProvider pluginProvider,
                      ServiceRegistry registry) {
        this.configService = configService;
        this.languageService = languageService;
        this.huntService = huntService;
        this.headService = headService;
        this.pluginProvider = pluginProvider;

        this.rewardsManager = new RewardsGui(registry);
        this.orderManager = new OrderGui(registry);
        this.hintManager = new HintGui(registry);
        this.behaviorSelectionManager = new BehaviorSelectionGui(registry);
        this.timedConfigManager = new TimedConfigGui(registry);
    }

    // --- Instance methods ---

    public void clearCache() {
        GuiBase.clearSharedCache();

        rewardsManager.clearCache();
        hintManager.clearCache();
    }

    public RewardsGui getRewardsManager() {
        return rewardsManager;
    }

    public OrderGui getOrderManager() {
        return orderManager;
    }

    public HintGui getHintManager() {
        return hintManager;
    }

    public BehaviorSelectionGui getBehaviorSelectionManager() {
        return behaviorSelectionManager;
    }

    public TimedConfigGui getTimedConfigManager() {
        return timedConfigManager;
    }

    public void openHuntSelectionOrDirect(Player player, BiConsumer<Player, Hunt> callback) {
        var hunts = huntService.getAllHunts();

        if (!huntService.isMultiHunt()) {
            callback.accept(player, hunts.iterator().next());
            return;
        }

        var huntSelectionMenu = new HBMenu(pluginProvider.getJavaPlugin(), this,
                languageService.message("Gui.TitleHuntSelection"), true, 5);

        int index = 0;
        for (Hunt hunt : hunts) {
            Material iconMaterial;
            try {
                iconMaterial = Material.valueOf(hunt.getIcon().toUpperCase());
            } catch (Exception e) {
                iconMaterial = Material.CHEST_MINECART;
            }

            var headCount = headService.getHeadLocationsForHunt(hunt).size();

            var huntItemGui = new ItemGUI(new ItemBuilder(iconMaterial)
                    .setName(languageService.message("Gui.HuntSelectionItemName")
                            .replaceAll("%huntName%", hunt.getDisplayName()))
                    .setLore(languageService.messageList("Gui.HuntSelectionItemLore").stream().map(s -> s
                                    .replaceAll("%huntName%", hunt.getDisplayName())
                                    .replaceAll("%headCount%", String.valueOf(headCount))
                                    .replaceAll("%state%", hunt.getState().getLocalizedName(languageService)))
                            .collect(Collectors.toList()))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> callback.accept((Player) event.getWhoClicked(), hunt));

            huntSelectionMenu.addItem(index, huntItemGui);
            index++;
        }

        player.openInventory(huntSelectionMenu.getInventory());
    }

    public void openOptionsGui(Player p) {
        var optionsMenu = new HBMenu(pluginProvider.getJavaPlugin(), this, languageService.message("Gui.TitleOptions"), false, 2);

        optionsMenu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.ENDER_EYE)
                .setName(languageService.message("Gui.HintName"))
                .setLore(languageService.messageList("Gui.HintLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> hintManager.openHintGui((Player) event.getWhoClicked())));

        optionsMenu.setItem(0, 14, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName(languageService.message("Gui.RewardsName"))
                .setLore(languageService.messageList("Gui.RewardsLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> rewardsManager.openRewardsSelectionGui((Player) event.getWhoClicked(), null)));

        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> optionsMenu.setItem(0, borders[index], new ItemGUI(configService.guiBorderIcon().setName("§7").toItemStack()))
        );

        p.openInventory(optionsMenu.getInventory());
    }

    public ItemGUI getDefaultPaginationButtonBuilder(HBPaginationButtonType type, HBMenu inventory) {
        switch (type) {
            case BACK_BUTTON:
                if (inventory.isNestedMenu()) {
                    return new ItemGUI(configService.guiBackIcon()
                            .setName(languageService.message("Gui.Back"))
                            .setLore(languageService.messageList("Gui.BackLore"))
                            .toItemStack()
                    ).addOnClickEvent(e -> openOptionsGui((Player) e.getWhoClicked()));
                } else {
                    return new ItemGUI(configService.guiBorderIcon().setName("§7").toItemStack());
                }
            case PREV_BUTTON:
                if (inventory.getCurrentPage() > 0) {
                    return new ItemGUI(configService.guiPreviousIcon()
                            .setName(languageService.message("Gui.Previous"))
                            .setLore(languageService.messageList("Gui.PreviousLore")
                                    .stream().map(s -> s.replaceAll("%page%", String.valueOf(inventory.getCurrentPage()))).collect(Collectors.toList()))
                            .toItemStack()
                    ).addOnClickEvent(event -> inventory.previousPage(event.getWhoClicked()));
                } else {
                    return new ItemGUI(configService.guiBorderIcon().setName("§7").toItemStack());
                }
            case NEXT_BUTTON:
                if (inventory.getCurrentPage() < inventory.getMaxPage() - 1) {
                    return new ItemGUI(configService.guiNextIcon()
                            .setName(languageService.message("Gui.Next"))
                            .setLore(languageService.messageList("Gui.NextLore")
                                    .stream().map(s -> s.replaceAll("%page%", String.valueOf((inventory.getCurrentPage() + 2)))).collect(Collectors.toList()))
                            .toItemStack()
                    ).addOnClickEvent(event -> inventory.nextPage(event.getWhoClicked()));
                } else {
                    return new ItemGUI(configService.guiBorderIcon().setName("§7").toItemStack());
                }
            case CLOSE_BUTTON:
                return new ItemGUI(configService.guiCloseIcon()
                        .setName(languageService.message("Gui.Close"))
                        .setLore(languageService.messageList("Gui.CloseLore"))
                        .toItemStack()
                ).addOnClickEvent(event -> event.getWhoClicked().closeInventory());
            default:
                return new ItemGUI(configService.guiBorderIcon().setName("§7").toItemStack());
        }
    }

}
