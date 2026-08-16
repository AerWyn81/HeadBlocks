package fr.aerwyn81.headblocks.data.hunt.requirement.area;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AreaProviderFactoryTest {

    @Test
    void fromSection_null_returnsNull() {
        assertThat(AreaProviderFactory.fromSection(null)).isNull();
    }

    @Test
    void fromSection_unknownType_returnsNull() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("area");
        section.set("type", "sphere");

        assertThat(AreaProviderFactory.fromSection(section)).isNull();
    }

    @Test
    void fromSection_cuboid_returnsCuboidProvider() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("area");
        section.set("type", "cuboid");
        section.set("world", "world");
        section.set("min.x", 0);
        section.set("min.y", 60);
        section.set("min.z", 0);
        section.set("max.x", 10);
        section.set("max.y", 70);
        section.set("max.z", 10);

        AreaProvider provider = AreaProviderFactory.fromSection(section);

        assertThat(provider).isInstanceOf(CuboidAreaProvider.class);
        assertThat(provider.getType()).isEqualTo("cuboid");
    }

    @Test
    void fromSection_worldguard_returnsWorldGuardProvider() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("area");
        section.set("type", "worldguard");
        section.set("world", "world");
        section.set("region", "spawn");

        AreaProvider provider = AreaProviderFactory.fromSection(section);

        assertThat(provider).isInstanceOf(WorldGuardAreaProvider.class);
        assertThat(((WorldGuardAreaProvider) provider).getRegionId()).isEqualTo("spawn");
    }
}
