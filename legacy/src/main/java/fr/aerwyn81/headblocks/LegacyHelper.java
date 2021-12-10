package fr.aerwyn81.headblocks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LegacyHelper implements LegacySupport {
    @Override
    public ItemStack getItemStackInHand(Object player) {
        return ((Player) player).getInventory().getItemInHand();
    }
}
