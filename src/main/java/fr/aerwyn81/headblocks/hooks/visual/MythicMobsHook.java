package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import fr.aerwyn81.headblocks.visual.renderers.MobRenderer;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicReloadedEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.DespawnMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class MythicMobsHook extends HandleProviderHook<ActiveMob> {

    @Override
    public String prefix() {
        return "mythicmobs";
    }

    @Override
    public String pluginName() {
        return "MythicMobs";
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onReloaded(MythicReloadedEvent e) {
                onReload.run();
            }
        }, plugin);
    }

    @Override
    public boolean exists(String id) {
        return MythicBukkit.inst().getMobManager().getMythicMob(id).isPresent();
    }

    @Override
    public ItemStack icon(HeadContent content) {
        var type = MythicBukkit.inst().getMobManager().getMythicMob(content.value())
                .map(mob -> Material.matchMaterial(mob.getEntityTypeString() + "_SPAWN_EGG"))
                .orElse(null);
        return new ItemStack(type == null ? Material.ZOMBIE_SPAWN_EGG : type);
    }

    @Override
    public boolean anchored() {
        return true;
    }

    @Override
    protected boolean usesBase() {
        return false;
    }

    @Override
    protected ActiveMob create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
        var mob = MythicBukkit.inst().getMobManager().getMythicMob(content.value())
                .orElseThrow(() -> new IllegalStateException("unknown MythicMob " + content.value()));

        var active = mob.spawn(BukkitAdapter.adapt(anchor), content.optionDouble("level", 1));
        if (active == null || active.getEntity() == null) {
            return null;
        }

        active.setDespawnMode(DespawnMode.NEVER);
        MobRenderer.freeze(active.getEntity().getBukkitEntity(), settings);
        return active;
    }

    @Override
    protected List<Entity> entitiesOf(ActiveMob handle) {
        return List.of(handle.getEntity().getBukkitEntity());
    }

    @Override
    protected void destroy(ActiveMob handle) {
        handle.setDespawned();
        MythicBukkit.inst().getMobManager().unregisterActiveMob(handle);
    }

    @Override
    protected Hitbox hitbox(ActiveMob handle, HeadContent content, RenderSettings settings) {
        var entity = handle.getEntity().getBukkitEntity();
        return new Hitbox(entity.getWidth(), entity.getHeight());
    }
}
