package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.CuboidZoneProvider;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneMessageMode;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.Bukkit;
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
class ZoneBehaviorTest {

    @Mock
    ServiceRegistry registry;

    @Mock
    LanguageService languageService;

    @Mock
    ConfigService configService;

    @Mock
    Player player;

    @Mock
    HeadLocation headLocation;

    @Test
    void getId_returnsZone() {
        ZoneBehavior behavior = new ZoneBehavior(registry, null, null, true, true, ZoneMessageMode.CHAT);
        assertThat(behavior.getId()).isEqualTo("zone");
    }

    @Test
    void canPlayerClick_alwaysAllows() {
        ZoneBehavior behavior = new ZoneBehavior(registry, null, null, true, true, ZoneMessageMode.CHAT);
        HBHunt hunt = new HBHunt(configService, "h", "H", HuntState.ACTIVE, 1, "D");

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void isAccessGate_false() {
        ZoneBehavior behavior = new ZoneBehavior(registry, null, null, true, true, ZoneMessageMode.CHAT);
        assertThat(behavior.isAccessGate()).isFalse();
    }

    @Test
    void getDisplayInfo_returnsLanguageMessage() {
        when(registry.getLanguageService()).thenReturn(languageService);
        when(languageService.message("Hunt.Behavior.Zone")).thenReturn("Zone Mode");
        ZoneBehavior behavior = new ZoneBehavior(registry, null, null, true, true, ZoneMessageMode.CHAT);
        HBHunt hunt = new HBHunt(configService, "h", "H", HuntState.ACTIVE, 1, "D");

        assertThat(behavior.getDisplayInfo(player, hunt)).isEqualTo("Zone Mode");
    }

    @Test
    void fromConfig_nullSection_returnsEmptyZone() {
        ZoneBehavior behavior = ZoneBehavior.fromConfig(registry, null);

        assertThat(behavior).isNotNull();
        assertThat(behavior.zone()).isNull();
        assertThat(behavior.returnPoint()).isNull();
    }

    @Test
    void fromConfig_cuboidWithReturnPoint_parsesBoth() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("behaviors.zone");
        ConfigurationSection zone = section.createSection("zone");
        zone.set("type", "cuboid");
        zone.set("world", "world");
        zone.set("min.x", 0);
        zone.set("min.y", 60);
        zone.set("min.z", 0);
        zone.set("max.x", 10);
        zone.set("max.y", 70);
        zone.set("max.z", 10);
        ConfigurationSection rp = section.createSection("returnPoint");
        rp.set("world", "world");
        rp.set("x", 5.0);
        rp.set("y", 65.0);
        rp.set("z", 5.0);
        rp.set("yaw", 90.0);
        rp.set("pitch", 0.0);

        World world = mock(World.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);

            ZoneBehavior behavior = ZoneBehavior.fromConfig(registry, section);

            assertThat(behavior.zone()).isInstanceOf(CuboidZoneProvider.class);
            assertThat(behavior.returnPoint()).isNotNull();
            assertThat(behavior.returnPoint().getX()).isEqualTo(5.0);
            assertThat(behavior.returnPoint().getYaw()).isEqualTo(90.0f);
        }
    }
}