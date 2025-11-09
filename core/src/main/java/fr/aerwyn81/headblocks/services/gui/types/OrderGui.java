package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OrderGui extends GuiBase {

    public void openOrderGui(Player player) {
        HBMenu orderMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleOrder"), true, 5);

        List<HeadLocation> headLocations = HeadService.getHeadLocations()
                .stream()
                .sorted(Comparator.comparingInt(HeadLocation::getOrderIndex))
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

        player.openInventory(orderMenu.getInventory());
    }
}
