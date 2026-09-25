package fr.aerwyn81.headblocks.hooks.visual;

import de.tr7zw.changeme.nbtapi.NBT;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ItemsAdderHook extends ItemProviderHook {

    private static final String DATA_KEY = "itemsadder";

    @Override
    public String prefix() {
        return "itemsadder";
    }

    @Override
    public String pluginName() {
        return "ItemsAdder";
    }

    @Override
    public boolean isReady() {
        return ItemsAdder.areItemsLoaded();
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onDataLoaded(ItemsAdderLoadDataEvent e) {
                onReload.run();
            }
        }, plugin);
    }

    @Override
    public boolean exists(String id) {
        return CustomStack.isInRegistry(id);
    }

    @Override
    protected ItemStack item(String id) {
        var stack = CustomStack.isInRegistry(id) ? CustomStack.getInstance(id) : null;
        return stack == null ? null : stack.getItemStack();
    }

    @Override
    protected ItemStack strip(ItemStack item) {
        var stripped = super.strip(item);
        NBT.modify(stripped, nbt -> {
            nbt.removeKey(DATA_KEY);
        });
        return stripped;
    }
}
