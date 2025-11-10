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

import java.util.List;
import java.util.stream.Collectors;

public class ClickCounterGui extends GuiBase {

    public void openClickCounterGui(Player player) {
        HBMenu clickCounterMenu = new HBMenu(HeadBlocks.getInstance(), LanguageService.getMessage("Gui.TitleClickCounter"), true, 5);

        List<HeadLocation> headLocations = HeadService.getHeadLocations()
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

                            openClickCounterGui((Player) event.getWhoClicked());
                        });

                clickCounterMenu.addItem(i, orderItemGui);
            }
        }

        player.openInventory(clickCounterMenu.getInventory());
    }
}
