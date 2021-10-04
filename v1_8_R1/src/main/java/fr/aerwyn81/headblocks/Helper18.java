package fr.aerwyn81.headblocks;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Helper18 implements IVersionCompatibility {

    @Override
    public ItemStack createHeadItemStack() {
        return new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
    }

    @Override
    public boolean isLeftHand(Object event) {
        return false;
    }

    @Override
    public ItemStack getItemStackInHand(Object player) {
        return ((Player) player).getInventory().getItemInHand();
    }

    @Override
    public void spawnParticle(Object location) {
        Location loc = (Location) location;
        loc.getWorld().playEffect(loc.clone().add(.5f, .1f, .5f), Effect.HAPPY_VILLAGER, Integer.MAX_VALUE);
    }
}
