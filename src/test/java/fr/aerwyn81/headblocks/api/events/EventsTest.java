package fr.aerwyn81.headblocks.api.events;

import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventsTest {

    @Mock
    Player player;

    @Mock
    Location location;

    @Mock
    HBHunt hunt;

    @Nested
    class HeadClickEventTest {

        @Test
        void constructorSetsFieldsCorrectly() {
            UUID headUuid = UUID.randomUUID();
            List<String> huntIds = List.of("hunt1", "hunt2");

            HeadClickEvent event = new HeadClickEvent(headUuid, player, location, true, huntIds);

            assertThat(event.getHeadUuid()).isEqualTo(headUuid);
            assertThat(event.getPlayer()).isSameAs(player);
            assertThat(event.getLocation()).isSameAs(location);
            assertThat(event.isSuccess()).isTrue();
            assertThat(event.getHuntIds()).containsExactly("hunt1", "hunt2");
        }

        @Test
        void shortConstructorDefaultsToEmptyHuntIds() {
            UUID headUuid = UUID.randomUUID();

            HeadClickEvent event = new HeadClickEvent(headUuid, player, location, false);

            assertThat(event.isSuccess()).isFalse();
            assertThat(event.getHuntIds()).isEmpty();
        }

        @Test
        void nullHuntIdsBecomesEmptyList() {
            UUID headUuid = UUID.randomUUID();

            HeadClickEvent event = new HeadClickEvent(headUuid, player, location, true, null);

            assertThat(event.getHuntIds()).isEmpty();
        }

        @Test
        void getHuntIdReturnsNullWhenEmpty() {
            HeadClickEvent event = new HeadClickEvent(UUID.randomUUID(), player, location, true, Collections.emptyList());

            assertThat(event.getHuntId()).isNull();
        }

        @Test
        void getHuntIdReturnsFirstElement() {
            HeadClickEvent event = new HeadClickEvent(UUID.randomUUID(), player, location, true, List.of("alpha", "beta"));

            assertThat(event.getHuntId()).isEqualTo("alpha");
        }

        @Test
        void getHuntIdReturnsNullForNullList() {
            HeadClickEvent event = new HeadClickEvent(UUID.randomUUID(), player, location, true, null);

            assertThat(event.getHuntId()).isNull();
        }

        @Test
        void handlerListIsAccessible() {
            HeadClickEvent event = new HeadClickEvent(UUID.randomUUID(), player, location, true);

            HandlerList staticList = HeadClickEvent.getHandlerList();
            HandlerList instanceList = event.getHandlers();

            assertThat(staticList).isNotNull();
            assertThat(instanceList).isSameAs(staticList);
        }
    }

    @Nested
    class HeadCreatedEventTest {

        @Test
        void constructorSetsAllFields() {
            UUID headUuid = UUID.randomUUID();

            HeadCreatedEvent event = new HeadCreatedEvent(headUuid, location, "myHunt");

            assertThat(event.getHeadUuid()).isEqualTo(headUuid);
            assertThat(event.getLocation()).isSameAs(location);
            assertThat(event.getHuntId()).isEqualTo("myHunt");
        }

        @Test
        void shortConstructorSetsHuntIdToNull() {
            UUID headUuid = UUID.randomUUID();

            HeadCreatedEvent event = new HeadCreatedEvent(headUuid, location);

            assertThat(event.getHeadUuid()).isEqualTo(headUuid);
            assertThat(event.getLocation()).isSameAs(location);
            assertThat(event.getHuntId()).isNull();
        }

        @Test
        void handlerListIsAccessible() {
            HeadCreatedEvent event = new HeadCreatedEvent(UUID.randomUUID(), location);

            HandlerList staticList = HeadCreatedEvent.getHandlerList();
            HandlerList instanceList = event.getHandlers();

            assertThat(staticList).isNotNull();
            assertThat(instanceList).isSameAs(staticList);
        }
    }

    @Nested
    class HeadDeletedEventTest {

        @Test
        void constructorSetsAllFields() {
            UUID headUuid = UUID.randomUUID();

            HeadDeletedEvent event = new HeadDeletedEvent(headUuid, location, "hunt42");

            assertThat(event.getHeadUuid()).isEqualTo(headUuid);
            assertThat(event.getLocation()).isSameAs(location);
            assertThat(event.getHuntId()).isEqualTo("hunt42");
        }

        @Test
        void shortConstructorSetsHuntIdToNull() {
            UUID headUuid = UUID.randomUUID();

            HeadDeletedEvent event = new HeadDeletedEvent(headUuid, location);

            assertThat(event.getHuntId()).isNull();
        }

        @Test
        void handlerListIsAccessible() {
            HeadDeletedEvent event = new HeadDeletedEvent(UUID.randomUUID(), location);

            HandlerList staticList = HeadDeletedEvent.getHandlerList();
            HandlerList instanceList = event.getHandlers();

            assertThat(staticList).isNotNull();
            assertThat(instanceList).isSameAs(staticList);
        }
    }

    @Nested
    class HuntCreateEventTest {

        @Test
        void constructorSetsHunt() {
            when(hunt.getId()).thenReturn("newHunt");

            HuntCreateEvent event = new HuntCreateEvent(hunt);

            assertThat(event.getHunt()).isSameAs(hunt);
            assertThat(event.getHuntId()).isEqualTo("newHunt");
        }

        @Test
        void cancellationDefaultsToFalse() {
            HuntCreateEvent event = new HuntCreateEvent(hunt);

            assertThat(event.isCancelled()).isFalse();
        }

        @Test
        void setCancelledTogglesCancellation() {
            HuntCreateEvent event = new HuntCreateEvent(hunt);

            event.setCancelled(true);
            assertThat(event.isCancelled()).isTrue();

            event.setCancelled(false);
            assertThat(event.isCancelled()).isFalse();
        }

        @Test
        void handlerListIsAccessible() {
            HuntCreateEvent event = new HuntCreateEvent(hunt);

            HandlerList staticList = HuntCreateEvent.getHandlerList();
            HandlerList instanceList = event.getHandlers();

            assertThat(staticList).isNotNull();
            assertThat(instanceList).isSameAs(staticList);
        }
    }

    @Nested
    class HuntDeleteEventTest {

        @Test
        void constructorSetsHuntId() {
            HuntDeleteEvent event = new HuntDeleteEvent("doomed");

            assertThat(event.getHuntId()).isEqualTo("doomed");
        }

        @Test
        void cancellationDefaultsToFalse() {
            HuntDeleteEvent event = new HuntDeleteEvent("x");

            assertThat(event.isCancelled()).isFalse();
        }

        @Test
        void setCancelledTogglesCancellation() {
            HuntDeleteEvent event = new HuntDeleteEvent("x");

            event.setCancelled(true);
            assertThat(event.isCancelled()).isTrue();

            event.setCancelled(false);
            assertThat(event.isCancelled()).isFalse();
        }

        @Test
        void handlerListIsAccessible() {
            HuntDeleteEvent event = new HuntDeleteEvent("x");

            HandlerList staticList = HuntDeleteEvent.getHandlerList();
            HandlerList instanceList = event.getHandlers();

            assertThat(staticList).isNotNull();
            assertThat(instanceList).isSameAs(staticList);
        }
    }

    @Nested
    class HuntStateChangeEventTest {

        @Test
        void constructorSetsAllFields() {
            when(hunt.getId()).thenReturn("stateHunt");

            HuntStateChangeEvent event = new HuntStateChangeEvent(hunt, HuntState.INACTIVE, HuntState.ACTIVE);

            assertThat(event.getHunt()).isSameAs(hunt);
            assertThat(event.getHuntId()).isEqualTo("stateHunt");
            assertThat(event.getOldState()).isEqualTo(HuntState.INACTIVE);
            assertThat(event.getNewState()).isEqualTo(HuntState.ACTIVE);
        }

        @Test
        void activeToArchived() {
            HuntStateChangeEvent event = new HuntStateChangeEvent(hunt, HuntState.ACTIVE, HuntState.ARCHIVED);

            assertThat(event.getOldState()).isEqualTo(HuntState.ACTIVE);
            assertThat(event.getNewState()).isEqualTo(HuntState.ARCHIVED);
        }

        @Test
        void archivedToInactive() {
            HuntStateChangeEvent event = new HuntStateChangeEvent(hunt, HuntState.ARCHIVED, HuntState.INACTIVE);

            assertThat(event.getOldState()).isEqualTo(HuntState.ARCHIVED);
            assertThat(event.getNewState()).isEqualTo(HuntState.INACTIVE);
        }

        @Test
        void sameStateTransition() {
            HuntStateChangeEvent event = new HuntStateChangeEvent(hunt, HuntState.ACTIVE, HuntState.ACTIVE);

            assertThat(event.getOldState()).isEqualTo(event.getNewState());
        }

        @Test
        void cancellationDefaultsToFalse() {
            HuntStateChangeEvent event = new HuntStateChangeEvent(hunt, HuntState.ACTIVE, HuntState.INACTIVE);

            assertThat(event.isCancelled()).isFalse();
        }

        @Test
        void setCancelledTogglesCancellation() {
            HuntStateChangeEvent event = new HuntStateChangeEvent(hunt, HuntState.ACTIVE, HuntState.INACTIVE);

            event.setCancelled(true);
            assertThat(event.isCancelled()).isTrue();

            event.setCancelled(false);
            assertThat(event.isCancelled()).isFalse();
        }

        @Test
        void handlerListIsAccessible() {
            HuntStateChangeEvent event = new HuntStateChangeEvent(hunt, HuntState.ACTIVE, HuntState.INACTIVE);

            HandlerList staticList = HuntStateChangeEvent.getHandlerList();
            HandlerList instanceList = event.getHandlers();

            assertThat(staticList).isNotNull();
            assertThat(instanceList).isSameAs(staticList);
        }
    }
}
