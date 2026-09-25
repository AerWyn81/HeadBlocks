package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NpcProviderHook<N> extends HandleProviderHook<NpcProviderHook.Handle<N>> {

    private static final double WIDTH = 0.8;
    private static final double HEIGHT = 2.0;

    private final Map<Object, Handle<N>> byNpc = new ConcurrentHashMap<>();

    public record Handle<N>(N npc, Set<UUID> hidden) {
    }

    protected abstract N createNpc(Location anchor, HeadContent content, RenderSettings settings);

    protected abstract void destroyNpc(N npc);

    protected abstract void hide(N npc, Player player);

    protected abstract void show(N npc, Player player);

    @Override
    public boolean exists(String id) {
        return id != null && !id.isBlank();
    }

    @Override
    public ItemStack icon(HeadContent content) {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    @Override
    protected final Handle<N> create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
        var npc = createNpc(anchor, content, settings);
        if (npc == null) {
            return null;
        }

        var handle = new Handle<>(npc, ConcurrentHashMap.<UUID>newKeySet());
        byNpc.put(npc, handle);
        return handle;
    }

    @Override
    protected final void destroy(Handle<N> handle) {
        byNpc.remove(handle.npc());
        destroyNpc(handle.npc());
    }

    @Override
    protected Hitbox hitbox(Handle<N> handle, HeadContent content, RenderSettings settings) {
        return Hitbox.of(content, settings, WIDTH, HEIGHT);
    }

    @Override
    protected void setVisible(Handle<N> handle, Player player, boolean visible) {
        if (visible) {
            if (handle.hidden().remove(player.getUniqueId())) {
                show(handle.npc(), player);
            }
        } else if (handle.hidden().add(player.getUniqueId())) {
            hide(handle.npc(), player);
        }
    }

    protected boolean isHidden(Object npc, Player player) {
        var handle = npc == null ? null : byNpc.get(npc);
        return handle != null && handle.hidden().contains(player.getUniqueId());
    }
}
