package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SpawnDraftTest {

    private final ServiceRegistry registry = mock(ServiceRegistry.class);

    @Test
    void newDraft_withoutTemplate_isInvalid() {
        assertThat(new SpawnDraft().isValid()).isFalse();
    }

    @Test
    void addTemplate_generatesUniqueIds() {
        var draft = new SpawnDraft();

        draft.addTemplate(HeadContent.head("a"));
        draft.addTemplate(HeadContent.head("b"));
        draft.templates.remove("template1");
        draft.addTemplate(HeadContent.head("c"));

        assertThat(draft.templates.keySet()).containsExactlyInAnyOrder("template2", "template3");
        assertThat(draft.isValid()).isTrue();
    }

    @Test
    void setWeight_neverGoesBelowZero_andZeroWeightIsInvalid() {
        var draft = new SpawnDraft();
        draft.addTemplate(HeadContent.head("a"));

        draft.setWeight("template1", -3);

        assertThat(draft.templates.get("template1").weight()).isZero();
        assertThat(draft.isValid()).isFalse();
    }

    @Test
    void buildThenOf_keepsEverything() {
        var draft = new SpawnDraft();
        draft.points = List.of(new SpawnPoint("world", 1, 2, 3, 90f));
        draft.active = 4;
        draft.goal = 8;
        draft.maxTotalSpawns = 30;
        draft.completion = SpawnCompletion.FIRST_WINS;
        draft.afterGoal = AfterGoal.CONTINUE;
        draft.onFind = false;
        draft.minDelay = 5;
        draft.maxDelay = 9;
        draft.interval = true;
        draft.intervalSeconds = 120;
        draft.resetProgress = true;
        draft.onStart = false;
        draft.addTemplate(HeadContent.head("a"));

        var copy = SpawnDraft.of(draft.build(registry));

        assertThat(copy).usingRecursiveComparison().isEqualTo(draft);
    }
}
