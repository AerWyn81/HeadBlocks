package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.data.reward.RewardType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpawnDataTest {

    @Test
    void enums_fallBackToTheirDefault() {
        assertThat(SpawnCompletion.of(null)).isEqualTo(SpawnCompletion.PER_PLAYER);
        assertThat(SpawnCompletion.of("nope")).isEqualTo(SpawnCompletion.PER_PLAYER);
        assertThat(SpawnCompletion.of("first_wins")).isEqualTo(SpawnCompletion.FIRST_WINS);
        assertThat(AfterGoal.of(null)).isEqualTo(AfterGoal.DENY);
        assertThat(AfterGoal.of("nope")).isEqualTo(AfterGoal.DENY);
        assertThat(AfterGoal.of("continue")).isEqualTo(AfterGoal.CONTINUE);
    }

    @Test
    void spawnPoint_of_usesTheBlockCoordinates() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        var point = SpawnPoint.of(new Location(world, 10.7, 64.2, -3.4), 45f);

        assertThat(point).isEqualTo(new SpawnPoint("world", 10, 64, -4, 45f));
        assertThat(point.matches(new Location(world, 10.1, 64.9, -3.9))).isTrue();
        assertThat(point.matches(new Location(world, 11, 64, -4))).isFalse();
        assertThat(point.matches(null)).isFalse();
    }

    @Test
    void spawnPoint_deserialize_rejectsInvalidEntries() {
        assertThat(SpawnPoint.deserialize("nope")).isNull();
        assertThat(SpawnPoint.deserialize(Map.of("x", 1))).isNull();
        assertThat(SpawnPoint.deserialize(Map.of("world", "w", "x", "a", "y", 1, "z", 1))).isNull();
        assertThat(SpawnPoint.deserialize(Map.of("world", "w", "x", "1.9", "y", 64, "z", -0.5)))
                .isEqualTo(new SpawnPoint("w", 1, 64, -1, 0f));
    }

    @Test
    void spawnPoint_serialize_omitsANullYaw() {
        assertThat(new SpawnPoint("w", 1, 2, 3, 0f).serialize()).doesNotContainKey("yaw");
        assertThat(new SpawnPoint("w", 1, 2, 3, 90f).serialize()).containsEntry("yaw", 90.0);
    }

    @Test
    void spawnTemplate_withoutSection_isNull() {
        assertThat(SpawnTemplate.fromConfig("id", null)).isNull();
    }

    @Test
    void spawnTemplate_negativeWeight_isClampedToZero() {
        var yaml = new YamlConfiguration();
        yaml.set("weight", -5);
        HeadContent.head("tex").save(yaml, "content");

        assertThat(SpawnTemplate.fromConfig("id", yaml).weight()).isZero();
    }

    @Test
    void respawnPolicy_normalizesItsValues() {
        var policy = new RespawnPolicy(true, -3, -10, true, 0, false, true);

        assertThat(policy.minDelay()).isZero();
        assertThat(policy.maxDelay()).isZero();
        assertThat(policy.intervalSeconds()).isEqualTo(1);
        assertThat(policy.nextFindDelay()).isZero();
        assertThat(new RespawnPolicy(true, 2, 4, false, 60, false, true).nextFindDelay()).isBetween(2, 4);
        assertThat(RespawnPolicy.fromConfig(null)).isEqualTo(RespawnPolicy.DEFAULT);
    }

    @Test
    void draft_setRewards_replacesTheTemplateRewards() {
        var draft = new SpawnDraft();
        draft.addTemplate(HeadContent.head("a"));

        draft.setRewards("template1", List.of(new Reward(RewardType.COMMAND, "say hi")));
        draft.setRewards("unknown", List.of());
        draft.setWeight("unknown", 3);

        assertThat(draft.templates.get("template1").rewards()).containsExactly(new Reward(RewardType.COMMAND, "say hi"));
        assertThat(draft.templates).hasSize(1);
    }

    private static SpawnTemplate template() {
        return new SpawnTemplate("t", "", 1, HeadContent.head("a"), List.of(
                new Reward(RewardType.MESSAGE, "1"), new Reward(RewardType.MESSAGE, "2")));
    }

    @Test
    void template_defaults_giveEveryRewardAndNeverTrap() {
        var template = template();

        assertThat(template.drawRewards()).hasSize(2);
        assertThat(template.rollTrap()).isFalse();
        assertThat(template.points()).isEqualTo(1);
        assertThat(template.particle()).isNull();
    }

    @Test
    void template_randomReward_givesExactlyOne() {
        var template = template().withRewardDraw(true, 100);

        for (int i = 0; i < 20; i++) {
            assertThat(template.drawRewards()).hasSize(1);
        }
    }

    @Test
    void template_zeroRewardChance_givesNothing() {
        assertThat(template().withRewardDraw(false, 0).drawRewards()).isEmpty();
        assertThat(template().withRewardDraw(true, 0).drawRewards()).isEmpty();
    }

    @Test
    void template_noRewards_givesNothing() {
        assertThat(new SpawnTemplate("t", "", 1, HeadContent.head("a"), List.of()).withRewardDraw(true, 100).drawRewards()).isEmpty();
    }

    @Test
    void template_fullTrapChance_alwaysTraps() {
        assertThat(template().withTrap(100, List.of()).rollTrap()).isTrue();
    }

    @Test
    void template_valuesAreClamped() {
        var template = template().withRewardDraw(false, 150).withTrap(-5, List.of()).withPoints(-3).withWeight(-1);

        assertThat(template.rewardChance()).isEqualTo(100);
        assertThat(template.trapChance()).isZero();
        assertThat(template.points()).isZero();
        assertThat(template.weight()).isZero();
    }

    @Test
    void template_saveAndLoad_keepsEverySetting() {
        var original = template().withRewardDraw(true, 40).withPoints(2.5).withTrap(12.5, List.of("say trap"))
                .withParticle(new SpawnParticle("DUST", 5, List.of("255,0,0")));
        var yaml = new YamlConfiguration();

        original.saveTo(yaml);
        var loaded = SpawnTemplate.fromConfig("t", yaml);

        assertThat(loaded).isEqualTo(original);
    }

    @Test
    void particle_withoutName_isIgnored() {
        var yaml = new YamlConfiguration();
        yaml.set("amount", 3);

        assertThat(SpawnParticle.fromConfig(yaml)).isNull();
        assertThat(SpawnParticle.fromConfig(null)).isNull();
        assertThat(new SpawnParticle("X", 0, List.of()).amount()).isEqualTo(1);
    }

    @Test
    void options_saveAndLoad_andFallBack() {
        var original = new SpawnOptions(true, true, false, SpawnOptions.Scoring.POINTS);
        var yaml = new YamlConfiguration();

        original.saveTo(yaml);

        assertThat(SpawnOptions.fromConfig(yaml)).isEqualTo(original);
        assertThat(SpawnOptions.fromConfig(null)).isEqualTo(SpawnOptions.DEFAULT);
        assertThat(SpawnOptions.Scoring.of("nope")).isEqualTo(SpawnOptions.Scoring.HEADS);
        assertThat(SpawnOptions.Scoring.of(null)).isEqualTo(SpawnOptions.Scoring.HEADS);
    }

    @Test
    void draft_keepsTheNewTemplateSettingsWhenChangingWeightOrRewards() {
        var draft = new SpawnDraft();
        draft.addTemplate(HeadContent.head("a"));
        draft.update("template1", t -> t.withTrap(50, List.of("x")).withPoints(3));

        draft.setWeight("template1", 4);
        draft.setRewards("template1", List.of(new Reward(RewardType.COMMAND, "c")));

        var template = draft.templates.get("template1");
        assertThat(template.trapChance()).isEqualTo(50);
        assertThat(template.points()).isEqualTo(3);
        assertThat(template.weight()).isEqualTo(4);
    }
}
