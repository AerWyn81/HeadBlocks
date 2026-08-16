package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceholderRequirementTest {

    private static final String PLACEHOLDER = "%vault_eco_balance%";

    @Mock
    ServiceRegistry registry;

    @Mock
    PluginProvider pluginProvider;

    @Mock
    LanguageService languageService;

    @Mock
    Player player;

    @Mock
    HeadLocation head;

    @Mock
    HBHunt hunt;

    @Test
    void getType_isPlaceholder() {
        assertThat(new PlaceholderRequirement(registry, PLACEHOLDER, ComparisonOperator.EQUALS, "1")
                .getType()).isEqualTo(RequirementType.PLACEHOLDER);
    }

    @Test
    void check_comparisonHolds_isSatisfied() {
        when(registry.getPluginProvider()).thenReturn(pluginProvider);
        when(pluginProvider.isPlaceholderApiActive()).thenReturn(true);

        try (MockedStatic<PlaceholderAPI> papi = mockStatic(PlaceholderAPI.class)) {
            papi.when(() -> PlaceholderAPI.setPlaceholders(player, PLACEHOLDER)).thenReturn("1500");

            var requirement = new PlaceholderRequirement(registry, PLACEHOLDER,
                    ComparisonOperator.GREATER_OR_EQUALS, "1000");

            assertThat(requirement.check(player, head, hunt).satisfied()).isTrue();
        }
    }

    @Test
    void check_comparisonFails_reportsExpectedAndActual() {
        when(registry.getPluginProvider()).thenReturn(pluginProvider);
        when(pluginProvider.isPlaceholderApiActive()).thenReturn(true);
        when(registry.getLanguageService()).thenReturn(languageService);
        when(languageService.message("Hunt.Requirement.PlaceholderUnmet"))
                .thenReturn("%placeholder% %operator% %expected% (%actual%)");

        try (MockedStatic<PlaceholderAPI> papi = mockStatic(PlaceholderAPI.class)) {
            papi.when(() -> PlaceholderAPI.setPlaceholders(player, PLACEHOLDER)).thenReturn("10");

            var result = new PlaceholderRequirement(registry, PLACEHOLDER,
                    ComparisonOperator.GREATER_OR_EQUALS, "1000").check(player, head, hunt);

            assertThat(result.satisfied()).isFalse();
            assertThat(result.reason()).isEqualTo("%vault_eco_balance% >= 1000 (10)");
        }
    }

    @Test
    void check_placeholderApiMissing_failsOpen() {
        when(registry.getPluginProvider()).thenReturn(pluginProvider);
        when(pluginProvider.isPlaceholderApiActive()).thenReturn(false);
        when(hunt.getId()).thenReturn("advanced");

        var requirement = new PlaceholderRequirement(registry, PLACEHOLDER, ComparisonOperator.EQUALS, "1");

        assertThat(requirement.check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void isComplete_needsAPlaceholderAndAValue() {
        assertThat(new PlaceholderRequirement(registry, PLACEHOLDER, ComparisonOperator.EQUALS, "1")
                .isComplete()).isTrue();
        assertThat(new PlaceholderRequirement(registry, " ", ComparisonOperator.EQUALS, "1")
                .isComplete()).isFalse();
        assertThat(new PlaceholderRequirement(registry, PLACEHOLDER, ComparisonOperator.EQUALS, null)
                .isComplete()).isFalse();
    }

    @Test
    void saveTo_thenFromConfig_roundTripsEveryField() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirement");
        new PlaceholderRequirement(registry, PLACEHOLDER, ComparisonOperator.LESS_THAN, "50").saveTo(section);

        PlaceholderRequirement loaded = PlaceholderRequirement.fromConfig(registry, section);

        assertThat(loaded.placeholder()).isEqualTo(PLACEHOLDER);
        assertThat(loaded.operator()).isEqualTo(ComparisonOperator.LESS_THAN);
        assertThat(loaded.expected()).isEqualTo("50");
    }
}
