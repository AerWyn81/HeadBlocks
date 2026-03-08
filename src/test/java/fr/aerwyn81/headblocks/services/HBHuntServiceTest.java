package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HBHuntServiceTest {

    @Mock
    private ConfigService configService;

    @Mock
    private HuntConfigService huntConfigService;

    @Mock
    private StorageService storageService;

    private HuntService huntService;

    @BeforeEach
    void setUp() throws Exception {
        // Return empty lists so doInitialize() doesn't fail
        when(huntConfigService.loadHunts()).thenReturn(new ArrayList<>());
        when(storageService.getHuntsFromDb()).thenReturn(new ArrayList<>());
        when(storageService.getHeadsForHunt(anyString())).thenReturn(new ArrayList<>());
        when(storageService.getHuntVersion()).thenReturn(0L);

        huntService = new HuntService(configService, huntConfigService, storageService);
    }

    // --- Registry ---

    @Test
    void registerHunt_addsToRegistry() {
        HBHunt hunt = new HBHunt(configService, "hunt1", "Test Hunt", HuntState.ACTIVE, 1, "D");

        huntService.registerHunt(hunt);

        assertThat(huntService.getHuntById("hunt1")).isSameAs(hunt);
        assertThat(huntService.huntExists("hunt1")).isTrue();
        assertThat(huntService.getHuntCount()).isGreaterThanOrEqualTo(2); // default + hunt1
    }

    @Test
    void registerHunt_replacesExistingWithSameId() {
        HBHunt hunt1 = new HBHunt(configService, "hunt1", "Original", HuntState.ACTIVE, 1, "D");
        HBHunt hunt2 = new HBHunt(configService, "hunt1", "Replaced", HuntState.ACTIVE, 2, "D");

        huntService.registerHunt(hunt1);
        huntService.registerHunt(hunt2);

        assertThat(huntService.getHuntById("hunt1").getDisplayName()).isEqualTo("Replaced");
    }

    @Test
    void unregisterHunt_removesFromRegistry() {
        HBHunt hunt = new HBHunt(configService, "hunt1", "Test", HuntState.ACTIVE, 1, "D");
        huntService.registerHunt(hunt);

        huntService.unregisterHunt("hunt1");

        assertThat(huntService.huntExists("hunt1")).isFalse();
    }

    @Test
    void unregisterHunt_nonExistent_noError() {
        huntService.unregisterHunt("nonexistent"); // should not throw
    }

    @Test
    void getHuntById_unknown_returnsNull() {
        assertThat(huntService.getHuntById("nonexistent")).isNull();
    }

    @Test
    void getDefaultHunt_returnsHuntWithIdDefault() {
        // The default hunt is auto-created by doInitialize()
        assertThat(huntService.getDefaultHunt()).isNotNull();
        assertThat(huntService.getDefaultHunt().getId()).isEqualTo("default");
    }

    @Test
    void getDefaultHunt_isAlwaysPresent() {
        // Default hunt is created during initialization
        assertThat(huntService.getDefaultHunt()).isNotNull();
    }

    // --- Active hunts ---

    @Test
    void getActiveHunts_filtersInactive() {
        HBHunt active = new HBHunt(configService, "a", "Active", HuntState.ACTIVE, 1, "D");
        HBHunt inactive = new HBHunt(configService, "b", "Inactive", HuntState.INACTIVE, 2, "D");
        HBHunt archived = new HBHunt(configService, "c", "Archived", HuntState.ARCHIVED, 3, "D");

        huntService.registerHunt(active);
        huntService.registerHunt(inactive);
        huntService.registerHunt(archived);

        List<HBHunt> activeHunts = huntService.getActiveHunts();
        assertThat(activeHunts).contains(active);
        assertThat(activeHunts).doesNotContain(inactive, archived);
    }

    @Test
    void getAllHunts_returnsUnmodifiableCollection() {
        HBHunt hunt = new HBHunt(configService, "h1", "Test", HuntState.ACTIVE, 1, "D");
        huntService.registerHunt(hunt);

        Collection<HBHunt> all = huntService.getAllHunts();
        assertThat(all).hasSizeGreaterThanOrEqualTo(1);
        assertThatThrownBy(() -> all.add(new HBHunt(configService, "h2", "X", HuntState.ACTIVE, 1, "D")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isMultiHunt_singleHunt_false() {
        // Only the default hunt exists
        assertThat(huntService.getHuntCount()).isEqualTo(1);
        assertThat(huntService.isMultiHunt()).isFalse();
    }

    @Test
    void isMultiHunt_twoHunts_true() {
        huntService.registerHunt(new HBHunt(configService, "h2", "T", HuntState.ACTIVE, 1, "D"));
        assertThat(huntService.isMultiHunt()).isTrue();
    }

    @Test
    void getHuntNames_returnsAllIds() {
        huntService.registerHunt(new HBHunt(configService, "alpha", "A", HuntState.ACTIVE, 1, "D"));
        huntService.registerHunt(new HBHunt(configService, "beta", "B", HuntState.INACTIVE, 2, "D"));

        assertThat(huntService.getHuntNames()).contains("alpha", "beta", "default");
    }

    // --- Head-to-hunts cache ---

    @Test
    void rebuildHeadToHuntsCache_mapsHeadsToHunts() {
        UUID head1 = UUID.randomUUID();
        HBHunt hunt = new HBHunt(configService, "h1", "Test", HuntState.ACTIVE, 1, "D");
        hunt.addHead(head1);

        huntService.registerHunt(hunt); // registerHunt calls rebuildHeadToHuntsCache

        assertThat(huntService.getHuntsForHead(head1)).contains(hunt);
    }

    @Test
    void rebuildHeadToHuntsCache_sortsByPriorityDesc() {
        UUID head = UUID.randomUUID();
        HBHunt low = new HBHunt(configService, "low", "Low", HuntState.ACTIVE, 1, "D");
        HBHunt high = new HBHunt(configService, "high", "High", HuntState.ACTIVE, 10, "D");
        HBHunt mid = new HBHunt(configService, "mid", "Mid", HuntState.ACTIVE, 5, "D");

        low.addHead(head);
        high.addHead(head);
        mid.addHead(head);

        huntService.registerHunt(low);
        huntService.registerHunt(high);
        huntService.registerHunt(mid);

        List<HBHunt> hunts = huntService.getHuntsForHead(head);
        assertThat(hunts).extracting(HBHunt::getId).containsSubsequence("high", "mid", "low");
    }

    @Test
    void getHuntsForHead_unknownHead_returnsEmptyList() {
        assertThat(huntService.getHuntsForHead(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getHighestPriorityHuntForHead_returnsFirstActive() {
        UUID head = UUID.randomUUID();
        HBHunt inactive = new HBHunt(configService, "inactive", "I", HuntState.INACTIVE, 10, "D");
        HBHunt active = new HBHunt(configService, "active", "A", HuntState.ACTIVE, 5, "D");

        inactive.addHead(head);
        active.addHead(head);

        huntService.registerHunt(inactive);
        huntService.registerHunt(active);

        assertThat(huntService.getHighestPriorityHuntForHead(head).getId()).isEqualTo("active");
    }

    @Test
    void getHighestPriorityHuntForHead_allInactive_returnsNull() {
        UUID head = UUID.randomUUID();
        HBHunt inactive = new HBHunt(configService, "i1", "I", HuntState.INACTIVE, 10, "D");
        inactive.addHead(head);

        huntService.registerHunt(inactive);

        assertThat(huntService.getHighestPriorityHuntForHead(head)).isNull();
    }

    @Test
    void getHighestPriorityHuntForHead_unknownHead_returnsNull() {
        assertThat(huntService.getHighestPriorityHuntForHead(UUID.randomUUID())).isNull();
    }

    // --- Selected hunt session ---

    @Test
    void selectedHunt_defaultIsDefault() {
        UUID player = UUID.randomUUID();
        assertThat(huntService.getSelectedHunt(player)).isEqualTo("default");
    }

    @Test
    void setSelectedHunt_returnsSetValue() {
        UUID player = UUID.randomUUID();
        huntService.setSelectedHunt(player, "custom");
        assertThat(huntService.getSelectedHunt(player)).isEqualTo("custom");
    }

    @Test
    void clearSelectedHunt_revertsToDefault() {
        UUID player = UUID.randomUUID();
        huntService.setSelectedHunt(player, "custom");
        huntService.clearSelectedHunt(player);
        assertThat(huntService.getSelectedHunt(player)).isEqualTo("default");
    }

    // --- Transfer head ---

    @Test
    void transferHead_movesHeadToTargetHunt() throws Exception {
        UUID head = UUID.randomUUID();
        HBHunt source = new HBHunt(configService, "source", "Source", HuntState.ACTIVE, 1, "D");
        HBHunt target = new HBHunt(configService, "target", "Target", HuntState.ACTIVE, 2, "D");
        source.addHead(head);

        huntService.registerHunt(source);
        huntService.registerHunt(target);

        huntService.transferHead(head, "target");

        assertThat(source.containsHead(head)).isFalse();
        assertThat(target.containsHead(head)).isTrue();

        verify(storageService).unlinkHeadFromHunt(head, "source");
        verify(storageService).linkHeadToHunt(head, "target");
        verify(storageService).incrementHuntVersion();
    }

    @Test
    void transferHead_targetNotFound_throwsIllegalArgument() {
        huntService.registerHunt(new HBHunt(configService, "source", "S", HuntState.ACTIVE, 1, "D"));

        assertThatThrownBy(() -> huntService.transferHead(UUID.randomUUID(), "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void transferHead_headInMultipleHunts_removedFromAll() throws Exception {
        UUID head = UUID.randomUUID();
        HBHunt h1 = new HBHunt(configService, "h1", "H1", HuntState.ACTIVE, 1, "D");
        HBHunt h2 = new HBHunt(configService, "h2", "H2", HuntState.ACTIVE, 2, "D");
        HBHunt target = new HBHunt(configService, "target", "T", HuntState.ACTIVE, 3, "D");

        h1.addHead(head);
        h2.addHead(head);

        huntService.registerHunt(h1);
        huntService.registerHunt(h2);
        huntService.registerHunt(target);

        huntService.transferHead(head, "target");

        assertThat(h1.containsHead(head)).isFalse();
        assertThat(h2.containsHead(head)).isFalse();
        assertThat(target.containsHead(head)).isTrue();

        verify(storageService).unlinkHeadFromHunt(head, "h1");
        verify(storageService).unlinkHeadFromHunt(head, "h2");
    }

    // --- Assign head to hunt ---

    @Test
    void assignHeadToHunt_addsHeadAndRebuildsCache() throws Exception {
        UUID head = UUID.randomUUID();
        HBHunt hunt = new HBHunt(configService, "h1", "Test", HuntState.ACTIVE, 1, "D");
        huntService.registerHunt(hunt);

        huntService.assignHeadToHunt(head, "h1");

        assertThat(hunt.containsHead(head)).isTrue();
        assertThat(huntService.getHuntsForHead(head)).contains(hunt);
        verify(storageService).linkHeadToHunt(head, "h1");
    }

    @Test
    void assignHeadToHunt_huntNotFound_throwsIllegalArgument() {
        assertThatThrownBy(() -> huntService.assignHeadToHunt(UUID.randomUUID(), "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }
}
