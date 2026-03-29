package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that exercise full schedule evaluation workflows.
 * These tests chain multiple operations: create mode → evaluate at multiple times →
 * persist → reload → verify identical behavior.
 */
class ScheduleIntegrationTest {

    // =========================================================================
    // Recurring schedule: weekly event with time slots
    // Simulates a "weekend event" that runs every Saturday from 10:00 to 20:00
    // and every Sunday from 12:00 to 18:00, recurring weekly from Saturday
    // with a 2-day duration window.
    // =========================================================================

    @Nested
    class WeekendEventRecurring {

        private RecurringScheduleMode createWeekendEvent() {
            TimeSlot saturday = new TimeSlot(
                    List.of(DayOfWeek.SATURDAY),
                    LocalTime.of(10, 0),
                    LocalTime.of(20, 0)
            );
            TimeSlot sunday = new TimeSlot(
                    List.of(DayOfWeek.SUNDAY),
                    LocalTime.of(12, 0),
                    LocalTime.of(18, 0)
            );
            return new RecurringScheduleMode(
                    RecurrenceUnit.WEEK,
                    "SATURDAY",
                    Duration.ofDays(2),
                    List.of(saturday, sunday)
            );
        }

        @Test
        void evaluateAcrossEntireWeek_onlyWeekendSlotsAllow() {
            RecurringScheduleMode mode = createWeekendEvent();

            // Monday 12:00 — in recurrence window? No (Sat+2d = Mon 00:00, so Mon 12:00 is out)
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);

            // Friday 15:00 — not in recurrence
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 10, 15, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);

            // Saturday 09:59 — in recurrence window, but before slot → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 11, 9, 59)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // Saturday 10:00 — in recurrence window + slot matches → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 11, 10, 0)))
                    .isNull();

            // Saturday 15:00 — allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 11, 15, 0)))
                    .isNull();

            // Saturday 20:00 — slot ends at 20:00 (exclusive) → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 11, 20, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // Sunday 11:59 — in recurrence window but before Sunday slot → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 12, 11, 59)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // Sunday 12:00 — allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 12, 12, 0)))
                    .isNull();

            // Sunday 17:59 — allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 12, 17, 59)))
                    .isNull();

