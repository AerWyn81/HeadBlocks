package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class HuntServiceTest {

    @BeforeEach
    void setUp() throws Exception {
        clearStaticField("huntsById");
        clearStaticField("headToHunts");
        clearStaticField("selectedHunt");
    }

    @SuppressWarnings("unchecked")
    private void clearStaticField(String fieldName) throws Exception {
        Field field = HuntService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    // --- Registry ---

    @Test
    void registerHunt_addsToRegistry() {
        Hunt hunt = new Hunt("hunt1", "Test Hunt", HuntState.ACTIVE, 1, "D");

        HuntService.registerHunt(hunt);

        assertThat(HuntService.getHuntById("hunt1")).isSameAs(hunt);
        assertThat(HuntService.huntExists("hunt1")).isTrue();
        assertThat(HuntService.getHuntCount()).isEqualTo(1);
    }

    @Test
    void registerHunt_replacesExistingWithSameId() {
        Hunt hunt1 = new Hunt("hunt1", "Original", HuntState.ACTIVE, 1, "D");
        Hunt hunt2 = new Hunt("hunt1", "Replaced", HuntState.ACTIVE, 2, "D");

        HuntService.registerHunt(hunt1);
        HuntService.registerHunt(hunt2);

        assertThat(HuntService.getHuntById("hunt1").getDisplayName()).isEqualTo("Replaced");
        assertThat(HuntService.getHuntCount()).isEqualTo(1);
    }

    @Test
    void unregisterHunt_removesFromRegistry() {
        Hunt hunt = new Hunt("hunt1", "Test", HuntState.ACTIVE, 1, "D");
        HuntService.registerHunt(hunt);

        HuntService.unregisterHunt("hunt1");

        assertThat(HuntService.huntExists("hunt1")).isFalse();
        assertThat(HuntService.getHuntCount()).isEqualTo(0);
    }

    @Test
    void unregisterHunt_nonExistent_noError() {
        HuntService.unregisterHunt("nonexistent"); // should not throw
        assertThat(HuntService.getHuntCount()).isEqualTo(0);
    }

    @Test
    void getHuntById_unknown_returnsNull() {
        assertThat(HuntService.getHuntById("nonexistent")).isNull();
    }

    @Test
    void getDefaultHunt_returnsHuntWithIdDefault() {
        Hunt defaultHunt = new Hunt("default", "Default", HuntState.ACTIVE, 0, "D");
        HuntService.registerHunt(defaultHunt);

        assertThat(HuntService.getDefaultHunt()).isSameAs(defaultHunt);
    }

    @Test
    void getDefaultHunt_noDefault_returnsNull() {
        Hunt other = new Hunt("custom", "Custom", HuntState.ACTIVE, 1, "D");
        HuntService.registerHunt(other);

        assertThat(HuntService.getDefaultHunt()).isNull();
    }

    // --- Active hunts ---

    @Test
    void getActiveHunts_filtersInactive() {
        Hunt active = new Hunt("a", "Active", HuntState.ACTIVE, 1, "D");
        Hunt inactive = new Hunt("b", "Inactive", HuntState.INACTIVE, 2, "D");
        Hunt archived = new Hunt("c", "Archived", HuntState.ARCHIVED, 3, "D");

        HuntService.registerHunt(active);
        HuntService.registerHunt(inactive);
        HuntService.registerHunt(archived);

        assertThat(HuntService.getActiveHunts()).containsExactly(active);
    }

    @Test
    void getAllHunts_returnsUnmodifiableCollection() {
        Hunt hunt = new Hunt("h1", "Test", HuntState.ACTIVE, 1, "D");
        HuntService.registerHunt(hunt);

        Collection<Hunt> all = HuntService.getAllHunts();
        assertThat(all).hasSize(1);
        assertThatThrownBy(() -> all.add(new Hunt("h2", "X", HuntState.ACTIVE, 1, "D")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isMultiHunt_singleHunt_false() {
        HuntService.registerHunt(new Hunt("h1", "T", HuntState.ACTIVE, 1, "D"));
        assertThat(HuntService.isMultiHunt()).isFalse();
    }

    @Test
    void isMultiHunt_twoHunts_true() {
        HuntService.registerHunt(new Hunt("h1", "T", HuntState.ACTIVE, 1, "D"));
        HuntService.registerHunt(new Hunt("h2", "T", HuntState.ACTIVE, 1, "D"));
        assertThat(HuntService.isMultiHunt()).isTrue();
    }

    @Test
    void getHuntNames_returnsAllIds() {
        HuntService.registerHunt(new Hunt("alpha", "A", HuntState.ACTIVE, 1, "D"));
        HuntService.registerHunt(new Hunt("beta", "B", HuntState.INACTIVE, 2, "D"));

        assertThat(HuntService.getHuntNames()).containsExactlyInAnyOrder("alpha", "beta");
    }

    // --- Head-to-hunts cache ---

    @Test
    void rebuildHeadToHuntsCache_mapsHeadsToHunts() {
        UUID head1 = UUID.randomUUID();
        Hunt hunt = new Hunt("h1", "Test", HuntState.ACTIVE, 1, "D");
        hunt.addHead(head1);

        HuntService.registerHunt(hunt); // registerHunt calls rebuildHeadToHuntsCache

        assertThat(HuntService.getHuntsForHead(head1)).containsExactly(hunt);
    }

    @Test
    void rebuildHeadToHuntsCache_sortsByPriorityDesc() {
        UUID head = UUID.randomUUID();
        Hunt low = new Hunt("low", "Low", HuntState.ACTIVE, 1, "D");
        Hunt high = new Hunt("high", "High", HuntState.ACTIVE, 10, "D");
        Hunt mid = new Hunt("mid", "Mid", HuntState.ACTIVE, 5, "D");

        low.addHead(head);
        high.addHead(head);
        mid.addHead(head);

        HuntService.registerHunt(low);
        HuntService.registerHunt(high);
        HuntService.registerHunt(mid);

        List<Hunt> hunts = HuntService.getHuntsForHead(head);
        assertThat(hunts).extracting(Hunt::getId).containsExactly("high", "mid", "low");
    }

    @Test
    void getHuntsForHead_unknownHead_returnsEmptyList() {
        assertThat(HuntService.getHuntsForHead(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getHighestPriorityHuntForHead_returnsFirstActive() {
        UUID head = UUID.randomUUID();
        Hunt inactive = new Hunt("inactive", "I", HuntState.INACTIVE, 10, "D");
        Hunt active = new Hunt("active", "A", HuntState.ACTIVE, 5, "D");

        inactive.addHead(head);
        active.addHead(head);

        HuntService.registerHunt(inactive);
        HuntService.registerHunt(active);

        assertThat(HuntService.getHighestPriorityHuntForHead(head).getId()).isEqualTo("active");
    }

    @Test
    void getHighestPriorityHuntForHead_allInactive_returnsNull() {
        UUID head = UUID.randomUUID();
        Hunt inactive = new Hunt("i1", "I", HuntState.INACTIVE, 10, "D");
        inactive.addHead(head);

        HuntService.registerHunt(inactive);

        assertThat(HuntService.getHighestPriorityHuntForHead(head)).isNull();
    }

    @Test
    void getHighestPriorityHuntForHead_unknownHead_returnsNull() {
        assertThat(HuntService.getHighestPriorityHuntForHead(UUID.randomUUID())).isNull();
    }

    // --- Selected hunt session ---

    @Test
    void selectedHunt_defaultIsDefault() {
        UUID player = UUID.randomUUID();
        assertThat(HuntService.getSelectedHunt(player)).isEqualTo("default");
    }

    @Test
    void setSelectedHunt_returnsSetValue() {
        UUID player = UUID.randomUUID();
        HuntService.setSelectedHunt(player, "custom");
        assertThat(HuntService.getSelectedHunt(player)).isEqualTo("custom");
    }

    @Test
    void clearSelectedHunt_revertsToDefault() {
        UUID player = UUID.randomUUID();
        HuntService.setSelectedHunt(player, "custom");
        HuntService.clearSelectedHunt(player);
        assertThat(HuntService.getSelectedHunt(player)).isEqualTo("default");
    }

    // --- Transfer head ---

    @Test
    void transferHead_movesHeadToTargetHunt() throws Exception {
        UUID head = UUID.randomUUID();
        Hunt source = new Hunt("source", "Source", HuntState.ACTIVE, 1, "D");
        Hunt target = new Hunt("target", "Target", HuntState.ACTIVE, 2, "D");
        source.addHead(head);

        HuntService.registerHunt(source);
        HuntService.registerHunt(target);

        try (MockedStatic<StorageService> ss = mockStatic(StorageService.class)) {
            HuntService.transferHead(head, "target");

            assertThat(source.containsHead(head)).isFalse();
            assertThat(target.containsHead(head)).isTrue();

            ss.verify(() -> StorageService.unlinkHeadFromHunt(head, "source"));
            ss.verify(() -> StorageService.linkHeadToHunt(head, "target"));
            ss.verify(StorageService::incrementHuntVersion);
        }
    }

    @Test
    void transferHead_targetNotFound_throwsIllegalArgument() {
        HuntService.registerHunt(new Hunt("source", "S", HuntState.ACTIVE, 1, "D"));

        assertThatThrownBy(() -> HuntService.transferHead(UUID.randomUUID(), "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void transferHead_headInMultipleHunts_removedFromAll() throws Exception {
        UUID head = UUID.randomUUID();
        Hunt h1 = new Hunt("h1", "H1", HuntState.ACTIVE, 1, "D");
        Hunt h2 = new Hunt("h2", "H2", HuntState.ACTIVE, 2, "D");
        Hunt target = new Hunt("target", "T", HuntState.ACTIVE, 3, "D");

        h1.addHead(head);
        h2.addHead(head);

        HuntService.registerHunt(h1);
        HuntService.registerHunt(h2);
        HuntService.registerHunt(target);

        try (MockedStatic<StorageService> ss = mockStatic(StorageService.class)) {
            HuntService.transferHead(head, "target");

            assertThat(h1.containsHead(head)).isFalse();
            assertThat(h2.containsHead(head)).isFalse();
            assertThat(target.containsHead(head)).isTrue();

            ss.verify(() -> StorageService.unlinkHeadFromHunt(head, "h1"));
            ss.verify(() -> StorageService.unlinkHeadFromHunt(head, "h2"));
        }
    }

    // --- Assign head to hunt ---

    @Test
    void assignHeadToHunt_addsHeadAndRebuildsCache() throws Exception {
        UUID head = UUID.randomUUID();
        Hunt hunt = new Hunt("h1", "Test", HuntState.ACTIVE, 1, "D");
        HuntService.registerHunt(hunt);

        try (MockedStatic<StorageService> ss = mockStatic(StorageService.class)) {
            HuntService.assignHeadToHunt(head, "h1");

            assertThat(hunt.containsHead(head)).isTrue();
            assertThat(HuntService.getHuntsForHead(head)).contains(hunt);
            ss.verify(() -> StorageService.linkHeadToHunt(head, "h1"));
        }
    }

    @Test
    void assignHeadToHunt_huntNotFound_throwsIllegalArgument() {
        assertThatThrownBy(() -> HuntService.assignHeadToHunt(UUID.randomUUID(), "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }
}
