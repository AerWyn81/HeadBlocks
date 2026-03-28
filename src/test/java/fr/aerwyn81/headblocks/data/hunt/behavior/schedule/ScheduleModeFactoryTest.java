package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleModeFactoryTest {

    @Test
    void fromConfig_nullSection_returnsEmptyRange() {
        ScheduleMode mode = ScheduleModeFactory.fromConfig(null);

        assertThat(mode).isInstanceOf(RangeScheduleMode.class);
        RangeScheduleMode range = (RangeScheduleMode) mode;
        assertThat(range.start()).isNull();
        assertThat(range.end()).isNull();
    }

    @Test
    void fromConfig_noModeKey_defaultsToRange() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("mode", "range")).thenReturn("range");

        ScheduleMode mode = ScheduleModeFactory.fromConfig(section);

        assertThat(mode).isInstanceOf(RangeScheduleMode.class);
    }

    @Test
    void fromConfig_modeRange_returnsRangeScheduleMode() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("mode", "range")).thenReturn("range");

        ScheduleMode mode = ScheduleModeFactory.fromConfig(section);

        assertThat(mode).isInstanceOf(RangeScheduleMode.class);
        assertThat(mode.getModeId()).isEqualTo("range");
    }

    @Test
    void fromConfig_modeSlots_returnsSlotsScheduleMode() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("mode", "range")).thenReturn("slots");

        ScheduleMode mode = ScheduleModeFactory.fromConfig(section);

        assertThat(mode).isInstanceOf(SlotsScheduleMode.class);
        assertThat(mode.getModeId()).isEqualTo("slots");
    }

    @Test
    void fromConfig_modeRecurring_returnsRecurringScheduleMode() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("mode", "range")).thenReturn("recurring");

        ScheduleMode mode = ScheduleModeFactory.fromConfig(section);

        assertThat(mode).isInstanceOf(RecurringScheduleMode.class);
        assertThat(mode.getModeId()).isEqualTo("recurring");
    }

    @Test
    void fromConfig_unknownMode_defaultsToRange() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("mode", "range")).thenReturn("foobar");

        ScheduleMode mode = ScheduleModeFactory.fromConfig(section);

        assertThat(mode).isInstanceOf(RangeScheduleMode.class);
    }
}
