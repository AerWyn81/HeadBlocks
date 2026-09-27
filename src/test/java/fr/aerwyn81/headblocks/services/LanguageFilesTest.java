package fr.aerwyn81.headblocks.services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageFilesTest {

    private static Set<String> keysOf(String file) throws Exception {
        try (var stream = Objects.requireNonNull(LanguageFilesTest.class.getResourceAsStream("/language/" + file));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var yaml = new YamlConfiguration();
            yaml.load(reader);
            var keys = new TreeSet<String>();
            yaml.getKeys(true).stream().filter(key -> !yaml.isConfigurationSection(key)).forEach(keys::add);
            return keys;
        }
    }

    @Test
    void spawnKeys_existInBothLanguages() throws Exception {
        var en = keysOf("messages_en.yml");
        var fr = keysOf("messages_fr.yml");

        var spawnEn = en.stream().filter(key -> key.contains("Spawn") || key.contains("FixedPosition")).toList();
        var spawnFr = fr.stream().filter(key -> key.contains("Spawn") || key.contains("FixedPosition")).toList();

        assertThat(spawnEn).isNotEmpty().containsExactlyElementsOf(spawnFr);
        assertThat(en).contains("Gui.SpawnConfigCompletion_FIRST_WINS", "Help.Spawn", "Hunt.Behavior.FixedPosition");
    }
}
