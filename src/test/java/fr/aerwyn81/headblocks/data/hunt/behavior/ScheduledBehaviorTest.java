package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.RangeScheduleMode;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.SlotsScheduleMode;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.TimeSlot;
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

import java.time.LocalDateTime;
import java.util.List;

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

    private HBHunt hunt;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mocked");
        hunt = new HBHunt(configService, "test", "Test", HuntState.ACTIVE, 1, "DIAMOND");
    }

    @Test
    void canPlayerClick_beforeStart_returnsDeny() {
        when(languageService.message("Hunt.Behavior.ScheduledNotStarted")).thenReturn("Not started");

        LocalDateTime futureStart = LocalDateTime.now().plusDays(10);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, futureStart, null);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void canPlayerClick_afterEnd_returnsDeny() {
        when(languageService.message("Hunt.Behavior.ScheduledEnded")).thenReturn("Ended");

        LocalDateTime pastEnd = LocalDateTime.now().minusDays(10);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, pastEnd);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void canPlayerClick_withinRange_returnsAllow() {
        LocalDateTime pastStart = LocalDateTime.now().minusDays(5);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(5);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, pastStart, futureEnd);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_startNull_noStartRestriction() {
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(5);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, futureEnd);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_endNull_noEndRestriction() {
        LocalDateTime pastStart = LocalDateTime.now().minusDays(5);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, pastStart, null);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_bothNull_alwaysAllow() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, (LocalDateTime) null, null);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_startInThePast_allows() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, LocalDateTime.now().minusSeconds(1), null);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_endInTheFuture_allows() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, null, LocalDateTime.now().plusSeconds(1));

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_outsideSlot_returnsDeny() {
        when(languageService.message("Hunt.Behavior.ScheduledOutsideSlot")).thenReturn("Outside slot");

        RangeScheduleMode mode = new RangeScheduleMode(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().plusDays(5),
                List.of(new TimeSlot(List.of(java.time.DayOfWeek.of(
                        LocalDateTime.now().getDayOfWeek().getValue() == 7 ? 1 : LocalDateTime.now().getDayOfWeek().getValue() + 1
                )), java.time.LocalTime.of(0, 0), java.time.LocalTime.of(23, 59)))
        );
        ScheduledBehavior behavior = new ScheduledBehavior(registry, mode);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void getDisplayInfo_formatsCorrectly() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 30);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, start, end);

        String info = behavior.getDisplayInfo(player, hunt);

        assertThat(info).isEqualTo("01/15/2025 10:30 → 12/31/2025 23:59");
    }

    @Test
    void getDisplayInfo_nullDatesShowInfinity() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, (LocalDateTime) null, null);

        String info = behavior.getDisplayInfo(player, hunt);

        assertThat(info).isEqualTo("∞ → ∞");
    }

    @Test
    void getId_returnsScheduled() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, (LocalDateTime) null, null);
        assertThat(behavior.getId()).isEqualTo("scheduled");
    }

    @Test
    void onHeadFound_doesNothing() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, (LocalDateTime) null, null);

        behavior.onHeadFound(player, headLocation, hunt);
        // No-op, no exception = pass
    }

    @Test
    void getScheduleMode_returnsRangeForCompatConstructor() {
        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 0, 0);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, start, end);

        assertThat(behavior.getScheduleMode()).isInstanceOf(RangeScheduleMode.class);
        RangeScheduleMode mode = (RangeScheduleMode) behavior.getScheduleMode();
        assertThat(mode.start()).isEqualTo(start);
        assertThat(mode.end()).isEqualTo(end);
    }

    @Test
    void isAccessGate_returnsTrue() {
        ScheduledBehavior behavior = new ScheduledBehavior(registry, (LocalDateTime) null, null);
        assertThat(behavior.isAccessGate()).isTrue();
    }

    @Test
    void getScheduleMode_returnsProvidedMode() {
        SlotsScheduleMode slotsMode = new SlotsScheduleMode(List.of(), null, null);
        ScheduledBehavior behavior = new ScheduledBehavior(registry, slotsMode);

        assertThat(behavior.getScheduleMode()).isSameAs(slotsMode);
    }

    // --- fromConfig ---

    @Test
    void fromConfig_nullSection_returnsNullDates() {
        ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, null);

        assertThat(behavior.getScheduleMode()).isInstanceOf(RangeScheduleMode.class);
        RangeScheduleMode mode = (RangeScheduleMode) behavior.getScheduleMode();
        assertThat(mode.start()).isNull();
        assertThat(mode.end()).isNull();
    }

    @Test
    void fromConfig_validDatesWithTime_parsesBoth() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection startSub = mock(ConfigurationSection.class);
        ConfigurationSection endSub = mock(ConfigurationSection.class);

        when(section.getString("mode", "range")).thenReturn("range");
        when(section.getConfigurationSection("start")).thenReturn(startSub);
        when(section.getConfigurationSection("end")).thenReturn(endSub);
        when(startSub.getString("date")).thenReturn("06/01/2025");
        when(startSub.getString("time")).thenReturn("09:00");
        when(endSub.getString("date")).thenReturn("12/31/2025");
        when(endSub.getString("time")).thenReturn("23:59");

        ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

        RangeScheduleMode mode = (RangeScheduleMode) behavior.getScheduleMode();
        assertThat(mode.start()).isEqualTo(LocalDateTime.of(2025, 6, 1, 9, 0));
        assertThat(mode.end()).isEqualTo(LocalDateTime.of(2025, 12, 31, 23, 59));
    }

    @Test
    void fromConfig_validDateWithoutTime_defaultsMidnight() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection startSub = mock(ConfigurationSection.class);

        when(section.getString("mode", "range")).thenReturn("range");
        when(section.getConfigurationSection("start")).thenReturn(startSub);
        when(section.getConfigurationSection("end")).thenReturn(null);
        when(startSub.getString("date")).thenReturn("06/01/2025");
        when(startSub.getString("time")).thenReturn(null);

        ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

        RangeScheduleMode mode = (RangeScheduleMode) behavior.getScheduleMode();
        assertThat(mode.start()).isEqualTo(LocalDateTime.of(2025, 6, 1, 0, 0));
        assertThat(mode.end()).isNull();
    }

    @Test
    void fromConfig_missingStartSection_startIsNull() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection endSub = mock(ConfigurationSection.class);

        when(section.getString("mode", "range")).thenReturn("range");
        when(section.getConfigurationSection("start")).thenReturn(null);
        when(section.getConfigurationSection("end")).thenReturn(endSub);
        when(endSub.getString("date")).thenReturn("12/31/2025");
        when(endSub.getString("time")).thenReturn(null);

        ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

        RangeScheduleMode mode = (RangeScheduleMode) behavior.getScheduleMode();
        assertThat(mode.start()).isNull();
        assertThat(mode.end()).isEqualTo(LocalDateTime.of(2025, 12, 31, 0, 0));
    }

    @Test
    void fromConfig_invalidStartDate_logsErrorAndStartIsNull() {
        try (MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            ConfigurationSection section = mock(ConfigurationSection.class);
            ConfigurationSection startSub = mock(ConfigurationSection.class);

            when(section.getString("mode", "range")).thenReturn("range");
            when(section.getConfigurationSection("start")).thenReturn(startSub);
            when(section.getConfigurationSection("end")).thenReturn(null);
            when(startSub.getString("date")).thenReturn("not-a-date");

            ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

            RangeScheduleMode mode = (RangeScheduleMode) behavior.getScheduleMode();
            assertThat(mode.start()).isNull();
            logUtil.verify(() -> LogUtil.error(anyString(), eq("start"), eq("not-a-date")));
        }
    }

    @Test
    void fromConfig_invalidEndDate_logsErrorAndEndIsNull() {
        try (MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            ConfigurationSection section = mock(ConfigurationSection.class);
            ConfigurationSection endSub = mock(ConfigurationSection.class);

            when(section.getString("mode", "range")).thenReturn("range");
            when(section.getConfigurationSection("start")).thenReturn(null);
            when(section.getConfigurationSection("end")).thenReturn(endSub);
            when(endSub.getString("date")).thenReturn("bad-end");

            ScheduledBehavior behavior = ScheduledBehavior.fromConfig(registry, section);

            RangeScheduleMode mode = (RangeScheduleMode) behavior.getScheduleMode();
            assertThat(mode.end()).isNull();
            logUtil.verify(() -> LogUtil.error(anyString(), eq("end"), eq("bad-end")));
        }
    }
}
