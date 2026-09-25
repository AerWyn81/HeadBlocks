package fr.aerwyn81.headblocks.hooks.visual;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisualProvidersTest {

    private static final List<String> PREFIXES = List.of("nexo", "itemsadder", "oraxen", "mythicmobs", "modelengine",
            "bettermodel", "fancynpcs", "citizens", "znpcs");

    @Test
    void knownPrefixes_nameTheirPlugin() {
        assertThat(VisualProviders.isKnown("nexo")).isTrue();
        assertThat(VisualProviders.isKnown("hdb")).isFalse();
        assertThat(VisualProviders.pluginOf("znpcs")).isEqualTo("ZNPCsPlus");
        assertThat(VisualProviders.pluginOf("mythicmobs")).isEqualTo("MythicMobs");
        assertThat(VisualProviders.pluginOf("other")).isEqualTo("other");
    }

    @Test
    void detect_withoutAnyPlugin_isEmpty() {
        assertThat(VisualProviders.detect(plugin -> false)).isEmpty();
    }

    @Test
    void detect_onlyAsksForTheKnownPlugins() {
        List<String> asked = new ArrayList<>();

        VisualProviders.detect(plugin -> {
            asked.add(plugin);
            return false;
        });

        assertThat(asked).containsExactly("Nexo", "ItemsAdder", "Oraxen", "MythicMobs", "ModelEngine",
                "BetterModel", "FancyNpcs", "Citizens", "ZNPCsPlus");
    }

    @Test
    void detect_hooksEveryEnabledPluginWithoutTouchingItsApi() {
        var providers = VisualProviders.detect(plugin -> true);

        assertThat(providers.keySet()).containsExactlyElementsOf(PREFIXES);
        providers.forEach((prefix, hook) -> {
            assertThat(hook.prefix()).isEqualTo(prefix);
            assertThat(hook.pluginName()).isEqualTo(VisualProviders.pluginOf(prefix));
        });
    }

    @Test
    void detect_onlyHooksTheEnabledPlugins() {
        var providers = VisualProviders.detect(plugin -> plugin.equals("Citizens"));

        assertThat(providers.keySet()).containsExactly("citizens");
        assertThat(providers.get("citizens")).isInstanceOf(CitizensHook.class);
    }
}
