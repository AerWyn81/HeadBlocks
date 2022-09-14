package fr.aerwyn81.headblocks.utils.gui;

import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonBuilder;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * All credits of this code go to @SamJakob (SpiGUI)
 * https://github.com/SamJakob/SpiGUI
 */
public class HBMenu implements InventoryHolder {
    private final JavaPlugin owner;

    private int rowsPerPage;
    private String name;
    private int currentPage;
    private boolean isNestedMenu;

    private final Map<Integer, ItemGUI> items;

    private Consumer<HBMenu> onClose;

    private HBPaginationButtonBuilder paginationButtonBuilder;

    public HBMenu(JavaPlugin owner, String name, boolean isNestedMenu, int rowsPerPage) {
        this.owner = owner;

        setName(name);
        this.rowsPerPage = rowsPerPage;
        this.isNestedMenu = isNestedMenu;
        this.items = new HashMap<>();

        this.currentPage = 0;
    }

    public JavaPlugin getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public void setRowName(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = MessageUtils.colorize(name);
    }

    public void addItem(int slot, ItemGUI item) {
        items.put(slot, item);
    }

    public int getPageSize() {
        return rowsPerPage * 9;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    public boolean isNestedMenu() {
        return isNestedMenu;
    }

    public void setNestedMenu(boolean nestedMenu) {
        isNestedMenu = nestedMenu;
    }

    public void setPaginationButtonBuilder(HBPaginationButtonBuilder paginationButtonBuilder) {
        this.paginationButtonBuilder = paginationButtonBuilder;
    }

    public HBPaginationButtonBuilder getPaginationButtonBuilder() {
        return this.paginationButtonBuilder;
    }

    public void setItem(int page, int slot, ItemGUI button) {
        if (slot < 0 || slot > getPageSize())
            return;

        addItem((page * getPageSize()) + slot, button);
    }

    public void removeItem(int slot) {
        items.remove(slot);
    }

    public void removeItem(int page, int slot) {
        if (slot < 0 || slot > getPageSize())
            return;

        removeItem((page * getPageSize()) + slot);
    }

    public ItemGUI getItem(int slot) {
        if (slot < 0 || slot > getHighestFilledSlot())
            return null;

        return items.get(slot);
    }

    public ItemGUI getItem(int page, int slot) {
        if (slot < 0 || slot > getPageSize())
            return null;

        return getItem((page * getPageSize()) + slot);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    public int getMaxPage() {
        return (int) Math.ceil(((double) getHighestFilledSlot() + 1) / ((double) getPageSize()));
    }

    public int getHighestFilledSlot() {
        int slot = 0;

        for (int nextSlot : items.keySet()) {
            if (items.get(nextSlot) != null && nextSlot > slot)
                slot = nextSlot;
        }

        return slot;
    }

    public void nextPage(HumanEntity viewer) {
        if (currentPage < getMaxPage() - 1) {
            currentPage++;
            refreshInventory(viewer);
        }
    }

    public void previousPage(HumanEntity viewer) {
        if (currentPage > 0) {
            currentPage--;
            refreshInventory(viewer);
        }
    }

    public Consumer<HBMenu> getOnClose() {
        return this.onClose;
    }

    public void setOnClose(Consumer<HBMenu> onClose) {
        this.onClose = onClose;
    }

    public void refreshInventory(HumanEntity viewer) {
        if (!(viewer.getOpenInventory().getTopInventory().getHolder() instanceof HBMenu)
                || viewer.getOpenInventory().getTopInventory().getHolder() != this) {
            return;
        }

        if (viewer.getOpenInventory().getTopInventory().getSize() != getPageSize() + (getMaxPage() > 0 ? 9 : 0)) {
            viewer.openInventory(getInventory());
            return;
        }

        viewer.getOpenInventory().getTopInventory().setContents(getInventory().getContents());
    }

    @Override
    public Inventory getInventory() {
        boolean needsPagination = getMaxPage() > 0;

        Inventory inventory = Bukkit.createInventory(this, (needsPagination ? getPageSize() + 9 : getPageSize()), name);

        for (int key = currentPage * getPageSize(); key < (currentPage + 1) * getPageSize(); key++) {
            if (key > getHighestFilledSlot()) break;

            if (items.containsKey(key)) {
                var item = items.get(key);
                var icon = item.isClickable() ? item.getIcon() : item.getIconBlocked();

                inventory.setItem(key - (currentPage * getPageSize()), icon);
            }
        }

        if (needsPagination) {
            int pageSize = getPageSize();
            for (int i = pageSize; i < pageSize + 9; i++) {
                int offset = i - pageSize;

                ItemGUI paginationButton = GuiService.getDefaultPaginationButtonBuilder(HBPaginationButtonType.forSlot(offset), this);
                inventory.setItem(i, paginationButton != null ? paginationButton.getIcon() : null);
            }
        }

        return inventory;
    }
}
