package fr.aerwyn81.headblocks.hooks.visual;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.events.NpcSpawnEvent;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class FancyNpcsHook extends NpcProviderHook<Npc> {

    private static final UUID CREATOR = new UUID(0, 0);
    private static final String EMPTY_NAME = "<empty>";

    @Override
    public String prefix() {
        return "fancynpcs";
    }

    @Override
    public String pluginName() {
        return "FancyNpcs";
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(ignoreCancelled = true)
            public void onSpawn(NpcSpawnEvent e) {
                if (isHidden(e.getNpc(), e.getPlayer())) {
                    e.setCancelled(true);
                }
            }
        }, plugin);
    }

    @Override
    protected Npc createNpc(Location anchor, HeadContent content, RenderSettings settings) {
        var data = new NpcData("headblocks_" + UUID.randomUUID(), CREATOR, anchor);
        data.setSkin(content.value());
        data.setDisplayName(content.option("name") == null ? EMPTY_NAME : content.option("name"));
        data.setShowInTab(false);
        data.setCollidable(false);
        data.setTurnToPlayer(content.optionBoolean("look", false));
        data.setGlowing(settings.glow());
        if (settings.scale() != 1.0) {
            data.setScale((float) settings.scale());
        }

        var type = content.option("type");
        if (type != null) {
            data.setType(EntityType.valueOf(type.toUpperCase()));
        }

        var plugin = FancyNpcsPlugin.get();
        var npc = plugin.getNpcAdapter().apply(data);
        npc.setSaveToFile(false);
        plugin.getNpcManager().registerNpc(npc);
        npc.create();
        npc.spawnForAll();
        return npc;
    }

    @Override
    protected void destroyNpc(Npc npc) {
        npc.removeForAll();
        FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
    }

    @Override
    protected void hide(Npc npc, Player player) {
        npc.remove(player);
    }

    @Override
    protected void show(Npc npc, Player player) {
        npc.checkAndUpdateVisibility(player);
    }

    @Override
    protected void rotate(NpcProviderHook.Handle<Npc> handle, Entity base, float yaw) {
        var data = handle.npc().getData();
        var location = data.getLocation().clone();
        location.setYaw(yaw);
        data.setLocation(location);
        handle.npc().moveForAll();
    }
}
