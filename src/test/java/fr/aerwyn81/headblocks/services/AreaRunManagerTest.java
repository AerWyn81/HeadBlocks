package fr.aerwyn81.headblocks.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AreaRunManagerTest {

    @BeforeEach
    void setUp() {
        AreaRunManager.clearAll();
    }

    @AfterEach
    void tearDown() {
        AreaRunManager.clearAll();
    }

    @Test
    void clearAllForHunt_removesEngagedAndReleasedForThatHunt() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        AreaRunManager.engage(p1, "hunt-1");
        AreaRunManager.markReleased(p2, "hunt-1");

        AreaRunManager.clearAllForHunt("hunt-1");

        assertThat(AreaRunManager.isEngaged(p1)).isFalse();
        assertThat(AreaRunManager.isReleased(p2, "hunt-1")).isFalse();
    }

    @Test
    void clearAllForHunt_leavesOtherHuntsUntouched() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        AreaRunManager.engage(p1, "hunt-1");
        AreaRunManager.engage(p2, "hunt-2");
        AreaRunManager.markReleased(p2, "hunt-2");

        AreaRunManager.clearAllForHunt("hunt-1");

        assertThat(AreaRunManager.isEngaged(p1)).isFalse();
        assertThat(AreaRunManager.getEngaged(p2)).isEqualTo("hunt-2");
        assertThat(AreaRunManager.isReleased(p2, "hunt-2")).isTrue();
    }

    @Test
    void clearAllForHunt_unknownHunt_noOp() {
        UUID player = UUID.randomUUID();
        AreaRunManager.engage(player, "hunt-1");

        AreaRunManager.clearAllForHunt("hunt-X");

        assertThat(AreaRunManager.getEngaged(player)).isEqualTo("hunt-1");
    }
}