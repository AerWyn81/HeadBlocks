package fr.aerwyn81.headblocks.commands.list.schedule;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.ScheduledBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.*;
import fr.aerwyn81.headblocks.services.HuntConfigService;
import fr.aerwyn81.headblocks.services.HuntService;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleCommandHandlerTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private LanguageService languageService;

    @Mock
    private HuntService huntService;

    @Mock
    private HuntConfigService huntConfigService;

    @Mock
    private CommandSender sender;

    private ScheduleCommandHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getHuntConfigService()).thenReturn(huntConfigService);

        lenient().when(languageService.message(anyString())).thenReturn("mock-message");

        handler = new ScheduleCommandHandler(registry);
    }

    // ==================== HANDLE ENTRY POINT ====================

    @Nested
    class Handle {
        @Test
        void tooFewArgs_sendsUsage() {
            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt"});

            verify(languageService).message("Messages.HuntUsage");
            verify(sender).sendMessage("mock-message");
        }

        @Test
        void huntNotFound_sendsError() {
            when(huntService.getHuntById("nope")).thenReturn(null);

            handler.handle(sender, new String[]{"hunt", "schedule", "nope", "info"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void unknownAction_sendsUsage() {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "banana"});

            verify(languageService).message("Messages.HuntUsage");
        }
    }

    // ==================== CLEAR ====================

    @Nested
    class Clear {
        @Test
        void noSchedule_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear"});

            verify(languageService).message("Messages.HuntScheduleNoScheduled");
        }

        @Test
        void clearAll_removesScheduledBehavior() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59),
                    List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear"});

            verify(huntConfigService).saveHunt(hunt);
            verify(languageService).message("Messages.HuntScheduleCleared");

            ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
            verify(hunt).setBehaviors(captor.capture());
            assertThat(captor.getValue()).noneMatch(b -> b instanceof ScheduledBehavior);
        }

        @Test
        void clearStart_keepsEndOnly() {
            LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(
                    LocalDateTime.of(2026, 1, 1, 0, 0), end, List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear", "start"});

            ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
            verify(hunt).setBehaviors(captor.capture());
            ScheduledBehavior sb = findScheduled(captor.getValue());
            assertThat(sb).isNotNull();
            RangeScheduleMode rsm = (RangeScheduleMode) sb.getScheduleMode();
            assertThat(rsm.start()).isNull();
            assertThat(rsm.end()).isEqualTo(end);
        }

        @Test
        void clearEnd_keepsStartOnly() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(
                    start, LocalDateTime.of(2026, 12, 31, 23, 59), List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear", "end"});

            ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
            verify(hunt).setBehaviors(captor.capture());
            ScheduledBehavior sb = findScheduled(captor.getValue());
            assertThat(sb).isNotNull();
            RangeScheduleMode rsm = (RangeScheduleMode) sb.getScheduleMode();
            assertThat(rsm.start()).isEqualTo(start);
            assertThat(rsm.end()).isNull();
        }
    }

    // ==================== START / END ====================

    @Nested
    class StartEnd {
        @Test
        void noDateArg_sendsUsage() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "start"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void invalidDate_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "start", "not-a-date"});

            verify(languageService).message("Messages.HuntScheduleInvalidDate");
        }

        @Test
        void setStart_dateOnly_createsRangeWithStartOfDay() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "start", "03/15/2026"});

            verify(huntConfigService).saveHunt(hunt);
            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.start()).isEqualTo(LocalDateTime.of(2026, 3, 15, 0, 0));
            assertThat(rsm.end()).isNull();
        }

        @Test
        void setStart_withTime() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "start", "03/15/2026", "14:30"});

            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.start()).isEqualTo(LocalDateTime.of(2026, 3, 15, 14, 30));
        }

        @Test
        void invalidTime_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "start", "03/15/2026", "bad"});

            verify(languageService).message("Messages.HuntScheduleInvalidDate");
        }

        @Test
        void setEnd_updatesExistingRange() {
            LocalDateTime existingStart = LocalDateTime.of(2026, 1, 1, 0, 0);
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(existingStart, null, List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "end", "12/31/2026"});

            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.start()).isEqualTo(existingStart);
            assertThat(rsm.end()).isEqualTo(LocalDateTime.of(2026, 12, 31, 0, 0));
        }

        @Test
        void setStart_overridesExistingStart() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 0, 0),
                    List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "start", "06/01/2026"});

            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.start()).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0));
            assertThat(rsm.end()).isEqualTo(LocalDateTime.of(2026, 12, 31, 0, 0));
        }
    }

    // ==================== MODE ====================

    @Nested
    class Mode {
        @Test
        void noModeArg_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "mode"});

            verify(languageService).message("Messages.HuntScheduleInvalidMode");
        }

        @Test
        void invalidMode_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "mode", "invalid"});

            verify(languageService).message("Messages.HuntScheduleInvalidMode");
        }

        @Test
        void setRange_createsRangeMode() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "mode", "range"});

            verify(huntConfigService).saveHunt(hunt);
            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(RangeScheduleMode.class);
            verify(languageService).message("Messages.HuntScheduleModeChanged");
        }

        @Test
        void setSlots_createsSlotsMode() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "mode", "slots"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(SlotsScheduleMode.class);
        }

        @Test
        void setRecurring_createsRecurringMode() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "mode", "recurring"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(RecurringScheduleMode.class);
        }
    }

    // ==================== ADD SLOT ====================

    @Nested
    class AddSlot {
        @Test
        void tooFewArgs_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "MON", "14:00"});

            verify(languageService).message("Messages.HuntScheduleInvalidSlot");
        }

        @Test
        void invalidDay_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "NOPE", "14:00", "18:00"});

            verify(languageService).message("Messages.HuntScheduleInvalidDay");
        }

        @Test
        void invalidTime_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "MON", "bad", "18:00"});

            verify(languageService).message("Messages.HuntScheduleInvalidSlot");
        }

        @Test
        void validSlot_addsToRangeMode() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(null, null, List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "MON,WED,FRI", "14:00", "18:00"});

            verify(huntConfigService).saveHunt(hunt);
            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.slots()).hasSize(1);
            TimeSlot slot = rsm.slots().get(0);
            assertThat(slot.days()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
            assertThat(slot.from()).isEqualTo(LocalTime.of(14, 0));
            assertThat(slot.to()).isEqualTo(LocalTime.of(18, 0));
        }

        @Test
        void validSlot_addsToSlotsMode() {
            HBHunt hunt = mockHuntWithSchedule(new SlotsScheduleMode(List.of(), null, null));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "SAT,SUN", "10:00", "16:00"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(SlotsScheduleMode.class);
            SlotsScheduleMode ssm = (SlotsScheduleMode) mode;
            assertThat(ssm.slots()).hasSize(1);
        }

        @Test
        void validSlot_addsToRecurringMode() {
            HBHunt hunt = mockHuntWithSchedule(new RecurringScheduleMode(
                    RecurrenceUnit.WEEK, "MONDAY", Duration.ofDays(2), List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "TUE", "09:00", "17:00"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(RecurringScheduleMode.class);
            RecurringScheduleMode rsm = (RecurringScheduleMode) mode;
            assertThat(rsm.slots()).hasSize(1);
            assertThat(rsm.every()).isEqualTo(RecurrenceUnit.WEEK);
        }

        @Test
        void noExistingSchedule_createsDefaultRangeWithSlot() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "MON", "08:00", "12:00"});

            verify(huntConfigService).saveHunt(hunt);
            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.start()).isNull();
            assertThat(rsm.end()).isNull();
            assertThat(rsm.slots()).hasSize(1);
        }

        @Test
        void appendsToExistingSlots() {
            TimeSlot existing = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(12, 0));
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(null, null, List.of(existing)));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "FRI", "14:00", "18:00"});

            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.slots()).hasSize(2);
        }

        @Test
        void fullDayNames_accepted() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(null, null, List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "addslot", "MONDAY,FRIDAY", "09:00", "17:00"});

            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.slots()).hasSize(1);
            assertThat(rsm.slots().get(0).days()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        }
    }

    // ==================== REMOVE SLOT ====================

    @Nested
    class RemoveSlot {
        @Test
        void noIndexArg_sendsUsage() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "removeslot"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void notANumber_sendsUsage() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "removeslot", "abc"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void noSchedule_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "removeslot", "1"});

            verify(languageService).message("Messages.HuntScheduleNoScheduled");
        }

        @Test
        void indexOutOfBounds_sendsNoSlots() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(null, null, List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "removeslot", "1"});

            verify(languageService).message("Messages.HuntScheduleNoSlots");
        }

        @Test
        void validIndex_removesSlot() {
            TimeSlot slot1 = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(12, 0));
            TimeSlot slot2 = new TimeSlot(List.of(DayOfWeek.FRIDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(null, null, List.of(slot1, slot2)));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "removeslot", "1"});

            verify(huntConfigService).saveHunt(hunt);
            RangeScheduleMode rsm = captureRangeMode(hunt);
            assertThat(rsm.slots()).hasSize(1);
            assertThat(rsm.slots().get(0).days()).containsExactly(DayOfWeek.FRIDAY);
        }

        @Test
        void removesFromSlotsMode() {
            TimeSlot slot = new TimeSlot(List.of(DayOfWeek.SATURDAY), LocalTime.of(10, 0), LocalTime.of(16, 0));
            HBHunt hunt = mockHuntWithSchedule(new SlotsScheduleMode(List.of(slot), null, null));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "removeslot", "1"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(SlotsScheduleMode.class);
            assertThat(((SlotsScheduleMode) mode).slots()).isEmpty();
        }

        @Test
        void removesFromRecurringMode() {
            TimeSlot slot = new TimeSlot(List.of(DayOfWeek.TUESDAY), LocalTime.of(9, 0), LocalTime.of(17, 0));
            HBHunt hunt = mockHuntWithSchedule(new RecurringScheduleMode(
                    RecurrenceUnit.WEEK, "MONDAY", Duration.ofDays(2), List.of(slot)));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "removeslot", "1"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(RecurringScheduleMode.class);
            assertThat(((RecurringScheduleMode) mode).slots()).isEmpty();
        }
    }

    // ==================== EVERY ====================

    @Nested
    class Every {
        @Test
        void noUnitArg_sendsUsage() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "every"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void invalidUnit_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "every", "daily"});

            verify(languageService).message("Messages.HuntScheduleInvalidMode");
        }

        @Test
        void setWeek_onExistingRecurring() {
            HBHunt hunt = mockHuntWithSchedule(new RecurringScheduleMode(
                    RecurrenceUnit.MONTH, "1", Duration.ofDays(5), List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "every", "week"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(RecurringScheduleMode.class);
            RecurringScheduleMode rsm = (RecurringScheduleMode) mode;
            assertThat(rsm.every()).isEqualTo(RecurrenceUnit.WEEK);
            assertThat(rsm.startRef()).isEqualTo("1");
            assertThat(rsm.duration()).isEqualTo(Duration.ofDays(5));
        }

        @Test
        void setYear_noExistingSchedule_createsNewRecurring() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "every", "year"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(RecurringScheduleMode.class);
            RecurringScheduleMode rsm = (RecurringScheduleMode) mode;
            assertThat(rsm.every()).isEqualTo(RecurrenceUnit.YEAR);
            assertThat(rsm.startRef()).isNull();
            assertThat(rsm.duration()).isNull();
        }

        @Test
        void setMonth_onNonRecurringMode_createsNew() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(null, null, List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "every", "month"});

            ScheduleMode mode = captureScheduleMode(hunt);
            assertThat(mode).isInstanceOf(RecurringScheduleMode.class);
            RecurringScheduleMode rsm = (RecurringScheduleMode) mode;
            assertThat(rsm.every()).isEqualTo(RecurrenceUnit.MONTH);
            assertThat(rsm.startRef()).isNull();
        }
    }

    // ==================== STARTREF ====================

    @Nested
    class StartRef {
        @Test
        void noArg_sendsUsage() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "startref"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void setsRef_onExistingRecurring() {
            HBHunt hunt = mockHuntWithSchedule(new RecurringScheduleMode(
                    RecurrenceUnit.WEEK, "MONDAY", Duration.ofDays(2), List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "startref", "FRIDAY"});

            RecurringScheduleMode rsm = (RecurringScheduleMode) captureScheduleMode(hunt);
            assertThat(rsm.startRef()).isEqualTo("FRIDAY");
            assertThat(rsm.every()).isEqualTo(RecurrenceUnit.WEEK);
            assertThat(rsm.duration()).isEqualTo(Duration.ofDays(2));
        }

        @Test
        void setsRef_noExistingSchedule_createsNew() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "startref", "15"});

            RecurringScheduleMode rsm = (RecurringScheduleMode) captureScheduleMode(hunt);
            assertThat(rsm.startRef()).isEqualTo("15");
            assertThat(rsm.every()).isNull();
            assertThat(rsm.duration()).isNull();
        }
    }

    // ==================== DURATION ====================

    @Nested
    class DurationCmd {
        @Test
        void noArg_sendsUsage() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "duration"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void invalidDuration_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "duration", "x"});

            verify(languageService).message("Messages.HuntScheduleInvalidDuration");
        }

        @Test
        void setsDuration_onExistingRecurring() {
            HBHunt hunt = mockHuntWithSchedule(new RecurringScheduleMode(
                    RecurrenceUnit.MONTH, "1", Duration.ofDays(3), List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "duration", "5d"});

            RecurringScheduleMode rsm = (RecurringScheduleMode) captureScheduleMode(hunt);
            assertThat(rsm.duration()).isEqualTo(Duration.ofDays(5));
            assertThat(rsm.every()).isEqualTo(RecurrenceUnit.MONTH);
            assertThat(rsm.startRef()).isEqualTo("1");
        }

        @Test
        void setsDuration_noExistingSchedule() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "duration", "2d"});

            RecurringScheduleMode rsm = (RecurringScheduleMode) captureScheduleMode(hunt);
            assertThat(rsm.duration()).isEqualTo(Duration.ofDays(2));
            assertThat(rsm.every()).isNull();
        }
    }

    // ==================== ACTIVE FROM / UNTIL ====================

    @Nested
    class ActiveDate {
        @Test
        void activeFrom_noArg_sendsUsage() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "activefrom"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void activeFrom_invalidDate_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "activefrom", "bad"});

            verify(languageService).message("Messages.HuntScheduleInvalidDate");
        }

        @Test
        void activeFrom_onExistingSlots() {
            LocalDate until = LocalDate.of(2026, 12, 31);
            HBHunt hunt = mockHuntWithSchedule(new SlotsScheduleMode(List.of(), null, until));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "activefrom", "01/01/2026"});

            SlotsScheduleMode ssm = (SlotsScheduleMode) captureScheduleMode(hunt);
            assertThat(ssm.activeFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(ssm.activeUntil()).isEqualTo(until);
        }

        @Test
        void activeUntil_onExistingSlots() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            HBHunt hunt = mockHuntWithSchedule(new SlotsScheduleMode(List.of(), from, null));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "activeuntil", "12/31/2026"});

            SlotsScheduleMode ssm = (SlotsScheduleMode) captureScheduleMode(hunt);
            assertThat(ssm.activeFrom()).isEqualTo(from);
            assertThat(ssm.activeUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        void activeFrom_noExistingSchedule_createsSlotsMode() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "activefrom", "06/15/2026"});

            SlotsScheduleMode ssm = (SlotsScheduleMode) captureScheduleMode(hunt);
            assertThat(ssm.activeFrom()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(ssm.activeUntil()).isNull();
            assertThat(ssm.slots()).isEmpty();
        }

        @Test
        void activeUntil_onNonSlotsMode_createsNewSlotsMode() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(null, null, List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "activeuntil", "03/01/2026"});

            SlotsScheduleMode ssm = (SlotsScheduleMode) captureScheduleMode(hunt);
            assertThat(ssm.activeFrom()).isNull();
            assertThat(ssm.activeUntil()).isEqualTo(LocalDate.of(2026, 3, 1));
        }
    }

    // ==================== INFO ====================

    @Nested
    class Info {
        @Test
        void noSchedule_sendsError() {
            HBHunt hunt = mockHuntWithBehaviors();
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "info"});

            verify(languageService).message("Messages.HuntScheduleNoScheduled");
        }

        @Test
        void withRangeSchedule_sendsInfo() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59),
                    List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "info"});

            verify(languageService).message("Messages.HuntScheduleInfo");
            verify(sender).sendMessage("mock-message");
        }

        @Test
        void withSlots_includesSlotInfo() {
            TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                    LocalTime.of(14, 0), LocalTime.of(18, 0));
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(null, null, List.of(slot)));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "info"});

            verify(languageService).message("Messages.HuntScheduleInfo");
        }
    }

    // ==================== TAB COMPLETION ====================

    @Nested
    class TabCompletion {
        @Test
        void fourthArg_returnsAllActions() {
            ArrayList<String> result = handler.tabComplete(new String[]{"hunt", "schedule", "myhunt", ""});

            assertThat(result).contains("start", "end", "clear", "mode", "addslot", "removeslot",
                    "every", "startref", "duration", "activefrom", "activeuntil", "info");
        }

        @Test
        void fourthArg_filters() {
            ArrayList<String> result = handler.tabComplete(new String[]{"hunt", "schedule", "myhunt", "st"});

            assertThat(result).containsExactlyInAnyOrder("start", "startref");
        }

        @Test
        void fifthArg_mode_returnsModes() {
            ArrayList<String> result = handler.tabComplete(new String[]{"hunt", "schedule", "myhunt", "mode", ""});

            assertThat(result).containsExactlyInAnyOrder("range", "slots", "recurring");
        }

        @Test
        void fifthArg_mode_filters() {
            ArrayList<String> result = handler.tabComplete(new String[]{"hunt", "schedule", "myhunt", "mode", "r"});

            assertThat(result).containsExactlyInAnyOrder("range", "recurring");
        }

        @Test
        void fifthArg_every_returnsUnits() {
            ArrayList<String> result = handler.tabComplete(new String[]{"hunt", "schedule", "myhunt", "every", ""});

            assertThat(result).containsExactlyInAnyOrder("year", "month", "week");
        }

        @Test
        void fifthArg_clear_returnsStartEnd() {
            ArrayList<String> result = handler.tabComplete(new String[]{"hunt", "schedule", "myhunt", "clear", ""});

            assertThat(result).containsExactlyInAnyOrder("start", "end");
        }

        @Test
        void fifthArg_otherAction_returnsEmpty() {
            ArrayList<String> result = handler.tabComplete(new String[]{"hunt", "schedule", "myhunt", "addslot", ""});

            assertThat(result).isEmpty();
        }

        @Test
        void sixthArg_returnsEmpty() {
            ArrayList<String> result = handler.tabComplete(new String[]{"hunt", "schedule", "myhunt", "mode", "range", ""});

            assertThat(result).isEmpty();
        }
    }

    // ==================== EDGE CASES / DATA LOSS ====================

    @Nested
    class EdgeCases {

        /**
         * Bug: handleClear with "start" on SlotsScheduleMode removes the entire schedule.
         * The code only preserves partial state for RangeScheduleMode — for all other modes,
         * the behavior is removed with no replacement.
         */
        @Test
        void clear_start_onSlotsMode_removesEntireSchedule() {
            TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
            HBHunt hunt = mockHuntWithSchedule(new SlotsScheduleMode(
                    List.of(slot), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear", "start"});

            ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
            verify(hunt).setBehaviors(captor.capture());
            // Bug: entire schedule is removed, not just "start" — slot config lost
            assertThat(captor.getValue()).noneMatch(b -> b instanceof ScheduledBehavior);
        }

        /**
         * Bug: handleClear with "end" on RecurringScheduleMode removes the entire schedule.
         * Same issue as above — no partial clear for non-range modes.
         */
        @Test
        void clear_end_onRecurringMode_removesEntireSchedule() {
            HBHunt hunt = mockHuntWithSchedule(new RecurringScheduleMode(
                    RecurrenceUnit.WEEK, "MONDAY", Duration.ofDays(2), List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear", "end"});

            ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
            verify(hunt).setBehaviors(captor.capture());
            // Bug: entire recurring schedule is removed
            assertThat(captor.getValue()).noneMatch(b -> b instanceof ScheduledBehavior);
        }

        /**
         * Bug: handleStartEnd on a RecurringScheduleMode silently replaces it with a
         * RangeScheduleMode, losing all recurring config (every, startRef, duration, slots).
         */
        @Test
        void setStart_onRecurringMode_replacesWithRange_losesRecurringConfig() {
            TimeSlot slot = new TimeSlot(List.of(DayOfWeek.TUESDAY), LocalTime.of(9, 0), LocalTime.of(17, 0));
            HBHunt hunt = mockHuntWithSchedule(new RecurringScheduleMode(
                    RecurrenceUnit.MONTH, "15", Duration.ofDays(5), List.of(slot)));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "start", "06/01/2026"});

            ScheduleMode mode = captureScheduleMode(hunt);
            // Bug: mode was silently changed from recurring to range
            assertThat(mode).isInstanceOf(RangeScheduleMode.class);
            RangeScheduleMode rsm = (RangeScheduleMode) mode;
            assertThat(rsm.start()).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0));
            // All recurring config is lost — slots, every, startRef, duration
            assertThat(rsm.slots()).isEmpty();
        }

        /**
         * Bug: handleStartEnd on a SlotsScheduleMode replaces it with RangeScheduleMode,
         * losing all slot config and activeFrom/activeUntil dates.
         */
        @Test
        void setEnd_onSlotsMode_replacesWithRange_losesSlotConfig() {
            TimeSlot slot = new TimeSlot(List.of(DayOfWeek.SATURDAY), LocalTime.of(10, 0), LocalTime.of(16, 0));
            HBHunt hunt = mockHuntWithSchedule(new SlotsScheduleMode(
                    List.of(slot), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 30)));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "end", "12/31/2026"});

            ScheduleMode mode = captureScheduleMode(hunt);
            // Bug: mode changed from slots to range, activeFrom/activeUntil and slot config lost
            assertThat(mode).isInstanceOf(RangeScheduleMode.class);
            RangeScheduleMode rsm = (RangeScheduleMode) mode;
            assertThat(rsm.end()).isEqualTo(LocalDateTime.of(2026, 12, 31, 0, 0));
            assertThat(rsm.start()).isNull();
            assertThat(rsm.slots()).isEmpty();
        }

        /**
         * Bug: handleActiveDate on a RangeScheduleMode replaces it with SlotsScheduleMode,
         * losing the range start/end datetimes.
         */
        @Test
        void activeFrom_onRangeMode_replacesWithSlots_losesRangeConfig() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59),
                    List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "activefrom", "03/01/2026"});

            ScheduleMode mode = captureScheduleMode(hunt);
            // Bug: mode changed from range to slots, start/end datetimes lost
            assertThat(mode).isInstanceOf(SlotsScheduleMode.class);
            SlotsScheduleMode ssm = (SlotsScheduleMode) mode;
            assertThat(ssm.activeFrom()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(ssm.slots()).isEmpty();
        }

        /**
         * Verify handleClear without "start"/"end" arg on non-range mode fully removes schedule.
         */
        @Test
        void clearAll_onRecurringMode_removesCompletely() {
            HBHunt hunt = mockHuntWithSchedule(new RecurringScheduleMode(
                    RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear"});

            ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
            verify(hunt).setBehaviors(captor.capture());
            assertThat(captor.getValue()).noneMatch(b -> b instanceof ScheduledBehavior);
            verify(languageService).message("Messages.HuntScheduleCleared");
        }

        /**
         * Verify that clear "start" on range with only end=null removes entire schedule
         * (because end is null, so no replacement is added).
         */
        @Test
        void clear_start_rangeWithNullEnd_removesEntireSchedule() {
            HBHunt hunt = mockHuntWithSchedule(new RangeScheduleMode(
                    LocalDateTime.of(2026, 6, 1, 0, 0), null, List.of()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear", "start"});

            ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
            verify(hunt).setBehaviors(captor.capture());
            // When end is null, "clear start" removes entire schedule (no replacement)
            assertThat(captor.getValue()).noneMatch(b -> b instanceof ScheduledBehavior);
        }

        /**
         * Preserves other behaviors (e.g., FreeBehavior) when clearing schedule.
         */
        @Test
        void clear_preservesNonScheduleBehaviors() {
            FreeBehavior free = new FreeBehavior();
            ScheduledBehavior scheduled = new ScheduledBehavior(registry,
                    new RangeScheduleMode(LocalDateTime.of(2026, 1, 1, 0, 0), null, List.of()));
            HBHunt hunt = mockHuntWithBehaviors(free, scheduled);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            handler.handle(sender, new String[]{"hunt", "schedule", "myhunt", "clear"});

            ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
            verify(hunt).setBehaviors(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0)).isInstanceOf(FreeBehavior.class);
        }
    }

    // ==================== HELPERS ====================

    private HBHunt mockHuntWithBehaviors(Behavior... behaviors) {
        HBHunt hunt = mock(HBHunt.class);
        lenient().when(hunt.getId()).thenReturn("myhunt");
        lenient().when(hunt.getBehaviors()).thenReturn(new ArrayList<>(List.of(behaviors)));
        return hunt;
    }

    private HBHunt mockHuntWithSchedule(ScheduleMode mode) {
        ScheduledBehavior sb = new ScheduledBehavior(registry, mode);
        return mockHuntWithBehaviors(sb);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<Behavior>> behaviorsCaptor() {
        ArgumentCaptor<List<Behavior>> captor = ArgumentCaptor.forClass(List.class);
        return captor;
    }

    private ScheduledBehavior findScheduled(List<Behavior> behaviors) {
        return behaviors.stream()
                .filter(b -> b instanceof ScheduledBehavior)
                .map(b -> (ScheduledBehavior) b)
                .findFirst().orElse(null);
    }

    private ScheduleMode captureScheduleMode(HBHunt hunt) {
        ArgumentCaptor<List<Behavior>> captor = behaviorsCaptor();
        verify(hunt).setBehaviors(captor.capture());
        ScheduledBehavior sb = findScheduled(captor.getValue());
        assertThat(sb).isNotNull();
        return sb.getScheduleMode();
    }

    private RangeScheduleMode captureRangeMode(HBHunt hunt) {
        ScheduleMode mode = captureScheduleMode(hunt);
        assertThat(mode).isInstanceOf(RangeScheduleMode.class);
        return (RangeScheduleMode) mode;
    }
}
