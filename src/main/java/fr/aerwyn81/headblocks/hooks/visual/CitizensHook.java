package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CitizensHook extends HandleProviderHook<NPC> {

    private NPCRegistry npcRegistry;

    @Override
    public String prefix() {
        return "citizens";
    }

    @Override
    public String pluginName() {
        return "Citizens";
    }

    @Override
    public boolean exists(String id) {
        return id != null && !id.isBlank();
    }

    @Override
    public ItemStack icon(HeadContent content) {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    @Override
    protected boolean usesBase() {
        return false;
    }

    @Override
    protected NPC create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
        var type = content.option("type") == null ? EntityType.PLAYER : EntityType.valueOf(content.option("type").toUpperCase());
        var name = content.option("name");

        var npc = npcRegistry().createNPC(type, name == null ? "" : MessageUtils.colorize(name));
        npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, name != null);
        npc.data().set(NPC.Metadata.COLLIDABLE, false);
        npc.data().set(NPC.Metadata.SILENT, true);
        npc.setProtected(true);
        npc.getOrAddTrait(LookClose.class).lookClose(content.optionBoolean("look", false));
        if (type == EntityType.PLAYER) {
            npc.getOrAddTrait(SkinTrait.class).setSkinName(content.value());
        }

        if (!npc.spawn(anchor) || npc.getEntity() == null) {
            npc.destroy();
            return null;
        }

        npc.getEntity().setGlowing(settings.glow());
        return npc;
    }

    @Override
    protected List<Entity> entitiesOf(NPC handle) {
        return handle.getEntity() == null ? List.of() : List.of(handle.getEntity());
    }

    @Override
    protected void destroy(NPC handle) {
        handle.destroy();
    }

    @Override
    protected Hitbox hitbox(NPC handle, HeadContent content, RenderSettings settings) {
        var entity = handle.getEntity();
        return entity == null ? new Hitbox(1.0, 2.0) : new Hitbox(entity.getWidth(), entity.getHeight());
    }

    private NPCRegistry npcRegistry() {
        if (npcRegistry == null) {
            npcRegistry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
        }
        return npcRegistry;
    }
}
