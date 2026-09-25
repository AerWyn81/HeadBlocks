package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import lol.pyr.znpcsplus.api.NpcApiProvider;
import lol.pyr.znpcsplus.api.event.NpcSpawnEvent;
import lol.pyr.znpcsplus.api.npc.NpcEntry;
import lol.pyr.znpcsplus.api.skin.SkinDescriptor;
import lol.pyr.znpcsplus.util.NpcLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.UUID;

public class ZNPCsHook extends NpcProviderHook<NpcEntry> {

    private static final String DEFAULT_TYPE = "player";

    @Override
    public String prefix() {
        return "znpcs";
    }

    @Override
    public String pluginName() {
        return "ZNPCsPlus";
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(ignoreCancelled = true)
            public void onSpawn(NpcSpawnEvent e) {
                if (isHidden(e.getEntry(), e.getPlayer())) {
                    e.setCancelled(true);
                }
            }
        }, plugin);
    }

    @Override
    protected NpcEntry createNpc(Location anchor, HeadContent content, RenderSettings settings) {
        var api = NpcApiProvider.get();
        var typeName = Objects.requireNonNullElse(content.option("type"), DEFAULT_TYPE).toLowerCase();
        var type = api.getNpcTypeRegistry().getByName(typeName);
        if (type == null) {
            throw new IllegalStateException("unknown ZNPCsPlus type " + typeName);
        }

        var entry = api.getNpcRegistry().create("headblocks_" + UUID.randomUUID().toString().replace("-", ""),
                Objects.requireNonNull(anchor.getWorld()), type, new NpcLocation(anchor));
        entry.setSave(false);
        entry.setAllowCommandModification(false);
        entry.setProcessed(true);

        var npc = entry.getNpc();
        var skin = api.getPropertyRegistry().getByName("skin", SkinDescriptor.class);
        if (skin != null && DEFAULT_TYPE.equals(typeName)) {
            npc.setProperty(skin, api.getSkinDescriptorFactory().createStaticDescriptor(content.value()));
        }

        var name = content.option("name");
        if (name != null) {
            npc.getHologram().addLine(MessageUtils.colorize(name));
        }

        npc.setEnabled(true);
        return entry;
    }

    @Override
    protected void destroyNpc(NpcEntry npc) {
        NpcApiProvider.get().getNpcRegistry().delete(npc.getId());
    }

    @Override
    protected void hide(NpcEntry npc, Player player) {
        npc.getNpc().hide(player);
    }

    @Override
    protected void show(NpcEntry npc, Player player) {
        if (npc.getNpc().getWorld() == player.getWorld()) {
            npc.getNpc().show(player);
        }
    }

    @Override
    protected void rotate(NpcProviderHook.Handle<NpcEntry> handle, Entity base, float yaw) {
        var npc = handle.npc().getNpc();
        var location = npc.getLocation();
        npc.setLocation(new NpcLocation(location.getX(), location.getY(), location.getZ(), yaw, location.getPitch()));
    }
}
