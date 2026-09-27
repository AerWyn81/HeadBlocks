package fr.aerwyn81.headblocks;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehaviors;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import fr.aerwyn81.headblocks.services.ConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HeadBlocksMetricsTest {

    private final ConfigService configService = mock(ConfigService.class);

    @Test
    void spawnFeatures_reportsWhatTheServerUses() {
        var trap = new SpawnTemplate("trap", "", 1, HeadContent.head("t"), List.of()).withTrap(50, List.of("say hi"));
        var spawn = SpawnBehaviors.points(null, List.of(), 1, 1, -1, SpawnCompletion.FIRST_WINS, AfterGoal.DENY,
                RespawnPolicy.DEFAULT, new SpawnOptions(true, false, false, SpawnOptions.Scoring.POINTS), List.of(trap));
        var hunt = new HBHunt(configService, "spawn", "Spawn", HuntState.ACTIVE, 1, "D");
        hunt.setBehaviors(List.of(new FreeBehavior(), spawn));
        var plain = new HBHunt(configService, "plain", "Plain", HuntState.ACTIVE, 1, "D");

        var features = HeadBlocks.spawnFeatures(List.of(hunt, plain));

        assertThat(features.get("Placement points")).containsExactly(1, 0);
        assertThat(features.get("Placement area")).containsExactly(0, 1);
        assertThat(features.get("First wins")).containsExactly(1, 0);
        assertThat(features.get("Score in points")).containsExactly(1, 0);
        assertThat(features.get("Announce")).containsExactly(1, 0);
        assertThat(features.get("Log file")).containsExactly(0, 1);
        assertThat(features.get("Traps")).containsExactly(1, 0);
        assertThat(features.get("Random reward")).containsExactly(0, 1);
        assertThat(features.get("Template particles")).containsExactly(0, 1);
        assertThat(features).hasSize(11);
    }

    @Test
    void spawnFeatures_withoutSpawnHunt_reportsNothingUsed() {
        var features = HeadBlocks.spawnFeatures(List.of(new HBHunt(configService, "plain", "Plain", HuntState.ACTIVE, 1, "D")));

        assertThat(features.values()).allSatisfy(value -> assertThat(value).containsExactly(0, 1));
    }
}
