package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledBehaviorTest {

    @Mock
    Player player;

    @Mock
    HeadLocation headLocation;

    @Mock
    ServiceRegistry registry;

    @Mock
    LanguageService languageService;

    @Mock
    ConfigService configService;

    private Hunt hunt;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mocked");
        hunt = new Hunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
    }

    @Test
    void canPlayerClick_beforeStart_returnsDeny() {
        when(languageService.message("Hunt.Behavior.ScheduledNotStarted")).thenReturn("Not started");

        LocalDate futureStart = LocalDate.now().plusDays(10);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, futureStart, null);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void canPlayerClick_afterEnd_returnsDeny() {
        when(languageService.message("Hunt.Behavior.ScheduledEnded")).thenReturn("Ended");

        LocalDate pastEnd = LocalDate.now().minusDays(10);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, pastEnd);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void canPlayerClick_withinRange_returnsAllow() {
        LocalDate pastStart = LocalDate.now().minusDays(5);
        LocalDate futureEnd = LocalDate.now().plusDays(5);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, pastStart, futureEnd);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_startNull_noStartRestriction() {
        LocalDate futureEnd = LocalDate.now().plusDays(5);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, futureEnd);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_endNull_noEndRestriction() {
        LocalDate pastStart = LocalDate.now().minusDays(5);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, pastStart, null);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_bothNull_alwaysAllow() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, null);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_startEqualsToday_allowsBecauseIsBeforeIsExclusive() {
        // isBefore(today) is false when start == today, so no denial
        ScheduledBehavior behavior = new ScheduledBehavior(registry, LocalDate.now(), null);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_endEqualsToday_allowsBecauseIsAfterIsExclusive() {
        // isAfter(today) is false when end == today, so no denial
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, LocalDate.now());

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void getDisplayInfo_formatsCorrectly() {
        LocalDate start = LocalDate.of(2025, 1, 15);
        LocalDate end = LocalDate.of(2025, 12, 31);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, start, end);

        String info = behavior.getDisplayInfo(player, hunt);

        assertThat(info).isEqualTo("2025-01-15 → 2025-12-31");
    }

    @Test
    void getDisplayInfo_nullDatesShowInfinity() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, null);

        String info = behavior.getDisplayInfo(player, hunt);

        assertThat(info).isEqualTo("∞ → ∞");
    }

    @Test
    void getId_returnsScheduled() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, null);
        assertThat(behavior.getId()).isEqualTo("scheduled");
    }

    @Test
    void onHeadFound_doesNothing() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, null);

        behavior.onHeadFound(player, headLocation, hunt);
        // No-op, no exception = pass
    }

    // --- fromConfig ---

    @Test
    void fromConfig_nullSection_returnsNullDates() {
        ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, null);

        assertThat(behavior.start()).isNull();
        assertThat(behavior.end()).isNull();
    }

    @Test
    void fromConfig_validDates_parsesBoth() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("start")).thenReturn("2025-06-01");
        when(section.getString("end")).thenReturn("2025-12-31");

        ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

        assertThat(behavior.start()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(behavior.end()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void fromConfig_missingStartKey_startIsNull() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("start")).thenReturn(null);
        when(section.getString("end")).thenReturn("2025-12-31");

        ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

        assertThat(behavior.start()).isNull();
        assertThat(behavior.end()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void fromConfig_invalidStartDate_logsErrorAndStartIsNull() {
        try (MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            ConfigurationSection section = mock(ConfigurationSection.class);
            when(section.getString("start")).thenReturn("not-a-date");
            when(section.getString("end")).thenReturn(null);

            ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

            assertThat(behavior.start()).isNull();
            logUtil.verify(() -> LogUtil.error(anyString(), eq("not-a-date"), anyString()));
        }
    }

    @Test
    void fromConfig_invalidEndDate_logsErrorAndEndIsNull() {
        try (MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            ConfigurationSection section = mock(ConfigurationSection.class);
            when(section.getString("start")).thenReturn(null);
            when(section.getString("end")).thenReturn("bad-end");

            ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

            assertThat(behavior.end()).isNull();
            logUtil.verify(() -> LogUtil.error(anyString(), eq("bad-end"), anyString()));
        }
    }
}
