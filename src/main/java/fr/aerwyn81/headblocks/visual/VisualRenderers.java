package fr.aerwyn81.headblocks.visual;

import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.VisualForm;
import fr.aerwyn81.headblocks.hooks.visual.VisualProviderHook;
import fr.aerwyn81.headblocks.visual.renderers.*;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class VisualRenderers {

    private final Map<VisualForm, BlockRenderer> blockRenderers = new EnumMap<>(VisualForm.class);
    private final Map<VisualForm, EntityRenderer> entityRenderers = new EnumMap<>(VisualForm.class);
    private final Map<String, VisualProviderHook> providers;

    public VisualRenderers(Map<String, VisualProviderHook> providers) {
        this.providers = providers == null ? Collections.emptyMap() : providers;

        blockRenderers.put(VisualForm.HEAD_BLOCK, new HeadBlockRenderer());
        blockRenderers.put(VisualForm.BLOCK, new PlainBlockRenderer());
        entityRenderers.put(VisualForm.ITEM_DISPLAY, new ItemDisplayRenderer());
        entityRenderers.put(VisualForm.BLOCK_DISPLAY, new BlockDisplayRenderer());
        entityRenderers.put(VisualForm.TEXT_DISPLAY, new TextDisplayRenderer());
        entityRenderers.put(VisualForm.ITEM_FRAME, new ItemFrameRenderer());
        entityRenderers.put(VisualForm.MOB, new MobRenderer());
    }

    public BlockRenderer block(VisualForm form) {
        return blockRenderers.get(form);
    }

    public EntityRenderer entity(VisualForm form, HeadContent content) {
        if (form == VisualForm.EXTERNAL) {
            return content == null ? null : provider(content.provider());
        }
        return entityRenderers.get(form);
    }

    public Map<String, VisualProviderHook> providers() {
        return providers;
    }

    public VisualProviderHook provider(String prefix) {
        var provider = prefix == null ? null : providers.get(prefix);
        return provider != null && provider.isAvailable() ? provider : null;
    }

    public ItemStack icon(HeadContent content) {
        if (content != null && content.kind() == ContentKind.EXTERNAL) {
            var provider = provider(content.provider());
            if (provider != null) {
                return provider.icon(content);
            }
        }
        return ContentItems.itemOf(content);
    }
}
