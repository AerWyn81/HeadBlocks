package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OrderGui extends GuiBase {

    public OrderGui(ServiceRegistry registry) {
        super(registry);
    }

    public void openOrderGui(Player player) {
        registry.getGuiService().openHuntSelectionOrDirect(player, this::openOrderGuiForHunt);
    }

    public void openOrderGuiForHunt(Player player, Hunt hunt) {
        HBMenu orderMenu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.TitleOrder"), true, 5);

        List<HeadLocation> headLocations = registry.getHeadService().getHeadLocationsForHunt(hunt)
                .stream()
                .sorted(Comparator.comparingInt(HeadLocation::getOrderIndex))
                .toList();

        if (headLocations.isEmpty()) {
            orderMenu.setItem(0, 22, new ItemGUI(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .setName(registry.getLanguageService().message("Gui.NoHeads"))
                    .toItemStack(), true));
        } else {
            for (int i = 0; i < headLocations.size(); i++) {
                HeadLocation headLocation = headLocations.get(i);

                var orderItemGui = new ItemGUI(new ItemBuilder(getHeadItemStackFromCache(headLocation))
                        .setName(LocationUtils.parseLocationPlaceholders(registry.getLanguageService().message("Gui.OrderItemName")
                                .replaceAll("%headName%", headLocation.getNameOrUnnamed(registry.getLanguageService().message("Gui.Unnamed"))), headLocation.getLocation()))
                        .setLore(registry.getLanguageService().messageList("Gui.OrderItemLore").stream().map(s ->
                                        s.replaceAll("%position%", headLocation.getDisplayedOrderIndex(registry.getLanguageService().message("Gui.NoOrder"))))
                                .collect(Collectors.toList())).toItemStack(), true)
                        .addOnClickEvent(event -> {
                            if (event.getClick() == ClickType.LEFT) {
                                if (headLocation.getOrderIndex() != -1) {
                                    headLocation.setOrderIndex(headLocation.getOrderIndex() - 1);
                                    registry.getHeadService().saveHeadInConfig(headLocation);
                                }
                            } else if (event.getClick() == ClickType.RIGHT) {
                                if (headLocation.getOrderIndex() != headLocations.size() + 1) {
                                    headLocation.setOrderIndex(headLocation.getOrderIndex() + 1);
                                    registry.getHeadService().saveHeadInConfig(headLocation);
                                }
                            }

                            openOrderGuiForHunt((Player) event.getWhoClicked(), hunt);
                        });

                orderMenu.addItem(i, orderItemGui);
            }
        }

        if (registry.getHuntService().isMultiHunt()) {
            orderMenu.setPaginationButtonBuilder((type, inventory) -> {
                if (type == HBPaginationButtonType.BACK_BUTTON) {
                    return new ItemGUI(registry.getConfigService().guiBackIcon()
                            .setName(registry.getLanguageService().message("Gui.Back"))
                            .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                            .toItemStack())
                            .addOnClickEvent(event -> openOrderGui((Player) event.getWhoClicked()));
                }

                return null;
            });
        }

        player.openInventory(orderMenu.getInventory());
    }
}
