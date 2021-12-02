package fr.aerwyn81.headblocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class Helper19 implements IVersionCompatibility {

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
    public void spawnParticle(Object location) {
        Location loc = (Location) location;
        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(.5f, .1f, .5f), 10, .5f, .5f, .5f, 0);
    }

    @Override
    public void sendTitle(Object player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        // not supported
    }
}
