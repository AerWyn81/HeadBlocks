package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaMessageMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.CuboidAreaProvider;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaRequirementTest {

    @Mock
    ServiceRegistry registry;

    @Mock
    LanguageService languageService;

    @Mock
    AreaProvider area;

    @Mock
    Player player;

    @Mock
    Location playerLocation;

    @Mock
    HeadLocation head;

    @Mock
    HBHunt hunt;

    private AreaRequirement requirement(AreaProvider provider, Location returnPoint, boolean blockExit) {
        return new AreaRequirement(registry, provider, returnPoint, blockExit, false, AreaMessageMode.CHAT);
    }

    @Test
    void getType_isArea() {
        assertThat(requirement(area, null, false).getType()).isEqualTo(RequirementType.AREA);
    }

    @Test
    void check_playerInside_isSatisfied() {
        when(player.getLocation()).thenReturn(playerLocation);
        when(area.isAvailable()).thenReturn(true);
        when(area.contains(playerLocation)).thenReturn(true);

        assertThat(requirement(area, null, false).check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_playerOutside_isUnmet() {
        when(player.getLocation()).thenReturn(playerLocation);
        when(area.isAvailable()).thenReturn(true);
        when(area.contains(playerLocation)).thenReturn(false);
        when(registry.getLanguageService()).thenReturn(languageService);
        when(languageService.message("Hunt.Requirement.AreaUnmet")).thenReturn("stay inside");

        var result = requirement(area, null, false).check(player, head, hunt);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.reason()).isEqualTo("stay inside");
    }

    @Test
    void check_noArea_failsOpen() {
        assertThat(requirement(null, null, false).check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_areaUnavailable_failsOpen() {
        when(area.isAvailable()).thenReturn(false);

        assertThat(requirement(area, null, false).check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_disabledArea_failsOpen() {
        AreaRequirement requirement = requirement(area, null, false);
        requirement.setDisabled(true);

        assertThat(requirement.check(player, head, hunt).satisfied()).isTrue();
        verifyNoInteractions(area);
    }

    @Test
    void isComplete_needsAnArea() {
        assertThat(requirement(null, null, false).isComplete()).isFalse();
        assertThat(requirement(area, null, false).isComplete()).isTrue();
    }

    @Test
    void isComplete_blockExitAlsoNeedsAReturnPoint() {
        assertThat(requirement(area, null, true).isComplete()).isFalse();
        assertThat(requirement(area, mock(Location.class), true).isComplete()).isTrue();
    }

    @Test
    void saveTo_thenFromConfig_roundTripsAreaAndOptions() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        Location returnPoint = new Location(world, 5.0, 65.0, 5.0, 90f, 10f);

        AreaRequirement original = new AreaRequirement(registry,
                new CuboidAreaProvider("world", 0, 60, 0, 10, 70, 10),
                returnPoint, true, true, AreaMessageMode.TITLE);

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirement");
        original.saveTo(section);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);

            AreaRequirement loaded = AreaRequirement.fromConfig(registry, section);

            assertThat(loaded.area()).isInstanceOf(CuboidAreaProvider.class);
            assertThat(loaded.area().getWorldName()).isEqualTo("world");
            assertThat(loaded.blockExit()).isTrue();
            assertThat(loaded.resetOnLeave()).isTrue();
            assertThat(loaded.messageMode()).isEqualTo(AreaMessageMode.TITLE);
            assertThat(loaded.returnPoint()).isNotNull();
            assertThat(loaded.returnPoint().getYaw()).isEqualTo(90f);
        }
    }

    @Test
    void fromConfig_readsTheLegacyZoneKey() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("behaviors.zone");
        ConfigurationSection legacy = section.createSection("zone");
        legacy.set("type", "cuboid");
        legacy.set("world", "world");
        legacy.set("min.x", 0);
        legacy.set("min.y", 60);
        legacy.set("min.z", 0);
        legacy.set("max.x", 10);
        legacy.set("max.y", 70);
        legacy.set("max.z", 10);

        AreaRequirement loaded = AreaRequirement.fromConfig(registry, section);

        assertThat(loaded.area()).isInstanceOf(CuboidAreaProvider.class);
        assertThat(loaded.isComplete()).isTrue();
    }

    @Test
    void fromConfig_nullSection_returnsNull() {
        assertThat(AreaRequirement.fromConfig(registry, null)).isNull();
    }
}
