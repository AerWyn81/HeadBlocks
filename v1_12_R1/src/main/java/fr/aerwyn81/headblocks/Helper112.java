package fr.aerwyn81.headblocks;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class Helper112 implements IVersionCompatibility {

    @Override
    public ItemStack createHeadItemStack() {
        return new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
    }

    @Override
    public boolean isLeftHand(Object event) {
        return ((PlayerInteractEvent) event).getHand() == EquipmentSlot.OFF_HAND;
    }

    @Override
    public ItemStack getItemStackInHand(Object player) {
        return ((Player) player).getInventory().getItemInMainHand();
    }

    @Override
    public void sendTitle(Object player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        ((Player) player).sendTitle(title, subTitle, fadeIn, stay, fadeOut);
    }
}
