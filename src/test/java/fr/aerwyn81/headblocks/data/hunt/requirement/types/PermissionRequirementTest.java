package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequirementTest {

    @Mock
    ServiceRegistry registry;

    @Mock
    LanguageService languageService;

    @Mock
    Player player;

    @Mock
    HeadLocation head;

    @Mock
    HBHunt hunt;

    @Test
    void getType_isPermission() {
        assertThat(new PermissionRequirement(registry, "hb.vip").getType()).isEqualTo(RequirementType.PERMISSION);
    }

    @Test
    void check_playerHasTheNode_isSatisfied() {
        when(player.hasPermission("hb.vip")).thenReturn(true);

        assertThat(new PermissionRequirement(registry, "hb.vip").check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_playerLacksTheNode_isUnmetWithTheNodeInTheReason() {
        when(player.hasPermission("hb.vip")).thenReturn(false);
        when(registry.getLanguageService()).thenReturn(languageService);
        when(languageService.message("Hunt.Requirement.PermissionUnmet")).thenReturn("need %permission%");

        var result = new PermissionRequirement(registry, "hb.vip").check(player, head, hunt);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.reason()).isEqualTo("need hb.vip");
    }

    @Test
    void isComplete_requiresANonBlankNode() {
        assertThat(new PermissionRequirement(registry, "hb.vip").isComplete()).isTrue();
        assertThat(new PermissionRequirement(registry, "  ").isComplete()).isFalse();
        assertThat(new PermissionRequirement(registry, null).isComplete()).isFalse();
    }

    @Test
    void saveTo_thenFromConfig_roundTripsTheNode() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirement");
        new PermissionRequirement(registry, "hb.vip").saveTo(section);

        assertThat(PermissionRequirement.fromConfig(registry, section).node()).isEqualTo("hb.vip");
    }

    @Test
    void fromConfig_nullSection_returnsNull() {
        assertThat(PermissionRequirement.fromConfig(registry, null)).isNull();
    }
}
