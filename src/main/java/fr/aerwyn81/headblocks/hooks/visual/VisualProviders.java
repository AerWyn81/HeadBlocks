package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.utils.internal.LogUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class VisualProviders {

    private static final Map<String, String> PLUGINS = knownPlugins();

    private VisualProviders() {
    }

    public static boolean isKnown(String prefix) {
        return PLUGINS.containsKey(prefix);
    }

    public static String pluginOf(String prefix) {
        return PLUGINS.getOrDefault(prefix, prefix);
    }

    private static Map<String, String> knownPlugins() {
        Map<String, String> plugins = new LinkedHashMap<>();
        plugins.put("nexo", "Nexo");
        plugins.put("itemsadder", "ItemsAdder");
        plugins.put("oraxen", "Oraxen");
        plugins.put("mythicmobs", "MythicMobs");
        plugins.put("modelengine", "ModelEngine");
        plugins.put("bettermodel", "BetterModel");
        plugins.put("fancynpcs", "FancyNpcs");
        plugins.put("citizens", "Citizens");
        plugins.put("znpcs", "ZNPCsPlus");
        return plugins;
    }

    public static Map<String, VisualProviderHook> detect(Predicate<String> enabled) {
        Map<String, VisualProviderHook> providers = new LinkedHashMap<>();
        for (var entry : PLUGINS.entrySet()) {
            var plugin = entry.getValue();
            if (!enabled.test(plugin)) {
                continue;
            }

            try {
                providers.put(entry.getKey(), create(entry.getKey()).get());
                LogUtil.success("{0} successfully hooked!", plugin);
            } catch (LinkageError | RuntimeException e) {
                LogUtil.error("Cannot hook {0}: {1}. Please update it or report the error on HeadBlocks discord.", plugin, e.toString());
            }
        }
        return providers;
    }

    private static Supplier<VisualProviderHook> create(String prefix) {
        return switch (prefix) {
            case "nexo" -> NexoHook::new;
            case "itemsadder" -> ItemsAdderHook::new;
            case "oraxen" -> OraxenHook::new;
            case "mythicmobs" -> MythicMobsHook::new;
            case "modelengine" -> ModelEngineHook::new;
            case "bettermodel" -> BetterModelHook::new;
            case "fancynpcs" -> FancyNpcsHook::new;
            case "citizens" -> CitizensHook::new;
            default -> ZNPCsHook::new;
        };
    }
}
