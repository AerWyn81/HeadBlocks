package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class OnPlayerClickInventoryEvent implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() == null || !(event.getInventory().getHolder() instanceof HBMenu)) {
            return;
        }

        var clickedGui = (HBMenu) event.getInventory().getHolder();

        if (!clickedGui.getOwner().equals(HeadBlocks.getInstance())) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() >= clickedGui.getPageSize()) {
            int offset = event.getRawSlot() - clickedGui.getPageSize();
            HBPaginationButtonType buttonType = HBPaginationButtonType.forSlot(offset);

            ItemGUI paginationButton = null;

            if (clickedGui.getPaginationButtonBuilder() != null) {
                paginationButton = clickedGui.getPaginationButtonBuilder().buildPaginationButton(buttonType, clickedGui);
            }

            if (paginationButton == null) {
                paginationButton = GuiService.getDefaultPaginationButtonBuilder(buttonType, clickedGui);
            }

            if (paginationButton != null && paginationButton.getOnClickEvent() != null) {
                paginationButton.getOnClickEvent().accept(event);
            }

            return;
        }

        ItemGUI button = clickedGui.getItem(clickedGui.getCurrentPage(), event.getRawSlot());
        if (button != null && button.getOnClickEvent() != null && button.isClickable()) {
            button.getOnClickEvent().accept(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != null && event.getInventory().getHolder() instanceof HBMenu) {
            var clickedGui = (HBMenu) event.getInventory().getHolder();

            if (!clickedGui.getOwner().equals(HeadBlocks.getInstance())) return;

            if (clickedGui.getOnClose() != null)
                clickedGui.getOnClose().accept(clickedGui);
        }
    }
}
