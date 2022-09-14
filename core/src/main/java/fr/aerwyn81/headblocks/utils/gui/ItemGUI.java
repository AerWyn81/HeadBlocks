package fr.aerwyn81.headblocks.utils.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * All credits of this code go to @SamJakob (SpiGUI)
 * https://github.com/SamJakob/SpiGUI
 */
public class ItemGUI {
    private Consumer<InventoryClickEvent> onClickEvent;
    private ItemStack icon;

    private final boolean isClickable;
    private ItemStack iconBlocked;

    public ItemGUI(ItemStack icon) {
        this(icon, true);
    }

    public ItemGUI(ItemStack icon, boolean isClickable) {
        this.isClickable = isClickable;
        this.icon = icon;
    }

    public void setOnClickEvent(Consumer<InventoryClickEvent> clickEvent) {
        this.onClickEvent = clickEvent;
    }

    public ItemGUI addOnClickEvent(Consumer<InventoryClickEvent> clickEvent) {
        this.onClickEvent = clickEvent;
        return this;
    }

    public Consumer<InventoryClickEvent> getOnClickEvent() {
        return onClickEvent;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public boolean isClickable() {
        return isClickable;
    }

    public ItemStack getIconBlocked() {
        return iconBlocked;
    }

    public void setIconBlocked(ItemStack iconBlocked) {
        this.iconBlocked = iconBlocked;
    }
}