            // Sunday 18:00 — slot ends (exclusive) → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 12, 18, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);
        }

        @Test
        void persistAndReload_identicalBehavior() {
            RecurringScheduleMode original = createWeekendEvent();

            // Persist
            MemoryConfiguration config = new MemoryConfiguration();
            original.saveTo(config);

            // Reload
            RecurringScheduleMode reloaded = RecurringScheduleMode.fromConfig(config);

            // Verify same behavior at multiple timestamps
            LocalDateTime[] testTimes = {
                    LocalDateTime.of(2025, 1, 6, 12, 0),   // Monday
                    LocalDateTime.of(2025, 1, 11, 10, 0),  // Saturday 10:00
                    LocalDateTime.of(2025, 1, 11, 20, 0),  // Saturday 20:00
                    LocalDateTime.of(2025, 1, 12, 12, 0),  // Sunday 12:00
                    LocalDateTime.of(2025, 1, 12, 18, 0),  // Sunday 18:00
            };

            for (LocalDateTime time : testTimes) {
                assertThat(reloaded.getDenyReason(time))
                        .as("at %s", time)
                        .isEqualTo(original.getDenyReason(time));
            }
        }

        @Test
        void isInWindow_matchesGetDenyReasonNull() {
            RecurringScheduleMode mode = createWeekendEvent();

            // Saturday 15:00 — should be allowed
            LocalDateTime allowed = LocalDateTime.of(2025, 1, 11, 15, 0);
            assertThat(mode.isInWindow(allowed)).isTrue();
            assertThat(mode.getDenyReason(allowed)).isNull();

            // Monday 12:00 — denied
            LocalDateTime denied = LocalDateTime.of(2025, 1, 6, 12, 0);
            assertThat(mode.isInWindow(denied)).isFalse();
            assertThat(mode.getDenyReason(denied)).isNotNull();
        }
    }

    // =========================================================================
    // Monthly recurring event: e.g., "1st of every month for 3 days"
    // with afternoon time slots
    // =========================================================================

    @Nested
    class MonthlyPaydayEvent {

        private RecurringScheduleMode createPaydayEvent() {
            // Active on any day in the 3-day window, afternoons only
            TimeSlot afternoons = new TimeSlot(
                    List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                    LocalTime.of(14, 0),
                    LocalTime.of(22, 0)
            );
            return new RecurringScheduleMode(
                    RecurrenceUnit.MONTH,
                    "1",  // 1st of every month
                    Duration.ofDays(3),
                    List.of(afternoons)
            );
        }

        @Test
        void evaluateFirstThreeDaysOfMonth() {
            RecurringScheduleMode mode = createPaydayEvent();

            // Jan 1, 2025 — 10:00 → in recurrence, but outside afternoon slot
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 1, 10, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // Jan 1, 2025 — 15:00 → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 1, 15, 0)))
                    .isNull();

            // Jan 2, 2025 — 14:00 → still in 3-day window, slot matches → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 2, 14, 0)))
                    .isNull();

            // Jan 3, 2025 — 21:59 → last minute of window + slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 3, 21, 59)))
                    .isNull();

            // Jan 4, 2025 — 00:00 → 3 days past, outside recurrence
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 4, 0, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);

            // Jan 15, 2025 — middle of month, no event
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 15, 15, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);
        }

        @Test
        void feb_day31_clampedTo28or29() {
            // "31" as startRef gets clamped to month length
            RecurringScheduleMode mode = new RecurringScheduleMode(
                    RecurrenceUnit.MONTH, "31", Duration.ofDays(1),
                    List.of()  // no slots = only recurrence check
            );

            // February 2025 (non-leap) — day 31 clamped to 28
            // Feb 28 at noon → in window (starts Feb 28, lasts 1 day = until Mar 1)
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 2, 28, 12, 0)))
                    .isNull();

            // Feb 27 at noon → not in window yet (window starts Feb 28)
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 2, 27, 12, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);

            // February 2024 (leap year) — day 31 clamped to 29
            assertThat(mode.getDenyReason(LocalDateTime.of(2024, 2, 29, 12, 0)))
                    .isNull();
        }

        @Test
        void roundTrip_monthlyMode_preservesBehavior() {
            RecurringScheduleMode original = createPaydayEvent();
            MemoryConfiguration config = new MemoryConfiguration();
            original.saveTo(config);

            RecurringScheduleMode reloaded = RecurringScheduleMode.fromConfig(config);

            // Same modeId
            assertThat(reloaded.getModeId()).isEqualTo("recurring");

            // Same behavior at key timestamps
            assertThat(reloaded.getDenyReason(LocalDateTime.of(2025, 1, 1, 15, 0))).isNull();
            assertThat(reloaded.getDenyReason(LocalDateTime.of(2025, 1, 4, 0, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);
        }
    }

    // =========================================================================
    // Slots schedule with active date boundaries
    // Simulates a seasonal event: only active June–August, weekdays 9:00–17:00
    // =========================================================================

    @Nested
    class SummerEventSlots {

        private SlotsScheduleMode createSummerEvent() {
            TimeSlot weekdays = new TimeSlot(
                    List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
            );
            return new SlotsScheduleMode(
                    List.of(weekdays),
                    LocalDate.of(2025, 6, 1),   // active from June 1
                    LocalDate.of(2025, 8, 31)    // active until August 31
            );
        }

        @Test
        void evaluateAcrossFullYear() {
            SlotsScheduleMode mode = createSummerEvent();

            // January — before active period
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0)))
                    .isEqualTo(DenyReason.NOT_STARTED);

            // May 31 — still before June 1
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 5, 31, 12, 0)))
                    .isEqualTo(DenyReason.NOT_STARTED);

            // June 1 — active from date (inclusive), but June 1, 2025 is a Sunday → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 6, 1, 12, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // June 2 (Monday) 9:00 — in slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 6, 2, 9, 0)))
                    .isNull();

            // June 2 (Monday) 8:59 — before slot start → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 6, 2, 8, 59)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // June 2 (Monday) 17:00 — slot end (exclusive) → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 6, 2, 17, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // July 4 (Friday) 12:00 — mid-summer, weekday, in slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 7, 4, 12, 0)))
                    .isNull();

            // July 5 (Saturday) 12:00 — weekend → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 7, 5, 12, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // August 31 — active until date (inclusive)
            // Aug 31, 2025 is a Sunday → OUTSIDE_SLOT (not ENDED)
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 8, 31, 12, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // September 1 — after active period
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 9, 1, 12, 0)))
                    .isEqualTo(DenyReason.ENDED);
        }

        @Test
        void persistAndReload_identicalBehavior() {
            SlotsScheduleMode original = createSummerEvent();

            MemoryConfiguration config = new MemoryConfiguration();
            original.saveTo(config);

            SlotsScheduleMode reloaded = SlotsScheduleMode.fromConfig(config);

            // Verify key timestamps produce same deny reasons
            LocalDateTime[] testTimes = {
                    LocalDateTime.of(2025, 5, 31, 12, 0),  // NOT_STARTED
                    LocalDateTime.of(2025, 6, 2, 9, 0),    // null (allowed)
                    LocalDateTime.of(2025, 6, 2, 17, 0),   // OUTSIDE_SLOT
                    LocalDateTime.of(2025, 7, 5, 12, 0),   // OUTSIDE_SLOT (weekend)
                    LocalDateTime.of(2025, 9, 1, 12, 0),   // ENDED
            };

            for (LocalDateTime time : testTimes) {
                assertThat(reloaded.getDenyReason(time))
                        .as("at %s", time)
                        .isEqualTo(original.getDenyReason(time));
            }
        }

        @Test
        void getDenyDetail_matchesDenyReason() {
            SlotsScheduleMode mode = createSummerEvent();

            // NOT_STARTED → should show activeFrom date
            String notStarted = mode.getDenyDetail(DenyReason.NOT_STARTED);
            assertThat(notStarted).isEqualTo("06/01/2025");

            // ENDED → should show activeUntil date
            String ended = mode.getDenyDetail(DenyReason.ENDED);
            assertThat(ended).isEqualTo("08/31/2025");

            // OUTSIDE_SLOT → should show slot info
            String outsideSlot = mode.getDenyDetail(DenyReason.OUTSIDE_SLOT);
            assertThat(outsideSlot).contains("09:00-17:00");
        }
    }

    // =========================================================================
    // Range schedule: fixed start/end dates
    // Simulates a one-time event: December 20–31 with time slots
    // =========================================================================

    @Nested
    class ChristmasEventRange {

        private RangeScheduleMode createChristmasEvent() {
            TimeSlot evenings = new TimeSlot(
                    List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                    LocalTime.of(18, 0),
                    LocalTime.of(23, 0)
            );
            return new RangeScheduleMode(
                    LocalDateTime.of(2025, 12, 20, 0, 0),
                    LocalDateTime.of(2025, 12, 31, 23, 59),
                    List.of(evenings)
            );
        }

        @Test
        void evaluateChristmasPeriod() {
            RangeScheduleMode mode = createChristmasEvent();

            // December 19 — before range
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 12, 19, 20, 0)))
                    .isEqualTo(DenyReason.NOT_STARTED);

            // December 20 00:00 — range start (inclusive), but 00:00 outside evening slot
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 12, 20, 0, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // December 20 18:00 — in range + slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 12, 20, 18, 0)))
                    .isNull();

            // December 25 22:59 — Christmas evening → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 12, 25, 22, 59)))
                    .isNull();

            // December 25 23:00 — slot ends (exclusive) → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 12, 25, 23, 0)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // December 31 22:00 — last day, in slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 12, 31, 22, 0)))
                    .isNull();

            // January 1 00:00 — after range (isAfter is strict, 23:59 end check)
            assertThat(mode.getDenyReason(LocalDateTime.of(2026, 1, 1, 0, 0)))
                    .isEqualTo(DenyReason.ENDED);
        }

        @Test
        void roundTrip_rangeMode_preservesBehavior() {
            RangeScheduleMode original = createChristmasEvent();

            MemoryConfiguration config = new MemoryConfiguration();
            original.saveTo(config);

            RangeScheduleMode reloaded = RangeScheduleMode.fromConfig(config);

            assertThat(reloaded.getModeId()).isEqualTo("range");

            // Same behavior
            assertThat(reloaded.getDenyReason(LocalDateTime.of(2025, 12, 19, 20, 0)))
                    .isEqualTo(DenyReason.NOT_STARTED);
            assertThat(reloaded.getDenyReason(LocalDateTime.of(2025, 12, 25, 20, 0)))
                    .isNull();
            assertThat(reloaded.getDenyReason(LocalDateTime.of(2026, 1, 1, 0, 0)))
                    .isEqualTo(DenyReason.ENDED);
        }
    }

    // =========================================================================
    // Complex recurring: yearly Halloween event with two slot windows
    // Active October 25 for 7 days, with different slots on weekdays vs weekends
    // =========================================================================

    @Nested
    class HalloweenYearlyEvent {

        private RecurringScheduleMode createHalloweenEvent() {
            TimeSlot weekdaySlot = new TimeSlot(
                    List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                    LocalTime.of(17, 0),
                    LocalTime.of(22, 0)
            );
            TimeSlot weekendSlot = new TimeSlot(
                    List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                    LocalTime.of(10, 0),
                    LocalTime.of(23, 0)
            );
            return new RecurringScheduleMode(
                    RecurrenceUnit.YEAR,
                    "10/25",   // October 25
                    Duration.ofDays(7),
                    List.of(weekdaySlot, weekendSlot)
            );
        }

        @Test
        void evaluateHalloweenWeek2025() {
            RecurringScheduleMode mode = createHalloweenEvent();

            // Oct 24 — before event start
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 10, 24, 18, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);

            // Oct 25 (Saturday) 10:00 — weekend slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 10, 25, 10, 0)))
                    .isNull();

            // Oct 25 (Saturday) 09:59 — before weekend slot → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 10, 25, 9, 59)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // Oct 27 (Monday) 17:00 — weekday slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 10, 27, 17, 0)))
                    .isNull();

            // Oct 27 (Monday) 16:59 — before weekday slot → OUTSIDE_SLOT
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 10, 27, 16, 59)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // Oct 31 (Friday) 21:00 — Halloween night! → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 10, 31, 21, 0)))
                    .isNull();

            // Nov 1 (Saturday) — day 8, outside 7-day window
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 11, 1, 12, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);
        }

        @Test
        void sameEventRecursNextYear() {
            RecurringScheduleMode mode = createHalloweenEvent();

            // Oct 25, 2026 (Sunday) 15:00 → weekend slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2026, 10, 25, 15, 0)))
                    .isNull();

            // Oct 30, 2026 (Friday) 18:00 → weekday slot → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2026, 10, 30, 18, 0)))
                    .isNull();
        }

        @Test
        void displayInfo_containsAllRelevantInfo() {
            RecurringScheduleMode mode = createHalloweenEvent();

            String info = mode.getDisplayInfo();
            assertThat(info).contains("Recurring");
            assertThat(info).contains("year");
            assertThat(info).contains("10/25");
            assertThat(info).contains("1w");
        }
    }

    // =========================================================================
    // Edge case: mode with no time slots — only recurrence/range/active dates
    // =========================================================================

    @Nested
    class NoSlotsOnlyRecurrence {

        @Test
        void recurringWithoutSlots_anyTimeInWindowIsAllowed() {
            RecurringScheduleMode mode = new RecurringScheduleMode(
                    RecurrenceUnit.WEEK,
                    "MONDAY",
                    Duration.ofDays(1),
                    List.of()  // no slots
            );

            // Monday 00:00 — in recurrence window, no slots → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 0, 0))).isNull();

            // Monday 23:59 — still in 1-day window → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 23, 59))).isNull();

            // Tuesday 00:00 — outside 1-day window
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 7, 0, 0)))
                    .isEqualTo(DenyReason.NOT_IN_RECURRENCE);
        }

        @Test
        void rangeWithoutSlots_anyTimeInRangeIsAllowed() {
            RangeScheduleMode mode = new RangeScheduleMode(
                    LocalDateTime.of(2025, 6, 1, 0, 0),
                    LocalDateTime.of(2025, 6, 30, 23, 59),
                    List.of()  // no slots
            );

            // Any time in June → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 6, 15, 3, 0))).isNull();
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 6, 1, 0, 0))).isNull();

            // Before → NOT_STARTED
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 5, 31, 23, 59)))
                    .isEqualTo(DenyReason.NOT_STARTED);

            // After → ENDED
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 7, 1, 0, 0)))
                    .isEqualTo(DenyReason.ENDED);
        }

        @Test
        void slotsWithNoActiveDate_onlySlotsMatter() {
            TimeSlot slot = new TimeSlot(
                    List.of(DayOfWeek.WEDNESDAY),
                    LocalTime.of(12, 0),
                    LocalTime.of(13, 0)
            );
            SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot), null, null);

            // Wednesday 12:30 → allowed
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 8, 12, 30))).isNull();

            // Wednesday 11:59 → outside slot
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 8, 11, 59)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);

            // Thursday any time → outside slot
            assertThat(mode.getDenyReason(LocalDateTime.of(2025, 1, 9, 12, 30)))
                    .isEqualTo(DenyReason.OUTSIDE_SLOT);
        }
    }

    // =========================================================================
    // Full round-trip: create each mode type, save, reload, verify
    // =========================================================================

    @Nested
    class FullRoundTrip {

        @Test
        void allThreeModes_saveReload_preserveState() {
            // --- Range ---
            RangeScheduleMode range = new RangeScheduleMode(
                    LocalDateTime.of(2025, 3, 1, 8, 0),
                    LocalDateTime.of(2025, 3, 31, 20, 0),
                    List.of(new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0)))
            );
            MemoryConfiguration rangeConfig = new MemoryConfiguration();
            range.saveTo(rangeConfig);
            RangeScheduleMode rangeReloaded = RangeScheduleMode.fromConfig(rangeConfig);
            assertThat(rangeReloaded.start()).isEqualTo(range.start());
            assertThat(rangeReloaded.end()).isEqualTo(range.end());
            assertThat(rangeReloaded.slots()).hasSize(1);

            // --- Slots ---
            SlotsScheduleMode slots = new SlotsScheduleMode(
                    List.of(new TimeSlot(List.of(DayOfWeek.FRIDAY), LocalTime.of(18, 0), LocalTime.of(23, 0))),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 12, 31)
            );
            MemoryConfiguration slotsConfig = new MemoryConfiguration();
            slots.saveTo(slotsConfig);
            SlotsScheduleMode slotsReloaded = SlotsScheduleMode.fromConfig(slotsConfig);
            assertThat(slotsReloaded.activeFrom()).isEqualTo(LocalDate.of(2025, 1, 1));
            assertThat(slotsReloaded.activeUntil()).isEqualTo(LocalDate.of(2025, 12, 31));
            assertThat(slotsReloaded.slots()).hasSize(1);

            // --- Recurring ---
            RecurringScheduleMode recurring = new RecurringScheduleMode(
                    RecurrenceUnit.MONTH,
                    "15",
                    Duration.ofDays(2),
                    List.of(new TimeSlot(List.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                            LocalTime.of(10, 0), LocalTime.of(16, 0)))
            );
            MemoryConfiguration recurringConfig = new MemoryConfiguration();
            recurring.saveTo(recurringConfig);
            RecurringScheduleMode recurringReloaded = RecurringScheduleMode.fromConfig(recurringConfig);
            assertThat(recurringReloaded.every()).isEqualTo(RecurrenceUnit.MONTH);
            assertThat(recurringReloaded.startRef()).isEqualTo("15");
            assertThat(recurringReloaded.duration()).isEqualTo(Duration.ofDays(2));
            assertThat(recurringReloaded.slots()).hasSize(1);
        }
    }
}
