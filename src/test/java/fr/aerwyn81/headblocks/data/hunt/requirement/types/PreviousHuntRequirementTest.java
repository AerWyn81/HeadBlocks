package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.services.HuntService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviousHuntRequirementTest {

    private static final String TARGET_ID = "tutorial";

    @Mock
    ServiceRegistry registry;

    @Mock
    HuntService huntService;

    @Mock
    StorageService storageService;

    @Mock
    LanguageService languageService;

    @Mock
    Player player;

    @Mock
    HeadLocation head;

    @Mock
    HBHunt hunt;

    @Mock
    HBHunt target;

    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(player.getUniqueId()).thenReturn(playerUuid);
        lenient().when(hunt.getId()).thenReturn("advanced");
        lenient().when(target.getDisplayName()).thenReturn("Tutorial");
    }

    private void foundHeads(int count) throws InternalException {
        ArrayList<UUID> found = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            found.add(UUID.randomUUID());
        }
        when(storageService.getHeadsPlayerForHunt(playerUuid, TARGET_ID)).thenReturn(found);
    }

    @Test
    void getType_isPreviousHunt() {
        assertThat(new PreviousHuntRequirement(registry, TARGET_ID, 0).getType())
                .isEqualTo(RequirementType.PREVIOUS_HUNT);
    }

    @Test
    void check_allHeads_huntCompleted_isSatisfied() throws InternalException {
        when(huntService.getHuntById(TARGET_ID)).thenReturn(target);
        when(target.getHeadCount()).thenReturn(5);
        foundHeads(5);

        var requirement = new PreviousHuntRequirement(registry, TARGET_ID, PreviousHuntRequirement.ALL_HEADS);

        assertThat(requirement.check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_allHeads_huntIncomplete_isUnmet() throws InternalException {
        when(huntService.getHuntById(TARGET_ID)).thenReturn(target);
        when(target.getHeadCount()).thenReturn(5);
        foundHeads(3);
        when(languageService.message("Hunt.Requirement.HuntUnmetAll"))
                .thenReturn("finish %hunt% (%found%/%required%)");

        var result = new PreviousHuntRequirement(registry, TARGET_ID, PreviousHuntRequirement.ALL_HEADS)
                .check(player, head, hunt);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.reason()).isEqualTo("finish Tutorial (3/5)");
    }

    @Test
    void check_countThreshold_usesTheConfiguredNumber() throws InternalException {
        when(huntService.getHuntById(TARGET_ID)).thenReturn(target);
        foundHeads(2);
        when(languageService.message("Hunt.Requirement.HuntUnmetCount"))
                .thenReturn("%found%/%required% in %hunt%");

        var result = new PreviousHuntRequirement(registry, TARGET_ID, 3).check(player, head, hunt);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.reason()).isEqualTo("2/3 in Tutorial");
    }

    @Test
    void check_countThresholdReached_isSatisfied() throws InternalException {
        when(huntService.getHuntById(TARGET_ID)).thenReturn(target);
        foundHeads(3);

        assertThat(new PreviousHuntRequirement(registry, TARGET_ID, 3).check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_targetHuntDeleted_failsOpen() {
        when(huntService.getHuntById(TARGET_ID)).thenReturn(null);

        assertThat(new PreviousHuntRequirement(registry, TARGET_ID, 3).check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_targetHuntWithoutHeads_failsOpen() {
        when(huntService.getHuntById(TARGET_ID)).thenReturn(target);
        when(target.getHeadCount()).thenReturn(0);

        var requirement = new PreviousHuntRequirement(registry, TARGET_ID, PreviousHuntRequirement.ALL_HEADS);

        assertThat(requirement.check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void check_storageError_failsOpen() throws InternalException {
        when(huntService.getHuntById(TARGET_ID)).thenReturn(target);
        when(storageService.getHeadsPlayerForHunt(playerUuid, TARGET_ID))
                .thenThrow(new InternalException(new RuntimeException("down")));

        assertThat(new PreviousHuntRequirement(registry, TARGET_ID, 1).check(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void isComplete_requiresAHuntId() {
        assertThat(new PreviousHuntRequirement(registry, TARGET_ID, 0).isComplete()).isTrue();
        assertThat(new PreviousHuntRequirement(registry, "", 0).isComplete()).isFalse();
        assertThat(new PreviousHuntRequirement(registry, null, 0).isComplete()).isFalse();
    }

    @Test
    void saveTo_thenFromConfig_roundTripsHuntAndThreshold() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirement");
        new PreviousHuntRequirement(registry, TARGET_ID, 4).saveTo(section);

        PreviousHuntRequirement loaded = PreviousHuntRequirement.fromConfig(registry, section);

        assertThat(loaded.huntId()).isEqualTo(TARGET_ID);
        assertThat(loaded.requiredHeads()).isEqualTo(4);
    }
}
