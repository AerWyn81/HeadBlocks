package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaytimeRequirementTest {

    private static final int TICKS_PER_MINUTE = 20 * 60;

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

    private void playedMinutes(int minutes) {
        lenient().when(player.getStatistic(any(Statistic.class))).thenReturn(minutes * TICKS_PER_MINUTE);
    }

    @Test
    void getType_isPlaytime() {
        assertThat(new PlaytimeRequirement(registry, 60).getType()).isEqualTo(RequirementType.PLAYTIME);
    }

    @Test
    void check_enoughPlaytime_isSatisfied() {
        playedMinutes(120);

        assertThat(new PlaytimeRequirement(registry, 60).check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_exactlyTheRequiredTime_isSatisfied() {
        playedMinutes(60);

        assertThat(new PlaytimeRequirement(registry, 60).check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_notEnoughPlaytime_reportsWhatIsMissing() {
        playedMinutes(30);
        when(registry.getLanguageService()).thenReturn(languageService);
        when(languageService.message("Hunt.Requirement.PlaytimeUnmet"))
                .thenReturn("need %required%, played %played%, missing %missing%");

        var result = new PlaytimeRequirement(registry, 90).check(player, head, hunt);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.reason()).isEqualTo("need 1h30, played 30min, missing 1h");
    }

    @Test
    void format_readsAsHoursAndMinutes() {
        assertThat(PlaytimeRequirement.format(0)).isEqualTo("0min");
        assertThat(PlaytimeRequirement.format(45)).isEqualTo("45min");
        assertThat(PlaytimeRequirement.format(60)).isEqualTo("1h");
        assertThat(PlaytimeRequirement.format(200)).isEqualTo("3h20");
        assertThat(PlaytimeRequirement.format(65)).isEqualTo("1h05");
        assertThat(PlaytimeRequirement.format(-10)).isEqualTo("0min");
    }

    @Test
    void constructor_clampsNegativeDurations() {
        assertThat(new PlaytimeRequirement(registry, -5).minutes()).isZero();
    }

    @Test
    void isComplete_requiresAPositiveDuration() {
        assertThat(new PlaytimeRequirement(registry, 1).isComplete()).isTrue();
        assertThat(new PlaytimeRequirement(registry, 0).isComplete()).isFalse();
    }

    @Test
    void saveTo_thenFromConfig_roundTripsTheDuration() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirement");
        new PlaytimeRequirement(registry, 150).saveTo(section);

        assertThat(PlaytimeRequirement.fromConfig(registry, section).minutes()).isEqualTo(150);
    }
}
