package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.services.gui.GuiBase;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.stream.Collectors;

public class ClickCounterGui extends GuiBase {

    public void openClickCounterGui(Player player) {
        GuiService.openHuntSelectionOrDirect(player, this::openClickCounterGuiForHunt);
    }

    public void openClickCounterGuiForHunt(Player player, Hunt hunt) {
        HBMenu clickCounterMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleClickCounter"), true, 5);

        List<HeadLocation> headLocations = HeadService.getHeadLocationsForHunt(hunt)
                .stream()
                .sorted((o1, o2) -> o2.getOrderIndex() - o1.getOrderIndex())
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

                            openClickCounterGuiForHunt((Player) event.getWhoClicked(), hunt);
                        });

                clickCounterMenu.addItem(i, orderItemGui);
            }
        }

        if (HuntService.isMultiHunt()) {
            clickCounterMenu.setPaginationButtonBuilder((type, inventory) -> {
                if (type == HBPaginationButtonType.BACK_BUTTON) {
                    return new ItemGUI(ConfigService.getGuiBackIcon()
                            .setName(LanguageService.getMessage("Gui.Back"))
                            .setLore(LanguageService.getMessages("Gui.BackLore"))
                            .toItemStack())
                            .addOnClickEvent(event -> openClickCounterGui((Player) event.getWhoClicked()));
                }

                return null;
            });
        }

        player.openInventory(clickCounterMenu.getInventory());
    }
}
