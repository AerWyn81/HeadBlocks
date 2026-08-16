package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.PermissionRequirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.PlaytimeRequirement;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementSetTest {

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

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message("Hunt.Requirement.DeniedAll")).thenReturn("Missing:");
        lenient().when(languageService.message("Hunt.Requirement.DeniedAny")).thenReturn("One of:");
        lenient().when(languageService.message("Hunt.Requirement.DeniedLine")).thenReturn("- %reason%");
    }

    private Requirement stub(boolean satisfied, String reason) {
        Requirement requirement = mock(Requirement.class);
        lenient().when(requirement.check(any(), any(), any()))
                .thenReturn(satisfied ? RequirementResult.ok() : RequirementResult.unmet(reason));
        return requirement;
    }

    @Test
    void evaluate_empty_isSatisfied() {
        RequirementSet set = RequirementSet.empty();

        assertThat(set.evaluate(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void evaluate_all_everyRequirementMet_isSatisfied() {
        RequirementSet set = new RequirementSet(registry, RequirementMode.ALL,
                List.of(stub(true, null), stub(true, null)));

        assertThat(set.evaluate(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void evaluate_all_listsEveryBlockingReason() {
        RequirementSet set = new RequirementSet(registry, RequirementMode.ALL,
                List.of(stub(false, "be in the area"), stub(true, null), stub(false, "play longer")));

        RequirementResult result = set.evaluate(player, head, hunt);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.reason()).isEqualTo("Missing:\n- be in the area\n- play longer");
    }

    @Test
    void evaluate_any_oneRequirementMet_isSatisfied() {
        Requirement second = stub(true, null);
        RequirementSet set = new RequirementSet(registry, RequirementMode.ANY,
                List.of(stub(false, "nope"), second));

        assertThat(set.evaluate(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void evaluate_any_shortCircuitsOnFirstMatch() {
        Requirement first = stub(true, null);
        Requirement second = stub(false, "never checked");
        RequirementSet set = new RequirementSet(registry, RequirementMode.ANY, List.of(first, second));

        set.evaluate(player, head, hunt);

        verify(second, never()).check(any(), any(), any());
    }

    @Test
    void evaluate_any_noneMet_usesTheAnyHeader() {
        RequirementSet set = new RequirementSet(registry, RequirementMode.ANY,
                List.of(stub(false, "first"), stub(false, "second")));

        RequirementResult result = set.evaluate(player, head, hunt);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.reason()).isEqualTo("One of:\n- first\n- second");
    }

    private Requirement unresolvable() {
        Requirement requirement = mock(Requirement.class);
        lenient().when(requirement.check(any(), any(), any())).thenReturn(RequirementResult.unresolvable());
        return requirement;
    }

    @Test
    void evaluate_any_unresolvableRequirement_doesNotUnlockTheHunt() {
        RequirementSet set = new RequirementSet(registry, RequirementMode.ANY,
                List.of(unresolvable(), stub(false, "hold the permission")));

        RequirementResult result = set.evaluate(player, head, hunt);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.reason()).isEqualTo("One of:\n- hold the permission");
    }

    @Test
    void evaluate_all_unresolvableRequirement_doesNotBlockTheHunt() {
        RequirementSet set = new RequirementSet(registry, RequirementMode.ALL,
                List.of(unresolvable(), stub(true, null)));

        assertThat(set.evaluate(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void evaluate_everyRequirementUnresolvable_letsTheClickThrough() {
        RequirementSet set = new RequirementSet(registry, RequirementMode.ANY,
                List.of(unresolvable(), unresolvable()));

        assertThat(set.evaluate(player, head, hunt).satisfied()).isTrue();
    }

    @Test
    void evaluate_blankReasons_areSkipped() {
        RequirementSet set = new RequirementSet(registry, RequirementMode.ALL,
                List.of(stub(false, " "), stub(false, "shown")));

        assertThat(set.evaluate(player, head, hunt).reason()).isEqualTo("Missing:\n- shown");
    }

    @Test
    void find_returnsTheFirstMatchingRequirement() {
        PermissionRequirement permission = new PermissionRequirement(registry, "hb.vip");
        RequirementSet set = new RequirementSet(registry, RequirementMode.ALL,
                List.of(new PlaytimeRequirement(registry, 10), permission));

        assertThat(set.find(PermissionRequirement.class)).contains(permission);
        assertThat(set.find(fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement.class)).isEmpty();
    }

    @Test
    void without_removesTheRequirementAndKeepsTheMode() {
        PermissionRequirement permission = new PermissionRequirement(registry, "hb.vip");
        RequirementSet set = new RequirementSet(registry, RequirementMode.ANY,
                List.of(permission, new PlaytimeRequirement(registry, 10)));

        RequirementSet reduced = set.without(permission);

        assertThat(reduced.size()).isEqualTo(1);
        assertThat(reduced.getMode()).isEqualTo(RequirementMode.ANY);
        assertThat(set.size()).isEqualTo(2);
    }

    @Test
    void saveTo_thenFromSection_roundTripsModeAndOrder() {
        RequirementSet set = new RequirementSet(registry, RequirementMode.ANY, List.of(
                new PermissionRequirement(registry, "hb.vip"),
                new PlaytimeRequirement(registry, 120)));

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirements");
        set.saveTo(section);

        RequirementSet loaded = RequirementSet.fromSection(registry, yaml.getConfigurationSection("requirements"));

        assertThat(loaded.getMode()).isEqualTo(RequirementMode.ANY);
        assertThat(loaded.getRequirements()).hasSize(2);
        assertThat(loaded.getRequirements().get(0)).isInstanceOf(PermissionRequirement.class);
        assertThat(((PermissionRequirement) loaded.getRequirements().get(0)).node()).isEqualTo("hb.vip");
        assertThat(loaded.getRequirements().get(1)).isInstanceOf(PlaytimeRequirement.class);
        assertThat(((PlaytimeRequirement) loaded.getRequirements().get(1)).minutes()).isEqualTo(120);
    }

    @Test
    void fromSection_nullSection_returnsEmptySet() {
        assertThat(RequirementSet.fromSection(registry, null).isEmpty()).isTrue();
    }

    @Test
    void fromSection_unknownType_isSkipped() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirements");
        section.set("mode", "ALL");
        section.createSection("list.0").set("type", "does-not-exist");
        section.createSection("list.1").set("type", "permission");
        section.set("list.1.permission", "hb.vip");

        RequirementSet loaded = RequirementSet.fromSection(registry, section);

        assertThat(loaded.getRequirements()).hasSize(1);
        assertThat(loaded.getRequirements().get(0)).isInstanceOf(PermissionRequirement.class);
    }

    @Test
    void fromSection_incompleteRequirement_isSkipped() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirements");
        section.createSection("list.0").set("type", "permission");

        assertThat(RequirementSet.fromSection(registry, section).isEmpty()).isTrue();
    }

    @Test
    void skippedEntries_areWrittenBackUntouched() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirements");
        section.set("mode", "ALL");
        section.createSection("list.0").set("type", "does-not-exist");
        section.set("list.0.nested.value", 7);
        section.createSection("list.1").set("type", "permission");
        section.set("list.1.permission", "hb.vip");

        RequirementSet loaded = RequirementSet.fromSection(registry, section);
        assertThat(loaded.getPreserved()).hasSize(1);

        YamlConfiguration rewritten = new YamlConfiguration();
        loaded.saveTo(rewritten.createSection("requirements"));

        // The understood entry comes first, the untouched one right after it.
        assertThat(rewritten.getString("requirements.list.0.type")).isEqualTo("permission");
        assertThat(rewritten.getString("requirements.list.1.type")).isEqualTo("does-not-exist");
        assertThat(rewritten.getInt("requirements.list.1.nested.value")).isEqualTo(7);
    }

    @Test
    void fromSection_readsEntriesInNumericOrder() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("requirements");
        section.createSection("list.10").set("type", "permission");
        section.set("list.10.permission", "last");
        section.createSection("list.2").set("type", "permission");
        section.set("list.2.permission", "first");

        RequirementSet loaded = RequirementSet.fromSection(registry, section);

        assertThat(loaded.getRequirements()).extracting(r -> ((PermissionRequirement) r).node())
                .containsExactly("first", "last");
    }
}
